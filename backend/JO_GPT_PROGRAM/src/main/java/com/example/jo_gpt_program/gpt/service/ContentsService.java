package com.example.jo_gpt_program.gpt.service;

import com.example.entitycom.entity.chat.ShowChat;
import com.example.entitycom.entity.log.CreateTimeLogs;
import com.example.entitycom.entity.member.Members;
import com.example.entitycom.entity.member.MyChat;
import com.example.jo_gpt_program.gpt.dto.MyChatDTO;
import com.example.jo_gpt_program.gpt.dto.ShowChatDTO;
import com.example.jo_gpt_program.gpt.repository.jpa.CreateTimeRepository;
import com.example.jo_gpt_program.gpt.repository.jpa.MyChatRepository;
import com.example.jo_gpt_program.gpt.repository.jpa.ShowChatRepository;
import com.example.memberssecurity.member.repository.jpa.MemberRepository;
import com.example.memberssecurity.security.config.jwt.JWTUtils;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@SuppressWarnings("ALL")
@Service("contentsService")
@Slf4j
public class ContentsService {

    private final MyChatRepository myChatRepository;
    private final RestTemplate restTemplate;

    private final MemberRepository memberRepository;

    private final ShowChatRepository showChatRepository;

    private final CreateTimeRepository createTimeRepository;

    private final JWTUtils jwtUtils;

    public ContentsService(@Qualifier("myChatRepository") MyChatRepository myChatRepository,
                           RestTemplate restTemplate, MemberRepository memberRepository, ShowChatRepository showChatRepository, CreateTimeRepository createTimeRepository, JWTUtils jwtUtils) {
        this.restTemplate = restTemplate;
        this.myChatRepository = myChatRepository;
        this.memberRepository = memberRepository;
        this.showChatRepository = showChatRepository;
        this.createTimeRepository = createTimeRepository;
        this.jwtUtils = jwtUtils;
    }
    /*유저 정보 불러오기*/

    public String userInfo(Long memberKey, MyChatDTO dto) {
        Optional<Members> members = memberRepository.findByMemberKey(memberKey);
        Members member = members.orElseThrow(() -> new RuntimeException("Member not found with key: " + memberKey));
        log.debug("member={}", member);
        String response = this.myChat(dto, member);
        return response;

    }

    /*내가 적은 치탱 BD 저장*/
    @Transactional
    public String myChat(MyChatDTO dto, Members member) {
        Optional<Members> members = memberRepository.findByMemberKey(member.getMemberKey());
        Optional<ShowChat> showChat = showChatRepository.findShowChatByShowChatKey(dto.getShowChatKey());
        log.debug("showChatTwo={}", showChat);

        Members members1 = members.orElseThrow(() -> new RuntimeException("Member not found with key: " + member.getMemberKey()));
       ShowChat showChat1 = showChat.orElseThrow(() -> new RuntimeException("ShowChat not found for showChatKey: " + dto.getShowChatKey()));
        MyChat chat = MyChat.builder()
                .member(members1)
                .showChat(showChat1)
                .myChatContents(dto.getMyChatContents())
                .myChatImage(dto.getMyChatImage())
                .createTimeLogs(CreateTimeLogs.builder()

                        .build()) // 여기서 builder로 생성만 하면, 위에서 추가한 @CreatedDate가 저장 시점에 시간을 자동 기입한다.
                .build();


        MyChat myChat = myChatRepository.save(chat);

        return myChat.getMyChatContents();
    }

    /*제미나이한테 보낼 메시지랑 제미나이 API연결*/
    public String sendGemini(MyChatDTO dto, String geminiKey) {

        Map<String, Object> body = Map.of("contents",
                List.of(Map.of("parts", List.of(Map.of("text", dto.getMyChatContents())))));

        ResponseEntity<String> response = restTemplate.postForEntity(
                "https://generativelanguage.googleapis.com/v1beta/models/gemini-flash-latest:generateContent?key=" + geminiKey,
                body, String.class);
        log.debug("response gemini :{}", response);
        String body2 = response.getBody();
        if (body2 == null) {
            return "{\"error\": \"No response from Gemini\"}";
        }

        // TODO Auto-generated method stub
        return response.getBody();
    }

    /*채팅방 만드는 메서드*/
    @Transactional
    public Long createChat(String authHeader, MyChatDTO dto) {
        Members members = this.authHeader(authHeader);
// 1. ShowChat 생성 및 '저장' (save 호출!)
        ShowChat showChat = ShowChat.builder()
                .members(members)
                .build();
        ShowChat showChat1 = showChatRepository.save(showChat); // DB에서 키값을 받아옴

        log.debug("showChat1={}", showChat1.getShowChatKey());
        // 2. 이제 키값이 있는 showChat을 MyChat에 연결
        MyChat myChat = MyChat.builder()
                .showChat(showChat1)
                .member(members) // Member 키도 잊지 말고 넣어주세요!
                .myChatContents(dto.getMyChatContents())
                .build();
        myChatRepository.save(myChat);

        CreateTimeLogs createTimeLogs = CreateTimeLogs.builder()
                .showChat(showChat1)
                .build();
        createTimeRepository.save(createTimeLogs);
        log.debug("showChat={}", showChat1);

        return showChat1.getShowChatKey();
    }

    /*JWT 토큰으로 사용자 정보 가져오기*/
    private Members authHeader(String authHeader) {
        if (authHeader == null) {
            throw new IllegalArgumentException("Authorization header is missing");
        }
        authHeader = authHeader.replace("Bearer ", "");
        Long memberKey = jwtUtils.getUsername(authHeader);
        Members members = userInfoTwo(memberKey);
        if (members == null) {
            throw new RuntimeException("Member not Object: " + members);
        }


        return members;
    }

    /*유저 정보 불러오기*/
    private Members userInfoTwo(Long memberKey) {
        Optional<Members> members = memberRepository.findByMemberKey(memberKey);
        Members member = members.orElseThrow(() -> new RuntimeException("Member not found with key: " + memberKey));
        log.debug("member={}", member);
        return member;

    }

    @Transactional
    public Set<ShowChatDTO> getChattingList(String authHeader) {
        Members members = authHeader(authHeader);
        Set<ShowChat> showChats = showChatRepository.findByMembers(members);
        showChats.forEach(chat -> {
            log.debug("showChatKey={}", chat.getShowChatKey());
            log.debug("members={}", chat.getMembers());
            log.debug("createTimeLogs={}", chat.getCreateTimeLogs());
            log.debug("myChat={}", chat.getMyChat());
            log.debug("gptChat={}", chat.getGptChat());
            log.debug("chatAttachment={}", chat.getChatAttachment());
        });

        log.debug("showChatReal:{}", showChats.stream());
        Set<ShowChatDTO> showChatDTOS = showChats.stream().map(chat -> ShowChatDTO.builder()
                .showChatRegistration(chat.getCreateTimeLogs() != null && !chat.getCreateTimeLogs().isEmpty() ? chat.getCreateTimeLogs().iterator().next().getCreatedAt() : null
                ).showMyChatContents(chat.getMyChat() != null && !chat.getMyChat().isEmpty() ? chat.getMyChat().iterator().next().getMyChatContents() : null).build()).collect(Collectors.toSet());
        return showChatDTOS;
    }
}
