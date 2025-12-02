package com.ssafy.trip.dto;

import lombok.Data;
import java.util.List;

@Data
public class RoutingRequest {
    private List<Point> points; // 순서대로 (0: 출발, 마지막: 도착, 중간: 경유)

    @Data
    public static class Point {
        private double lat;
        private double lng;
    }
}
