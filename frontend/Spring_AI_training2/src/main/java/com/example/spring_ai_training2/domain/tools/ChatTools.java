package com.example.spring_ai_training2.domain.tools;

import com.example.spring_ai_training2.domain.dto.MessageDto;
import com.example.spring_ai_training2.domain.entity.Chat;
import com.example.spring_ai_training2.domain.mapper.SpringAiMapper;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.stereotype.Component;

import java.util.List;
@Component
public class ChatTools {

    private final SpringAiMapper springAiMapper;

    public ChatTools(SpringAiMapper springAiMapper) {
        this.springAiMapper = springAiMapper;
    }

    // 날씨 조회 Tool
    @Tool
    public String getWeather(String city){
        return "서울 현재 25도";
    }

    // DB 조회 Tool
    @Tool(description = "채팅 히스토리를 조회합니다")
    public List<Chat> getChatHistory(MessageDto dto, String userId) {
        return springAiMapper.showHistory(dto,userId);
    }

    // 계산 Tool
    @Tool(description = "두 숫자를 더합니다")
    public int add(int a, int b) {
        return a + b;
    }
}
