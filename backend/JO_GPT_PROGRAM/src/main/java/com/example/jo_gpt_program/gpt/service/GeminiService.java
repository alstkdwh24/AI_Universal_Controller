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
import org.springframework.ai.google.genai.GoogleGenAiChatOptions;
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

    public GeminiService(ChatClient chatClient, ChatModel chatModel, ChatMemory chatMemory, RagService ragService, ShowChatRepository showChatRepository, GptChatRepository gptChatRepository, ScholarSearchService scholarSearchService, KakaoMapService kakaoMapService) {
        this.chatClient = chatClient;
        this.chatModel = chatModel;
        this.chatMemory = chatMemory;
        this.ragService = ragService;
        this.showChatRepository = showChatRepository;
        this.gptChatRepository = gptChatRepository;
        this.scholarSearchService = scholarSearchService;
        this.kakaoMapService = kakaoMapService;
    }

    private String extractLocationWithAI(String userMessage) {
        // 지도관련 프롬프트 명령어 작성
        String keywords = String.join(", ", MAP_KEYWORDS);
        String prompt = """
                다음 문장에서 지도 검색에 사용할 장소명을 추출하세요.
                
                장소 유형 키워드:
                """ + keywords + """
                
                규칙:
                1. 지역명 + 장소유형이면 → 둘 다 반환
                   예) "홍대 카페 찾아줘"   → 홍대 카페
                   예) "강남역 맛집 알려줘" → 강남역 맛집
                
                2. 장소명만 있으면 → 그대로 반환
                   예) "스타벅스 찾아줘"    → 스타벅스
                   예) "경복궁 알려줘"      → 경복궁
                
                3. 장소유형만 있으면 → CURRENT_LOCATION 반환
                   예) "근처 카페 알려줘"   → CURRENT_LOCATION:카페
                   예) "주변 맛집 찾아줘"   → CURRENT_LOCATION:맛집
                   예) "편의점 어디야"      → CURRENT_LOCATION:편의점
                
                4. 장소 특정 불가능 → NONE 반환
                   예) "오늘 날씨 어때"     → NONE
                5. 사용자가 특정 지역명이나 역명 등을 언급하면 반드시 그 지역 or 역 근처에 있는 장소만 검색하고 추천해줘
                    다른 지역 장소는 절대 포함하지마
                
                6. 직접 장소를 언급하진 않았지만
                   문맥상 장소와 관련된 키워드가 있으면
                   → CURRENT_LOCATION:추출한 장소유형
                
                   예) "치킨 먹으러 갈 만한 곳"  → CURRENT_LOCATION:치킨집
                   예) "데이트 코스 추천해줘"     → CURRENT_LOCATION:데이트코스
                   예) "주말에 어디 가면 좋을까"  → CURRENT_LOCATION:관광지
                        6. 조사/어미 제거 규칙:
                           장소명 뒤에 붙는 아래 조사/어미는 반드시 제거하고 순수 장소명만 반환하세요.
                           제거 대상: 에, 에서, 에서는, 에서도, 의, 에는, 에도,
                                      로, 으로, 로는, 으로는, 로부터, 으로부터,
                                      쪽, 쪽으로, 방면, 방향,
                                      이랑, 랑, 와, 과, 보다, 처럼, 같은
                
                           행정구역 단위는 유지하세요 (시, 구, 동, 읍, 면, 군):
                           예) "시흥시에 맛집"    → 시흥시 맛집  (시흥시는 유지!)
                           예) "강남구에서 카페"  → 강남구 카페
                           예) "홍대에서 맛집"    → 홍대 맛집
                           예) "제주도의 카페"    → 제주도 카페
                           예) "부산으로 여행"    → CURRENT_LOCATION:관광지  (장소유형 불명확)
                           예) "신촌 쪽 맛집"    → 신촌 맛집
                           예) "인천 방면 카페"   → 인천 카페
                
                추출한 결과만 반환하고 다른 말은 절대 하지 마세요.
                
                문장:
                """ + userMessage;

        try {
            // 이미지 모델 컨텍스트와 독립된 새 클라이언트 생성
            ChatClient independentClient = ChatClient.builder(chatModel).build();
            String result = independentClient.prompt()
                    .options(GoogleGenAiChatOptions.builder()
                            .maxOutputTokens(200)
                            .build())
                    .user(prompt)
                    .call()
                    .content();
            if (result == null) return "NONE";
            // 따옴표, 콜론, 대괄호 등 특수문자 제거
            String cleaned = result.trim().replaceAll("[\"':\\[\\]]", "").trim();
            return cleaned.isEmpty() ? "NONE" : cleaned;
        } catch (Exception e) {
            log.error("[extractLocationWithAI] AI 추출 실패: {}", e.getMessage());
            return "NONE";
        }
    }

    // MAP_KEYWORDS 선언 아래, extractLocationWithAI() 위에 추가!
    // 메시지에 MAP_KEYWORDS 키가 포함되어 있는지
    private boolean isMapRequest(String message) {
        return MAP_KEYWORDS.stream().anyMatch(message::contains);
    }

    public String sendGeminiAI(MyChatDTO dto, String model, String customPrompt) {
        UserMessage message;
        if (dto.getFiles() != null && !dto.getFiles().isEmpty()) {
            List<Media> mediaList = dto.getFiles().stream()
                    .filter(f -> f.getMimeType() != null)
                    .map(f -> new Media(MimeTypeUtils.parseMimeType(f.getMimeType()),
                            new ByteArrayResource(Base64.getDecoder().decode(f.getData()))))
                    .toList();
            message = UserMessage.builder()
                    .text(dto.getMyChatContents())
                    .media(mediaList)
                    .build();
        } else {
            message = UserMessage.builder()
                    .text(dto.getMyChatContents())
                    .build();
        }
        // 앱 검색
        String webResults = scholarSearchService.searchWithTavily(dto.getMyChatContents());
        String ragResult = ragService.findDocument(dto.getMyChatContents());
        String systemPrompt = getString(customPrompt, webResults, ragResult);
        String conversationId = dto.getShowChatKey() != null ? dto.getShowChatKey().toString() : null;
        List<Message> history = conversationId != null ? chatMemory.get(conversationId) : List.of();
        log.info("[DEBUG] conversationId={}, historySize={}", conversationId, history.size());
        history.forEach(msg -> log.info("[DEBUG] history msg - type={}, text={}", msg.getMessageType(), msg.getText()));

        if (model.contains("image")) {
            return sendGeminiImageDirect(dto.getMyChatContents(), dto.getFiles(), model, systemPrompt,
                    dto.getShowChatKey(), conversationId, history, message, ragResult);
        }

        List<Message> allMessages = new ArrayList<>(history);
        allMessages.add(message);

        GoogleGenAiChatOptions options = GoogleGenAiChatOptions.builder()
                .model(model)
                .temperature(0.7)
                .maxOutputTokens(6024)
                .topP(0.9)
                .topK(100)
                .build();

        String response = chatClient.prompt()
                .system(systemPrompt)
                .messages(allMessages)
                .options(options)
                .call()
                .content();

        log.info("[LLM 원본 응답] response={}", response);

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
        if (response != null && conversationId != null) {
            chatMemory.add(conversationId, message);
            // MAP 태그 제거 후 memory 저장 (히스토리에 [[MAP:...]] 남으면 다음 응답에 중복 생성됨)
            String cleanResponse = response.replaceAll("\\[\\[MAP_START:.*?:MAP_END\\]\\]", "").trim();
            chatMemory.add(conversationId, new AssistantMessage(cleanResponse));
            GptChat gptChat = GptChat.builder()
                    .gptChatContents(response)
                    .showChat(showChatRepository.findById(dto.getShowChatKey()).orElse(null))
                    .build();
            gptChatRepository.save(gptChat);
            log.info("[sendGeminiAI] 응답 길이={}", response.length());
        } else {
            log.warn("[sendGeminiAI] 응답이 null이거나 conversationId가 없습니다.");
        }
        saveToVectorStore(ragResult, response);

        return response;
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
        return webResults.isBlank() ? ragResult.isBlank() ? basePrompt + datePrompt : ragResult + basePrompt + datePrompt : ragResult.isBlank()?  basePrompt + datePrompt + webResults : basePrompt + datePrompt + webResults + ragResult;
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
        saveToVectorStore( ragResult, cleanTextContent);

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
    private void saveToVectorStore( String ragResult, String llmAnswer){
        log.info("[saveToVectorStore 호출됨] ...");
        log.info("llmAnswer={}", llmAnswer);
        log.info("ragResult={}", ragResult);

            String context =
                    ragResult.isBlank() ? llmAnswer : "\n\n[RAG 검색 결과]\n" + ragResult
                            + "\n\n[AI 답변]\n" + llmAnswer;



        log.info("context{}", context);
        ragService.saveDocument(context, "chat", "대화");
    }
}















