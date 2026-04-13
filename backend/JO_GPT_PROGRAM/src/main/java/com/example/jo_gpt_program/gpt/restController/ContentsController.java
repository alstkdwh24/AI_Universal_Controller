package com.example.jo_gpt_program.gpt.restController;

import com.example.entitycom.dto.MessageDTO;
import com.example.jo_gpt_program.gpt.dto.MyChatDTO;
import com.example.jo_gpt_program.gpt.dto.ShowChatDTO;
import com.example.jo_gpt_program.gpt.service.AlertService;
import com.example.jo_gpt_program.gpt.service.ContentsService;
import com.example.memberssecurity.member.service.MemberService;
import com.example.memberssecurity.security.config.jwt.JWTUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.Set;

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
    public ContentsController(ContentsService contentsService, @Value("${spring.llm.key}") String geminiKey, JWTUtils jwtUtils, MemberService memberService, AlertService alertService) {
        this.geminiKey = geminiKey;
        this.contentsService = contentsService;
        this.jwtUtils = jwtUtils;
        this.memberService = memberService;
        this.alertService = alertService;
    }

    @PostMapping("/myContents")
    public ResponseEntity<String> getMyContents(@RequestBody MyChatDTO dto, @RequestHeader("Authorization") String authHeader) {
        if (authHeader == null) {
            return ResponseEntity.badRequest().body("Authorization header is missing");
        }
        authHeader = authHeader.replace("Bearer ", "");

        Long memberKey = jwtUtils.getUsername(authHeader);
        String success = contentsService.userInfo(memberKey, dto);

        return ResponseEntity.ok(success);
    }

    @PostMapping("/gptContents")
    public ResponseEntity<String> getGptContents(@RequestBody MyChatDTO dto) {
        String response = contentsService.sendGemini(dto, geminiKey);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/chatRoom")
    public ResponseEntity<Long> createChatRoom(@RequestBody MyChatDTO dto, @RequestHeader("Authorization") String authHeader) {
        Long showChatKey=contentsService.createChat(authHeader, dto);
        log.debug("dtosss={}", authHeader);

        return ResponseEntity.ok(showChatKey);
    }

    //여기서는 엔티티를 넣는 것보다는 DTO필드를 넣으면 된다 조인한 데이터가 필요하다면 DTO에 넣으면 된다.
    @GetMapping("/chattingList")
    public ResponseEntity<Set<ShowChatDTO>> getChattingList(@RequestHeader("Authorization") String authHeader) {
        Set<ShowChatDTO> showChatList = contentsService.getChattingList(authHeader);
        log.debug("showChatListssss={}", showChatList);
        return ResponseEntity.ok(showChatList);
    }

    @PostMapping(value = "/notifications" , produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter getNotifications(@RequestBody  MessageDTO messageDTO, @RequestHeader("Authorization") String authHeader) {

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

}
