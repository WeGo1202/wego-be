package com.ssafy.trip.dto.ai;

import lombok.Data;

@Data
public class AiRouteRequest {

    /**
     * 사용자의 자연어 질문
     * 예: "졸업 여행으로 갈만한 2박3일 공주 여행코스 추천해줘"
     */
    private String query;

    /**
     * 선택: 선호 지역 힌트 (예: "공주", "부산", "제주" 등)
     * 없으면 null 가능
     */
    private String preferredRegion;

    /**
     * 선택: 총 여행 일수 (null이면 GPT가 추론)
     */
    private Integer totalDaysHint;
}
