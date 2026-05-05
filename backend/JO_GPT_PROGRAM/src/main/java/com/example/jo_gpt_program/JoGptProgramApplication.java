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
})
@EnableJpaRepositories(basePackages = {
        "com.example.jo_gpt_program"
})
@EnableJpaAuditing // ✅ 이것도 필요
public class JoGptProgramApplication {

    public static void main(String[] args) {
        new SpringApplicationBuilder(JoGptProgramApplication.class).run(args);

    }

}
