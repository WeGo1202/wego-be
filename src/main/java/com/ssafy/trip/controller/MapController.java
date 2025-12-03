package com.ssafy.trip.controller;

import com.ssafy.trip.domain.Attraction;
import com.ssafy.trip.dto.AttractionDto;
import com.ssafy.trip.repository.AttractionRepository;
import com.ssafy.trip.service.MapService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/map")
public class MapController {

    private final MapService mapService;
    private final AttractionRepository attractionRepository;

    @GetMapping("/attractions")
    public ResponseEntity<List<AttractionDto>> getAttractions() {
        return ResponseEntity.ok(mapService.getAllAttractions());
    }

    @GetMapping("/attractions/search")
    public List<Attraction> search(
            @RequestParam(required = false) String q,
            @RequestParam(required = false, name = "type") Integer typeId
    ) {
        String keyword = (q == null || q.isBlank()) ? null : q.trim();
        return attractionRepository.searchAttractions(keyword, typeId);
    }

}
