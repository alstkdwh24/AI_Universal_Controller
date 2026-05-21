package com.example.spring_ai_training2.domain.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
public class PageController {

    @GetMapping("/Chat")
    public String chat() {
        return "Chat";
    }
}
