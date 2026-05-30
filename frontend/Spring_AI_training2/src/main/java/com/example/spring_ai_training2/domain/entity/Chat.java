package com.example.spring_ai_training2.domain.entity;

import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;

@Getter
@NoArgsConstructor
public class Chat {
    @Id
    private Long id;
    private String content;

    public Chat(String content) {
        this.content = content;
    }
}
