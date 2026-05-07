package com.example.jo_gpt_program.gpt.config;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.InMemoryChatMemoryRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

// AI 관련 설정을 하는 클래스
@Configuration
public class AiConfig {

    // 기본 시스템 프롬프트
    // 여기서 대화 메모리를 관리하는 ChatMemory 빈을 생성하여, ChatClient에서 사용할 수 있도록 합니다.
    // ImMemoryChatMemoryRepository는 간단한 구현체로, 실제 운영에서는 Redis나 데이터베이스 등을 이용한 구현체로 교체
    // 가능하다.
    @Bean
    public InMemoryChatMemoryRepository chatMemoryRepository() {
        return new InMemoryChatMemoryRepository();
    }

    // 기본 시스템 프롬프트
    // chatClient는 Spring AI에서 제공하는 AI모델과 통신하는 클라이언트입니다. 그러니까 일반 RestTemplate 같은 것
    // 입니다.
    @Bean
    public ChatClient chatClient(ChatClient.Builder builder, ChatMemory chatMemory) {
        return builder
                // AI에게 기본 역할 부여
                .defaultSystem("당신은 JO-GPT 어시스턴트입니다. 항상 한국어로 친절하게 답변하세요.")
                // 대화 히스토리를 자동으로 AI에게 전달 ( 이전 대화 내역)
                .defaultAdvisors(MessageChatMemoryAdvisor.builder(chatMemory).build())
                .build();
    }
}
