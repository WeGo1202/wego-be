package com.ssafy.trip.controller;

import com.ssafy.trip.dto.AttractionDto;
import com.ssafy.trip.service.MapService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/map")
public class MapController {

    private final MapService mapService;

    @GetMapping("/attractions")
    public ResponseEntity<List<AttractionDto>> getAttractions() {
        return ResponseEntity.ok(mapService.getAllAttractions());
    }

}
