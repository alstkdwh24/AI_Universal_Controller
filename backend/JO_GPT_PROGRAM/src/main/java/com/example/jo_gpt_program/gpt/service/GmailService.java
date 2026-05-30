package com.example.jo_gpt_program.gpt.service;

import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.gmail.Gmail;
import com.google.api.services.gmail.model.Message;
import com.google.api.services.gmail.model.MessagePartHeader;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.Base64;
import java.util.List;

@Service
@RequiredArgsConstructor
public class GmailService {

    private final GoogleApiService googleApiService;
    // 메일 조회 메서드
    public List<String> getRecentEmails(Long memberKey) throws Exception {
        Gmail gmail = new Gmail.Builder(
                // 통신이 통하는 길을 만들고
                GoogleNetHttpTransport.newTrustedTransport(),
                // JSON을 java 객체로 저장을 하고
                GsonFactory.getDefaultInstance(),
                // 입장권을 받는다 신분증 같은거
                googleApiService.getCredentials(memberKey)
                // google서버에 이름표
        ).setApplicationName("jo-gpt").build();
            // 내 Gmail 계정 접근
        return gmail.users()
                .messages() // 메일함 접근
                .list("me") // "me" = 현재 로그인한 사용자의 메일 목록 요청
                .setMaxResults(10L).execute() // 최대 10개만 가져오는 것
                .getMessages().stream() // 응답에서 메일 ID 목록 꺼내기 하나씩 처리하기 위해 스트림으로 변환
                .map(m -> { // m = 메일 ID 하나 여기서 ID로 실제 메일 상세정보를 다시 요청
                    try {
                        Message msg = gmail.users().messages().get("me", m.getId()) // 특정 메일 ID의 상세 정보 요청
                                .setFormat("metadata") // 메일 전체 말고 헤더 정보만 가져와
                                .setMetadataHeaders(List.of("Subject")) // metadata 중에서도 "Subject(제목)" 헤더만 가져와
                                .execute();

                        return msg.getPayload() //메일 내용 객체 꺼내기
                                .getHeaders().stream()// 헤더 목록 꺼내기 그리고 스트림으로 전환
                                .filter(h -> "Subject".equals(h.getName())) // 헤더 중에 Subject인 것만 필터링
                                .map(MessagePartHeader::getValue)
                                .findFirst().orElse("(제목 없음)"); // 헤더 값을 map에서 꺼내가 없으면 (제목 없음) 기본값
                    } catch (Exception e) {
                        return "오류";
                    }
                }).toList();
    }

    // 메일 발송
    public void sendEmail(Long memberKey, String to, String subject, String body) throws GeneralSecurityException, IOException {
        Gmail gmail = new Gmail.Builder(
        GoogleNetHttpTransport.newTrustedTransport(),
                GsonFactory.getDefaultInstance(),
                googleApiService.getCredentials(memberKey)
        ).setApplicationName("jo-gpt").build(); // gmail 객체 완성 입장권

        String rawEmail = "To: " + to + "\r\nSubject: " + subject + "\r\nContent-Type: text/plain; charset=utf-8\r\n\r\n" + body;
        String encoded = Base64.getUrlEncoder().encodeToString(rawEmail.getBytes(java.nio.charset.StandardCharsets.UTF_8));
        Message message = new Message().setRaw(encoded);
        gmail.users().messages().send("me", message).execute(); // 구글 메일 보내기
    }
}
