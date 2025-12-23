package com.ssafy.trip.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ssafy.trip.domain.*;
import com.ssafy.trip.dto.*;
import com.ssafy.trip.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.*;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

@Slf4j
@Service
@RequiredArgsConstructor
public class RouteService {



    private final RouteRepository routeRepository;
    private final MemberRepository memberRepository;
    private final PlanRepository planRepository;
    private final PlanService planService;
    private final RouteLikeRepository routeLikeRepository;
    private final RoutePlanRepository routePlanRepository;

    private final RedisTemplate<String, String> redisTemplate;
    private final ObjectMapper objectMapper;

    private static final String PUBLIC_ROUTE_CACHE_PREFIX = "route_public:";
    private static final Duration PUBLIC_ROUTE_CACHE_TTL = Duration.ofMinutes(5);

    @Transactional
    public Route createRoute(String loginEmail, RouteCreateRequest request) {
        // 1) 로그인된 회원 조회
        Member member = memberRepository.findByEmail(loginEmail)
                .orElseThrow(() -> new IllegalArgumentException("회원 정보를 찾을 수 없습니다."));

        // 2) Route 생성
        Route route = Route.builder()
                .title(request.getTitle())
                .description(request.getDescription())
                .totalDays(request.getTotalDays())
                .member(member)
                .isPublic(Boolean.TRUE.equals(request.getIsPublic()))
                .likeCount(0L)
                .build();

        // 3) Route에 포함될 Plan들 세팅
        if (request.getItems() != null) {
            for (RouteCreateRequest.RoutePlanItem item : request.getItems()) {
                Plan plan = planRepository.findById(item.getPlanId())
                        .orElseThrow(() -> new IllegalArgumentException("Plan을 찾을 수 없습니다. id=" + item.getPlanId()));

                // ★ 보안: 본인 Plan만 추가 가능하게 체크
                if (!plan.getMember().getId().equals(member.getId())) {
                    throw new IllegalArgumentException("해당 Plan은 현재 회원의 계획이 아닙니다. id=" + item.getPlanId());
                }

                RoutePlan routePlan = RoutePlan.builder()
                        .route(route)
                        .plan(plan)
                        .dayIndex(item.getDayIndex())
                        .orderIndex(item.getOrderIndex())
                        .build();

                route.getRoutePlans().add(routePlan);
            }
        }

        Route saved = routeRepository.save(route);

        // 공개 루트 생성 시 게시판 캐시 무효
        if (Boolean.TRUE.equals(saved.getIsPublic())) {
            clearPublicRouteCache();
        }

        return saved;
    }

    @Transactional(readOnly = true)
    public List<Route> getMyRoutes(String loginEmail) {
        Member member = memberRepository.findByEmail(loginEmail)
                .orElseThrow(() -> new IllegalArgumentException("회원 정보를 찾을 수 없습니다."));
        return routeRepository.findByMember(member);
    }

    @Transactional(readOnly = true)
    public Route getRouteDetail(String loginEmail, Long routeId) {
        Route route = routeRepository.findById(routeId)
                .orElseThrow(() -> new IllegalArgumentException("Route를 찾을 수 없습니다."));

        // 본인 것만 조회
        if (!route.getMember().getEmail().equals(loginEmail)) {
            throw new IllegalArgumentException("해당 Route에 접근할 수 없습니다.");
        }

        return route;
    }

    @Transactional
    public RouteResponse updateRoute(String email, Long routeId, RouteUpdateRequest request) {
        Route route = routeRepository.findById(routeId)
                .orElseThrow(() -> new IllegalArgumentException("Route를 찾을 수 없습니다."));

        if (!route.getMember().getEmail().equals(email)) {
            throw new IllegalArgumentException("해당 Route에 대해 수정 권한이 없습니다.");
        }

        route.updateRoute(request);
        Route saved = routeRepository.save(route);

        // 공개 루트의 수정되면 게시판 목록 캐시 무효
        if (Boolean.TRUE.equals(saved.getIsPublic())) {
            clearPublicRouteCache();
        }

        return RouteResponse.from(saved);
    }

    @Transactional
    public Route addPlanToRoute(String loginEmail, Long routeId, PlanRequest planRequest) {
        Member member = memberRepository.findByEmail(loginEmail)
                .orElseThrow(() -> new IllegalArgumentException("회원 정보를 찾을 수 없습니다."));

        Route route = routeRepository.findById(routeId)
                .orElseThrow(() -> new IllegalArgumentException("Route를 찾을 수 없습니다."));

        if (!route.getMember().getId().equals(member.getId())) {
            throw new IllegalArgumentException("해당 Route에 접근할 수 없습니다.");
        }

        // 기존 PlanService 재사용해서 Plan 생성
        Plan plan = planService.createPlan(loginEmail, planRequest);

        int nextOrder = route.getRoutePlans().stream()
                .map(RoutePlan::getOrderIndex)
                .filter(java.util.Objects::nonNull)
                .max(Integer::compareTo)
                .orElse(0) + 1;

        RoutePlan routePlan = RoutePlan.builder()
                .route(route)
                .plan(plan)
                .dayIndex(1)       // 일단 1일차로 고정, 나중에 UI에서 선택 가능하게 확장
                .orderIndex(nextOrder)
                .build();

        route.getRoutePlans().add(routePlan);

        return route;
    }

    @Transactional
    public void deleteRoute(String loginEmail, Long routeId) {
        Member member = memberRepository.findByEmail(loginEmail)
                .orElseThrow(() -> new IllegalArgumentException("회원 정보를 찾을 수 없습니다."));

        Route route = routeRepository.findById(routeId)
                .orElseThrow(() -> new IllegalArgumentException("Route를 찾을 수 없습니다."));

        if (!route.getMember().getId().equals(member.getId())) {
            throw new IllegalArgumentException("해당 Route에 대한 삭제 권한이 없습니다.");
        }

        boolean wasPublic = Boolean.TRUE.equals(route.getIsPublic());

        // RoutePlan에 orphanRemoval = true 걸려 있으면 Route 삭제 시 자동 제거
        routeRepository.delete(route);

        // 🔥 공개 루트 삭제 → 캐시 무효화
        if (wasPublic) {
            clearPublicRouteCache();
        }
    }

    /**
     * 공개 루트 게시판 (로그인 유무에 따라 처리)
     *  - 비로그인(guest) 요청: Redis 캐시 사용
     *  - 로그인 사용자: liked 플래그가 사용자마다 달라서 캐시 쓰지 않고 DB에서 직접 조회
     */
    @Transactional(readOnly = true)
    public Page<RouteSummaryResponse> getPublicRoutes(String sort, int page, int size, String email) {

        Sort sortSpec;
        if ("popular".equalsIgnoreCase(sort)) {
            // 좋아요 순
            sortSpec = Sort.by(Sort.Direction.DESC, "likeCount");
        } else {
            // 기본: 최신순 (id DESC)
            sortSpec = Sort.by(Sort.Direction.DESC, "id");
        }

        Pageable pageable = PageRequest.of(page, size, sortSpec);

        Optional<Member> memberOpt = (email == null) ? Optional.empty() : memberRepository.findByEmail(email);
        boolean isGuest = memberOpt.isEmpty();
        Member member = memberOpt.orElse(null);

        // 🔹 비로그인(guest) → 캐시 사용
        if (isGuest) {
            String cacheKey = buildPublicRouteCacheKey(sort, page, size);

            // 1) Redis에서 캐시 조회
            try {
                String cachedJson = redisTemplate.opsForValue().get(cacheKey);
                if (cachedJson != null) {
                    RouteSummaryPageCache cache =
                            objectMapper.readValue(cachedJson, RouteSummaryPageCache.class);

                    log.debug("[PUBLIC ROUTE] 캐시 HIT: key={}", cacheKey);

                    return new PageImpl<>(
                            cache.getContent(),
                            pageable,
                            cache.getTotalElements()
                    );
                }
            } catch (Exception e) {
                log.warn("[PUBLIC ROUTE] 캐시 조회 실패, DB 조회로 fallback. key={}", cacheKey, e);
            }

            // 2) DB에서 공개 루트 조회
            Page<Route> routes = routeRepository.findByIsPublicTrue(pageable);

            // guest이므로 liked=false, isGuest=true 고정
            Page<RouteSummaryResponse> result = routes.map(route ->
                    RouteSummaryResponse.from(route, false, true)
            );

            // 3) 캐시에 저장
            try {
                RouteSummaryPageCache cache = new RouteSummaryPageCache();
                cache.setContent(result.getContent());
                cache.setPage(result.getNumber());
                cache.setSize(result.getSize());
                cache.setTotalElements(result.getTotalElements());

                String json = objectMapper.writeValueAsString(cache);
                redisTemplate.opsForValue().set(cacheKey, json, PUBLIC_ROUTE_CACHE_TTL);

                log.debug("[PUBLIC ROUTE] 캐시 SET: key={}", cacheKey);
            } catch (Exception e) {
                log.warn("[PUBLIC ROUTE] 캐시 저장 실패: key={}", cacheKey, e);
            }

            return result;
        }

        // 🔹 로그인 사용자 → 사용자별 liked 플래그가 다르므로 캐시 없이 직접 조회
        Page<Route> routes = routeRepository.findByIsPublicTrue(pageable);

        return routes.map(route -> {
            boolean liked = routeLikeRepository.findByRouteAndMember(route, member).isPresent();
            return RouteSummaryResponse.from(route, liked, false);
        });
    }

    // 🔹 공개 여부 수정
    @Transactional
    public Route updateVisibility(String loginEmail, Long routeId, RouteVisibilityRequest request) {
        Member member = memberRepository.findByEmail(loginEmail)
                .orElseThrow(() -> new IllegalArgumentException("회원 정보를 찾을 수 없습니다."));

        Route route = routeRepository.findById(routeId)
                .orElseThrow(() -> new IllegalArgumentException("Route를 찾을 수 없습니다."));

        if (!route.getMember().getId().equals(member.getId())) {
            throw new IllegalArgumentException("해당 Route 수정 권한이 없습니다.");
        }

        if (request.getIsPublic() != null) {
            route.setIsPublic(request.getIsPublic());
        }

        Route saved = routeRepository.save(route);

        // 🔥 공개 여부 변경 시 캐시 무효화
        clearPublicRouteCache();

        return saved;
    }

    // 좋아요 토글
    @Transactional
    public RouteLikeResponse toggleLike(String email, Long routeId) {
        Route route = routeRepository.findById(routeId)
                .orElseThrow(() -> new IllegalArgumentException("Route를 찾을 수 없습니다."));

        Optional<Member> memberOpt = memberRepository.findByEmail(email);
        if (memberOpt.isEmpty())
            return RouteLikeResponse.builder()
                    .routeId(routeId)
                    .liked(false)
                    .likeCount(route.getLikeCount())
                    .build();

        Member member = memberOpt.orElse(null);

        AtomicBoolean liked = new AtomicBoolean(false);
        routeLikeRepository.findByRouteAndMember(route, member).ifPresentOrElse(
                like -> {
                    route.decreaseLike();
                    routeLikeRepository.delete(like);
                }, () -> {
                    route.increaseLike();
                    liked.set(true);
                    routeLikeRepository.save(
                            RouteLike.builder()
                                    .route(route)
                                    .member(member)
                                    .build());
                }
        );

        Route saved = routeRepository.save(route);

        // 🔥 좋아요 수 변경 → 인기순 정렬에 영향 → 캐시 무효화
        if (Boolean.TRUE.equals(saved.getIsPublic())) {
            clearPublicRouteCache();
        }

        return RouteLikeResponse.builder()
                .routeId(routeId)
                .liked(liked.get())
                .likeCount(saved.getLikeCount())
                .build();
    }

    @Transactional
    public RouteDetailResponse getPublicRouteDetail(String email, Long routeId) {
        Route route = routeRepository.findById(routeId)
                .orElseThrow(() -> new IllegalArgumentException("Route를 찾을 수 없습니다."));

        Optional<Member> memberOpt = (email == null) ? Optional.empty() : memberRepository.findByEmail(email);
        boolean isGuest = memberOpt.isEmpty();
        Member member = memberOpt.orElse(null);

        AtomicBoolean liked = new AtomicBoolean(false);
        if (!isGuest && member != null) {
            routeLikeRepository.findByRouteAndMember(route, member).ifPresent(like -> liked.set(true));
        }

        return RouteDetailResponse.from(route, liked.get(), isGuest, null);
    }

    // ==========================
    // 🔥 캐시 유틸
    // ==========================
    private String buildPublicRouteCacheKey(String sort, int page, int size) {
        String safeSort = (sort == null || sort.isBlank()) ? "latest" : sort.toLowerCase();
        return PUBLIC_ROUTE_CACHE_PREFIX + "sort=" + safeSort + ":page=" + page + ":size=" + size;
    }

    private void clearPublicRouteCache() {
        try {
            Set<String> keys = redisTemplate.keys(PUBLIC_ROUTE_CACHE_PREFIX + "*");
            if (keys != null && !keys.isEmpty()) {
                redisTemplate.delete(keys);
                log.debug("[PUBLIC ROUTE] 캐시 전체 삭제: {}개 key", keys.size());
            }
        } catch (Exception e) {
            log.warn("[PUBLIC ROUTE] 캐시 전체 삭제 실패", e);
        }
    }
}
