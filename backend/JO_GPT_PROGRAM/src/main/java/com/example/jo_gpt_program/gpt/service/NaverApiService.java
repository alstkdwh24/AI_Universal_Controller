package com.example.jo_gpt_program.gpt.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
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
@Service("naverApiService")
public class NaverApiService {

    private final RestTemplate restTemplate;

    @Value("${map.naver.client-id}")
    private String clientId;

    @Value("${map.naver.client-secret}")
    private String clientSecret;

    public NaverApiService(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    public String findMap(String query) {
        log.info("[NaverAPI] 검색어={}", query);

        HttpHeaders headers = new HttpHeaders();
        headers.set("X-NCP-APIGW-API-KEY-ID", clientId);
        headers.set("X-NCP-APIGW-API-KEY", clientSecret);

        URI uri = UriComponentsBuilder
                .fromHttpUrl("https://maps.apigw.ntruss.com/map-geocode/v2/geocode")
                .queryParam("query", query)
                .build()
                .encode()
                .toUri();
        log.info("[NaverAPI] 요청 URI={}", uri);

        ResponseEntity<String> response = this.restTemplate.exchange(
                uri,
                HttpMethod.GET,
                new HttpEntity<>(headers),
                String.class
        );
        log.info("[NaverAPI] 응답={}", response.getBody());

        try {
            ObjectMapper mapper = new ObjectMapper();
            JsonNode root = mapper.readTree(response.getBody());
            JsonNode addresses = root.path("addresses");
            if (addresses.isArray() && addresses.size() > 0) {
                JsonNode first = addresses.get(0);
                String x = first.path("x").asText();
                String y = first.path("y").asText();
                log.info("[NaverAPI] 추출된 좌표 x={}, y={}", x, y);
                return mapper.writeValueAsString(mapper.createObjectNode().put("x", x).put("y", y));
            } else {
                log.warn("[NaverAPI] addresses 비어있음. 응답={}", response.getBody());
            }
        } catch (Exception e) {
            log.error("[NaverAPI] 좌표 파싱 실패", e);
        }
        return null;
    }
}
