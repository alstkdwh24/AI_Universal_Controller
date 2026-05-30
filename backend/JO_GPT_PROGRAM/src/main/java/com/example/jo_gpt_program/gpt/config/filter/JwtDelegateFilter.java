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

    // ✅ /connect/** 경로는 토큰 검증 건너뜀
    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        return path.startsWith("/connect/");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String refreshToken = null;
        String token = null;
        if (request.getCookies() != null) {
            for (Cookie cookie : request.getCookies()) {
                if ("ACCESS_TOKEN".equals(cookie.getName())) {
                    token = cookie.getValue();
                    if (token != null) token = token.trim();
                    break;
                }
                if ("REFRESH_TOKEN".equals(cookie.getName())) {
                    refreshToken = cookie.getValue();
                }
            }
        }

        try {
            String validateUrl = memberSecurityUrl + "/auth/validate";
            log.debug("[JwtDelegateFilter] 검증 요청 URL={}", validateUrl);

            URL url = new URL(validateUrl);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setRequestProperty("Authorization", "Bearer " + token);
            conn.setRequestProperty("Cookie", "REFRESH_TOKEN=" + refreshToken);
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setInstanceFollowRedirects(false);
            conn.setConnectTimeout(3000);
            conn.setReadTimeout(3000);
            int statusCode = conn.getResponseCode();

            String setCookie = conn.getHeaderField("Set-Cookie");
            if (setCookie != null) {
                response.setHeader("Set-Cookie", setCookie);
                log.debug("[JwtDelegateFilter] 새 쿠키 브라우저에 전달: {}", setCookie);
            }
            log.debug("[JwtDelegateFilter] 응답 코드={}", statusCode);

            if (statusCode == 200) {
                String body;
                try (BufferedReader br = new BufferedReader(
                        new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
                    body = br.lines().collect(Collectors.joining());
                }
                log.debug("[JwtDelegateFilter] 응답 body={}", body);

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
