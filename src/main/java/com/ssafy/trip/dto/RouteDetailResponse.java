package com.ssafy.trip.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.ssafy.trip.domain.Route;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class RouteDetailResponse {

    private String title;
    private String description;

    @JsonProperty("isPublic")
    private boolean isPublic;

    private long likeCount;
    private Integer totalDays;
    private LocalDateTime createdAt;

    private String ownerName;
    private String ownerNickname;

    private boolean liked;

    @JsonProperty("isGuest")
    private boolean isGuest;

    private List<DaysDto> days;

    public static RouteDetailResponse from(Route route, boolean liked, boolean isGuest
            , List<DaysDto> days) {
        return RouteDetailResponse.builder()
                .title(route.getTitle())
                .description(route.getDescription())
                .isPublic(route.getIsPublic())
                .likeCount(route.getLikeCount())
                .totalDays(route.getTotalDays())
                .createdAt(route.getCreatedAt())
                .ownerName(route.getMember().getName())  // Member에 맞게 수정
                .ownerNickname(route.getMember().getNickname())
                .liked(liked)
                .isGuest(isGuest)
                .days(days)
                .build();
    }

}
