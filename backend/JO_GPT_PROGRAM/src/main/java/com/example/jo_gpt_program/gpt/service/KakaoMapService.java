package com.example.jo_gpt_program.gpt.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;

@Slf4j
@Service("kakaoMapService")
public class KakaoMapService {

    @Value("${spring.security.oauth2.client.registration.kakao.client-id}")
    private String kakaoApiKey;

    private final RestTemplate restTemplate;

    public KakaoMapService(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    public String findMap(String query) {
        log.info("[KakaoAPI] 검색어={}", query);

        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "KakaoAK " + kakaoApiKey);

        URI uri = UriComponentsBuilder
                .fromHttpUrl("https://dapi.kakao.com/v2/local/search/keyword.json")
                .queryParam("query", query)
                .queryParam("size", 5)  // ← 최대 5개 결과
                .build()
                .encode()
                .toUri();
        log.info("[KakaoAPI] 요청 URI={}", uri);

        ResponseEntity<String> response = this.restTemplate.exchange(
                uri,
                HttpMethod.GET,
                new HttpEntity<>(headers),
                String.class
        );
        log.info("[KakaoAPI] 응답={}", response.getBody());

        try {
            ObjectMapper mapper = new ObjectMapper();
            JsonNode root = mapper.readTree(response.getBody());
            JsonNode documents = root.path("documents");

            if (documents.isArray() && documents.size() > 0) {
                // 여러 장소 배열로 반환
                ArrayNode places = mapper.createArrayNode();
                for (JsonNode doc : documents) {
                    ObjectNode place = mapper.createObjectNode();
                    place.put("name", doc.path("place_name").asText());
                    place.put("x", doc.path("x").asText());       // 경도
                    place.put("y", doc.path("y").asText());       // 위도
                    place.put("address", doc.path("road_address_name").asText());
                    place.put("url", doc.path("place_url").asText());
                    places.add(place);
                }
                log.info("[KakaoAPI] 장소 {}개 추출 완료", places.size());
                return mapper.writeValueAsString(places);
            } else {
                log.warn("[KakaoAPI] documents 비어있음. 응답={}", response.getBody());
            }
        } catch (Exception e) {
            log.error("[KakaoAPI] 좌표 파싱 실패", e);
        }
        return null;
    }
}
