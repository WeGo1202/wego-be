package com.ssafy.trip.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ssafy.trip.domain.*;
import com.ssafy.trip.domain.Address;
import com.ssafy.trip.dto.ai.AiGptRouteResponse;
import com.ssafy.trip.dto.ai.AiRouteRequest;
import com.ssafy.trip.client.OpenAiClient;
import com.ssafy.trip.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class RouteAiService {

    private final OpenAiClient openAiClient;
    private final ObjectMapper objectMapper;

    private final MemberRepository memberRepository;
    private final RouteRepository routeRepository;
    private final PlanRepository planRepository;
    private final RoutePlanRepository routePlanRepository;
    private final AttractionRepository attractionRepository;

    /**
     * 메인 엔트리:
     *  - 사용자 질문(AiRouteRequest) + 로그인 email 기반으로
     *  - GPT에게 코스 추천을 받고
     *  - Attraction DB와 매칭하여 Route/Plan/RoutePlan을 생성
     */
    @Transactional
    public Route createRouteByGpt(String loginEmail, AiRouteRequest request) {
        Member member = memberRepository.findByEmail(loginEmail)
                .orElseThrow(() -> new IllegalArgumentException("회원 정보를 찾을 수 없습니다: " + loginEmail));

        // 1. GPT에 보낼 프롬프트 만들기
        String prompt = buildPromptForRoute(request);

        // 2. GPT 호출
        String rawText = openAiClient.generateRawText(prompt);
        log.info("AI raw response: {}", rawText);

        // 3. JSON 파싱
        AiGptRouteResponse aiRoute;
        try {
            aiRoute = objectMapper.readValue(rawText, AiGptRouteResponse.class);
        } catch (Exception e) {
            log.error("AI 응답 파싱 실패, rawText={}", rawText, e);
            throw new IllegalStateException("AI 응답 파싱에 실패했습니다.");
        }

        // 4. 파싱된 응답을 기반으로 실제 Route/Plan/RoutePlan 생성
        return buildRouteFromAiResponse(member, aiRoute);
    }

    /**
     * GPT에 전달할 프롬프트 문자열 구성
     */
    private String buildPromptForRoute(AiRouteRequest req) {
        String userQuery = req.getQuery();
        String region = req.getPreferredRegion();
        Integer daysHint = req.getTotalDaysHint();

        StringBuilder sb = new StringBuilder();
        sb.append("당신은 한국 여행 코스를 설계하는 전문 여행 플래너입니다.\n");
        sb.append("사용자의 요청에 맞는 여행 일정을 추천해 주세요.\n\n");

        sb.append("중요 규칙:\n");
        sb.append("1. 한국에 실제로 존재하는 도시와 관광지(명소) 이름만 사용해 주세요.\n");
        sb.append("2. 각 관광지의 name 필드는 실제 검색 가능한 공식 명칭을 사용해 주세요.\n");
        sb.append("3. address에는 대략적인 주소(시/구/동 수준), region에는 도시/지역명을 넣어 주세요.\n");
        sb.append("4. 출력은 반드시 아래 JSON 형식만 사용하고, JSON 이외의 텍스트는 절대 넣지 마세요.\n");
        sb.append("5. 모든 설명과 텍스트는 한국어로 작성하세요.\n\n");

        sb.append("JSON 스키마:\n");
        sb.append("{\n");
        sb.append("  \"title\": \"코스 전체 제목\",\n");
        sb.append("  \"description\": \"코스 전체 설명\",\n");
        sb.append("  \"totalDays\": 2,\n");
        sb.append("  \"days\": [\n");
        sb.append("    {\n");
        sb.append("      \"dayIndex\": 1,\n");
        sb.append("      \"spots\": [\n");
        sb.append("        {\n");
        sb.append("          \"name\": \"관광지 이름\",\n");
        sb.append("          \"address\": \"대략적인 주소 또는 동/구\",\n");
        sb.append("          \"region\": \"도시/지역명\",\n");
        sb.append("          \"note\": \"이 장소에 대한 간단 설명\"\n");
        sb.append("        }\n");
        sb.append("      ]\n");
        sb.append("    }\n");
        sb.append("  ]\n");
        sb.append("}\n\n");

        sb.append("사용자 요청: \"").append(userQuery).append("\"\n");
        if (region != null && !region.isBlank()) {
            sb.append("선호 지역(힌트): ").append(region).append("\n");
        }
        if (daysHint != null && daysHint > 0) {
            sb.append("원하는 총 여행 일수(힌트): ").append(daysHint).append("일\n");
        }

        sb.append("\n위 규칙을 지키면서 JSON만 반환하세요.");

        return sb.toString();
    }

    /**
     * AI 응답(AiGptRouteResponse)을 실제 JPA 엔티티(Route/Plan/RoutePlan)로 변환
     */
    private Route buildRouteFromAiResponse(Member member, AiGptRouteResponse aiRoute) {
        // 1. Route 생성
        Route route = new Route();
        route.setTitle(aiRoute.getTitle());
        route.setDescription(aiRoute.getDescription());
        route.setMember(member);
        route.setTotalDays(aiRoute.getTotalDays() != null ? aiRoute.getTotalDays() : 1);
        route.setIsPublic(false);     // AI로 생성된 루트는 기본 비공개
        route.setLikeCount(0L);      // 좋아요 0으로 시작

        // createdAt, updatedAt은 Auditing으로 자동 세팅됨
        routeRepository.save(route);

        List<RoutePlan> routePlans = new ArrayList<>();

        if (aiRoute.getDays() == null) {
            return route;
        }

        for (AiGptRouteResponse.DayPlan dayPlan : aiRoute.getDays()) {
            int dayIndex = dayPlan.getDayIndex() != null ? dayPlan.getDayIndex() : 1;

            if (dayPlan.getSpots() == null) continue;

            int orderIndex = 0;
            for (AiGptRouteResponse.Spot spot : dayPlan.getSpots()) {
                orderIndex++;

                // 2. Spot → Attraction 매칭
                Attraction attraction = findBestMatchingAttraction(spot);
                if (attraction == null) {
                    log.warn("매칭되는 Attraction을 찾지 못해 건너뜀: {}", spot);
                    continue;
                }

                // 3. Plan 생성 (Plan 엔티티 구조에 맞춰서)
                Plan plan = createPlanFromAttraction(member, attraction, spot);
                planRepository.save(plan);

                // 4. RoutePlan 생성 및 연결
                RoutePlan rp = new RoutePlan();
                rp.setRoute(route);
                rp.setPlan(plan);
                rp.setDayIndex(dayIndex);
                rp.setOrderIndex(orderIndex);

                routePlanRepository.save(rp);
                routePlans.add(rp);
            }
        }

        route.setRoutePlans(routePlans);
        return route;
    }

    /**
     * GPT가 준 Spot을 기반으로 DB에서 Attraction을 찾는 로직
     *  - region + name
     *  - name만
     *  - address 기반
     */
    private Attraction findBestMatchingAttraction(AiGptRouteResponse.Spot spot) {
        String name = safe(spot.getName());
        String region = safe(spot.getRegion());
        String address = safe(spot.getAddress());

        if (name.isEmpty()) return null;

        // 1) region + name
        if (!region.isEmpty()) {
            var byRegion = attractionRepository
                    .findTop10BySidoContainingIgnoreCaseAndTitleContainingIgnoreCaseOrderByIdAsc(
                            region, name
                    );
            if (!byRegion.isEmpty()) return byRegion.get(0);
        }

        // 2) name만
        var byName = attractionRepository
                .findTop10ByTitleContainingIgnoreCaseOrderByIdAsc(name);
        if (!byName.isEmpty()) return byName.get(0);

        // 3) address 기반
        if (!address.isEmpty()) {
            var byAddr = attractionRepository
                    .findTop10ByAddr1ContainingIgnoreCaseOrderByIdAsc(address);
            if (!byAddr.isEmpty()) return byAddr.get(0);
        }

        return null;
    }

    private String safe(String s) {
        return s == null ? "" : s.trim();
    }

    /**
     * Attraction 정보를 기반으로 Plan 엔티티를 구성
     *  - Plan: id, title, description, thumbnail, period, isPublic, Address location, Member member
     */
    private Plan createPlanFromAttraction(
            Member member,
            Attraction attraction,
            AiGptRouteResponse.Spot spot
    ) {
        Plan plan = new Plan();
        plan.setTitle(attraction.getTitle());
        plan.setDescription(
                spot.getNote() != null && !spot.getNote().isBlank()
                        ? spot.getNote()
                        : attraction.getAddr1()
        );
        plan.setThumbnail(attraction.getImageUrl());
        plan.setPeriod(1);          // 일단 1일짜리 일정으로
        plan.setPublic(false);      // 개별 Plan은 비공개로 시작 (필요에 따라 변경)

        // Address(위치) 세팅
        Address addr = new Address();
        addr.setName(attraction.getTitle());
        addr.setSido(attraction.getSido());
        addr.setGugun(attraction.getGugun());
        addr.setTown(null);   // 필요하면 나중에 확장
        if (attraction.getLatitude() != null) {
            addr.setLatitude(attraction.getLatitude().floatValue());
        }
        if (attraction.getLongitude() != null) {
            addr.setLongitude(attraction.getLongitude().floatValue());
        }
        addr.setDetailAddress(attraction.getAddr1());

        plan.setLocation(addr);
        plan.setMember(member);

        return plan;
    }
}
