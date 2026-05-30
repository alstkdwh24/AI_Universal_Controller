package com.example.spring_ai.domain.openai.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.springframework.ai.chat.messages.MessageType;

import java.time.LocalDateTime;

@Entity
@Setter
@Getter
public class ChatEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name ="user_id")
    private String userId;

    @Column(columnDefinition = "TEXT")
    private String content;

    @Enumerated(EnumType.STRING)
    private MessageType type;

    @CreationTimestamp
    @Column(updatable = false, name = "createdAt")
    private LocalDateTime createdAt;
}
