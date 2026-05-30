package com.example.spring_ai_training2.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.jedis.JedisConnectionFactory; // 💡 제디스 커넥션 팩토리 임포트
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.RedisSerializer;
import redis.clients.jedis.JedisPooled;

@Configuration
public class RedisConfig {

    /**
     * 💡 [인프라 단일화 승리 지점]
     * 민상님의 도커 외부 포트(4863) 주소와 묶인 순수 JedisPooled 클라이언트를 단 하나만 선언합니다.
     */
    @Bean
    public JedisPooled jedisPooled() {
        return new JedisPooled("localhost", 4863);
    }

    /**
     * 💡 [핵심 버그 격파 지점]
     * 파라미터로 모호한 RedisConnectionFactory를 주입받아 충돌을 일으키는 대신,
     * 위에서 우리가 만든 도커 포트(4863) 전용 Jedis 연결을 기반으로 팩토리를 직접 직조해서 세팅합니다.
     * 이렇게 하면 Lettuce와 Jedis의 주도권 싸움이 완벽하게 해결됩니다.
     */
    @Bean
    public RedisTemplate<String, Object> redisTemplate() {
        RedisTemplate<String, Object> template = new RedisTemplate<>();

        // 💡 Lettuce 대신 우리가 선언한 4863 포트 기반의 Jedis 연결 공장을 만들어 이식합니다.
        JedisConnectionFactory jedisConnectionFactory = new JedisConnectionFactory();
        jedisConnectionFactory.setHostName("localhost");
        jedisConnectionFactory.setPort(4863);
        jedisConnectionFactory.afterPropertiesSet(); // 팩토리 초기화 가동

        template.setConnectionFactory(jedisConnectionFactory);

        // 민상님의 기존 소중한 시리얼라이저 스펙 완벽 보존
        template.setKeySerializer(RedisSerializer.string());
        template.setValueSerializer(RedisSerializer.json());
        template.setHashKeySerializer(RedisSerializer.string());
        template.setHashValueSerializer(RedisSerializer.json());

        return template;
    }

    // 날짜 포맷팅 정상화를 위한 ObjectMapper Bean 유지
    @Bean
    public ObjectMapper objectMapper(){
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);
        return objectMapper;
    }
}