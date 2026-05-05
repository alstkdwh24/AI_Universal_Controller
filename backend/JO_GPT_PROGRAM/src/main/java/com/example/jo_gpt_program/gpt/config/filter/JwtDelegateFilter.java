package com.example.jo_gpt_program.gpt.config.filter;

import java.io.IOException;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
public class JwtDelegateFilter extends OncePerRequestFilter {

    @Value("${spring.memberSecurity.url}")
    private String memberSecurityUrl;

    @Value("${spring.joGptProgram.url}")
    private String joGptProgramUrl;

    private final RestTemplate restTemplate;

    public JwtDelegateFilter(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    // MembersSecurity에 토큰 검증 요청
    private HttpHeaders createHeaders(String token) {
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", token);
        return headers;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        // ACCESS_TOKEN 쿠키에서 토큰 추출
        String token = null;
        if (request.getCookies() != null) {
            for (Cookie cookie : request.getCookies()) {
                if ("ACCESS_TOKEN".equals(cookie.getName())) {
                    token = cookie.getValue();
                    if (token != null) token = token.trim();
                    break;
                }
            }
        }

        // 토큰 없으면 그냥 통과
        if (token == null || token.isEmpty()) {
            filterChain.doFilter(request, response);
            return;
        }

        // MembersSecurity에 토큰 검증 위임
        try {
            ResponseEntity<UserInfoDto> responses = restTemplate.exchange(
                    memberSecurityUrl + "/auth/validate",
                    HttpMethod.GET,
                    new HttpEntity<>(createHeaders("Bearer " + token)),
                    UserInfoDto.class);

            if (responses.getStatusCode().is2xxSuccessful() && responses.getBody() != null) {
                UserInfoDto userInfo = responses.getBody();
                SecurityContextHolder.getContext().setAuthentication(
                        new UsernamePasswordAuthenticationToken(userInfo, null, userInfo.getAuthorities()));
            }
        } catch (Exception e) {
            // 검증 실패 시 인증 정보 없이 통과 (Security 설정에서 401 처리)
            SecurityContextHolder.clearContext();
        }

        filterChain.doFilter(request, response);
    }
}
