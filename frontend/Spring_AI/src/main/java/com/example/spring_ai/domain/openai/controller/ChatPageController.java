package com.example.spring_ai.domain.openai.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class ChatPageController {

    @GetMapping("/Chat")
    public String chatPage(){
        return "Chat";
    }
}
