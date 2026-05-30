package com.example.spring_ai_training2.config;

import com.example.spring_ai_training2.domain.dto.MessageDto;
import com.example.spring_ai_training2.domain.tools.ChatTools;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.ai.chat.memory.repository.redis.RedisChatMemoryRepository;

import org.springframework.ai.google.genai.GoogleGenAiEmbeddingConnectionDetails;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import redis.clients.jedis.JedisPooled;

import static org.springframework.ai.chat.memory.ChatMemory.CONVERSATION_ID;

@Configuration
public class AiConfig {
    @Bean
    public GoogleGenAiEmbeddingConnectionDetails googleGenAiEmbeddingConnectionDetails(
            @Value("${spring.ai.google.genai.api-key}") String apiKey) {

        return GoogleGenAiEmbeddingConnectionDetails.builder()
                .apiKey(apiKey)   // API 키만 넣으면 끝!
                .build();
    }

    /**
     * 💡 [대망의 마지막 퍼즐: 구글 임베딩 모순 버그 해결용 커스텀 부품]
     * 우리가 치워버린 프레임워크의 악성 부품을 대체하는 순정 연결 통로입니다.
     * projectId를 강제로 요구하지도 않고, 기업용 Vertex AI로 착각하게 만들지도 않는 완벽한 우회로입니다.
     */


    /**
     * 레디스 챗 메모리 레포지토리 (프레임워크 버그 우회용 공식 빌더 패턴 적용 완료)
     */
    @Bean
    public RedisChatMemoryRepository redisChatMemoryRepository(JedisPooled jedisPooled) {
        return RedisChatMemoryRepository.builder()
                .jedisClient(jedisPooled)
                .keyPrefix("ai-universal-controller-chat-memory:")
                .build();
    }
    /**
     * 💡 [최종 버그 격파: 상속 불가 문제 해결]
     * final 클래스이므로 상속 대신 직접 생성자 호출 방식을 사용합니다.
     * 생성자 파라미터로 필요한 값들을 넣어주면 프레임워크의 Assert 검증 로직이
     * "어? 데이터가 있네?" 하고 통과합니다.
     */

    /**
     * 최근 10개 대화 보존용 챗 메모리
     */
    @Bean
    public ChatMemory chatMemory(RedisChatMemoryRepository redisChatMemoryRepository) {
        return MessageWindowChatMemory.builder()
                .chatMemoryRepository(redisChatMemoryRepository)
                .maxMessages(10)
                .build();
    }

    /**
     * 범용 챗 클라이언트
     */
    @Primary
    @Bean
    public ChatClient chatClient(
            @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection") ChatModel chatModel,
            ChatTools chatTools) {

        return ChatClient.builder(chatModel)
                .defaultTools(chatTools)
                .build();
    }
}