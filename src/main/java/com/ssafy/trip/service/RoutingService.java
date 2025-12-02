package com.ssafy.trip.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ssafy.trip.dto.RoutingRequest;
import com.ssafy.trip.dto.RoutingResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RoutingService {

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    @Value("${naver.map.client-id}")
    private String naverClientId;

    @Value("${naver.map.client-secret}")
    private String naverClientSecret;

    private static final String NAVER_DIRECTION_URL =
            "https://maps.apigw.ntruss.com/map-direction/v1/driving";

    public RoutingResponse getRoute(RoutingRequest request) {
        List<RoutingRequest.Point> original = request.getPoints();
        if (original == null || original.size() < 2) {
            throw new IllegalArgumentException("최소 2개 이상의 좌표가 필요합니다.");
        }

        List<RoutingRequest.Point> points = optimizeExactTspPath(original);

        RoutingRequest.Point startPoint = points.get(0);
        RoutingRequest.Point goalPoint  = points.get(points.size() - 1);

        String start = startPoint.getLng() + "," + startPoint.getLat();
        String goal  = goalPoint.getLng() + "," + goalPoint.getLat();

        String waypoints = null;
        if (points.size() > 2) {
            waypoints = points.subList(1, points.size() - 1).stream()
                    .map(p -> p.getLng() + "," + p.getLat())
                    .collect(Collectors.joining("|"));
        }

        UriComponentsBuilder builder = UriComponentsBuilder
                .fromHttpUrl(NAVER_DIRECTION_URL)
                .queryParam("start", start)
                .queryParam("goal", goal)
                // trafast: 빠른(시간 위주), traoptimal: 최적 (시간/비용 종합)
                .queryParam("option", "trafast");

        if (waypoints != null && !waypoints.isBlank()) {
            builder.queryParam("waypoints", waypoints);
        }

        String url = builder.toUriString();

        HttpHeaders headers = new HttpHeaders();
        headers.set("X-NCP-APIGW-API-KEY-ID", naverClientId);
        headers.set("X-NCP-APIGW-API-KEY", naverClientSecret);

        HttpEntity<Void> entity = new HttpEntity<>(headers);

        try {
            ResponseEntity<String> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    entity,
                    String.class
            );

            String body = response.getBody();
            if (body == null) {
                throw new RuntimeException("네이버 길찾기 응답이 비어 있습니다.");
            }

            JsonNode root = objectMapper.readTree(body);
            JsonNode routesNode = root.path("route").path("trafast");
            if (!routesNode.isArray() || routesNode.isEmpty()) {
                throw new RuntimeException("Naver Direction: trafast 경로가 없습니다.");
            }

            JsonNode bestRoute = null;
            int bestDistance = Integer.MAX_VALUE;

            for (JsonNode routeNode : routesNode) {
                JsonNode summaryNode = routeNode.path("summary");
                int dist = summaryNode.path("distance").asInt(Integer.MAX_VALUE);
                if (dist < bestDistance) {
                    bestDistance = dist;
                    bestRoute = routeNode;
                }
            }

            if (bestRoute == null) {
                throw new RuntimeException("Naver Direction: 유효한 경로를 찾지 못했습니다.");
            }

            JsonNode summary = bestRoute.path("summary");
            int distance = summary.path("distance").asInt();
            int durationMs = summary.path("duration").asInt();

            // ✅ 4. path → polyline
            List<RoutingResponse.LatLng> polyline = new ArrayList<>();
            for (JsonNode p : bestRoute.path("path")) {
                double lng = p.get(0).asDouble();
                double lat = p.get(1).asDouble();
                RoutingResponse.LatLng latLng = new RoutingResponse.LatLng();
                latLng.setLat(lat);
                latLng.setLng(lng);
                polyline.add(latLng);
            }

            RoutingResponse res = new RoutingResponse();
            res.setTotalDistanceMeters(distance);
            res.setTotalDurationSeconds(durationMs / 1000);
            res.setPolyline(polyline);
            return res;

        } catch (HttpClientErrorException e) {
            System.out.println("[NAVER ERROR] " + e.getStatusCode());
            System.out.println(e.getResponseBodyAsString());
            throw new RuntimeException("네이버 길찾기 API 호출 실패: " + e.getStatusCode());
        } catch (Exception e) {
            throw new RuntimeException("네이버 길찾기 처리 중 오류", e);
        }
    }


    private List<RoutingRequest.Point> optimizeExactTspPath(List<RoutingRequest.Point> original) {
        int n = original.size();
        if (n <= 2)
            return original;

        double[][] dist = buildDistanceMatrix(original);

        int FULL = 1 << n;
        double INF = 1e18;

        double[][] dp = new double[FULL][n];
        int[][] parent = new int[FULL][n];

        for (int m = 0; m < FULL; m++) {
            Arrays.fill(dp[m], INF);
            Arrays.fill(parent[m], -1);
        }

        int start = 0;
        int end = n - 1;

        // 시작점만 방문했을 때, 위치 = start
        dp[1 << start][start] = 0.0;

        for (int mask = 0; mask < FULL; mask++) {
            // 시작점이 포함되지 않은 상태는 무시
            if ((mask & (1 << start)) == 0) continue;

            for (int u = 0; u < n; u++) {
                if (dp[mask][u] >= INF) continue;
                // 아직 방문 안 한 v로 이동
                for (int v = 0; v < n; v++) {
                    if ((mask & (1 << v)) != 0) continue; // 이미 방문한 곳

                    double nd = dp[mask][u] + dist[u][v];
                    int nmask = mask | (1 << v);
                    if (nd < dp[nmask][v]) {
                        dp[nmask][v] = nd;
                        parent[nmask][v] = u;
                    }
                }
            }
        }

        int all = FULL - 1;
        double best = dp[all][end];
        if (best >= INF) {
            return original;
        }

        List<Integer> orderIdx = new ArrayList<>();
        int cur = end;
        int mask = all;
        while (cur != -1) {
            orderIdx.add(cur);
            int p = parent[mask][cur];
            mask &= ~(1 << cur);
            cur = p;
        }
        Collections.reverse(orderIdx);

        List<RoutingRequest.Point> ordered = new ArrayList<>();
        for (int idx : orderIdx) {
            ordered.add(original.get(idx));
        }
        return ordered;
    }

    // 직선 거리 저장
    private double[][] buildDistanceMatrix(List<RoutingRequest.Point> pts) {
        int n = pts.size();
        double[][] dist = new double[n][n];
        for (int i = 0; i < n; i++) {
            for (int j = 0; j < n; j++) {
                if (i == j) {
                    dist[i][j] = 0;
                } else {
                    dist[i][j] = haversine(
                            pts.get(i).getLat(), pts.get(i).getLng(),
                            pts.get(j).getLat(), pts.get(j).getLng()
                    );
                }
            }
        }
        return dist;
    }

    // 직선 거리
    private double haversine(double lat1, double lon1, double lat2, double lon2) {
        double R = 6371.0; // km
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);

        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLon / 2) * Math.sin(dLon / 2);

        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return R * c;
    }
}
