package com.example.jo_gpt_program.gpt.service;

import com.example.entitycom.entity.connect.ConnectedAccounts;
import com.example.jo_gpt_program.gpt.repository.jpa.ConnectedAccountsRepository;
import com.google.auth.http.HttpCredentialsAdapter;
import com.google.auth.oauth2.AccessToken;
import com.google.auth.oauth2.GoogleCredentials;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class GoogleApiService {

    private final ConnectedAccountsRepository connectedAccountsRepository;

    // memberKey 로 Google AccessToken 꺼내기

    public String getAccessToken(Long memberKey) {
        // db에서 accessToken 꺼내기
        ConnectedAccounts connectedAccounts = connectedAccountsRepository.findByMember_MemberKeyAndProvider(memberKey, "google").orElseThrow(() -> new RuntimeException("Google account not connected"));
        // accessToken 보내기
        return connectedAccounts.getAccessToken();
    }

    // GoogleCredential 생성
    public HttpCredentialsAdapter getCredentials(Long memberKey) {
        // 1단게 : db에서 accessToken 꺼내기 그리고 Google인증 자격증명 객체로 만드는 것
        GoogleCredentials googleCredential = GoogleCredentials.create(new AccessToken(getAccessToken(memberKey), null));
        //인증 객체를 HTTP 요청에 붙일 수 있게 포장
        return new HttpCredentialsAdapter(googleCredential);
    }
}
