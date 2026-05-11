package com.example.spring_ai.domain.openai.controller;

import com.example.spring_ai.domain.openai.dto.CityResponseDTO;
import com.example.spring_ai.domain.openai.entity.ChatEntity;
import com.example.spring_ai.domain.openai.service.ChatService;
import com.example.spring_ai.domain.openai.service.OpenAiService;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

import java.util.List;
import java.util.Map;

@RestController
public class ChatController {

    private final OpenAiService openAiService;
    private final ChatService chatService;
    public ChatController(OpenAiService openAiService, ChatService chatService) {
        this.openAiService = openAiService;
        this.chatService = chatService;
    }

    @PostMapping("/chat")
    public CityResponseDTO chat(@RequestBody Map<String, String> request){
        return openAiService.generate(request.get("text"));
    }

    @PostMapping("/chat/stream")
    public Flux<String> streamChat(@RequestBody Map<String, String> request){
        return openAiService.generateStream(request.get("text"));

    }

    @PostMapping("/chat/history/{userId}")
    public List<ChatEntity> readAllChats(@PathVariable String userId){
        return chatService.readAllChats(userId);
    }
}
