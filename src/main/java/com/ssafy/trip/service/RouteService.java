package com.ssafy.trip.service;

import com.ssafy.trip.domain.*;
import com.ssafy.trip.dto.*;
import com.ssafy.trip.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RouteService {

    private final RouteRepository routeRepository;
    private final MemberRepository memberRepository;
    private final PlanRepository planRepository;
    private final PlanService planService;
    private final RouteLikeRepository routeLikeRepository;
    private final RoutePlanRepository routePlanRepository;
    private final CommentRepository commentRepository;

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

        return routeRepository.save(route);
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

        // ★ 본인 것만 조회 가능
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

        // JPA 영속 상태라 save() 안 해도 flush 되지만, 명시적으로
        routeRepository.save(route);

        return RouteResponse.from(route);
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

        // RoutePlan에 orphanRemoval = true 걸려 있으면 Route 삭제 시 자동 제거
        routeRepository.delete(route);
    }


    @Transactional(readOnly = true)
    public Page<RouteSummaryResponse> getPublicRoutes(String sort, int page, int size, String email) {

        Sort sortSpec;
        if ("popular".equalsIgnoreCase(sort)) {
            // 좋아요 순
            sortSpec = Sort.by(Sort.Direction.DESC, "likeCount");
        } else {
            // 기본: 최신순 (id DESC 또는 createdAt DESC, 둘 중 하나 선택)
            sortSpec = Sort.by(Sort.Direction.DESC, "id");
        }

        Pageable pageable = PageRequest.of(page, size, sortSpec);

        Page<Route> routes = routeRepository.findByIsPublicTrue(pageable);

        Optional<Member> memberOpt = memberRepository.findByEmail(email);
        boolean isGuest;
        if (memberOpt.isEmpty()) isGuest = true;
        else {
            isGuest = false;
        }
        Member member = memberOpt.orElse(null);

        AtomicBoolean liked = new AtomicBoolean(false);

        return routes.map(route -> {
            routeLikeRepository.findByRouteAndMember(route, member).ifPresent(like -> liked.set(true));
            return RouteSummaryResponse.from(route, liked.get(), isGuest);
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

        // JPA 영속 상태라 save() 안 해도 flush 되지만, 명시적으로
        return routeRepository.save(route);
    }

    // 좋아요 (한 명이 여러 번 눌러도 그냥 +1/-1 관리)
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

        return RouteLikeResponse.builder()
                .routeId(routeId)
                .liked(liked.get())
                .likeCount(route.getLikeCount())
                .build();
    }

    @Transactional
    public RouteDetailResponse getPublicRouteDetail(String email, Long routeId) {
        Route route = routeRepository.findById(routeId)
                .orElseThrow(() -> new IllegalArgumentException("Route를 찾을 수 없습니다."));

        Optional<Member> memberOpt = memberRepository.findByEmail(email);
        boolean isGuset = false;
        if (memberOpt.isEmpty()) isGuset = true;

        Member member = memberOpt.orElse(null);
        AtomicBoolean liked = new AtomicBoolean(false);
        routeLikeRepository.findByRouteAndMember(route, member).ifPresent(like -> liked.set(true));

        List<RoutePlan> routePlans = routePlanRepository.findAllByRouteIdWithPlan(routeId);
        Map<Integer, List<RoutePlan>> groupedByDay = routePlans.stream()
                .collect(Collectors.groupingBy(
                        RoutePlan::getDayIndex,
                        TreeMap::new,
                        Collectors.toList()
                ));

        List<DaysDto> daysDtoList = groupedByDay.entrySet().stream()
                .map(entry -> {
                    int dayIndex = entry.getKey();
                    List<RoutePlan> plansInDay = entry.getValue();

                    // 해당 일차 내에서 orderIndex 순으로 정렬하여 PlansDto 생성
                    List<PlansDto> plansDtoList = plansInDay.stream()
                            .sorted(Comparator.comparingInt(RoutePlan::getOrderIndex))
                            .map(rp -> {
                                Plan plan = rp.getPlan();
                                return PlansDto.from(plan, rp.getOrderIndex());
                            })
                            .collect(Collectors.toList());

                    return DaysDto.from(dayIndex, plansDtoList);
                })
                .toList();

        return RouteDetailResponse.from(route, liked.get(), isGuset, daysDtoList);
    }


    @Transactional
    public List<CommentResponse> getComments(Long routeId) {
        return commentRepository.findAllByRouteId(routeId).stream()
                .map(comment -> CommentResponse.from(comment, comment.getMember()))
                .toList();
    }

    @Transactional
    public Comment postComment(String email, CommentRequest request, Long routeId) {
        Member member = memberRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("회원 정보를 찾을 수 없습니다."));

        Route route = routeRepository.findById(routeId)
                .orElseThrow(() -> new IllegalArgumentException("루트 정보를 찾을 수 없습니다."));

        Comment comment = Comment.builder()
                .content(request.getContent())
                .member(member)
                .route(route)
                .build();

        return commentRepository.save(comment);
    }
}
