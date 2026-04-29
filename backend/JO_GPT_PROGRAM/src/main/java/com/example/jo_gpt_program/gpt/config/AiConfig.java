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
    @Bean
    public InMemoryChatMemoryRepository chatMemory() {
        return new InMemoryChatMemoryRepository();
    }
    // 기본 시스템 프롬프트

    @Bean
    public ChatClient chatClient(ChatClient.Builder builder, ChatMemory chatMemory) {
        return builder
                .defaultSystem("당신은 JO-GPT 어시스턴트입니다. 항상 한국어로 친절하게 답변하세요.")
                .defaultAdvisors(MessageChatMemoryAdvisor.builder(chatMemory).build())// 대화 메모리를 활용하는 어드바이저 추가
                .build();
    }
}
