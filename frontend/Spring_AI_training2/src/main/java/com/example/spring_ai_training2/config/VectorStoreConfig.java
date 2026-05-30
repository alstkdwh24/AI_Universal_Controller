package com.example.spring_ai_training2.config;

import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.SimpleVectorStore;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

@Configuration
public class VectorStoreConfig {

    /**
     * 💡 [제미나이 코어 엔진 부활 지점]
     * 주석을 해제하고 무결점 빌더 구조로 메모리 기반 벡터 스토어를 빈 등록합니다.
     * 이 빈이 컨테이너에 정상적으로 안착해야만 스프링 부트가 구글 임베딩 모델과
     * 연쇄적으로 'ChatModel' 빈을 비로소 세상 밖으로 만들어 냅니다!
     */
    @Primary
    @Bean
    public VectorStore vectorStore(EmbeddingModel embeddingModel) {
        // 2.0.0-M6 규격에 맞는 정석 빌더 패턴으로 로컬 인메모리 벡터 저장소를 활성화합니다.
        return SimpleVectorStore.builder(embeddingModel).build();
    }

    /**
     * 민상님이 텍스트 분할을 위해 정교하게 세팅해두신 토큰 스플리터 스펙 유지
     */
    @Bean
    public TokenTextSplitter tokenTextSplitter() {
        return TokenTextSplitter.builder()
                .withChunkSize(800) // 800토큰 단위로 쪼개기
                .withMinChunkSizeChars(200)
                .withMinChunkLengthToEmbed(100)
                .withMaxNumChunks(10)
                .build();
    }
}