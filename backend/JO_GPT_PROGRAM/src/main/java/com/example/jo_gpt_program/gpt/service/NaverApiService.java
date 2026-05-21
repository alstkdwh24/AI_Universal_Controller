package com.example.jo_gpt_program.gpt.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

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

}
