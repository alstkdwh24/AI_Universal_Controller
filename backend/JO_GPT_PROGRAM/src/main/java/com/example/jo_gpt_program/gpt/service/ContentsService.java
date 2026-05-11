package com.example.jo_gpt_program.gpt.service;

import java.util.ArrayList;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.content.Media;
import org.springframework.ai.document.Document;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.google.genai.GoogleGenAiChatOptions;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.util.MimeTypeUtils;
import org.springframework.web.client.RestTemplate;

import com.example.entitycom.entity.chat.ShowChat;
import com.example.entitycom.entity.gpt.GptChat;
import com.example.entitycom.entity.log.CreateTimeLogs;
import com.example.entitycom.entity.member.Members;
import com.example.entitycom.entity.member.MyChat;
import com.example.jo_gpt_program.gpt.config.filter.UserInfoDto;
import com.example.jo_gpt_program.gpt.dto.ChatMessageDTO;
import com.example.jo_gpt_program.gpt.dto.MyChatDTO;
import com.example.jo_gpt_program.gpt.dto.ShowChatDTO;
import com.example.jo_gpt_program.gpt.repository.jpa.CreateTimeRepository;
import com.example.jo_gpt_program.gpt.repository.jpa.GptChatRepository;
import com.example.jo_gpt_program.gpt.repository.jpa.MemberRepository;
import com.example.jo_gpt_program.gpt.repository.jpa.MyChatRepository;
import com.example.jo_gpt_program.gpt.repository.jpa.ShowChatRepository;
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
        private final ScholarSearchService scholarSearchService;
        private final GptChatRepository gptChatRepository;

        public ContentsService(@Qualifier("myChatRepository") MyChatRepository myChatRepository,
                        RestTemplate restTemplate, MemberRepository memberRepository,
                        ShowChatRepository showChatRepository,
                        CreateTimeRepository createTimeRepository, ChatClient chatClient,
                        EmbeddingModel embeddingModel, VectorStore vectorStore,
                        ScholarSearchService scholarSearchService, GptChatRepository gptChatRepository) {
                this.restTemplate = restTemplate;
                this.myChatRepository = myChatRepository;
                this.gptChatRepository = gptChatRepository;

                this.memberRepository = memberRepository;
                this.showChatRepository = showChatRepository;
                this.createTimeRepository = createTimeRepository;
                this.chatClient = chatClient;
                this.embeddingModel = embeddingModel;
                this.vectorStore = vectorStore;
                this.scholarSearchService = scholarSearchService;

        }

        /* 유저 정보 불러오기 */
        public String userInfo(Long memberKey, MyChatDTO dto) {
                // Optional은 null 이 될수도 값이 있을수도 있음을 표현하는 컨테이너이다.
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
                // 값이 있을수도 없을수도 있음을 표현하는 컨테이너 null대신 Optional로 감싸서 NullPointerException 방지
                Optional<Members> members = memberRepository.findByMemberKey(member.getMemberKey());
                Optional<ShowChat> showChat = showChatRepository.findShowChatByShowChatKey(dto.getShowChatKey());
                log.debug("showChatTwo={}", showChat);

                Members members1 = members
                                .orElseThrow(() -> new RuntimeException(
                                                "Member not found with key: " + member.getMemberKey()));
                // 값이 있으면 memberrs1에 저장, 없으면 예외 발생
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
                // DB에 저장
                MyChat myChat = myChatRepository.save(chat);
                return myChat.getMyChatContents();
        }

        /* 채팅방 만드는 메서드 */
        @Transactional
        public Long createChat(MyChatDTO dto) {
                Members members = this.getMemberFromContext();

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

        /* SecurityContextHolder에서 인증된 사용자 정보 추출 */
        private Members getMemberFromContext() {
                // 유저 정보 추출 위한 과정
                UserInfoDto userInfo = (UserInfoDto) SecurityContextHolder.getContext().getAuthentication()
                                .getPrincipal();
                Long memberKey = Long.parseLong(userInfo.getMemberId());
                return userInfoTwo(memberKey);

        }

        /* 유저 정보 불러오기 */
        private Members userInfoTwo(Long memberKey) {
                // Optional은 null 이 될수도 값이 있을수도 있음을 표현하는 컨테이너이다. memberKey로 멤버 정보 조회
                Members member = memberRepository.findByMemberKey(memberKey)
                                .orElseThrow(() -> new RuntimeException("Member not found with key: " + memberKey));
                log.debug("member={}", member);
                return member;
        }

        /* 채팅 리스트 불러오기 */
        @Transactional
        public Set<ShowChatDTO> getChattingList() {
                // 인증된 사용자 정보 추출
                Members members = getMemberFromContext();
                // 맴버 정보로 채팅방 조회 Set은 중복 없는 컬렉션, showChatRepository의 findByMembers 메서드로 해당 멤버가
                // 속한 채팅방들 조회
                // set 의 특징
                // 1. 중복불가
                // 2. 순서 없음
                // 3. null 하나만 허용
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
                                .showChatKey(chat.getShowChatKey())
                                .showChatRegistration(
                                                chat.getCreateTimeLogs() != null && !chat.getCreateTimeLogs().isEmpty()
                                                                ? chat.getCreateTimeLogs().iterator().next()
                                                                                .getCreatedAt()
                                                                : null)
                                // 채팅방 생성 시각
                                .showMyChatContents(chat.getMyChat() != null && !chat.getMyChat().isEmpty()
                                                ? chat.getMyChat().iterator().next().getMyChatContents()
                                                : null)
                                // 채팅방 목록에 미리보기로 보여줄 첫 번째 메시지 내용이다.

                                .build()).collect(Collectors.toSet());
                // Stream의 최종 연산으로, 스트림의 모든 요소를 Set 컬렉션으로 수집한다. 중복된 요소는 하나로 합쳐지고, 순서는 보장되지 않는다.
                return showChatDTOS;
        }

        /* 채팅방 삭제 */
        @Transactional
        public void deleteChat(String authHeader, Long showChatKey) {
                // 채팅방 삭제 메서드
                showChatRepository.deleteById(showChatKey);
        }

        /* 채팅방의 대화 내역 불러오기 (user + ai 메시지를 시간순 정렬) */
        @Transactional
        public List<ChatMessageDTO> getChatMessages(Long showChatKey) {
                // 채팅방 조회
                ShowChat showChat = showChatRepository.findShowChatByShowChatKey(showChatKey)
                                .orElseThrow(() -> new RuntimeException("ShowChat not found: " + showChatKey));

                List<Object[]> entries = new ArrayList<>();
                // User 메시지 추가
                if (showChat.getMyChat() != null) {
                        showChat.getMyChat().forEach(m -> entries
                                        .add(new Object[] { m.getMyChatKey(), "user", m.getMyChatContents() }));
                }
                if (showChat.getGptChat() != null) {
                        // AI 메시지 추가
                        showChat.getGptChat().forEach(g -> entries
                                        .add(new Object[] { g.getGptChatKey(), "ai", g.getGptChatContents() }));

                }
                // 시간순으로 정렬
                entries.sort(java.util.Comparator.comparingLong(e -> (Long) e[0]));

                return entries.stream()
                                .map(e -> new ChatMessageDTO((String) e[1], (String) e[2]))
                                .collect(Collectors.toList());
        }

        // AI 관련 코드
        /* 이미지 모델이면 Google GenAI SDK 직접 호출, 텍스트 모델이면 Spring AI ChatClient 사용 */
        public String sendGeminiAI(MyChatDTO dto, String model, String customPrompt) {
                // 유저 메시지 컨텍스트 + 인라인 이미지 포함하여 UserMessage 객체 생성
                UserMessage message;

                if (dto.getFiles() != null && !dto.getFiles().isEmpty()) {
                        // 프론트에서 파일을Base64로 변환해서 서버에 전송하면, 이 코드에서 AI가 읽을 수 있는 Media 객체로 변환하는 과정

                        List<Media> mediaList = dto.getFiles().stream()
                                        // 첨부파일 목록을 스트림으로 변환
                                        .filter(f -> f.getMimeType() != null)
                                        // mimeType이 null인 파일 제거 이거는 파일의 종류 / 형식을 문자열로 표현한 식별자입니다.
                                        .map(f -> new Media(MimeTypeUtils.parseMimeType(f.getMimeType()), // "image/png"
                                                                                                          // 를 MimeType
                                                                                                          // 객체로 파씽
                                                        new ByteArrayResource( // ByteArrayResource로 감쌈 (스프링이 읽을 수 있는
                                                                               // 리소스 타입)
                                                                        Base64.getDecoder().decode(f.getData())))) // fi.getData()
                                                                                                                   // ->
                                                                                                                   // Base64
                                                                                                                   // 문자열
                                                                                                                   // ->
                                                                                                                   // 바이트
                                                                                                                   // 배열로
                                                                                                                   // 디코딩
                                        .toList();
                        // mediaList에는 AI가 이해할 수 있는 형태로 변환된 첨부파일들이 담겨있음
                        // mssage 객체는 텍스트 + 미디어를 모두 포함하는 형태로 생성
                        message = UserMessage.builder()
                                        .text(dto.getMyChatContents())
                                        .media(mediaList)
                                        .build();

                } else {
                        message = UserMessage.builder()
                                        .text(dto.getMyChatContents())
                                        .build();
                }

                String systemPrompt = customPrompt != null && !customPrompt.isBlank() ? customPrompt
                                : "당신은 JO-GPT 어시스턴트입니다. 항상 한국어로 친절하게 답변하세요.";

                if (model.contains("image")) {
                        return sendGeminiImageDirect(dto.getMyChatContents(), dto.getFiles(), model, systemPrompt);
                }
                // RstTemplate 기반으로 Spring AI ChatClient를 사용하는 기존 방식 유지
                String messages = chatClient.prompt()
                                .system(systemPrompt)
                                .messages(message)
                                .options(GoogleGenAiChatOptions.builder()
                                                .model(model)
                                                .temperature(0.7)
                                                .maxOutputTokens(1024)
                                                .topP(0.9)
                                                .topK(100)
                                                .build())
                                .call()
                                .content();
                if (messages != null) {
                        GptChat gptChat = GptChat.builder()
                                        .GptChatContents(messages)
                                        .build();
                        gptChatRepository.save(gptChat);
                        log.info("[sendGeminiAI] 응답 길이={}, 내용={}", messages.length(), messages);
                } else {
                        log.warn("[sendGeminiAI] 응답이 null입니다.");
                }

                return messages;
        }

        /* Google GenAI SDK 직접 호출 — TEXT + IMAGE 모달리티 설정 후 인라인 이미지 추출 */
        private String sendGeminiImageDirect(String userMessage, List<MyChatDTO.FilePartDTO> files, String model,
                        String systemText) {

                List<Part> parts = new ArrayList<>();
                parts.add(Part.fromText(userMessage)); // 사용자 텍스트 질문 추가
                if (files != null) {
                        files.stream()
                                        .filter(f -> f.getMimeType() != null && f.getData() != null)
                                        .forEach(f -> parts.add(Part.fromBytes(Base64.getDecoder().decode(f.getData()),
                                                        f.getMimeType())));
                }
                // Gemini 클라이언트 직접 호출 왜 ChatClient 가 아닌 Google GenAI SDK의 Client를 직접 사용합니다. 이미지
                // 생성 은 Spring AI가 아직 지원하지 않아서 직접 호출하는 것입니다.
                Client client = Client.builder().apiKey(geminiApiKey).build();

                Content systemInstruction = Content.builder()
                                .role("system")
                                .parts(Part.fromText(systemText))
                                .build();
                // 사용자 메시지 구성
                List<Content> contents = List.of(
                                Content.builder()
                                                .role("user")
                                                .parts(parts) // 텍스트 + 이미지 파일이 담긴 리스트
                                                .build());
                // 응답 설정 시스템 프롬프트, 텍스트 + 이미지 둘다 받겠다고 하고, 최대 응답길이 (주로 응답 형식제어)
                GenerateContentConfig config = GenerateContentConfig.builder()
                                .systemInstruction(systemInstruction)
                                .responseModalities("TEXT", "IMAGE")
                                .maxOutputTokens(2048)
                                .build();

                log.info("[ImageGen] 요청 모델={}, 프롬프트={}", model, userMessage);
                // Gemini API 호출
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
                GptChat gptChat = GptChat.builder()
                                .GptChatContents(textContent)
                                .build();

                gptChatRepository.save(gptChat);

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

        // ----------------------------------- RAG: 문서 기반 답변
        // -----------------------------------

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
                // 벡터 DB에 문서를 저장하는 메서드, RAG에서 검색할 수 있도록 텍스트를 벡터로 변환하여 저장합니다.
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
                                // GoogleGenAiChatOptions는 Spring AI에서 Google GenAI 모델을 사용할 때 옵션을 설정하는 클래스입니다.
                                // 모델 선택, 온도, 최대 토큰 수 등 다양한 옵션을 설정할 수 있습니다.
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
                // 1단계 - 벡터 DB에서 유사 문서 검색
                List<Document> docs = vectorStore.similaritySearch(
                                SearchRequest.builder()
                                                .query(dto.getMyChatContents())
                                                .topK(3) // 유사한 문서 3개만
                                                .build());
                // 문서들을 하나의 문자열로 합치기
                String ragContext = docs.stream()
                                // 각 문서에서 텍스트만 추출
                                .map(Document::getText)
                                // 문서 사이에 구분선 삽입
                                .collect(Collectors.joining("\n---\n"));
                // 학술 검색 결과 가져오기
                String scholarResults = scholarSearchService.search(dto.getMyChatContents());
                // 시스템 프롬프트 조립
                String systemPrompt =
                                // ragContext 삽입 scholarResults 삽입 customPrompt 삽입
                                """
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