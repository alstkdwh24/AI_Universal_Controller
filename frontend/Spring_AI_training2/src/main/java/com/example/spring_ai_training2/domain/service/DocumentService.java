package com.example.spring_ai_training2.domain.service;

import org.springframework.ai.document.Document;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class DocumentService {

    private final VectorStore vectorStore;
    private final TokenTextSplitter textSplitter;

    public DocumentService(VectorStore vectorStore, TokenTextSplitter textSplitter) {
        this.vectorStore = vectorStore;
        this.textSplitter = textSplitter;
    }

    public void saveDocument(String text){
        List<Document> docs = List.of(new Document(text));
        List<Document> splitDocs = docs.stream().flatMap(doc -> textSplitter.split(doc).stream()).toList();
        vectorStore.add(splitDocs);

    }
}
