package com.ssafy.trip.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ssafy.trip.dto.AttractionDto;
import com.ssafy.trip.repository.AttractionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

//@Service
//public class MapService {
//    private static final String SERVICE_KEY = "";
//
//    public List<Map<String, Object>> getAttractions() {
//        List<Map<String, Object>> list = new ArrayList<>();
//
//        try {
//            // ✅ 공공데이터 API URL 생성
//            String url = UriComponentsBuilder
//                    .fromHttpUrl("https://apis.data.go.kr/B551011/KorService2/areaBasedList2")
//                    .queryParam("serviceKey", SERVICE_KEY)
//                    .queryParam("MobileOS", "ETC")
//                    .queryParam("MobileApp", "TourApp")
//                    .queryParam("numOfRows", "1000")
//                    .queryParam("pageNo", "1")
//                    .queryParam("_type", "json")
//                    .toUriString();
//
//            // ✅ 요청 보내기
//            RestTemplate restTemplate = new RestTemplate();
//            String response = restTemplate.getForObject(url, String.class);
//
//            // ✅ Jackson ObjectMapper로 파싱
//            ObjectMapper mapper = new ObjectMapper();
//            JsonNode root = mapper.readTree(response);
//
//            JsonNode items = root.path("response").path("body").path("items").path("item");
//            if (items.isArray()) {
//                for (JsonNode item : items) {
//                    double lat = item.path("mapy").asDouble(0);
//                    double lng = item.path("mapx").asDouble(0);
//                    if (lat == 0 || lng == 0) continue;
//
//                    Map<String, Object> map = new HashMap<>();
//                    map.put("name", item.path("title").asText("무제"));
//                    map.put("address", item.path("addr1").asText(""));
//                    map.put("lat", lat);
//                    map.put("lng", lng);
//                    list.add(map);
//                }
//            }
//
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
//
//        return list;
//    }
//}

// com.ssafy.trip.service.MapService
@Service
@RequiredArgsConstructor
public class MapService {

    private final AttractionRepository attractionRepository;

    /**
     * 전체 관광지 목록
     */
    @Transactional(readOnly = true)
    public List<AttractionDto> getAllAttractions() {
        return attractionRepository.findAll()
                .stream()
                .map(AttractionDto::from)
                .toList();
    }

    /**
     * 시/군/구별 조회 등 필터가 필요하면 이런식으로 추가
     */
    @Transactional(readOnly = true)
    public List<AttractionDto> getAttractionsByRegion(String sido, String gugun) {
        return attractionRepository.findBySidoAndGugun(sido, gugun)
                .stream()
                .map(AttractionDto::from)
                .toList();
    }
}
