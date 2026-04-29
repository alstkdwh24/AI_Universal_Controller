package com.example.jo_gpt_program.gpt.service;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.document.Document;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.google.genai.GoogleGenAiChatOptions;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Qualifier;
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

import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;

@SuppressWarnings("ALL")
@Service("contentsService")
@Slf4j
public class ContentsService {

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

        /* 내가 적은 치탱 BD 저장 */
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
                                .createTimeLogs(CreateTimeLogs.builder()

                                                .build()) // 여기서 builder로 생성만 하면, 위에서 추가한 @CreatedDate가 저장 시점에 시간을 자동
                                                          // 기입한다.
                                .build();

                MyChat myChat = myChatRepository.save(chat);

                return myChat.getMyChatContents();
        }

        // /* 제미나이한테 보낼 메시지랑 제미나이 API연결 기존 제미나이 교체 */
        // public String sendGemini(MyChatDTO dto, String geminiKey) {

        // Map<String, Object> body = Map.of("contents",
        // List.of(Map.of("parts", List.of(Map.of("text", dto.getMyChatContents())))));

        // ResponseEntity<String> response = restTemplate.postForEntity(
        // "https://generativelanguage.googleapis.com/v1beta/models/gemini-flash-latest:generateContent?key="
        // + geminiKey,
        // body, String.class);
        // log.debug("response gemini :{}", response);
        // String body2 = response.getBody();
        // if (body2 == null) {
        // return "{\"error\": \"No response from Gemini\"}";
        // }

        // // TODO Auto-generated method stub
        // return response.getBody();
        // }

        /* 채팅방 만드는 메서드 */
        @Transactional
        public Long createChat(String authHeader, MyChatDTO dto) {
                Members members = this.authHeader(authHeader);
                // 1. ShowChat 생성 및 '저장' (save 호출!)
                ShowChat showChat = ShowChat.builder()
                                .members(members)
                                .build();
                ShowChat showChat1 = showChatRepository.save(showChat); // DB에서 키값을 받아옴

                log.debug("showChat1={}", showChat1.getShowChatKey());
                // 2. 이제 키값이 있는 showChat을 MyChat에 연결
                MyChat myChat = MyChat.builder()
                                .showChat(showChat1)
                                .member(members) // Member 키도 잊지 말고 넣어주세요!
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

        // 채팅 리스트 불러오기
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
        /* 제미나이한테 보낼 메시지랑 제미나이 API연결 기존 제미나이 교체 */
        public String sendGeminiAI(MyChatDTO dto, String model, String customPrompt) {
                return chatClient.prompt() // 사실상 빌더 역할
                                .system(customPrompt != null ? customPrompt
                                                : "당신은 JO-GPT 어시스턴트입니다. 항상 한국어로 친절하게 답변하세요.") // 시스템 프롬프트 설정
                                .user(dto.getMyChatContents()) // 사용자 메시지 설정
                                .options(GoogleGenAiChatOptions.builder()
                                                .model(model) // 모델
                                                .temperature(0.7) // 응답의 창의성 조절
                                                .maxOutputTokens(1024) // 문장의 길이 제한
                                                .topP(0.9) // 응답 다양성
                                                .topK(100) // 다음 단어 후보를 몇개로 제한할지 설정
                                                .build())
                                .call()
                                .content(); // 응답에서 텍스트 콘텐츠 추출
        }

        // ----------------------------------- RAG: 문서 기반 답변
        // ---------------------------------------------

        public String ragAnswer(MyChatDTO dto) {
                // 1. 유사 문서 검색
                List<Document> docs = vectorStore.similaritySearch(
                                SearchRequest.builder().query(dto.getMyChatContents()).topK(5) // 상위 5개 문서 검색
                                                .build());

                // 2. 컨텍스트 조합
                String context = docs.stream() // 리스트를 스트림으로 변환
                                .map(Document::getText) // 2. 각 문서에서 텍스트만
                                .collect(Collectors.joining("\n---\n")); // 문서들을 구분자와 함께 하나의 문자열로 결합

                // 제미나이한테 전달
                return chatClient.prompt()
                                .system("당신은 JO-GPT 어시스턴트입니다. 다음 컨텍스트를 참고하여 질문에 답변하세요:\n" + context)
                                .user(dto.getMyChatContents())

                                .call()
                                .content();
        }

        // 문서 저장 (RAG용) 벡터 데이터베이스에다가 저장하는 메서드
        public void saveDocument(String context) {
                vectorStore.add(List.of(new Document(context)));
        }

        // -----임베딩 (*텍스트 -> 벡터)-----------
        // 잊고 있었을 거지만 이 형태도 있었다 그리고 임베딩은 정밀도보다 속도 / 메모리가 중요
        public float[] getEmbedding(String text) {
                return embeddingModel.embed(text); // 임베딩 변환 메서드
        }

        // ------------------- 학술검색 + AI 답변-------------------------------
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
                                scholarResults.isEmpty() ? "검색 결과 없음" : scholarResults, // 첫 번째 %s
                                customPrompt != null ? customPrompt : "친절하고 학술적으로 답변하세요." // 두 번째 %s
                );

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

        // 잠시 stream, map, collect, builder, prompt 설명
        // 배열, 리스트를 스트림으로 변환 즉, 리스트를 하나씩 처리할수 있는 파이프라인으로 변환
        //

        // ------------------- 학술검색 + RAG 답변-------------------------------
        public String sendWithRagAndScholar(MyChatDTO dto, String model, String customPrompt) {

                // 1. 벡터 DB 검색
                List<Document> docs = vectorStore.similaritySearch(
                                SearchRequest.builder()
                                                .query(dto.getMyChatContents())
                                                .topK(3)
                                                .build());
                // RAG 로 검색한 문서들을 하나로 합치는 과정
                String ragContext = docs.stream()
                                .map(Document::getText)
                                .collect(Collectors.joining("\n---\n"));

                // 2. 학술 검색
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
