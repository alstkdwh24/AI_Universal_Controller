package com.example.spring_ai.domain.openai.service;

import org.springframework.ai.tool.annotation.Tool;

public class ChatTools {

    @Tool(description = "User personal information : name, age, address, phone, etc")
    public UserResponseDTO getUserinfotools(){
        return new UserResponseDTO("김지훈", 15L, "서울 특별시 종로구 청화대로 1", "010-0000-0000");
    }
}
