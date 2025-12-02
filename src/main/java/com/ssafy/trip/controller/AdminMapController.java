package com.ssafy.trip.controller;

import com.ssafy.trip.service.TourDataService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/tour")
@RequiredArgsConstructor
public class AdminMapController {

    private final TourDataService tourDataSyncService;

    @PostMapping("/sync")
    public ResponseEntity<String> sync() {
        tourDataSyncService.syncAllAttractions();
        return ResponseEntity.ok("관광지 동기화 완료");
    }
}