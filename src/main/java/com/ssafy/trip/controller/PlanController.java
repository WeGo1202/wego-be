// com.ssafy.trip.web.plan.PlanController
package com.ssafy.trip.controller;

import com.ssafy.trip.domain.Plan;
import com.ssafy.trip.dto.PlanRequest;
import com.ssafy.trip.service.PlanService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/plans")
@RequiredArgsConstructor
public class PlanController {

    private final PlanService planService;

    // 🔹 플랜 생성 (로그인 필요)
    @PostMapping
    public ResponseEntity<Plan> createPlan(
            @RequestBody PlanRequest request,
            Authentication authentication   // 🔥 여기로 JWT 인증 정보 들어옴
    ) {
        String email = authentication.getName();  // TokenProvider에서 email을 username으로 넣었다고 가정
        Plan saved = planService.createPlan(email, request);
        return ResponseEntity.ok(saved);
    }

    // 🔹 내가 만든 플랜 목록 조회
    @GetMapping("/me")
    public ResponseEntity<List<Plan>> getMyPlans(Authentication authentication) {
        String email = authentication.getName();
        List<Plan> plans = planService.getMyPlans(email);
        return ResponseEntity.ok(plans);
    }
}
