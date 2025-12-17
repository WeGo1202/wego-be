package com.ssafy.trip.service;

import com.ssafy.trip.domain.Member;
import com.ssafy.trip.domain.Plan;
import com.ssafy.trip.domain.Route;
import com.ssafy.trip.domain.RoutePlan;
import com.ssafy.trip.dto.*;
import com.ssafy.trip.repository.MemberRepository;
import com.ssafy.trip.repository.PlanRepository;
import com.ssafy.trip.repository.RouteRepository;
import lombok.Builder;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class RouteService {

    private final RouteRepository routeRepository;
    private final MemberRepository memberRepository;
    private final PlanRepository planRepository;
    private final PlanService planService;

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
    public Page<RouteSummaryResponse> getPublicRoutes(String sort, int page, int size) {

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

        // ❗ 여기서 네 static 메서드 재사용
        return routes.map(RouteSummaryResponse::from);
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
    public RouteLikeResponse toggleLike(String loginEmail, Long routeId) {
        Member member = memberRepository.findByEmail(loginEmail)
                .orElseThrow(() -> new IllegalArgumentException("회원 정보를 찾을 수 없습니다."));

        Route route = routeRepository.findById(routeId)
                .orElseThrow(() -> new IllegalArgumentException("Route를 찾을 수 없습니다."));

        route.setLikeCount(route.getLikeCount() + 1);
        Route saved = routeRepository.save(route);

        return RouteLikeResponse.builder()
                .routeId(saved.getId())
                .liked(true)
                .likeCount(saved.getLikeCount())
                .build();
    }

}
