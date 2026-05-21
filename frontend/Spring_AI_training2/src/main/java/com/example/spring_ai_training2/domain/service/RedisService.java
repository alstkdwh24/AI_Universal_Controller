package com.example.spring_ai_training2.domain.service;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.Set;
import java.util.concurrent.TimeUnit;

@Service("redisService")
public class RedisService {

    private final RedisTemplate<String, Object> redisTemplate;

    public RedisService(RedisTemplate<String, Object> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }
    // 저장 - 만료시간 포함
    public void save(String key, Object value, long expireTime){
        redisTemplate.opsForValue().set(key, value, expireTime, TimeUnit.MICROSECONDS);
    }

    public Object get(String key){
        // opsForValue는  Redis의 String 타입 데이터를 다루는 연산 객체
        return redisTemplate.opsForValue().get(key);

    }

    public void delete(String key){
         redisTemplate.delete(key);
    }
    public void deleteAll(){
        Set<String> keys = redisTemplate.keys("*");
        if(keys.isEmpty()) return;
        redisTemplate.delete(keys);
    }


}
