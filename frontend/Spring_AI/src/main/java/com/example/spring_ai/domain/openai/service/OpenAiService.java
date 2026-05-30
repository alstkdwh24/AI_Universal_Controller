package com.example.spring_ai.domain.openai.service;

import com.example.spring_ai.domain.openai.dto.CityResponseDTO;
import com.example.spring_ai.domain.openai.entity.ChatEntity;
import com.example.spring_ai.domain.openai.repository.ChatRepository;
import org.springframework.ai.audio.transcription.AudioTranscriptionPrompt;
import org.springframework.ai.audio.transcription.AudioTranscriptionResponse;
import org.springframework.ai.audio.tts.TextToSpeechPrompt;
import org.springframework.ai.audio.tts.TextToSpeechResponse;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.api.Advisor;
import org.springframework.ai.chat.client.advisor.vectorstore.QuestionAnswerAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.ChatMemoryRepository;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.MessageType;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.embedding.Embedding;
import org.springframework.ai.embedding.EmbeddingOptions;
import org.springframework.ai.embedding.EmbeddingRequest;
import org.springframework.ai.embedding.EmbeddingResponse;
import org.springframework.ai.image.ImagePrompt;
import org.springframework.ai.image.ImageResponse;
import org.springframework.ai.openai.*;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.ai.openai.api.OpenAiAudioApi;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.util.List;

@Service
public class OpenAiService {

    private final OpenAiChatModel openAiChatModel;
    private final OpenAiEmbeddingModel openAiEmbeddingModel;
    private final OpenAiImageModel openAiImageModel;
    private final OpenAiAudioSpeechModel openAiAudioSpeechModel;
    private final OpenAiAudioTranscriptionModel openAiAudioTranscriptionModel;

    private final ChatMemoryRepository chatMemoryRepository;

    private final ChatRepository chatRepository;

    private final VectorStore vectorStore;


    public OpenAiService(OpenAiChatModel openAiChatModel, OpenAiEmbeddingModel openAiEmbeddingModel, OpenAiImageModel openAiImageModel, OpenAiAudioSpeechModel openAiAudioSpeechModel, OpenAiAudioTranscriptionModel openAiAudioTranscriptionModel, ChatMemoryRepository chatMemoryRepository, ChatRepository chatRepository, VectorStore vectorStore) {
        this.openAiChatModel = openAiChatModel;
        this.openAiEmbeddingModel = openAiEmbeddingModel;
        this.openAiImageModel = openAiImageModel;
        this.openAiAudioSpeechModel = openAiAudioSpeechModel;
        this.openAiAudioTranscriptionModel = openAiAudioTranscriptionModel;
        this.chatMemoryRepository = chatMemoryRepository;
        this.chatRepository = chatRepository;
        this.vectorStore = vectorStore;
    }

    // 1. chatmodel
    public CityResponseDTO generate(String text) {
        ChatClient chatClient = ChatClient.create(openAiChatModel);
        // 메시지
        SystemMessage systemMessage = new SystemMessage("");
        UserMessage userMessage = new UserMessage(text);
        AssistantMessage assistantMessage = new AssistantMessage("");

        OpenAiChatOptions options = OpenAiChatOptions.builder()
                .model("gpt-4.1-mini")
                .temperature(0.7) // 창의성
                .build();
        // 프롬프트
        Prompt prompt = new Prompt(List.of(systemMessage, userMessage, assistantMessage), options);
        // 요청 및 응답
        return chatClient.prompt(prompt).call().entity(CityResponseDTO.class);

    }

    // 1. chatmodel : response stream

    public Flux<String> generateStream(String text) {
        ChatClient chatClient = ChatClient.create(openAiChatModel);
        String userId = "xxxjjhhh" + "_" + "1";

        // 전체 저장용
        ChatEntity chatEntity = new ChatEntity();
        chatEntity.setUserId(userId);
        chatEntity.setType(MessageType.USER);
        chatEntity.setContent(text);
        //메시지
        ChatMemory chatMemory = MessageWindowChatMemory.builder()
                .maxMessages(10)
                .chatMemoryRepository(chatMemoryRepository)
                .build();
        chatMemory.add(userId, new UserMessage(text));
        // 옵션
        OpenAiChatOptions options = OpenAiChatOptions.builder()
                .model(OpenAiApi.ChatModel.GPT_4_1_MINI.value)
                .temperature(0.7)
                .build();

        // RAG
        Advisor ragAdvisor = QuestionAnswerAdvisor.builder(vectorStore)
                .searchRequest(SearchRequest.builder().similarityThreshold(0.8d). topK(6).build())
                .build();


        // 프롬프트
        Prompt prompt = new Prompt(chatMemory.get(userId), options);

        // 응답 메시지를 저장할 임시 버퍼
        StringBuilder responseBuffer = new StringBuilder();

        //RAG
        // text -> 임베딩
        // 민베딩 -> BD에서 조회
        // 문서를 프롬프트에 붙여서


        return chatClient.prompt(prompt)
                .tools(new ChatTools())
                .advisors(ragAdvisor)
                .stream()
                .content()
                .map(token -> {
                    responseBuffer.append(token);
                    return token;
                }).doOnComplete(() -> {
                    // chatMemory 저장
                    chatMemory.add(userId, new AssistantMessage(responseBuffer.toString()));
                    chatMemoryRepository.saveAll(userId, chatMemory.get(userId));

                    //전체 대화 저장용
                    ChatEntity chatEntity1 = new ChatEntity();
                    chatEntity1.setUserId(userId);
                    chatEntity1.setType(MessageType.ASSISTANT);
                    chatEntity1.setContent(responseBuffer.toString());
                    chatRepository.saveAll(List.of(chatEntity, chatEntity1));
                });
    }

    // chatClien를 사용하는 이유
    // ObservationRegistry: 로깅
    // tools : LLM에게 사용할 퉁을 붙여줌
    // advisors : RAG
    // entity : 응답 데이터 객체 파싱 (call 메소드만)
    // 추상화 : 모델 변경되어도 동일한 메소드
//
//        // 요청 및 응답
//        return openAiChatModel.stream(prompt)
//                .mapNotNull(response -> {String token =response.getResult().getOutput().getText();
//                    responseBuffer.append(token);
//                return token;}).doOnComplete(()->{
//                    chatMemory.add(userId, new AssistantMessage(responseBuffer.toString()));
//                    chatMemoryRepository.saveAll(userId, chatMemory.get(userId));
//
//                    // 전체 대화 저장용
//                    ChatEntity chatEntity1 = new ChatEntity();
//                    chatEntity1.setUserId(userId);
//                    chatEntity1.setType(MessageType.ASSISTANT);
//                    chatEntity1.setContent(responseBuffer.toString());
//                    chatRepository.saveAll(List.of(chatEntity,chatEntity1));
//                });


    // 3. 임베딩 api 호출 메서드

    public List<float[]> generateEmbedding(List<String> texts, String model) {

        // 옵션
        EmbeddingOptions embeddingOptions = EmbeddingOptions.builder()
                .model(model)
                .build();

        // 프롬프트
        EmbeddingRequest prompt = new EmbeddingRequest(texts, embeddingOptions);

        // 요청 및 응답
        EmbeddingResponse response = openAiEmbeddingModel.call(prompt);

        return response.getResults().stream()
                .map(Embedding::getOutput)
                .toList();
    }

    // 3. 이미지 모델 apo 호출 메서드
    public List<String> generateImages(String text, int count, int height, int width) {

        // 옵션
        OpenAiImageOptions imageOptions = OpenAiImageOptions.builder()
                .quality("hd")
                .N(count)
                .height(height)
                .width(width)
                .build();

        // 프롬프트
        ImagePrompt prompt = new ImagePrompt(text, imageOptions);

        ImageResponse response = openAiImageModel.call(prompt);

        return response.getResults().stream()
                .map(image -> image.getOutput().getUrl())
                .toList();
    }

    // TTS
    public byte[] tts(String text) {
        // 옵션
        OpenAiAudioSpeechOptions speechOptions = OpenAiAudioSpeechOptions.builder()
                .responseFormat(OpenAiAudioApi.SpeechRequest.AudioResponseFormat.MP3)
                .speed(1.0)
                .model(OpenAiAudioApi.TtsModel.TTS_1.value)
                .build();

        TextToSpeechPrompt prompt = new TextToSpeechPrompt(text, speechOptions);

        TextToSpeechResponse response = openAiAudioSpeechModel.call(prompt);
        return response.getResult().getOutput();
    }


    // STT
    public String stt(Resource audioFile) {
        // 옵션
        OpenAiAudioApi.TranscriptResponseFormat responseFormat = OpenAiAudioApi.TranscriptResponseFormat.VTT;
        OpenAiAudioTranscriptionOptions openAiAudioTranscriptionOptions = OpenAiAudioTranscriptionOptions.builder()
                .language("ko")  // 인식할 언어
                .prompt("Ask not this, but ask that")  // 응성 인식 전 참고할 텍스트 프롬프트
                .temperature(0f)
                .model(OpenAiAudioApi.TtsModel.TTS_1.value)
                .responseFormat(responseFormat) // VTT 자막형식
                .build();

        AudioTranscriptionPrompt prompt = new AudioTranscriptionPrompt(audioFile, openAiAudioTranscriptionOptions);

        AudioTranscriptionResponse response = openAiAudioTranscriptionModel.call(prompt);
        return response.getResult().getOutput();
    }
}
