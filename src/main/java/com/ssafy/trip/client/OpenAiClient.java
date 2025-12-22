package com.ssafy.trip.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;

@Slf4j
@Component
public class OpenAiClient {

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper;

    @Value("${openai.api-key}")
    private String apiKey;   // ✅ GMS KEY 넣어야 함

    @Value("${openai.model:gpt-4.1}")
    private String model;

    @Value("${openai.base-url:https://gms.ssafy.io/gmsapi/api.openai.com/v1}")
    private String baseUrl;

    public OpenAiClient(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    /**
     * RouteAiService에서 사용하는 메서드:
     *  - 프롬프트를 보내서
     *  - chat/completions 응답 중 choices[0].message.content 문자열만 꺼내서 리턴
     *  - content 안에는 JSON 문자열(AiGptRouteResponse용)이 들어가게 프롬프트에서 강제
     */
    public String generateRawText(String prompt) {
        try {
            // 1) 요청 바디 JSON 만들기
            ObjectNode body = objectMapper.createObjectNode();
            body.put("model", model);

            ArrayNode messages = body.putArray("messages");
            ObjectNode userMsg = messages.addObject();
            userMsg.put("role", "user");
            userMsg.put("content", prompt);

            // JSON만 강제 (지원 모델 기준)
            ObjectNode responseFormat = objectMapper.createObjectNode();
            responseFormat.put("type", "json_object");
            body.set("response_format", responseFormat);

            String url = baseUrl + "/chat/completions";

            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(apiKey);                    // 🔹 GMS KEY
            headers.setContentType(MediaType.APPLICATION_JSON);

            String bodyString = objectMapper.writeValueAsString(body);
            log.info("GMS/OpenAI 요청 URL: {}", url);
            log.info("GMS/OpenAI 요청 Body: {}", bodyString);

            HttpEntity<String> entity = new HttpEntity<>(bodyString, headers);

            ResponseEntity<String> response = restTemplate.exchange(
                    url,
                    HttpMethod.POST,
                    entity,
                    String.class
            );

            log.info("GMS/OpenAI 응답 status: {}", response.getStatusCode());
            log.info("GMS/OpenAI 응답 body: {}", response.getBody());

            if (!response.getStatusCode().is2xxSuccessful()) {
                throw new IllegalStateException("GMS/OpenAI 호출 실패 status=" + response.getStatusCode());
            }

            String responseStr = response.getBody();
            if (responseStr == null) {
                throw new IllegalStateException("GMS/OpenAI 응답이 비어 있습니다.");
            }

            // 3) 응답 JSON 파싱: choices[0].message.content
            JsonNode root = objectMapper.readTree(responseStr);
            JsonNode choices = root.path("choices");
            if (!choices.isArray() || choices.isEmpty()) {
                throw new IllegalStateException("응답에 choices가 없습니다: " + responseStr);
            }

            JsonNode message = choices.get(0).path("message");
            JsonNode contentNode = message.path("content");

            if (!contentNode.isTextual()) {
                throw new IllegalStateException("응답 content가 문자열이 아닙니다: " + responseStr);
            }

            String content = contentNode.asText();
            log.info("GMS/OpenAI content(모델이 생성한 텍스트): {}", content);
            return content;

        } catch (HttpStatusCodeException e) {
            // 4xx / 5xx일 때 실제 바디까지 로그로 보기
            log.error("GMS/OpenAI HTTP 오류, status={}, body={}",
                    e.getStatusCode(), e.getResponseBodyAsString(), e);
            throw new IllegalStateException("GMS/OpenAI HTTP 오류", e);
        } catch (Exception e) {
            log.error("GMS/OpenAI 호출 또는 파싱 중 예외", e);
            throw new IllegalStateException("AI 호출에 실패했습니다.", e);
        }
    }
}
