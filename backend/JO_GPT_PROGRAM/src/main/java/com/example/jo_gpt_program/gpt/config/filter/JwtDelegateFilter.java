package com.example.jo_gpt_program.gpt.config.filter;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.stream.Collectors;

@Slf4j
@Component
public class JwtDelegateFilter extends OncePerRequestFilter {

    @Value("${spring.memberSecurity.url}")
    private String memberSecurityUrl;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String refreshToken = null;
        // 쿠키에서 ACCESS_TOKEN 추출
        String token = null;
        if (request.getCookies() != null) {
            for (Cookie cookie : request.getCookies()) {
                if ("ACCESS_TOKEN".equals(cookie.getName())) {
                    token = cookie.getValue();
                    if (token != null) token = token.trim();
                    break;
                }
                if ("REFRESH_TOKEN".equals(cookie.getName())) {
                    refreshToken = cookie.getValue();  // ← 같이 읽기!
                }
            }
        }



        // ✅ RestTemplate 대신 HttpURLConnection 직접 사용
        // → 리다이렉트 없이 헤더 유실 없이 안전하게 요청
        try {
            String validateUrl = memberSecurityUrl + "/auth/validate";
            log.debug("[JwtDelegateFilter] 검증 요청 URL={}", validateUrl);

            URL url = new URL(validateUrl);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setRequestProperty("Authorization", "Bearer " + token );
            conn.setRequestProperty("Cookie", "REFRESH_TOKEN=" + refreshToken);
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setInstanceFollowRedirects(false); // 리다이렉트 절대 따라가지 않음
            conn.setConnectTimeout(3000);
            conn.setReadTimeout(3000);
            conn.getHeaderField("Set-Cookie"); // ← 이걸 읽어서 response 에 반영해야 해요! 쿠키 변경사항 적용


            int statusCode = conn.getResponseCode();
            log.debug("[JwtDelegateFilter] 응답 코드={}", statusCode);

            if (statusCode == 200) {
                // 응답 body 읽기
                String body;
                try (BufferedReader br = new BufferedReader(
                        new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
                    body = br.lines().collect(Collectors.joining());
                }
                log.debug("[JwtDelegateFilter] 응답 body={}", body);

                // JSON → UserInfoDto 파싱 (현재 ClassLoader 기준 → devtools 충돌 없음)
                UserInfoDto userInfo = objectMapper.readValue(body, UserInfoDto.class);
                SecurityContextHolder.getContext().setAuthentication(
                        new UsernamePasswordAuthenticationToken(userInfo, null, userInfo.getAuthorities()));

            } else {
                log.warn("[JwtDelegateFilter] 토큰 검증 실패 statusCode={}", statusCode);
                SecurityContextHolder.clearContext();
            }

            conn.disconnect();

        } catch (Exception e) {
            log.error("[JwtDelegateFilter] 검증 중 에러: {}", e.getMessage());
            SecurityContextHolder.clearContext();
        }

        filterChain.doFilter(request, response);
    }
}