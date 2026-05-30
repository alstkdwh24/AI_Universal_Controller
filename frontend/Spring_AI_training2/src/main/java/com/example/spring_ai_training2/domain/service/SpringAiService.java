package com.example.spring_ai_training2.domain.service;

import com.example.spring_ai_training2.config.RedisConfig;
import com.example.spring_ai_training2.domain.dto.MessageDto;
import com.example.spring_ai_training2.domain.entity.Chat;
import com.example.spring_ai_training2.domain.mapper.SpringAiMapper;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.client.advisor.vectorstore.QuestionAnswerAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.google.genai.GoogleGenAiChatOptions;
import org.springframework.ai.vectorstore.elasticsearch.ElasticsearchVectorStore;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.List;

@Service("springAiService")

public class SpringAiService {

    private static final String conversationId = "memory:";

    private final SpringAiMapper springAiMapper;

    private final ChatClient chatClient;

    private final RedisTemplate<String, Object> redisTemplate;
    private final ChatMemory chatMemory;
    private final ElasticsearchVectorStore vectorStore;


    public SpringAiService(SpringAiMapper springAiMapper, ChatClient chatClient, RedisConfig redisConfig, RedisTemplate<String, Object> redisTemplate, ChatMemory chatMemory, ElasticsearchVectorStore vectorStore) {
        this.springAiMapper = springAiMapper;
        this.chatClient = chatClient;
        this.redisTemplate = redisTemplate;
        this.chatMemory = chatMemory;
        this.vectorStore = vectorStore;
    }

    public String insertMessage(MessageDto messageDto, int randomNumber) {
        String conversationId = "memory:" + messageDto.userId();

        // ✅ 이미 주입된 chatClient 그냥 쓰기
        String answer = chatClient.prompt()
                .advisors(a -> a
                        .advisors(MessageChatMemoryAdvisor.builder(chatMemory).build(),
                        QuestionAnswerAdvisor.builder(vectorStore).build())
                        .param("chat_memory_conversation_id", conversationId)
                )
                .system("당신은 친절한 AI어시스턴트 입니다. 항상 한국어로 답변하센요.")

                .user(messageDto.content())
                .call()
                .content();
        int result = springAiMapper.insertMessage(messageDto, randomNumber);

        if (result == 1) {
            redisTemplate.opsForValue().set("message:" + randomNumber, messageDto);
            return answer;
        } else {
            return "fail";
        }
    }

    public List<Chat> showHistory(MessageDto dto, String userId) {

        return springAiMapper.showHistory(dto,userId);
    }
}
