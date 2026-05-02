package com.example.jo_gpt_program.gpt.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@AllArgsConstructor
@NoArgsConstructor
public class ChatMessageDTO {
    private String role;    // "user" 또는 "ai"
    private String content;
    private Long key;       // TSID 기반 정렬 키
}