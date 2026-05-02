package com.example.jo_gpt_program.gpt.config;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.ChatMemoryRepository;
import org.springframework.ai.chat.memory.InMemoryChatMemoryRepository;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AiConfig {
    // 대화 메모리 관리
    // ChatMemoryRepository 대화 기록을 어디에 저장할지 정의하는 인터페이스
    // InMemoryChatMemoryRepository 그 구현체로, 서버 메모리(ram)에 대화기록을 저장
    @Bean
    public InMemoryChatMemoryRepository chatMemoryRepository() {

        return new InMemoryChatMemoryRepository();
    }

    // 대화 기록을 최대 10개 메시지까지 유지하는 메모리 빈
    @Bean
    public ChatMemory chatMemory(ChatMemoryRepository chatMemoryRepository) {
        return MessageWindowChatMemory.builder()
                .chatMemoryRepository(chatMemoryRepository)
                .maxMessages(10)
                .build();
    }

    @Bean
    public ChatClient chatClient(ChatClient.Builder builder, ChatMemory chatMemory) {
        return builder
                .defaultSystem("당신은 JO-GPT 어시스턴트입니다. 항상 한국어로 친절하게 답변하세요.")
                .defaultAdvisors(MessageChatMemoryAdvisor.builder(chatMemory).build())
                .build();
    }
}
