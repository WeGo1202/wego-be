// com.ssafy.trip.service.PlanService
package com.ssafy.trip.service;

import com.ssafy.trip.domain.Address;
import com.ssafy.trip.domain.Member;
import com.ssafy.trip.domain.Plan;
import com.ssafy.trip.dto.PlanRequest;
import com.ssafy.trip.repository.MemberRepository;
import com.ssafy.trip.repository.PlanRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class PlanService {

    private final PlanRepository planRepository;
    private final MemberRepository memberRepository;

    @Transactional
    public Plan createPlan(String loginEmail, PlanRequest request) {

        // 1) 로그인된 이메일로 Member 조회
        Member member = memberRepository.findByEmail(loginEmail)
                .orElseThrow(() -> new IllegalArgumentException("회원 정보를 찾을 수 없습니다."));

        Address address = new Address();
        address.setName(request.getName());               // 장소명
        address.setSido(request.getSido());               // 시도
        address.setGugun(request.getGugun());             // 구/군
        address.setTown(request.getTown());               // 읍/면/동
        address.setLatitude(request.getLatitude());       // 위도
        address.setLongitude(request.getLongitude());     // 경도
        address.setDetailAddress(request.getDetailAddress()); // 상세주소

        log.info("createPlan address = {}", address);

        // 3) Plan 엔티티 생성
        Plan plan = new Plan();
        plan.setTitle(request.getTitle());
        plan.setDescription(request.getDescription());
        plan.setThumbnail(request.getThumbnail());
        plan.setPeriod(request.getPeriod());
        plan.setPublic(request.isPublic());   // isPublic 필드 → setPublic()
        plan.setLocation(address);            // 🔥 여기 Address 세팅
        plan.setMember(member);               // 🔥 로그인된 회원 연결

        return planRepository.save(plan);
    }

    @Transactional(readOnly = true)
    public List<Plan> getMyPlans(String loginEmail) {
        Member member = memberRepository.findByEmail(loginEmail)
                .orElseThrow(() -> new IllegalArgumentException("회원 정보를 찾을 수 없습니다."));
        return planRepository.findByMember(member);
    }
}
