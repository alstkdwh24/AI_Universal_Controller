package com.example.jo_gpt_program.gpt.restController;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Set;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import com.example.entitycom.dto.MessageDTO;
import com.example.jo_gpt_program.gpt.dto.ChatMessageDTO;
import com.example.jo_gpt_program.gpt.dto.MyChatDTO;
import com.example.jo_gpt_program.gpt.dto.ShowChatDTO;
import com.example.jo_gpt_program.gpt.service.AlertService;
import com.example.jo_gpt_program.gpt.service.ContentsService;
import com.example.memberssecurity.member.service.MemberService;
import com.example.memberssecurity.security.config.jwt.JWTUtils;




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

    /* Gemini 호출 — Authorization으로 멤버 식별, DB의 활성 프롬프트 자동 적용 */
    @PostMapping("/gptContents")
    public ResponseEntity<String> getGptContents(@RequestBody MyChatDTO dto,
            @RequestHeader(value = "X-Model", defaultValue = "gemini-3.1-flash-image-preview") String model,
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

    @GetMapping("/chattingList")
    public ResponseEntity<Set<ShowChatDTO>> getChattingList(@RequestHeader("Authorization") String authHeader) {
        Set<ShowChatDTO> showChatList = contentsService.getChattingList(authHeader);
        log.debug("showChatListssss={}", showChatList);
        return ResponseEntity.ok(showChatList);
    }

    /* 채팅방 대화 내역 조회 */
    @GetMapping("/chatRoom/{showChatKey}/messages")
    public ResponseEntity<List<ChatMessageDTO>> getChatHistory(
            @RequestHeader("Authorization") String authHeader,
            @PathVariable Long showChatKey) {
        List<ChatMessageDTO> messages = contentsService.getChatHistory(showChatKey, authHeader);
        return ResponseEntity.ok(messages);
    }

    /* 채팅방 삭제 */
    @DeleteMapping("/chatRoom/{showChatKey}")
    public ResponseEntity<Void> deleteChatRoom(
            @RequestHeader("Authorization") String authHeader,
            @PathVariable Long showChatKey) {
        contentsService.deleteChat(authHeader, showChatKey);
        return ResponseEntity.ok().build();
    }

    @PostMapping(value = "/notifications", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter getNotifications(@RequestBody MessageDTO messageDTO,
            @RequestHeader("Authorization") String authHeader) {
        String message = messageDTO.getMessage();
        log.debug("messagesssss={}", message);
        SseEmitter emitter = new SseEmitter(60_000L);
        try {
            if (message != null) {
                emitter.send(SseEmitter.event()
                        .name("notification")
                        .data(message));
            }
        } catch (Exception e) {
            emitter.completeWithError(e);
        }
        return emitter;
    }

    /* 학술 검색 + AI 답변 */
    @PostMapping("/getScholarContents")
    public ResponseEntity<String> postMethodName(@RequestBody MyChatDTO dto,
            @RequestHeader(value = "X-Model", defaultValue = "gemini-3.1-flash-image-preview") String model,
            @RequestHeader(value = "X-Custom-Prompt", required = false) String customPrompt) {

        String decoded = customPrompt != null ? URLDecoder.decode(customPrompt, StandardCharsets.UTF_8) : null;
        String response = contentsService.sendWithScholar(dto, model, decoded);
        // TODO: process POST request


        return ResponseEntity.ok(response);
    }

    /* RAG + 학술 검색 동시 적용 */
    @PostMapping("/getRagScholarContents")
    public ResponseEntity<String> getGptRagScholarContents(
            @RequestBody MyChatDTO dto,
            @RequestHeader(value = "X-Model", defaultValue = "gemini-3.1-flash-image-preview") String model,
            @RequestHeader(value = "X-Custom-Prompt", required = false) String customPrompt) {
        String decoded = customPrompt != null ? URLDecoder.decode(customPrompt, StandardCharsets.UTF_8) : null;
        String response = contentsService.sendWithRagAndScholar(dto, model, decoded);

        return ResponseEntity.ok(response);
    }

    @GetMapping("/chatRoom/{key}/messages")
    public ResponseEntity<List<ChatMessageDTO>> getChatMessages(@PathVariable Long key) {
        List<ChatMessageDTO> messages = contentsService.getChatMessages(key);
        return ResponseEntity.ok(messages);
    }

    // 문서 저장


    @PostMapping("/saveDocument")
    public ResponseEntity<Void> saveDocument(@RequestBody String entity) {
        contentsService.saveDocument(entity);
        return ResponseEntity.ok().build();
    }

    // ----------------------------- 프롬프트 관리 -----------------------------

    /* 내 프롬프트 목록 조회 */
    @GetMapping("/myPrompts")
    public ResponseEntity<List<MemberPromptDTO>> getMyPrompts(
            @RequestHeader("Authorization") String authHeader) {
        List<MemberPromptDTO> prompts = contentsService.getMyPrompts(authHeader);
        return ResponseEntity.ok(prompts);
    }

    /* 새 프롬프트 저장 */
    @PostMapping("/myPrompts")
    public ResponseEntity<Void> saveMyPrompt(
            @RequestHeader("Authorization") String authHeader,
            @RequestBody MemberPromptDTO dto) {
        contentsService.saveMyPrompt(authHeader, dto);
        return ResponseEntity.ok().build();
    }

    /* 프롬프트 삭제 */
    @DeleteMapping("/myPrompts/{promptKey}")
    public ResponseEntity<Void> deleteMyPrompt(
            @RequestHeader("Authorization") String authHeader,
            @PathVariable Long promptKey) {
        contentsService.deleteMyPrompt(authHeader, promptKey);
        return ResponseEntity.ok().build();
    }

    /* 활성 프롬프트 변경 */
    @PatchMapping("/myPrompts/{promptKey}/activate")
    public ResponseEntity<Void> activateMyPrompt(
            @RequestHeader("Authorization") String authHeader,
            @PathVariable Long promptKey) {
        contentsService.activateMyPrompt(authHeader, promptKey);
        return ResponseEntity.ok().build();
    }
}
