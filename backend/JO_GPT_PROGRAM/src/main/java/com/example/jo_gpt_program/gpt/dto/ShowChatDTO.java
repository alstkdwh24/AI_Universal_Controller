package com.example.jo_gpt_program.gpt.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ShowChatDTO {

    private String showMyChatContents;


    private LocalDateTime showChatRegistration;





}
