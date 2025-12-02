package com.ssafy.trip.dto;

import lombok.Data;

import java.util.List;

@Data
public class RouteCreateRequest {

    private String title;
    private String description;
    private Integer totalDays;

    // Route에 포함할 Plan 목록
    private List<RoutePlanItem> items;

    @Data
    public static class RoutePlanItem {
        private Long planId;       // 이미 존재하는 Plan의 ID
        private Integer dayIndex;  // 1일차, 2일차...
        private Integer orderIndex;// 하루 안에서 순서
    }
}
