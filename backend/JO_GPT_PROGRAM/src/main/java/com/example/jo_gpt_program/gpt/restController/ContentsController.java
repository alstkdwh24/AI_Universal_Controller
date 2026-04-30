package com.example.jo_gpt_program.gpt.restController;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Set;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import com.example.entitycom.dto.MessageDTO;
import com.example.jo_gpt_program.gpt.dto.MyChatDTO;
import com.example.jo_gpt_program.gpt.dto.ShowChatDTO;
import com.example.jo_gpt_program.gpt.service.AlertService;
import com.example.jo_gpt_program.gpt.service.ContentsService;
import com.example.memberssecurity.member.service.MemberService;
import com.example.memberssecurity.security.config.jwt.JWTUtils;

import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/contents")
@Slf4j
public class ContentsController {

    private final String geminiKey;

    private final JWTUtils jwtUtils;

    @Qualifier("memberService")
    private final MemberService memberService;

    private final ContentsService contentsService;

    private final AlertService alertService;

    // static 메서드 사용 시, 생성자 사용 불가
    public ContentsController(ContentsService contentsService, @Value("${spring.llm.key}") String geminiKey,
            JWTUtils jwtUtils, MemberService memberService, AlertService alertService) {
        this.geminiKey = geminiKey;
        this.contentsService = contentsService;
        this.jwtUtils = jwtUtils;
        this.memberService = memberService;
        this.alertService = alertService;
    }

    @PostMapping("/myContents")
    public ResponseEntity<String> getMyContents(@RequestBody MyChatDTO dto,
            @RequestHeader("Authorization") String authHeader) {
        if (authHeader == null) {
            return ResponseEntity.badRequest().body("Authorization header is missing");
        }
        authHeader = authHeader.replace("Bearer ", "");

        Long memberKey = jwtUtils.getUsername(authHeader);
        String success = contentsService.userInfo(memberKey, dto);

        return ResponseEntity.ok(success);
    }

    @PostMapping("/gptContents")
    public ResponseEntity<String> getGptContents(@RequestBody MyChatDTO dto,
            @RequestHeader(value = "X-Model", defaultValue = "gemini-3-flash-preview") String model,
            @RequestHeader(value = "X-Custom-Prompt", required = false) String customPrompt) {// 프론트에서 보내는 프롬프트
        String decoded = customPrompt != null ? URLDecoder.decode(customPrompt, StandardCharsets.UTF_8) : null;
        String response = contentsService.sendGeminiAI(dto, model, decoded);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/chatRoom")
    public ResponseEntity<Long> createChatRoom(@RequestBody MyChatDTO dto,
            @RequestHeader("Authorization") String authHeader) {
        Long showChatKey = contentsService.createChat(authHeader, dto);
        log.debug("dtosss={}", authHeader);

        return ResponseEntity.ok(showChatKey);
    }

    // 여기서는 엔티티를 넣는 것보다는 DTO필드를 넣으면 된다 조인한 데이터가 필요하다면 DTO에 넣으면 된다.
    @GetMapping("/chattingList")
    public ResponseEntity<Set<ShowChatDTO>> getChattingList(@RequestHeader("Authorization") String authHeader) {
        Set<ShowChatDTO> showChatList = contentsService.getChattingList(authHeader);
        log.debug("showChatListssss={}", showChatList);
        return ResponseEntity.ok(showChatList);
    }

    @PostMapping(value = "/notifications", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter getNotifications(@RequestBody MessageDTO messageDTO,
            @RequestHeader("Authorization") String authHeader) {

        String message = messageDTO.getMessage();
        log.debug("messagesssss={}", message);
        SseEmitter emitter = new SseEmitter(60_000L);
        log.debug("emitter created{}", emitter);

        // 비동기로 메시지 전송
        try {
            if (message != null) {
                emitter.send(SseEmitter.event()
                        .name("notification")
                        .data(message));
            }
        } catch (Exception e) {
            emitter.completeWithError(e);
        }

        log.debug("emitter created{}", emitter);
        return emitter;
    }

    // 학술 검색 + AI 답변
    @PostMapping("/getScholarContents")
    public ResponseEntity<String> postMethodName(@RequestBody MyChatDTO dto,
            @RequestHeader(value = "X-Model", defaultValue = "gemini-3.0-flash") String model,
            @RequestHeader(value = "X-Custom-Prompt", required = false) String customPrompt) {

        String decoded = customPrompt != null ? URLDecoder.decode(customPrompt, StandardCharsets.UTF_8) : null;
        String response = contentsService.sendWithScholar(dto, model, decoded);
        // TODO: process POST request

        return ResponseEntity.ok(response);
    }

    // RAG + 학술 검색 동시 적용
    @PostMapping("/getRagScholarContents")
    public ResponseEntity<String> getGptRagScholarContents(
            @RequestBody MyChatDTO dto,
            @RequestHeader(value = "X-Model", defaultValue = "gemini-3.0-flash") String model,
            @RequestHeader(value = "X-Custom-Prompt", required = false) String customPrompt) {
        String decoded = customPrompt != null ? URLDecoder.decode(customPrompt, StandardCharsets.UTF_8) : null;
        String response = contentsService.sendWithRagAndScholar(dto, model, decoded);
        return ResponseEntity.ok(response);

    }

    // 문서 저장

    @PostMapping("/saveDocument")
    public ResponseEntity<Void> postMethodName(@RequestBody String entity) {
        // TODO: process POST request
        contentsService.saveDocument(entity);
        return ResponseEntity.ok().build();
    }

    // RAG 답변 엔드포인

}
