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
import org.springframework.ai.content.Media;
import org.springframework.ai.google.genai.GoogleGenAiChatOptions;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.stereotype.Service;
import org.springframework.util.MimeTypeUtils;

import java.util.*;

@Slf4j
@Service("geminiService")
public class GeminiService {
    @Value("${spring.llm.key}")
    private String geminiApiKey;
    private final ChatClient chatClient;
    private final ChatMemory chatMemory;
    private final ShowChatRepository showChatRepository;
    private final GptChatRepository gptChatRepository;
    private final ScholarSearchService scholarSearchService;
    private final NaverApiService naverApiService;

    public GeminiService(ChatClient chatClient, ChatMemory chatMemory, ShowChatRepository showChatRepository, GptChatRepository gptChatRepository, ScholarSearchService scholarSearchService, NaverApiService naverApiService) {
        this.chatClient = chatClient;
        this.chatMemory = chatMemory;
        this.showChatRepository = showChatRepository;
        this.gptChatRepository = gptChatRepository;
        this.scholarSearchService = scholarSearchService;
        this.naverApiService = naverApiService;
    }

    // 장소 관련 키워드 감지
    private boolean isMapRequest(String message) {
        List<String> mapKeywords = List.of(
            // 장소 유형
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
            // 위치 키워드
            "어디", "위치", "장소", "지도", "근처", "주변", "찾아줘", "알려줘"
        );
        return mapKeywords.stream().anyMatch(message::contains);
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

        String webResults = scholarSearchService.searchWithTavily(dto.getMyChatContents());
        String systemPrompt = getString(customPrompt, webResults);
        String conversationId = dto.getShowChatKey() != null ? dto.getShowChatKey().toString() : null;
        List<Message> history = conversationId != null ? chatMemory.get(conversationId) : List.of();

        if (model.contains("image")) {
            return sendGeminiImageDirect(dto.getMyChatContents(), dto.getFiles(), model, systemPrompt,
                    dto.getShowChatKey(), conversationId, history, message);
        }

        List<Message> allMessages = new ArrayList<>(history);
        allMessages.add(message);

        GoogleGenAiChatOptions options = GoogleGenAiChatOptions.builder()
                .model(model)
                .temperature(0.7)
                .maxOutputTokens(1024)
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
            String location = dto.getMyChatContents();
            log.info("[search_map] 검색어={}", location);
            String coords = naverApiService.findMap(location);
            log.info("[search_map] 좌표 결과={}", coords);
            response = response + "\n[[MAP:" + coords + "]]";
        }

        if (response != null && conversationId != null) {
            chatMemory.add(conversationId, message);
            // MAP 태그 제거 후 memory 저장 (히스토리에 [[MAP:...]] 남으면 다음 응답에 중복 생성됨)
            String cleanResponse = response.replaceAll("\\[\\[MAP:.*?\\]\\]", "").trim();
            chatMemory.add(conversationId, new AssistantMessage(cleanResponse));
            GptChat gptChat = GptChat.builder()
                    .GptChatContents(response)
                    .showChat(showChatRepository.findById(dto.getShowChatKey()).orElse(null))
                    .build();
            gptChatRepository.save(gptChat);
            log.info("[sendGeminiAI] 응답 길이={}", response.length());
        } else {
            log.warn("[sendGeminiAI] 응답이 null이거나 conversationId가 없습니다.");
        }
        return response;
    }

    private static @NonNull String getString(String customPrompt, String webResults) {
        String basePrompt = customPrompt != null && !customPrompt.isBlank() ? customPrompt
                : """
                당신은 JO-GPT 어시스턴트입니다.
                항상 한국어로 친절하게 답변하세요.
                이전 대화 내역을 기억하고 대화 맥락을 유지하며 답변하세요.
                """;
        return webResults.isBlank() ? basePrompt : basePrompt + "\n\n[웹 검색 결과]\n" + webResults;
    }

    private String sendGeminiImageDirect(String userMessage, List<MyChatDTO.FilePartDTO> files, String model,
                                         String systemText, Long showChatKey, String conversationId, List<Message> history, UserMessage message) {

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

        // 장소 키워드 감지 → 지도 좌표 붙이기
        if (isMapRequest(userMessage)) {
            String location = userMessage;
            log.info("[search_map] 검색어={}", location);
            String coords = naverApiService.findMap(location);
            log.info("[search_map] 좌표 결과={}", coords);
            textContent = textContent + "\n[[MAP:" + coords + "]]";
        }

        ShowChat showChat = showChatKey != null ? showChatRepository.findShowChatByShowChatKey(showChatKey).orElse(null) : null;
        log.info("[Memory] conversationId={}, historySize={}", conversationId, history.size());
        chatMemory.add(conversationId, UserMessage.builder().text(userMessage).build());
        // MAP 태그 제거 후 memory 저장 (히스토리에 [[MAP:...]] 남으면 다음 응답에 중복 생성됨)
        String cleanTextContent = textContent.replaceAll("\\[\\[MAP:.*?\\]\\]", "").trim();
        chatMemory.add(conversationId, new AssistantMessage(cleanTextContent));
        GptChat gptChat = GptChat.builder()
                .GptChatContents(textContent)
                .showChat(showChat)
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
}


