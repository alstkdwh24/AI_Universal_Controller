package com.example.spring_ai_training2;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class SpringAiTraining2Application {

    public static void main(String[] args) {
        /*
         * 💡 [악성 프레임워크 버그 원천 차단]
         * 1. 방해되는 엘라스틱서치 자동 설정을 끕니다.
         * 2. (추가됨) 끝까지 project-id를 요구하며 자폭하던 구글 임베딩 '연결' 자동 설정을 완전히 꺼버립니다!
         * (클래스 2개를 쉼표(,)로 연결하여 완벽하게 격리합니다.)
         */
        System.setProperty("spring.autoconfigure.exclude",
                "org.springframework.ai.vectorstore.elasticsearch.autoconfigure.ElasticsearchVectorStoreAutoConfiguration,"
                        );

        SpringApplication.run(SpringAiTraining2Application.class, args);
    }
}