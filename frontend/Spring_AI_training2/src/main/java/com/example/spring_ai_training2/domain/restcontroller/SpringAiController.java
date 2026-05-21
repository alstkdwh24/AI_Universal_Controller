package com.example.spring_ai_training2.domain.restcontroller;

import com.example.spring_ai_training2.domain.dto.MessageDto;
import com.example.spring_ai_training2.domain.entity.Chat;
import com.example.spring_ai_training2.domain.service.DocumentService;
import com.example.spring_ai_training2.domain.service.SpringAiService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;


import java.util.List;
import java.util.Random;

@RestController
@RequestMapping("/chat")
public class SpringAiController {
    private final DocumentService documentService;

    private final SpringAiService springAiService;

    public SpringAiController(DocumentService documentService, SpringAiService springAiService) {
        this.documentService = documentService;
        this.springAiService = springAiService;
    }

    @PostMapping("/stream")
    public ResponseEntity<String> message(@RequestBody MessageDto messageDto){
        System.out.println("받은 데이터: " + messageDto); // ← 이거 추가!

        Random random = new Random();
        int randomNumber = random.nextInt(10000);
        String messageFlux = springAiService.insertMessage(messageDto, randomNumber);
        return ResponseEntity.ok(messageFlux);
    }

    @GetMapping("/history/{userId}")
    public ResponseEntity<List<Chat>> history(@RequestBody MessageDto dto,@PathVariable String userId){
        List<Chat> history = springAiService.showHistory(dto,userId);

        return ResponseEntity.ok(history);
    }

    @PostMapping("/document")
    public void saveDocument(@RequestBody String text){
        documentService.saveDocument(text);
    }
}
