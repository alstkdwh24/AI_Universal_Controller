package com.example.spring_ai.domain.openai.service;

import com.example.spring_ai.domain.openai.entity.ChatEntity;
import com.example.spring_ai.domain.openai.repository.ChatRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class ChatService {

    private final ChatRepository chatRepository;
    public ChatService(ChatRepository chatRepository) {
        this.chatRepository = chatRepository;
    }

    @Transactional(readOnly = true) // 트렌젝션은 db작업이 필요할 때만 필요
    public List<ChatEntity> readAllChats(String userId){
        return chatRepository.findByUserIdOrderByCreatedAtAsc(userId);
    }
}
