package com.ssafy.trip.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

@Slf4j
@Component
public class OpenAiClient {

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper;

    @Value("${openai.api-key}")
    private String apiKey;

    @Value("${openai.model:gpt-4o-mini}")
    private String model;

    @Value("${openai.base-url:https://api.openai.com/v1}")
    private String baseUrl;

    public OpenAiClient(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    /**
     * RouteAiService에서 사용하는 메서드:
     *  - 프롬프트를 보내서
     *  - chat/completions 응답 중 choices[0].message.content 문자열만 꺼내서 리턴
     *  - content는 JSON 문자열(AiGptRouteResponse용)로 받는 것이 목표
     */
    public String generateRawText(String prompt) {
        try {
            // 1) 요청 바디 JSON 만들기
            ObjectNode body = objectMapper.createObjectNode();
            body.put("model", model);

            // messages 배열 구성 (chat completions 형식)
            ArrayNode messages = body.putArray("messages");
            ObjectNode userMsg = messages.addObject();
            userMsg.put("role", "user");
            userMsg.put("content", prompt);

            // JSON만 깔끔하게 받기 위해 response_format 설정 (지원되는 모델 기준)
            ObjectNode responseFormat = objectMapper.createObjectNode();
            responseFormat.put("type", "json_object");
            body.set("response_format", responseFormat);

            String url = baseUrl + "/chat/completions";

            // 2) 헤더 + 요청 전송
            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(apiKey);
            headers.setContentType(MediaType.APPLICATION_JSON);

            String bodyString = objectMapper.writeValueAsString(body);
            HttpEntity<String> entity = new HttpEntity<>(bodyString, headers);

            String responseStr = restTemplate.postForObject(url, entity, String.class);
            if (responseStr == null) {
                throw new IllegalStateException("OpenAI 응답이 비어 있습니다.");
            }

            // 3) 응답 JSON 파싱: choices[0].message.content
            JsonNode root = objectMapper.readTree(responseStr);
            JsonNode choices = root.path("choices");
            if (!choices.isArray() || choices.isEmpty()) {
                throw new IllegalStateException("OpenAI 응답에 choices가 없습니다: " + responseStr);
            }

            JsonNode message = choices.get(0).path("message");
            JsonNode contentNode = message.path("content");

            if (!contentNode.isTextual()) {
                throw new IllegalStateException("OpenAI 응답에서 content를 문자열로 찾을 수 없습니다: " + responseStr);
            }

            String content = contentNode.asText();
            log.debug("OpenAI content: {}", content);
            return content;

        } catch (Exception e) {
            log.error("OpenAI 호출 또는 파싱 중 오류", e);
            throw new IllegalStateException("AI 호출에 실패했습니다.", e);
        }
    }
}
