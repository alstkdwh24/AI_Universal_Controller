package com.example.jo_gpt_program.gpt.service;

import com.example.entitycom.entity.chat.ShowChat;
import com.example.entitycom.entity.gpt.GptChat;
import com.example.jo_gpt_program.gpt.dto.MyChatDTO;
import com.example.jo_gpt_program.gpt.repository.jpa.GptChatRepository;
import com.example.jo_gpt_program.gpt.repository.jpa.ShowChatRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.genai.Client;
import com.google.genai.types.Content;
import com.google.genai.types.GenerateContentConfig;
import com.google.genai.types.GenerateContentResponse;
import com.google.genai.types.Part;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.content.Media;
import org.springframework.ai.document.Document;
import org.springframework.ai.google.genai.GoogleGenAiChatOptions;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.stereotype.Service;
import org.springframework.util.MimeTypeUtils;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Slf4j
@Service("geminiService")
public class GeminiService {
    @Value("${spring.llm.key}")
    private String geminiApiKey;
    private final ChatClient chatClient;
    private final ChatModel chatModel;
    private final ChatMemory chatMemory;
    private final RagService ragService;

    private final ShowChatRepository showChatRepository;
    private final GptChatRepository gptChatRepository;
    private final ScholarSearchService scholarSearchService;

    private final VectorStore vectorStore;

    private final KakaoMapService kakaoMapService;
    // ↓ 여기! 필드 선언부에 추가! 키워드
    private static final List<String> MAP_KEYWORDS = List.of(
            "카페", "커피숍", "커피전문점", "맛집", "식당", "음식점", "레스토랑", "한식당",
            "고깃집", "분식", "분식집", "술집", "이자카야", "바", "펍", "포차",
            "빵집", "베이커리", "디저트", "아이스크림",
            "공원", "해변", "해수욕장", "계곡", "산", "호수", "폭포", "섬",
            "쇼핑몰", "백화점", "마트", "시장", "아울렛",
            "박물관", "미술관", "갤러리", "전시관", "전시회",
            "도서관", "스터디카페", "독서실",
            "헬스장", "피트니스", "수영장", "볼링장", "당구장",
            "호텔", "모텔", "펜션", "게스트하우스", "리조트",
            "놀이공원", "워터파크", "동물원", "식물원",
            "병원", "약국", "은행", "편의점",
            "영화관", "공연장", "극장",
            "어디", "위치", "장소", "지도", "근처", "주변", "찾아줘", "알려줘"
    );

    public GeminiService(ChatClient chatClient, ChatModel chatModel, ChatMemory chatMemory, RagService ragService, ShowChatRepository showChatRepository, GptChatRepository gptChatRepository, ScholarSearchService scholarSearchService, VectorStore vectorStore, KakaoMapService kakaoMapService) {
        this.chatClient = chatClient;
        this.chatModel = chatModel;
        this.chatMemory = chatMemory;
        this.ragService = ragService;
        this.showChatRepository = showChatRepository;
        this.gptChatRepository = gptChatRepository;
        this.scholarSearchService = scholarSearchService;
        this.vectorStore = vectorStore;
        this.kakaoMapService = kakaoMapService;
    }

    private String extractLocationWithAI(String userMessage) {
        // 지도관련 프롬프트 명령어 작성
        String keywords = String.join(", ", MAP_KEYWORDS);
        String prompt = """
                다음 문장에서 지도 검색할 장소명을 추출하세요.
                장소 유형 키워드: %s
                
                규칙:
                1. 지역명 + 장소유형 → 둘 다 반환  (예: 홍대 카페)
                2. 장소명만 있으면 → 그대로 반환  (예: 스타벅스)
                3. 장소유형만 있으면 → CURRENT_LOCATION:장소유형  (예: CURRENT_LOCATION:카페)
                4. 특정 불가 → NONE
                5. 특정 지역/역 언급 시 → 반드시 그 지역 장소만 반환
                6. 조사/어미(에서, 의, 으로, 쪽 등) 제거, 행정구역(시/구/동)은 유지
                
                결과만 반환, 설명 금지.
                문장: %s
                """.formatted(keywords, userMessage);

        try {
            // 모델 호출
            String result = this.ModelCall(chatModel, prompt);
            // 모델 컨텍스트와 독립된 새 클라이언트 생성
            if (result == null) return "NONE";
            // 따옴표, 콜론, 대괄호 등 특수문자 제거
            String cleaned = result.trim().replaceAll("[\"':\\[\\]]", "").trim();
            return cleaned.isEmpty() ? "NONE" : cleaned;

        } catch (Exception e) {
            log.error("[extractLocationWithAI] AI 추출 실패: {}", e.getMessage());
            return "NONE";
        }
    }

    //  모델 호출
    private String ModelCall(ChatModel chatModel, String prompt) {
        ChatClient independentClient = ChatClient.builder(chatModel).build();
        return independentClient.prompt()
                .options(GoogleGenAiChatOptions.builder()
                        .maxOutputTokens(200)
                        .build())
                .user(prompt)
                .call()
                .content();

    }

    // MAP_KEYWORDS 선언 아래, extractLocationWithAI() 위에 추가!
    // 메시지에 MAP_KEYWORDS 키가 포함되어 있는지
    private boolean isMapRequest(String message) {
        return MAP_KEYWORDS.stream().anyMatch(message::contains);
    }

    public String sendGeminiAI(MyChatDTO dto, String model, String customPrompt) {
        // 우저 메시지 생성
        UserMessage message = this.userMessageGet(dto);

        // 앱 검색
        String webResults = scholarSearchService.searchWithTavily(dto.getMyChatContents());

        // customPrompt에 이미 RAG 결과가 있으면 중복 호출 안 함
        String ragResult = (customPrompt != null && !customPrompt.isBlank())
                ? ""
                : ragService.findDocument(dto.getMyChatContents()); // 힌트
        // 시스템 프롬프트
        String systemPrompt = getString(customPrompt, webResults, ragResult);
        // showChatKey 스트링으로 저장
        String conversationId = dto.getShowChatKey() != null ? dto.getShowChatKey().toString() : null;
        // 메시지 내역 저장
        List<Message> history = conversationId != null ? chatMemory.get(conversationId) : List.of();

        log.info("[DEBUG] conversationId={}, historySize={}", conversationId, history.size());
        history.forEach(msg -> log.info("[DEBUG] history msg - type={}, text={}", msg.getMessageType(), msg.getText()));

        if (model.contains("image")) {
            return sendGeminiImageDirect(dto.getMyChatContents(), dto.getFiles(), model, systemPrompt,
                    dto.getShowChatKey(), conversationId, history, message, ragResult);
        }
        // 메시지 받기
        String response = this.sendMessage(history, message, model, systemPrompt);


        log.info("[LLM 원본 응답] response={}", response);
        // 위도 구하는 메서드
        response = this.mapLoad(dto, response, conversationId, message);
        // 메모리 저장
        response = this.saveResult(conversationId, message, response, dto.getShowChatKey());
        // 문서 저장
        saveToVectorStore(ragResult, response);

        return response;
    }

    private String saveResult(String conversationId, UserMessage message, String response, Long showChatKey) {
        if (response == null || conversationId == null) {
            log.warn("[saveResult] 응답이 null이거나 conversationId가 없습니다.");
            return response;
        }
        chatMemory.add(conversationId, message);
        String cleanResponse = response.replaceAll("\\[\\[MAP_START:.*?:MAP_END\\]\\]", "").trim();
        chatMemory.add(conversationId, new AssistantMessage(cleanResponse));
        GptChat gptChat = GptChat.builder()
                .gptChatContents(response)
                .showChat(showChatRepository.findById(showChatKey).orElse(null))
                .build();
        gptChatRepository.save(gptChat);
        log.info("[saveResult] 응답 길이={}", response.length());
        return response;
    }


    // 지도 위도 구하는 것
    private String mapLoad(MyChatDTO dto, String response, String conversationId, UserMessage message) {
        // 장소 키워드 감지 → 지도 좌표 붙이기
        if (isMapRequest(dto.getMyChatContents())) {
            String location = extractLocationWithAI(dto.getMyChatContents()); // ① AI로 장소명 추출
            log.info("[search_map] AI 추출 결과={}", location);

            if (location.equals("NONE") || location.isBlank()) {
                // 장소 특정 불가 → 지도 표시 안 함

            } else if (location.startsWith("CURRENT_LOCATION:")) {
                // ② 장소유형만 있는 경우 → 현재 위치 요청
                String placeType = location.split(":")[1];
                response = response + "\n[[MAP_START:CURRENT:" + placeType + ":MAP_END]]";

            } else {
                // ③ 일반 장소 검색
                String coords = kakaoMapService.findMap(location);
                log.info("[search_map] 좌표 결과={}", coords);
                if (coords != null) { // ④ null 체크
                    response = response + "\n[[MAP_START:" + coords + ":MAP_END]]";
                }
            }
        }
        return response;

    }
    // gpt에 메시지 보내는 것
    private String sendMessage(List<Message> history, UserMessage message, String model, String systemPrompt) {
        List<Message> allMessages = new ArrayList<>(history);
        allMessages.add(message);

        GoogleGenAiChatOptions options = GoogleGenAiChatOptions.builder()
                .model(model)
                .temperature(0.7)
                .maxOutputTokens(6024)
                .topP(0.9)
                .topK(100)
                .build();

        return chatClient.prompt()
                .system(systemPrompt)
                .messages(allMessages)
                .options(options)
                .call()
                .content();
    }
    // 사용자 메시지 담는 메서드
    private UserMessage userMessageGet(MyChatDTO dto) {
        if (dto.getFiles() != null && !dto.getFiles().isEmpty()) {
            List<Media> mediaList = dto.getFiles().stream()
                    .filter(f -> f.getMimeType() != null)
                    .map(f -> new Media(MimeTypeUtils.parseMimeType(f.getMimeType()),
                            new ByteArrayResource(Base64.getDecoder().decode(f.getData()))))
                    .toList();
            return UserMessage.builder()
                    .text(dto.getMyChatContents())
                    .media(mediaList)
                    .build();
        } else {
            return UserMessage.builder()
                    .text(dto.getMyChatContents())
                    .build();
        }
    }

    private static @NonNull String getString(String customPrompt, String webResults, String ragResult) {

        String today = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));

        String basePrompt = customPrompt != null && !customPrompt.isBlank() ? customPrompt
                : """
                당신은 JO-GPT 어시스턴트입니다.
                항상 한국어로 친절하게 답변하세요.
                이전 대화 내역을 기억하고 대화 맥락을 유지하며 답변하세요.
                """;

        String datePrompt = """
                [현재 날짜]
                오늘은 %s 입니다.
                최신 정보가 필요한 질문은 반드시 아래 웹 검색 결과를 우선으로 답변하세요.
                웹 검색 결과가 없으면 학습 데이터 기준으로 답변하되,
                "정확한 최신 정보는 직접 확인이 필요합니다."라고 꼭 덧붙이세요.
                """.formatted(today);
        return webResults.isBlank() ? ragResult.isBlank() ? basePrompt + datePrompt : ragResult + basePrompt + datePrompt : ragResult.isBlank() ? basePrompt + datePrompt + webResults : basePrompt + datePrompt + webResults + ragResult;
    }

    private String sendGeminiImageDirect(String userMessage, List<MyChatDTO.FilePartDTO> files, String model,
                                         String systemText, Long showChatKey, String conversationId, List<Message> history, UserMessage message, String ragResult) {

        List<Part> parts = new ArrayList<>();
        parts.add(Part.fromText(userMessage));

        if (files != null) {
            files.stream()
                    .filter(f -> f.getMimeType() != null && f.getData() != null)
                    .forEach(f -> parts.add(Part.fromBytes(Base64.getDecoder().decode(f.getData()), f.getMimeType())));
        }

        Client client = Client.builder().apiKey(geminiApiKey).build();

        Content systemInstruction = Content.builder()
                .role("system")
                .parts(Part.fromText(systemText))
                .build();

        List<Content> contents = new ArrayList<>();
        for (Message msg : history) {
            String role = (msg instanceof AssistantMessage) ? "model" : "user";
            contents.add(Content.builder().role(role).parts(Part.fromText(msg.getText())).build());
        }
        // 현재 사용자 메시지(이미지 포함) 추가 - 없으면 'contents is not specified' 에러 발생!
        contents.add(Content.builder().role("user").parts(parts).build());


        GenerateContentConfig config = GenerateContentConfig.builder()
                .systemInstruction(systemInstruction)
                .responseModalities("TEXT", "IMAGE")
                .maxOutputTokens(2048)
                .build();

        GenerateContentResponse response = client.models.generateContent(model, contents, config);

        StringBuilder textBuilder = new StringBuilder();
        List<Map<String, String>> images = new ArrayList<>();

        response.candidates().orElse(List.of()).forEach(candidate -> candidate.content()
                .ifPresent(content -> content.parts().orElse(List.of()).forEach(part -> {
                    log.info("[ImageGen] part - text={}, inlineData={}", part.text().isPresent(), part.inlineData().isPresent());
                    part.text().ifPresent(textBuilder::append);
                    part.inlineData().ifPresent(blob -> blob.data().ifPresent(data -> {
                        log.info("[ImageGen] 이미지 추출 성공 mimeType={}, size={}bytes", blob.mimeType().orElse("unknown"), data.length);
                        Map<String, String> img = new LinkedHashMap<>();
                        img.put("mimeType", blob.mimeType().orElse("image/png"));
                        img.put("data", Base64.getEncoder().encodeToString(data));
                        images.add(img);
                    }));
                })));

        String textContent = textBuilder.toString();
        log.info("[ImageGen] 결과 - text길이={}, 이미지수={}", textContent.length(), images.size());

        // // 장소 키워드 감지 → 지도 좌표 붙이기
        if (isMapRequest(userMessage)) {
            String location = extractLocationWithAI(userMessage);  // ① AI 추출
            log.info("[search_map] AI 추출 결과={}", location);

            if (location.equals("NONE") || location.isBlank()) {
                // 장소 특정 불가 → 지도 표시 안 함

            } else if (location.startsWith("CURRENT_LOCATION:")) {
                String placeType = location.split(":")[1];
                textContent = textContent + "\n[[MAP_START:CURRENT:" + placeType + ":MAP_END]]";

            } else {
                String coords = kakaoMapService.findMap(location);
                log.info("[search_map] 좌표 결과={}", coords);
                if (coords != null) {
                    textContent = textContent + "\n[[MAP_START:" + coords + ":MAP_END]]";
                }
            }
        } // ← if 블록 여기서 닫기!

        // 항상 실행 - 지도 요청 여부 관계없이 저장/반환
        ShowChat showChat = showChatKey != null ? showChatRepository.findShowChatByShowChatKey(showChatKey).orElse(null) : null;
        log.info("[Memory] conversationId={}, historySize={}", conversationId, history.size());
        chatMemory.add(conversationId, UserMessage.builder().text(userMessage).build());
        String cleanTextContent = textContent.replaceAll("\\[\\[MAP_START:.*?:MAP_END\\]\\]", "").trim();
        chatMemory.add(conversationId, new AssistantMessage(cleanTextContent));
        GptChat gptChat = GptChat.builder()
                .gptChatContents(textContent)
                .showChat(showChat)
                .build();
        gptChatRepository.save(gptChat);
        saveToVectorStore(ragResult, cleanTextContent);

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
        // 벡터 db저장
        return textContent;
    }

    // 벡터 db에 저장 로직
    private void saveToVectorStore(String ragResult, String llmAnswer) {
        log.info("[saveToVectorStore 호출됨] ...");
        log.info("llmAnswer={}", llmAnswer);
        log.info("ragResult={}", ragResult);


        List<Document> existing = vectorStore.similaritySearch(
                SearchRequest.builder()
                        .query(llmAnswer)
                        .topK(1)
                        .similarityThreshold(0.9)
                        .build()
        );

        if (!existing.isEmpty()) {
            log.info("[saveToVectorStore] 유사 문서 이미 존재 → 저장 생략");
            return;
        }
        String context =
                ragResult.isBlank() ? llmAnswer : "\n\n[RAG 검색 결과]\n" + ragResult
                        + "\n\n[AI 답변]\n" + llmAnswer;


        log.info("context{}", context);
        ragService.saveDocument(context, "chat", "대화");
    }
}















