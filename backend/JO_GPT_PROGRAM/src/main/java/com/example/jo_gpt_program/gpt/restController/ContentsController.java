package com.example.jo_gpt_program.gpt.restController;

import com.example.entitycom.dto.MessageDTO;
import com.example.jo_gpt_program.gpt.config.filter.UserInfoDto;
import com.example.jo_gpt_program.gpt.dto.ChatMessageDTO;
import com.example.jo_gpt_program.gpt.dto.MyChatDTO;
import com.example.jo_gpt_program.gpt.dto.ShowChatDTO;
import com.example.jo_gpt_program.gpt.service.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Set;

@RestController
@RequestMapping("/contents")
@Slf4j
public class ContentsController {

    private final String geminiKey;

    private final ShowChatService showChatService;

    private final MyAuthService myAuthService;

    private final GeminiService geminiService;
    private final RagService ragService;
    private final ScholarSearchService scholarSearchService;
    private final ChatMysqlService chatMysqlService;

    // static 메서드 사용 시, 생성자 사용 불가
    public ContentsController( @Value("${spring.llm.key}") String geminiKey,
                              ShowChatService showChatService, MyAuthService myAuthService, GeminiService geminiService, RagService ragService, ScholarSearchService scholarSearchService, ChatMysqlService chatMysqlService) {

        this.geminiKey = geminiKey;
        this.showChatService = showChatService;

        this.myAuthService = myAuthService;
        this.geminiService = geminiService;
        this.ragService = ragService;
        this.scholarSearchService = scholarSearchService;
        this.chatMysqlService = chatMysqlService;
    }

    @PostMapping("/myContents")
    public ResponseEntity<String> getMyContents(@RequestBody MyChatDTO dto  ) {
        // SecurityContextHolder -> Spring Security가 인증 정보를 저장하는 전역 저장소
        // .getContext() -> 현재 요청의 보안 컨텍스트를 가져옴
        // .getAUthentication() -> 인증 객체 (JWT 검증 후 저장된 것)
        // .getPrincipal() -> 인증된 사용자 주체 (UserInfoDto로 캐스팅) 유저 정보를 꺼내는 것
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        if (!(principal instanceof UserInfoDto userInfo)) {
            return ResponseEntity.status(401).build();
        }
        Long memberKey = Long.parseLong(userInfo.getMemberId());

        String success = chatMysqlService.userInfo(memberKey, dto);
        return ResponseEntity.ok(success);
    }

    /* Gemini 호출 — Authorization으로 멤버 식별, DB의 활성 프롬프트 자동 적용 */
    @PostMapping("/gptContents")
    public ResponseEntity<String> getGptContents(@RequestBody MyChatDTO dto,
                                                 @RequestHeader(value = "X-Model", defaultValue = "gemini-3.1-flash-image-preview") String model,
                                                 @RequestHeader(value = "X-Custom-Prompt", required = false) String customPrompt) {// 프론트에서 보내는 프롬프트
        String decoded = customPrompt != null ? URLDecoder.decode(customPrompt, StandardCharsets.UTF_8) : null;
        // 이게 null이 아니라면 디코딩해서 decoded에 저장, null이라면 decoded도 null
        String response = geminiService.sendGeminiAI(dto, model, decoded);

        return ResponseEntity.ok(response);
    }

    // 채팅방 생성 API
    @PostMapping("/chatRoom")
    public ResponseEntity<Long> createChatRoom(@RequestBody MyChatDTO dto) {
        Long showChatKey = showChatService.createChat(dto);

        return ResponseEntity.ok(showChatKey);
    }

    // 여기서는 엔티티를 넣는 것보다는 DTO필드를 넣으면 된다 조인한 데이터가 필요하다면 DTO에 넣으면 된다.
    // 채팅방 목록 조회 API
    @GetMapping("/chattingList")
    public ResponseEntity<Set<ShowChatDTO>> getChattingList() {
        Set<ShowChatDTO> showChatList = chatMysqlService.getChattingList();
        return ResponseEntity.ok(showChatList);
    }

    /* 채팅방 대화 내역 조회 */
    @GetMapping("/chatRoom/{showChatKey}/messages")
    public ResponseEntity<List<ChatMessageDTO>> getChatHistory(
            @PathVariable Long showChatKey) {
        // showChatKey를 기반으로 채팅방 대화 내역을 조회하는 메서드이다.
        List<ChatMessageDTO> messages = showChatService.getChatMessages(showChatKey);
        return ResponseEntity.ok(messages);
    }

    /* 채팅방 삭제 */
    @DeleteMapping("/chatRoom/{showChatKey}")
    public ResponseEntity<Void> deleteChatRoom(
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @PathVariable Long showChatKey) {
        // 채팅방 삭제 메서드
        showChatService.deleteChat(authHeader, showChatKey);
        return ResponseEntity.ok().build();
    }

    // SSE를 이용한 실시간 알림 API AI 답변이 오면 프론트에 실시간으로 알림을 보내주는 API
    @PostMapping(value = "/notifications", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    // sse 란 서버 -> 클라이언트 방향으로 단방향 실시간 데이터 스트림을 전송하는 기숭입니다.
    public SseEmitter getNotifications(@RequestBody MessageDTO messageDTO) {
        String message = messageDTO.getMessage();
        SseEmitter emitter = new SseEmitter(60_000L); // 60초 타임아웃
        try {
            if (message != null) {
                emitter.send(SseEmitter.event()
                        .name("notification")
                        .data(message));
            }
            emitter.complete(); // ← 추가!

        } catch (Exception e) {
            emitter.completeWithError(e);
        }
        return emitter; // 연결 유지하면서 클라이언트로 실시간 알림을 보낼 수 있다.
    }

    // 학술 검색 + AI 답변
    @PostMapping("/getScholarContents")
    public ResponseEntity<String> postMethodName(@RequestBody MyChatDTO dto,
                                                 @RequestHeader(value = "X-Model", defaultValue = "gemini-3.0-flash") String model,

                                                 @RequestHeader(value = "X-Custom-Prompt", required = false) String customPrompt) {
        // 프론트에서 보내는 프롬프트가 있을수도 있고 없을 수도 있기 때문에 required = false로 저장
        String decoded = customPrompt != null ? URLDecoder.decode(customPrompt, StandardCharsets.UTF_8) : null;
        // 이게 null이 아니라면 디코딩해서 decoded에 저정, null이면 null
        String response = scholarSearchService.sendWithScholar(dto, model, decoded);
        // AI 답변 추출 학술 답변
        // TODO: process POST request

        return ResponseEntity.ok(response);
    }

    // RAG + 학술 검색 동시 적용

    @PostMapping("/getRagScholarContents")
    public ResponseEntity<String> getGptRagScholarContents(
            @RequestBody MyChatDTO dto,
            @RequestHeader(value = "X-Model", defaultValue = "gemini-3.1-flash-image-preview") String model,
            @RequestHeader(value = "X-Custom-Prompt", required = false) String customPrompt) {
        // 프롬프트 적용
        String decoded = customPrompt != null ? URLDecoder.decode(customPrompt, StandardCharsets.UTF_8) : null;
        // RAD + 학술 검색 동시 적용
        String response = scholarSearchService.sendWithRagAndScholar(dto, model, decoded);
        return ResponseEntity.ok(response);

    }

    // 문서 저장

    @PostMapping("/saveDocument")
    public ResponseEntity<Void> postMethodName(@RequestBody String entity) {
        // TODO: process POST request
        // 문서 저장 API
        ragService.saveDocument(entity);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/documents/search")
    public ResponseEntity<String> findDocument(@RequestBody Map<String, String> body){
        try {
            String context = ragService.findDocument(body.get("query"));
            return ResponseEntity.ok(context);
        } catch (Exception e) {
            return ResponseEntity.ok("");
        }
    }

}
