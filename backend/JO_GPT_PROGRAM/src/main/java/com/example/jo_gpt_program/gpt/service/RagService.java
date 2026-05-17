package com.example.jo_gpt_program.gpt.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.document.Document;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service("ragService")
public class RagService {
    private final VectorStore vectorStore;
    private final EmbeddingModel embeddingModel;
    private final ChatClient chatClient;
    private final ChatMemory chatMemory;

    public RagService(VectorStore vectorStore, EmbeddingModel embeddingModel, ChatClient chatClient, ChatMemory chatMemory) {
        this.vectorStore = vectorStore;
        this.embeddingModel = embeddingModel;
        this.chatClient = chatClient;
        this.chatMemory = chatMemory;
    }
// 문서 저장 메서드
    
    public void saveDocument(String context) {

        String summary = chatClient.prompt()
                .user("다음 내용을 3줄로 요약해줘")
                .call()// AI한테 요청을 보내는 것
                .content(); // 응답 객체에서 텍스트만 꺼내는 것
        // 벡터 DB에 문서를 저장하는 메서드, RAG에서 검색할 수 있도록 텍스트를 벡터로 변환하여 저장합니다.
        assert summary != null;
        vectorStore.add(List.of(new Document(summary)));
    }

    /* 임베딩 */

    // ----------------------------------- RAG: 문서 기반 답변
    // -----------------------------------


    public String findDocument(String query) {
        if (query == null || query.isBlank()) return "";

        List<Document> docs = vectorStore.similaritySearch(
                SearchRequest.builder().query(query).topK(5).build());
        log.debug("문서갯수", docs);
        return docs.stream()
                .map(Document::getText)
                .collect(Collectors.joining("\n---\n"));
    }
}
