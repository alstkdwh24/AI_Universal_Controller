package com.example.jo_gpt_program.gpt.service;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.google.genai.GoogleGenAiChatOptions;
import org.springframework.stereotype.Service;

@Service
public class YouTubeSummaryService {

    private final ChatModel chatModel;

    public YouTubeSummaryService(ChatModel chatModel) {
        this.chatModel = chatModel;
    }
    // 유튜브 URL 형식에 따라 영상 ID 추출
    private String extractVideoId(String url) {
        if (url.contains("v=")) {
            return url.split("v=")[1].split("&")[0];
        } else if (url.contains("youtu.be/")) {
            return url.split("youtu.be/")[1].split("\\?")[0];
        }
        throw new IllegalArgumentException("Invalid YouTube URL");
    }
    // 자바에서 Python 스트립트를 실행하서 유튜브 자막을 가져오는 코드
    public String getTranscript(String videoId) throws Exception {
        // 파이썬 스크립트 실행
        ProcessBuilder pb = new ProcessBuilder("python", "-c",
                "from youtube_transcript_api import YouTubeTranscriptApi;" +
                "t=YouTubeTranscriptApi.get_transcript('" + videoId + "',languages=['ko','en']);" +
                "print(' '.join([x['text'] for x in t]))");
        // 에러 메시지도 출력 스트림에 합침
        pb.redirectErrorStream(true);
        // 파이썬 프로세스 실제 실행 시작
        Process p = pb.start();
        // 파이썬이 출력한 내용 자바로 가져오기
        return new String(p.getInputStream().readAllBytes());
    }

    public String summarize(String youtubeUrl) throws Exception {
        String videoId = extractVideoId(youtubeUrl);
        // videoId로 유튜브 자막 API 호출
        // 자막 전체 택스트를 String으로 반환
        String transcript = getTranscript(videoId);
        // 프롬프트 설정
        String prompt = "다음 유튜브 영상 자막을 한국어로 핵심만 요약해줘:\n\n" + transcript;
        return ChatClient.builder(chatModel).build()
                .prompt()
                .options(GoogleGenAiChatOptions.builder()
                        .maxOutputTokens(2048)
                        .build())
                .user(prompt)
                .call()
                .content();
    }
}

