package com.example.jo_gpt_program.gpt.service;

import java.util.*;
import java.util.stream.Collectors;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.document.Document;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.google.genai.GoogleGenAiChatOptions;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import com.example.entitycom.entity.chat.ShowChat;
import com.example.entitycom.entity.log.CreateTimeLogs;
import com.example.entitycom.entity.member.Members;
import com.example.entitycom.entity.member.MyChat;
import com.example.jo_gpt_program.gpt.dto.MyChatDTO;
import com.example.jo_gpt_program.gpt.dto.ShowChatDTO;
import com.example.jo_gpt_program.gpt.repository.jpa.CreateTimeRepository;
import com.example.jo_gpt_program.gpt.repository.jpa.MyChatRepository;
import com.example.jo_gpt_program.gpt.repository.jpa.ShowChatRepository;
import com.example.memberssecurity.member.repository.jpa.MemberRepository;
import com.example.memberssecurity.security.config.jwt.JWTUtils;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.genai.Client;
import com.google.genai.types.Content;
import com.google.genai.types.GenerateContentConfig;
import com.google.genai.types.GenerateContentResponse;
import com.google.genai.types.Part;

import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;

@SuppressWarnings("ALL")
@Service("contentsService")
@Slf4j
public class ContentsService {

        @Value("${spring.llm.key}")
        private String geminiApiKey;

        private final MyChatRepository myChatRepository;
        private final RestTemplate restTemplate;
        private final MemberRepository memberRepository;
        private final ShowChatRepository showChatRepository;
        private final CreateTimeRepository createTimeRepository;
        private final ChatClient chatClient;
        private EmbeddingModel embeddingModel;
        private final VectorStore vectorStore;
        private final JWTUtils jwtUtils;
        private final ScholarSearchService scholarSearchService;

        public ContentsService(@Qualifier("myChatRepository") MyChatRepository myChatRepository,
                        RestTemplate restTemplate, MemberRepository memberRepository,
                        ShowChatRepository showChatRepository,
                        CreateTimeRepository createTimeRepository, JWTUtils jwtUtils, ChatClient chatClient,
                        EmbeddingModel embeddingModel, VectorStore vectorStore,
                        ScholarSearchService scholarSearchService) {
                this.restTemplate = restTemplate;
                this.myChatRepository = myChatRepository;
                this.memberRepository = memberRepository;
                this.showChatRepository = showChatRepository;
                this.createTimeRepository = createTimeRepository;
                this.chatClient = chatClient;
                this.embeddingModel = embeddingModel;
                this.vectorStore = vectorStore;
                this.jwtUtils = jwtUtils;
                this.scholarSearchService = scholarSearchService;
        }

        /* 유저 정보 불러오기 */
        public String userInfo(Long memberKey, MyChatDTO dto) {
                Optional<Members> members = memberRepository.findByMemberKey(memberKey);
                Members member = members
                                .orElseThrow(() -> new RuntimeException("Member not found with key: " + memberKey));
                log.debug("member={}", member);
                String response = this.myChat(dto, member);
                return response;
        }

        /* 내가 적은 채팅 DB 저장 */
        @Transactional
        public String myChat(MyChatDTO dto, Members member) {
                Optional<Members> members = memberRepository.findByMemberKey(member.getMemberKey());
                Optional<ShowChat> showChat = showChatRepository.findShowChatByShowChatKey(dto.getShowChatKey());
                log.debug("showChatTwo={}", showChat);

                Members members1 = members
                                .orElseThrow(() -> new RuntimeException(
                                                "Member not found with key: " + member.getMemberKey()));
                ShowChat showChat1 = showChat
                                .orElseThrow(() -> new RuntimeException(
                                                "ShowChat not found for showChatKey: " + dto.getShowChatKey()));
                MyChat chat = MyChat.builder()
                                .member(members1)
                                .showChat(showChat1)
                                .myChatContents(dto.getMyChatContents())
                                .myChatImage(dto.getMyChatImage())
                                .createTimeLogs(CreateTimeLogs.builder().build())
                                .build();

                MyChat myChat = myChatRepository.save(chat);
                return myChat.getMyChatContents();
        }

        /* 채팅방 만드는 메서드 */
        @Transactional
        public Long createChat(String authHeader, MyChatDTO dto) {
                Members members = this.authHeader(authHeader);
                ShowChat showChat = ShowChat.builder()
                                .members(members)
                                .build();
                ShowChat showChat1 = showChatRepository.save(showChat);

                log.debug("showChat1={}", showChat1.getShowChatKey());
                MyChat myChat = MyChat.builder()
                                .showChat(showChat1)
                                .member(members)
                                .myChatContents(dto.getMyChatContents())
                                .build();
                myChatRepository.save(myChat);

                CreateTimeLogs createTimeLogs = CreateTimeLogs.builder()
                                .showChat(showChat1)
                                .build();
                createTimeRepository.save(createTimeLogs);
                log.debug("showChat={}", showChat1);

                return showChat1.getShowChatKey();
        }

        /* JWT 토큰으로 사용자 정보 가져오기 */
        private Members authHeader(String authHeader) {
                if (authHeader == null) {
                        throw new IllegalArgumentException("Authorization header is missing");
                }
                authHeader = authHeader.replace("Bearer ", "");
                Long memberKey = jwtUtils.getUsername(authHeader);
                Members members = userInfoTwo(memberKey);
                if (members == null) {
                        throw new RuntimeException("Member not Object: " + members);
                }
                return members;
        }

        /* 유저 정보 불러오기 */
        private Members userInfoTwo(Long memberKey) {
                Optional<Members> members = memberRepository.findByMemberKey(memberKey);
                Members member = members
                                .orElseThrow(() -> new RuntimeException("Member not found with key: " + memberKey));
                log.debug("member={}", member);
                return member;
        }

        /* 채팅 리스트 불러오기 */
        @Transactional
        public Set<ShowChatDTO> getChattingList(String authHeader) {
                Members members = authHeader(authHeader);
                Set<ShowChat> showChats = showChatRepository.findByMembers(members);
                showChats.forEach(chat -> {
                        log.debug("showChatKey={}", chat.getShowChatKey());
                        log.debug("members={}", chat.getMembers());
                        log.debug("createTimeLogs={}", chat.getCreateTimeLogs());
                        log.debug("myChat={}", chat.getMyChat());
                        log.debug("gptChat={}", chat.getGptChat());
                        log.debug("chatAttachment={}", chat.getChatAttachment());
                });

                log.debug("showChatReal:{}", showChats.stream());
                Set<ShowChatDTO> showChatDTOS = showChats.stream().map(chat -> ShowChatDTO.builder()
                                .showChatRegistration(
                                                chat.getCreateTimeLogs() != null && !chat.getCreateTimeLogs().isEmpty()
                                                                ? chat.getCreateTimeLogs().iterator().next()
                                                                                .getCreatedAt()
                                                                : null)
                                .showMyChatContents(chat.getMyChat() != null && !chat.getMyChat().isEmpty()
                                                ? chat.getMyChat().iterator().next().getMyChatContents()
                                                : null)
                                .build()).collect(Collectors.toSet());
                return showChatDTOS;
        }

        // AI 관련 코드
        /* 이미지 모델이면 Google GenAI SDK 직접 호출, 텍스트 모델이면 Spring AI ChatClient 사용 */
        public String sendGeminiAI(MyChatDTO dto, String model, String customPrompt) {
                String systemPrompt = customPrompt != null && !customPrompt.isBlank() ? customPrompt
                                : "당신은 JO-GPT 어시스턴트입니다. 항상 한국어로 친절하게 답변하세요.";

                if (model.contains("image")) {
                        return sendGeminiImageDirect(dto.getMyChatContents(), model, systemPrompt);
                }

                return chatClient.prompt()
                                .system(systemPrompt)
                                .user(dto.getMyChatContents())
                                .options(GoogleGenAiChatOptions.builder()
                                                .model(model)
                                                .temperature(0.7)
                                                .maxOutputTokens(1024)
                                                .topP(0.9)
                                                .topK(100)
                                                .build())
                                .call()
                                .content();
        }

        /* Google GenAI SDK 직접 호출 — TEXT + IMAGE 모달리티 설정 후 인라인 이미지 추출 */
        private String sendGeminiImageDirect(String userMessage, String model, String systemText) {
                Client client = Client.builder().apiKey(geminiApiKey).build();

                Content systemInstruction = Content.builder()
                                .role("system")
                                .parts(Part.fromText(systemText))
                                .build();

                List<Content> contents = List.of(
                                Content.builder()
                                                .role("user")
                                                .parts(Part.fromText(userMessage))
                                                .build());

                GenerateContentConfig config = GenerateContentConfig.builder()
                                .systemInstruction(systemInstruction)
                                .responseModalities("TEXT", "IMAGE")
                                .maxOutputTokens(2048)
                                .build();

                log.info("[ImageGen] 요청 모델={}, 프롬프트={}", model, userMessage);
                GenerateContentResponse response = client.models.generateContent(model, contents, config);

                StringBuilder textBuilder = new StringBuilder();
                List<Map<String, String>> images = new ArrayList<>();

                response.candidates().orElse(List.of()).forEach(candidate -> candidate.content()
                                .ifPresent(content -> content.parts().orElse(List.of()).forEach(part -> {
                                        log.info("[ImageGen] part - text={}, inlineData={}",
                                                        part.text().isPresent(),
                                                        part.inlineData().isPresent());
                                        part.text().ifPresent(textBuilder::append);
                                        part.inlineData().ifPresent(blob -> blob.data().ifPresent(data -> {
                                                log.info("[ImageGen] 이미지 추출 성공 mimeType={}, size={}bytes",
                                                                blob.mimeType().orElse("unknown"), data.length);
                                                Map<String, String> img = new LinkedHashMap<>();
                                                img.put("mimeType", blob.mimeType().orElse("image/png"));
                                                img.put("data", Base64.getEncoder().encodeToString(data));
                                                images.add(img);
                                        }));
                                })));

                log.info("[ImageGen] 결과 - text길이={}, 이미지수={}", textBuilder.length(), images.size());
                String textContent = textBuilder.toString();

                if (!images.isEmpty()) {
                        Map<String, Object> result = new LinkedHashMap<>();
                        result.put("text", textContent);
                        result.put("images", images);
                        try {
                                return new ObjectMapper().writeValueAsString(result);
                        } catch (Exception e) {
                                log.error("이미지 응답 직렬화 실패", e);
                        }
                }

                return textContent;
        }

        // ----------------------------------- RAG: 문서 기반 답변 -----------------------------------

        public String ragAnswer(MyChatDTO dto) {
                List<Document> docs = vectorStore.similaritySearch(
                                SearchRequest.builder().query(dto.getMyChatContents()).topK(5).build());

                String context = docs.stream()
                                .map(Document::getText)
                                .collect(Collectors.joining("\n---\n"));

                return chatClient.prompt()
                                .system("당신은 JO-GPT 어시스턴트입니다. 다음 컨텍스트를 참고하여 질문에 답변하세요:\n" + context)
                                .user(dto.getMyChatContents())
                                .call()
                                .content();
        }

        /* 문서 저장 (RAG용) */
        public void saveDocument(String context) {
                vectorStore.add(List.of(new Document(context)));
        }

        /* 임베딩 */
        public float[] getEmbedding(String text) {
                return embeddingModel.embed(text);
        }

        // ------------------- 학술검색 + AI 답변 -------------------
        public String sendWithScholar(MyChatDTO dto, String model, String customPrompt) {
                String scholarResults = scholarSearchService.search(dto.getMyChatContents());

                String systemPrompt = """
                                아래 학술 논문 결과를 참고해서 답변하세요.
                                검색 결과가 없으면 알고 있는 내용으로 답변하세요.

                                [논문 검색 결과]
                                %s        ← 첫 번째 자리

                                [추가 지침]
                                %s        ← 두 번째 자리
                                """.formatted(
                                scholarResults.isEmpty() ? "검색 결과 없음" : scholarResults,
                                customPrompt != null ? customPrompt : "친절하고 학술적으로 답변하세요.");

                return chatClient.prompt()
                                .system(systemPrompt)
                                .user(dto.getMyChatContents())
                                .options(GoogleGenAiChatOptions.builder()
                                                .model(model)
                                                .temperature(0.7)
                                                .maxOutputTokens(1024)
                                                .topP(0.9)
                                                .topK(100)
                                                .build())
                                .call()
                                .content();
        }

        // ------------------- 학술검색 + RAG 답변 -------------------
        public String sendWithRagAndScholar(MyChatDTO dto, String model, String customPrompt) {
                List<Document> docs = vectorStore.similaritySearch(
                                SearchRequest.builder()
                                                .query(dto.getMyChatContents())
                                                .topK(3)
                                                .build());
                String ragContext = docs.stream()
                                .map(Document::getText)
                                .collect(Collectors.joining("\n---\n"));

                String scholarResults = scholarSearchService.search(dto.getMyChatContents());

                String systemPrompt = """
                                아래 정보를 참고해서 답변하세요.

                                [RAG 검색 결과]
                                %s

                                [학술 검색 결과]
                                %s

                                [추가 지침]
                                %s
                                """.formatted(
                                ragContext.isEmpty() ? "검색 결과 없음" : ragContext,
                                scholarResults.isEmpty() ? "검색 결과 없음" : scholarResults,
                                customPrompt != null ? customPrompt : "친절하고 학술적으로 답변하세요.");

                return chatClient.prompt()
                                .system(systemPrompt)
                                .user(dto.getMyChatContents())
                                .options(GoogleGenAiChatOptions.builder()
                                                .model(model)
                                                .temperature(0.7)
                                                .maxOutputTokens(1024)
                                                .topP(0.9)
                                                .topK(100)
                                                .build())
                                .call()
                                .content();
        }
}