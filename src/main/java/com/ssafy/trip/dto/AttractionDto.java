package com.ssafy.trip.dto;

import com.ssafy.trip.domain.Attraction;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AttractionDto {
    private Long id;
    private Long contentId;
    private String title;
    private String addr1;
    private String addr2;
    private String sido;
    private String gugun;
    private Double latitude;
    private Double longitude;
    private Integer contentTypeId;
    private String imageUrl;
    private String tel;

    public static AttractionDto from(Attraction a) {
        return AttractionDto.builder()
                .id(a.getId())
                .contentId(a.getContentId())
                .title(a.getTitle())
                .addr1(a.getAddr1())
                .addr2(a.getAddr2())
                .sido(a.getSido())
                .gugun(a.getGugun())
                .latitude(a.getLatitude())
                .longitude(a.getLongitude())
                .contentTypeId(a.getContentTypeId())
                .imageUrl(a.getImageUrl())
                .tel(a.getTel())
                .build();
    }
}
