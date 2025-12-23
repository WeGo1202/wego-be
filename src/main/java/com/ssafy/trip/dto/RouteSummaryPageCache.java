// src/main/java/com/ssafy/trip/dto/route/RouteSummaryPageCache.java
package com.ssafy.trip.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

/**
 * Redis 캐시에 저장하기 위한 얇은 DTO
 *  - content: 실제 목록 (RouteSummaryResponse 리스트)
 *  - page: 현재 페이지 번호
 *  - size: 페이지 크기
 *  - totalElements: 전체 개수
 */
@Getter
@Setter
@NoArgsConstructor
public class RouteSummaryPageCache {

    private List<RouteSummaryResponse> content;
    private int page;
    private int size;
    private long totalElements;
}
