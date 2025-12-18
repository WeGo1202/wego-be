package com.ssafy.trip.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class RouteUpdateRequest {

    @NotNull
    private String title;

    private String description;

    @Min(value = 1)
    private Integer totalDays;

    @NotNull
    private Boolean isPublic;

}
