package com.ssafy.trip.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class PlansDto {
    private Long planId;
    private Long attractionId;
    private String title;
    private String addr1;
    private Integer orderIndex;

    public static PlansDto from() {
        return PlansDto.builder()
                .build();
    }
}
