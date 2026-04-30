package com.example.jo_gpt_program.gpt.service;

import java.util.ArrayList;
import java.util.Base64;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
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
import com.example.entitycom.entity.gpt.GptChat;
import com.example.entitycom.entity.log.CreateTimeLogs;
import com.example.entitycom.entity.member.MemberPrompt;
import com.example.entitycom.entity.member.Members;
import com.example.entitycom.entity.member.MyChat;
import com.example.jo_gpt_program.gpt.dto.ChatMessageDTO;
import com.example.jo_gpt_program.gpt.dto.MemberPromptDTO;
import com.example.jo_gpt_program.gpt.dto.MyChatDTO;
import com.example.jo_gpt_program.gpt.dto.ShowChatDTO;
import com.example.jo_gpt_program.gpt.repository.jpa.CreateTimeRepository;
import com.example.jo_gpt_program.gpt.repository.jpa.GptChatRepository;
import com.example.jo_gpt_program.gpt.repository.jpa.MemberPromptRepository;
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
        private final GptChatRepository gptChatRepository;
        private final RestTemplate restTemplate;
        private final MemberRepository memberRepository;
        private final ShowChatRepository showChatRepository;
        private final CreateTimeRepository createTimeRepository;
        private final ChatClient chatClient;
        private final EmbeddingModel embeddingModel;
        private final VectorStore vectorStore;
        private final JWTUtils jwtUtils;
        private final ScholarSearchService scholarSearchService;
        private final MemberPromptRepository memberPromptRepository;

        public ContentsService(@Qualifier("myChatRepository") MyChatRepository myChatRepository,
                        GptChatRepository gptChatRepository,
                        RestTemplate restTemplate, MemberRepository memberRepository,
                        ShowChatRepository showChatRepository,
                        CreateTimeRepository createTimeRepository, JWTUtils jwtUtils, ChatClient chatClient,
                        EmbeddingModel embeddingModel, VectorStore vectorStore,
                        ScholarSearchService scholarSearchService,
                        MemberPromptRepository memberPromptRepository) {
                this.restTemplate = restTemplate;
                this.myChatRepository = myChatRepository;
                this.gptChatRepository = gptChatRepository;
                this.memberRepository = memberRepository;
                this.showChatRepository = showChatRepository;
                this.createTimeRepository = createTimeRepository;
                this.chatClient = chatClient;
                this.embeddingModel = embeddingModel;
                this.vectorStore = vectorStore;
                this.jwtUtils = jwtUtils;
                this.scholarSearchService = scholarSearchService;
                this.memberPromptRepository = memberPromptRepository;
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

                Set<ShowChatDTO> showChatDTOS = showChats.stream().map(chat -> ShowChatDTO.builder()
                                .showChatKey(chat.getShowChatKey())
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

        /* 채팅방 삭제 (ShowChat cascade로 하위 데이터 전부 삭제) */
        @Transactional
        public void deleteChat(String authHeader, Long showChatKey) {
                Members members = this.authHeader(authHeader);
                ShowChat showChat = showChatRepository.findShowChatByShowChatKey(showChatKey)
                                .orElseThrow(() -> new RuntimeException("ShowChat not found: " + showChatKey));
                if (!showChat.getMembers().getMemberKey().equals(members.getMemberKey())) {
                        throw new RuntimeException("권한이 없습니다.");
                }
                showChatRepository.delete(showChat);
        }

        // ----------------------------- 프롬프트 관리 -----------------------------

        /* 내 프롬프트 목록 조회 */
        public List<MemberPromptDTO> getMyPrompts(String authHeader) {
                Members members = this.authHeader(authHeader);
                List<MemberPrompt> prompts = memberPromptRepository.findByMember(members);
                return prompts.stream()
                                .map(p -> MemberPromptDTO.builder()
                                                .promptKey(p.getPromptKey())
                                                .promptName(p.getPromptName())
                                                .promptContent(p.getPromptContent())
                                                .isActive(p.getIsActive())
                                                .build())
                                .collect(Collectors.toList());
        }

        /* 새 프롬프트 저장 */
        @Transactional
        public void saveMyPrompt(String authHeader, MemberPromptDTO dto) {
                Members members = this.authHeader(authHeader);
                MemberPrompt prompt = MemberPrompt.builder()
                                .member(members)
                                .promptName(dto.getPromptName())
                                .promptContent(dto.getPromptContent())
                                .build();
                memberPromptRepository.save(prompt);
        }

        /* 프롬프트 삭제 */
        @Transactional
        public void deleteMyPrompt(String authHeader, Long promptKey) {
                Members members = this.authHeader(authHeader);
                MemberPrompt prompt = memberPromptRepository.findById(promptKey)
                                .orElseThrow(() -> new RuntimeException("Prompt not found: " + promptKey));
                if (!prompt.getMember().getMemberKey().equals(members.getMemberKey())) {
                        throw new RuntimeException("권한이 없습니다.");
                }
                memberPromptRepository.delete(prompt);
        }

        /* 활성 프롬프트 변경 */
        @Transactional
        public void activateMyPrompt(String authHeader, Long promptKey) {
                Members members = this.authHeader(authHeader);
                // 기존 활성 프롬프트 비활성화
                memberPromptRepository.findByMemberAndIsActiveTrue(members)
                                .ifPresent(p -> p.deactivate());
                // 새 프롬프트 활성화
                MemberPrompt prompt = memberPromptRepository.findById(promptKey)
                                .orElseThrow(() -> new RuntimeException("Prompt not found: " + promptKey));
                if (!prompt.getMember().getMemberKey().equals(members.getMemberKey())) {
                        throw new RuntimeException("권한이 없습니다.");
                }
                prompt.activate();
        }

        /* 활성 프롬프트 내용 조회 (내부 헬퍼) */
        private String getActivePromptContent(String authHeader) {
                Members members = this.authHeader(authHeader);
                return memberPromptRepository.findByMemberAndIsActiveTrue(members)
                                .map(MemberPrompt::getPromptContent)
                                .orElse(null);
        }

        // ----------------------------- AI 관련 -----------------------------

        /* Gemini 호출 — 이미지 모델은 SDK 직접 호출, 그 외는 Spring AI ChatClient 사용 */
        @Transactional
        public String sendGeminiAI(MyChatDTO dto, String model, String authHeader) {
                String customPrompt = getActivePromptContent(authHeader);
                String systemPrompt = customPrompt != null && !customPrompt.isBlank() ? customPrompt
                                : "당신은 JO-GPT 어시스턴트입니다. 항상 한국어로 친절하게 답변하세요.";

                String responseText;

                if (model.contains("image")) {
                        /* 이미지 생성 모델: Google GenAI SDK로 직접 호출 */
                        responseText = sendGeminiImageDirect(dto.getMyChatContents(), model, systemPrompt);
                } else {
                        /* 일반 텍스트 모델: 이전 대화 맥락 로드 후 Spring AI ChatClient 호출 */
                        List<Message> history = dto.getShowChatKey() != null
                                        ? loadChatHistory(dto.getShowChatKey())
                                        : List.of();

                        responseText = chatClient.prompt()
                                        .system(systemPrompt)
                                        .messages(history)
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

                /* AI 답변을 gpt_chat 테이블에 저장 */
                if (responseText != null && dto.getShowChatKey() != null) {
                        saveGptResponse(dto.getShowChatKey(), responseText);
                }

                return responseText;
        }

        /* DB의 이전 대화를 Spring AI Message 목록으로 변환 (텍스트 모델 맥락용) */
        private List<Message> loadChatHistory(Long showChatKey) {
                Optional<ShowChat> optionalShowChat = showChatRepository.findShowChatByShowChatKey(showChatKey);
                if (optionalShowChat.isEmpty())
                        return List.of();

                ShowChat showChat = optionalShowChat.get();
                List<ChatMessageDTO> msgs = new ArrayList<>();

                myChatRepository.findByShowChat(showChat)
                                .forEach(mc -> msgs.add(
                                                new ChatMessageDTO("user", mc.getMyChatContents(), mc.getMyChatKey())));
                gptChatRepository.findByShowChat(showChat)
                                .forEach(gc -> msgs.add(
                                                new ChatMessageDTO("ai", gc.getGptChatContents(), gc.getGptChatKey())));

                msgs.sort(Comparator.comparingLong(ChatMessageDTO::getKey));

                return msgs.stream()
                                .map(m -> "user".equals(m.getRole())
                                                ? (Message) new UserMessage(m.getContent())
                                                : new AssistantMessage(m.getContent()))
                                .collect(Collectors.toList());
        }

        /* AI 답변을 gpt_chat 테이블에 저장 */
        private void saveGptResponse(Long showChatKey, String response) {
                showChatRepository.findShowChatByShowChatKey(showChatKey).ifPresent(showChat -> {
                        GptChat gptChat = GptChat.builder()
                                        .GptChatContents(response)
                                        .showChat(showChat)
                                        .build();
                        gptChatRepository.save(gptChat);
                });
        }

        /* Google GenAI SDK 직접 호출 — TEXT + IMAGE 모달리티 설정 후 인라인 이미지 추출 */
        private String sendGeminiImageDirect(String userMessage, String model, String systemText) {
                Client client = Client.builder().apiKey(geminiApiKey).build();

                /* 시스템 프롬프트는 systemInstruction으로 분리 */
                Content systemInstruction = Content.builder()
                                .role("system")
                                .parts(Part.fromText(systemText))
                                .build();
                // 사용자 메시지는 일반 프롬프트로 전달
                List<Content> contents = List.of(
                                Content.builder()
                                                .role("user")
                                                .parts(Part.fromText(userMessage))
                                                .build());
                // TXT + IMAGE 모달러티 생성, 최대 토큰 수는 2048로 설정
                GenerateContentConfig config = GenerateContentConfig.builder()
                                .systemInstruction(systemInstruction)
                                .responseModalities("TEXT", "IMAGE")
                                .maxOutputTokens(2048)
                                .build();

                log.info("[ImageGen] 요청 모델={}, 프롬프트={}", model, userMessage);
                // Google GenAI SDK로 Gemini 모델 직접 호출 그리고 response에 응답을 받음
                GenerateContentResponse response = client.models.generateContent(model, contents, config);

                // 텍스트를 조각조각 이어 붙이기 위한 JAVA 기본 클래스 Gemini의 응답이 텍스트가 여러 part로 나누어 올 수 있기 때문에
                // StringBuilder로 이어 붙임 그리고 마지막 한 번만 변환해서 성능상 더 좋음

                StringBuilder textBuilder = new StringBuilder();

                // AI 응답에서 이미지가 여러 장 나올 수 있기 대문입니다. 각 이미지는 두 가지 정보가 필요합니다. 이미지 현식과 실제 이미지 데이터
                // 입니다.
                List<Map<String, String>> images = new ArrayList<>();
                // response
                // ㄴ candidate
                // ㄴ content
                // ㄴ part
                // part -> text 면 -> textBuilder에 이어 붙이기
                // part -> inlineData 있으면 -> 이미지 추출
                // ifPresent 값이 있으면 이걸 해라, 없으면 넘어가라는 의미입니다.
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
                // 모아놓은 StringBuilder의 텍스트들을 String으로 변환
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

        // ----------------------------------- RAG -----------------------------

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

        // ----------------------------- 학술검색 -----------------------------

        public String sendWithScholar(MyChatDTO dto, String model, String authHeader) {
                String customPrompt = getActivePromptContent(authHeader);
                String scholarResults = scholarSearchService.search(dto.getMyChatContents());

                String systemPrompt = """
                                아래 학술 논문 결과를 참고해서 답변하세요.
                                검색 결과가 없으면 알고 있는 내용으로 답변하세요.

                                [논문 검색 결과]
                                %s

                                [추가 지침]
                                %s
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

        /* 특정 채팅방의 대화 내역 조회 (유저 메시지 + AI 답변을 시간순으로 정렬) */
        @Transactional
        public List<ChatMessageDTO> getChatHistory(Long showChatKey, String authHeader) {
                authHeader(authHeader); // 인증 검증
                ShowChat showChat = showChatRepository.findShowChatByShowChatKey(showChatKey)
                                .orElseThrow(() -> new RuntimeException("채팅방을 찾을 수 없습니다: " + showChatKey));

                List<ChatMessageDTO> messages = new ArrayList<>();

                myChatRepository.findByShowChat(showChat).forEach(mc -> messages
                                .add(new ChatMessageDTO("user", mc.getMyChatContents(), mc.getMyChatKey())));

                gptChatRepository.findByShowChat(showChat).forEach(gc -> messages
                                .add(new ChatMessageDTO("ai", gc.getGptChatContents(), gc.getGptChatKey())));

                messages.sort(Comparator.comparingLong(ChatMessageDTO::getKey));
                return messages;
        }

        public String sendWithRagAndScholar(MyChatDTO dto, String model, String authHeader) {
                String customPrompt = getActivePromptContent(authHeader);

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
