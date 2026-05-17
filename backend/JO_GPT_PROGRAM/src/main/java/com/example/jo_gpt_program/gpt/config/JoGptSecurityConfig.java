package com.example.jo_gpt_program.gpt.config;

import com.example.jo_gpt_program.gpt.config.filter.JwtDelegateFilter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;

import java.util.Arrays;
import java.util.Collections;

// jo-gpt-program 자체 Security 설정
@Configuration
@EnableWebSecurity
public class JoGptSecurityConfig {

    @Value("${spring.memberSecurity.url:https://agentcloudllm.me}")
    private String memberSecurityUrl;

    @Value("${spring.joGptProgram.url:https://agentcloudllm.me}")
    private String joGptProgramUrl;

    @Value("${spring.frontend.url:https://agentcloudllm.me}")
    private String frontendUrl;

    private final JwtDelegateFilter jwtDelegateFilter;

    public JoGptSecurityConfig(JwtDelegateFilter jwtDelegateFilter) {
        this.jwtDelegateFilter = jwtDelegateFilter;
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        return request -> {
            CorsConfiguration cors = new CorsConfiguration();
            // 어떤 cross-origin 요청을 허용할지 규칙을 담는 객체
            cors.setAllowedOrigins(Arrays.asList(
                    memberSecurityUrl,
                    joGptProgramUrl,
                    frontendUrl,
                    "http://localhost:5173",
                    "http://agentcloudllm.me",
                    "https://agentcloudllm.me"));
            cors.setAllowedMethods(Collections.singletonList("*"));
            // 모든 메서드 허용
            cors.setAllowedHeaders(Arrays.asList(
                    "Authorization", "Content-Type", "Cache-Control",
                    "X-Requested-With", "X-Model", "X-Custom-Prompt", "X-NCP-APIGW-API-KEY-ID", "X-NCP-APIGW-API-KEY"));
            // 이 헤더들 허용
            cors.setAllowCredentials(true);
            // 쿠키 허용
            cors.setExposedHeaders(Arrays.asList("Authorization", "Set-Cookie"));
            // 클라이언트에서 이 헤더들 접근 허용
            cors.setMaxAge(3600L);
            // 브라우저 보안 정책상 cross-origin 요청 시 preflight 를 먼저 보내야 하는데,
            // 매 요청마다 보내면 네트워크가 너무 많이 사용하기 때문에 1시간에 한번만 요청을 보내 성능을 높이기 위해 설정한 것
            // 하지만 새로고침이나 탭을 닫았다가 열면 캐시가 초기화되어 preflight 요청을 다시 한번 보내게 됩니다.
            return cors;
        };
    }

    // 스프링 시큐러티 설정에서 jwtDelegateFilter를 사용하여 JWT 인증을 처리할 수 있도록 설정
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                // cors 설정을 위해서 corsConfigurationSource 빈을 참조해서 CORS 설정을 적용
                .csrf(AbstractHttpConfigurer::disable)
                // CSRF 공격 방지 기능 비활성화 (API 서버에서는 일반적으로 비활성화)
                .formLogin(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/contents/**", "/auth/**").permitAll()
                        .anyRequest().authenticated())
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED)))

                .addFilterBefore(jwtDelegateFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }

}
