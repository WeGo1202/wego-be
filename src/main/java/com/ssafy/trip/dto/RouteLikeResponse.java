package com.ssafy.trip.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@AllArgsConstructor
@Builder
public class RouteLikeResponse {
    private Long routeId;
    private Long likeCount;
    private Boolean liked;  // 현재 유저가 좋아요 눌렀는지 여부
}
