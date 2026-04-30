package com.example.jo_gpt_program.gpt.service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
// 학술지 검색
public class ScholarSearchService {

    private final RestTemplate restTemplate;

    public String search(String query) {
        try {
            // url관련
            String url = UriComponentsBuilder
                    // 기본 url 설정
                    .fromUriString("https://api.semanticscholar.org/graph/v1/paper/search")
                    // ? query = 스프링 AI 추가 검색어 추
                    .queryParam("query", query)
                    // 어떤 정보를 받을지 지정하는 파라미터이다 title: 논문 제목, abstract: 논문 요약, authors: 저자 목록, year:
                    // 출판 년도, url: 논문 링크
                    .queryParam("field", "title,abstract,authors,year,url")
                    .queryParam("limit", 5) // 논문 5개 반환 (현재 설정)
                    .build()
                    .toString();
            // 요청을 해서 응답을 받기
            ResponseEntity<Map> response = restTemplate.getForEntity(url, Map.class);

            // 바디만 꺼내기
            List<Map<String, Object>> papers = (List<Map<String, Object>>) response.getBody().get("data");

            if (papers == null || papers.isEmpty())
                return "";

            return papers.stream()
                    .map(p -> String.format("제목: %s\n요약: %s\n년도: %s\n링크: %s",
                            p.getOrDefault("title", "없음"),
                            p.getOrDefault("abstract", "없음"),
                            p.getOrDefault("year", "없음"),
                            p.getOrDefault("url", "없음")))
                    .collect(Collectors.joining("\n---\n"));

        } catch (Exception e) {
            log.error("Semantic Scholar 검색 실패: {}", e.getMessage());
            return "";
        }
    }

}
