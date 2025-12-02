package com.ssafy.trip.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ssafy.trip.domain.Attraction;
import com.ssafy.trip.repository.AttractionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;


import java.util.*;

// com.ssafy.trip.service.TourDataSyncService
@Service
@RequiredArgsConstructor
@Slf4j
public class TourDataService {

    private final AttractionRepository attractionRepository;
    private final RestTemplate restTemplate;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${tour.api.base-url}")
    private String baseUrl;

    @Value("${tour.api.service-key}")
    private String serviceKey;

    /**
     * 공공데이터 API 전체 페이지를 돌면서 attractions 테이블을 갱신
     */
    public void syncAllAttractions() {
        int pageNo = 1;
        int pageSize = 1000;
        int total = 0;

        while (true) {
            List<Map<String, Object>> items = callTourApi(pageNo, pageSize);
            if (items.isEmpty()) break;

            for (Map<String, Object> item : items) {
                try {
                    saveNewAttraction(item);   // 여기서 UNIQUE 위반 나면 catch 쪽으로
                    total++;
                } catch (DataIntegrityViolationException e) {
                    log.warn("중복 contentId={}, 건너뜀", item.get("contentid"));
                }
            }

            pageNo++;
        }

        log.info("sync done. totalInserted={}", total);
    }

    private List<Map<String, Object>> callTourApi(int pageNo, int pageSize) {
        List<Map<String, Object>> list = new ArrayList<>();

        try {
            String url = UriComponentsBuilder
                    .fromHttpUrl(baseUrl)
                    .queryParam("serviceKey", serviceKey)
                    .queryParam("MobileOS", "ETC")
                    .queryParam("MobileApp", "TourApp")
                    .queryParam("numOfRows", pageSize)
                    .queryParam("pageNo", pageNo)
                    .queryParam("_type", "json")
                    .toUriString();

            String response = restTemplate.getForObject(url, String.class);

            JsonNode root = objectMapper.readTree(response);
            JsonNode items = root.path("response").path("body").path("items").path("item");

            if (items.isArray()) {
                for (JsonNode item : items) {
                    double lat = item.path("mapy").asDouble(0);
                    double lng = item.path("mapx").asDouble(0);
                    if (lat == 0 || lng == 0) continue; // 좌표 없는 건 스킵

                    Map<String, Object> map = new HashMap<>();

                    map.put("contentid", item.path("contentid").asLong());
                    map.put("title", item.path("title").asText("무제"));
                    map.put("addr1", item.path("addr1").asText(""));
                    map.put("addr2", item.path("addr2").asText(""));
                    map.put("sido", item.path("sido").asText(""));
                    map.put("gugun", item.path("gugun").asText(""));

                    map.put("mapy", lat);
                    map.put("mapx", lng);

                    if (!item.path("contenttypeid").isMissingNode()) {
                        map.put("contenttypeid", item.path("contenttypeid").asInt());
                    }

                    map.put("firstimage", item.path("firstimage").asText(""));
                    map.put("tel", item.path("tel").asText(""));

                    list.add(map);
                }
            }
        } catch (Exception e) {
            log.error("Tour API 호출/파싱 실패 pageNo={}", pageNo, e);
        }

        return list;
    }

//    private void saveOrUpdateAttraction(Map<String, Object> item) {
//        Long contentId = Long.valueOf(item.get("contentid").toString());
//
//        Attraction attraction = attractionRepository
//                .findByContentId(contentId)
//                .orElseGet(Attraction::new);
//
//        if (attraction.getId() == null) {
//            attraction.setContentId(contentId);
//        }
//
//        // 공공데이터 필드명에 맞게 매핑 (필드명은 실제 응답 기준으로 수정)
//        attraction.setTitle((String) item.get("title"));
//        attraction.setAddr1((String) item.get("addr1"));
//        attraction.setAddr2((String) item.get("addr2"));
//        attraction.setSido((String) item.get("sido"));
//        attraction.setGugun((String) item.get("gugun"));
//
//        double mapY = Double.parseDouble(item.get("mapy").toString()); // 위도
//        double mapX = Double.parseDouble(item.get("mapx").toString()); // 경도
//        attraction.setLatitude(mapY);
//        attraction.setLongitude(mapX);
//
//        if (item.get("contenttypeid") != null) {
//            attraction.setContentTypeId(Integer.parseInt(item.get("contenttypeid").toString()));
//        }
//        attraction.setImageUrl((String) item.get("firstimage"));
//        attraction.setTel((String) item.get("tel"));
//
//        attractionRepository.save(attraction);
//    }

    private void saveNewAttraction(Map<String, Object> item) {
        Attraction attraction = new Attraction();

        attraction.setContentId(
                Long.parseLong(item.get("contentid").toString())
        );

        // 문자열 필드들
        attraction.setTitle((String) item.get("title"));
        attraction.setAddr1((String) item.get("addr1"));
        attraction.setAddr2((String) item.get("addr2"));
        attraction.setSido((String) item.get("sido"));
        attraction.setGugun((String) item.get("gugun"));

        double mapY = Double.parseDouble(item.get("mapy").toString());
        double mapX = Double.parseDouble(item.get("mapx").toString());
        attraction.setLatitude(mapY);
        attraction.setLongitude(mapX);

        Object ctype = item.get("contenttypeid");
        if (ctype != null) {
            attraction.setContentTypeId(Integer.parseInt(ctype.toString()));
        }

        attraction.setImageUrl(Objects.toString(item.get("firstimage"), ""));
        attraction.setTel(Objects.toString(item.get("tel"), ""));

        attractionRepository.save(attraction);
    }
}
