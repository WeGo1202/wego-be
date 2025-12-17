package com.ssafy.trip.dto;

import com.ssafy.trip.domain.Route;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class RouteSummaryResponse {

    private Long id;
    private String title;
    private String description;
    private Boolean isPublic;
    private long likeCount;
    private Integer totalDays;

    private String ownerName;        // 작성자 이름 / 닉네임
    private LocalDateTime createdAt;

    public static RouteSummaryResponse from(Route route) {
        return RouteSummaryResponse.builder()
                .id(route.getId())
                .title(route.getTitle())
                .description(route.getDescription())
                .isPublic(route.getIsPublic())
                .likeCount(route.getLikeCount())
                .totalDays(route.getTotalDays())
                .ownerName(route.getMember().getName())  // Member에 맞게 수정
                .createdAt(route.getCreatedAt())
                .build();
    }
}
