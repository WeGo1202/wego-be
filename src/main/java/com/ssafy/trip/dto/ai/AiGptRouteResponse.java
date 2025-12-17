package com.ssafy.trip.dto.ai;

import lombok.Data;

import java.util.List;

@Data
public class AiGptRouteResponse {

    // 전체 코스 제목
    private String title;

    // 전체 코스 설명
    private String description;

    // 총 여행 일수
    private Integer totalDays;

    // 일자별 계획
    private List<DayPlan> days;

    @Data
    public static class DayPlan {
        // 1일부터 시작하는 day index (1, 2, 3, ...)
        private Integer dayIndex;
        private List<Spot> spots;
    }

    @Data
    public static class Spot {
        // 여행지 이름 (필수)
        private String name;

        // 선택: 주소(모르면 대략 지역명이라도)
        private String address;

        // 선택: 도시/지역(ex. 공주, 부산, 서울 등)
        private String region;

        // 선택: 간단 설명
        private String note;
    }
}
