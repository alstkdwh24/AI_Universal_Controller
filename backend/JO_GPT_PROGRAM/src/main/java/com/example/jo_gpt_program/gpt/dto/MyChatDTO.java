package com.example.jo_gpt_program.gpt.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class MyChatDTO {
    Long myChatKey;
    @JsonProperty("myChatContents") // JSON의 "gptContents"를 이 필드에 담겠다는 의미
    String myChatContents;
    String myChatImage;
    LocalDateTime myChatRegistration;
    Long showChatKey;

    public void changeMyChatContents(String myChatContents) {
        this.myChatContents = myChatContents;
    }

    public void changeMyChatImage(String myChatImage) {
        this.myChatImage = myChatImage;
    }

    public void changeMyChatRegistration(LocalDateTime myChatRegistration) {
        this.myChatRegistration = myChatRegistration;
    }

    public void myChatKey(Long myChatKey) {
        this.myChatKey = myChatKey;

    }
}
