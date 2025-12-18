package com.ssafy.trip.dto;

import com.ssafy.trip.domain.Attraction;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class AttractionDetailResponse {

    private String title;

    private String addr1;
    private String addr2;

    private Double latitude;
    private Double longitude;

    private String imageUrl;
    private String tel;

    public static AttractionDetailResponse from(Attraction attraction) {
        return AttractionDetailResponse.builder()
                .title(attraction.getTitle())
                .addr1(attraction.getAddr1())
                .addr2(attraction.getAddr2())
                .latitude(attraction.getLatitude())
                .longitude(attraction.getLongitude())
                .imageUrl(attraction.getImageUrl())
                .tel(attraction.getTel())
                .build();
    }
}
