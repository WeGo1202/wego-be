// com.ssafy.trip.controller.RouteController
package com.ssafy.trip.controller;

import com.ssafy.trip.domain.Route;
import com.ssafy.trip.dto.PlanRequest;
import com.ssafy.trip.dto.RouteCreateRequest;
//import com.ssafy.trip.service.RouteAiService;
import com.ssafy.trip.service.RouteService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/routes")
@RequiredArgsConstructor
public class RouteController {

    private final RouteService routeService;

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


//    @GetMapping("/{routeId}/ai-summary")
//    public ResponseEntity<AiRouteResponse> getAiSummary(@PathVariable Long routeId,
//                                                        Authentication authentication) {
//        String email = authentication.getName();
//        AiRouteResponse response = routeAiService.explainRoute(routeId, email);
//        return ResponseEntity.ok(response);
//    }

}
