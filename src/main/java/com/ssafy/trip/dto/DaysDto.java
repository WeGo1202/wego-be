package com.ssafy.trip.dto;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class DaysDto {
    private int dayIndex;           // Day 1, Day 2 할 때 그 숫자
    private List<PlansDto> plans; // 해당 날짜의 계획들

    public static DaysDto from(int dayIndex, List<PlansDto> plans) {
        return DaysDto.builder()
                .dayIndex(dayIndex)
                .plans(plans)
                .build();
    }
}
