package com.ssafy.trip.dto;

import com.ssafy.trip.domain.Plan;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class PlansDto {
    private Long planId;
    private Long attractionId;
    private String title;
    private String address;
    private Integer orderIndex;

    public static PlansDto from(Plan plan, int orderIndex) {
        return PlansDto.builder()
                .planId(plan.getId())
                .attractionId(plan.getAttractionId())
                .title(plan.getTitle())
                .address(plan.getDescription())
                .orderIndex(orderIndex)
                .build();
    }
}
