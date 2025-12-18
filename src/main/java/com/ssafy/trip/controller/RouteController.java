// com.ssafy.trip.controller.RouteController
package com.ssafy.trip.controller;

import com.ssafy.trip.domain.Route;
import com.ssafy.trip.dto.*;
import com.ssafy.trip.dto.ai.AiRouteRequest;
import com.ssafy.trip.service.RouteAiService;
import com.ssafy.trip.service.RouteService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/routes")
@RequiredArgsConstructor
public class RouteController {

    private final RouteService routeService;
    private final RouteAiService routeAiService;

    // Route 생성 (여러 Plan을 묶어서)
    @PostMapping
    public ResponseEntity<Route> createRoute(@RequestBody RouteCreateRequest request,
                                             Authentication authentication) {
        String email = authentication.getName();
        Route saved = routeService.createRoute(email, request);
        return ResponseEntity.ok(saved);
    }

    // 내 Route 목록 조회
    @GetMapping("/me")
    public ResponseEntity<List<Route>> getMyRoutes(Authentication authentication) {
        String email = authentication.getName();
        List<Route> routes = routeService.getMyRoutes(email);
        return ResponseEntity.ok(routes);
    }

    // Route 상세 조회
    @GetMapping("/{id}")
    public ResponseEntity<Route> getRouteDetail(@PathVariable Long id,
                                                Authentication authentication) {
        String email = authentication.getName();
        Route route = routeService.getRouteDetail(email, id);
        return ResponseEntity.ok(route);
    }

    @PutMapping("/{id}")
    public RouteResponse updateRoute(@PathVariable Long id,
                                     @RequestBody @Valid RouteUpdateRequest request,
                                     Authentication authentication) {

        String email = authentication.getName();
        return routeService.updateRoute(email, id, request);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteRoute(@PathVariable Long id,
                                            Authentication authentication) {
        String email = authentication.getName();
        routeService.deleteRoute(email, id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{routeId}/plans")
    public ResponseEntity<Route> addPlanToRoute(@PathVariable Long routeId,
                                                @RequestBody PlanRequest planRequest,
                                                Authentication authentication) {
        String email = authentication.getName();
        Route updated = routeService.addPlanToRoute(email, routeId, planRequest);
        return ResponseEntity.ok(updated);
    }

    // 🔹 공개 여부 수정
    @PatchMapping("/{id}/visibility")
    public ResponseEntity<Route> updateVisibility(@PathVariable Long id,
                                                  @RequestBody RouteVisibilityRequest request,
                                                  Authentication authentication) {
        String email = authentication.getName();
        Route updated = routeService.updateVisibility(email, id, request);
        return ResponseEntity.ok(updated);
    }

    // 🔹 좋아요 토글
    @PostMapping("/{id}/like")
    public ResponseEntity<RouteLikeResponse> toggleLike(@PathVariable Long id,
                                                        Authentication authentication) {

        String email = null;
        if (authentication != null) email = authentication.getName();
        RouteLikeResponse res = routeService.toggleLike(email, id);
        return ResponseEntity.ok(res);
    }

    /**
     * 🔹 AI 기반 루트 생성
     * - body: AiRouteRequest (query, preferredRegion, totalDaysHint)
     * - 반환: 생성된 Route (id 포함)
     */
    @PostMapping("/ai")
    public ResponseEntity<Route> createRouteByAi(
            @RequestBody AiRouteRequest request,
            Authentication authentication
    ) {
        String email = authentication.getName();
        Route route = routeAiService.createRouteByGpt(email, request);
        return ResponseEntity.ok(route);
    }

//    @GetMapping("/{routeId}/ai-summary")
//    public ResponseEntity<AiRouteResponse> getAiSummary(@PathVariable Long routeId,
//                                                        Authentication authentication) {
//        String email = authentication.getName();
//        AiRouteResponse response = routeAiService.explainRoute(routeId, email);
//        return ResponseEntity.ok(response);
//    }

}
