// com.ssafy.trip.web.plan.dto.PlanRequest.java
package com.ssafy.trip.dto;

import lombok.Data;

@Data
public class PlanRequest {

    // Plan 기본 정보
    private String title;
    private String description;
    private String thumbnail;
    private Integer period;
    private boolean isPublic;

    private String name;          // 장소명
    private String sido;          // 시도
    private String gugun;         // 구/군
    private String town;          // 읍/면/동
    private Float latitude;       // 위도
    private Float longitude;      // 경도
    private String detailAddress; // 상세주소
}
