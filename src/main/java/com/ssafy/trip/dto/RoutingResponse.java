package com.ssafy.trip.dto;

import lombok.Data;
import java.util.List;

@Data
public class RoutingResponse {
    private double totalDistanceMeters;
    private int totalDurationSeconds;
    private List<LatLng> polyline;
    @Data
    public static class LatLng {
        private double lat;
        private double lng;
    }
}
