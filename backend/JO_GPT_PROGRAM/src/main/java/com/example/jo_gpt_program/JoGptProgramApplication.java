package com.example.jo_gpt_program;

import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication
@EntityScan(basePackages = "com.example.entitycom.entity")
@ComponentScan(basePackages = {
        "com.example.entitycom.entity",
        "com.example.jo_gpt_program",
        "com.example.memberssecurity.security.config.jwt",
        "com.example.memberssecurity.member.service",
        "com.example.memberssecurity.security.config.restTemplate",
        "com.example.memberssecurity.security.config" // config 하위의 security, jwt 모두 포함
         })
@EnableJpaRepositories(basePackages = {
        "com.example.jo_gpt_program",
        "com.example.memberssecurity.member.repository.jpa"
})
@EnableJpaAuditing // ✅ 이것도 필요
public class JoGptProgramApplication {

    public static void main(String[] args) {
        new SpringApplicationBuilder(JoGptProgramApplication.class)
                .properties("spring.config.location=file:C:/courage3/JavaRealProject/potfolio_project/AI_Universal_Controller/backend/JO_GPT_PROGRAM/src/main/resources/application.yml")
                .run(args);
    }

}
