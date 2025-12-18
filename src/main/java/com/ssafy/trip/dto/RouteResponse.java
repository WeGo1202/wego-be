package com.ssafy.trip.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.ssafy.trip.domain.Route;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class RouteResponse {

    private Long id;
    private String title;
    private String description;
    private Integer totalDays;

    @JsonProperty("isPublic")
    private Boolean isPublic;
    private long likeCount;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static RouteResponse from(Route route) {
        return RouteResponse.builder()
                .id(route.getId())
                .title(route.getTitle())
                .description(route.getDescription())
                .totalDays(route.getTotalDays())
                .isPublic(route.getIsPublic())
                .likeCount(route.getLikeCount())
                .createdAt(route.getCreatedAt())
                .updatedAt(route.getUpdatedAt())
                .build();
    }
}