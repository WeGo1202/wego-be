package com.ssafy.trip.controller;

import com.ssafy.trip.dto.RoutingRequest;
import com.ssafy.trip.dto.RoutingResponse;
import com.ssafy.trip.service.RoutingService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/routing")
public class RoutingController {

    private final RoutingService routingService;

    @PostMapping
    public ResponseEntity<RoutingResponse> getRoute(@RequestBody RoutingRequest request) {
        RoutingResponse response = routingService.getRoute(request);
        return ResponseEntity.ok(response);
    }
}
