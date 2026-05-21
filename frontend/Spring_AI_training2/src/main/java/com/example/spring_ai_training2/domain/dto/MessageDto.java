package com.example.spring_ai_training2.domain.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;

public record MessageDto(
        @JsonProperty("id") Long id,
        @JsonProperty("userId") String userId,
        @JsonProperty("content") String content,
        @JsonProperty("type") String type) {
}
