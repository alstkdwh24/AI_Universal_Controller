package com.example.jo_gpt_program.gpt.service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@Service("alertService")
public class AlertService {

    // ConcurrentHashMap 은 자바에서 여러 개의 쓰레드가 동시에 접근하더라도 데이터의 안정성을 보장하면서 성능까지 챙긴 해시맵이다.
    // HashMap은 자바에서 데이터를 '키(Key)'와 '값(Value)'의 쌍으로 묶어서 저장하는 아주 대표적인 바구니(자료구조)입니다.
    private final Map<Long, SseEmitter> emitters = new ConcurrentHashMap<>();
}
