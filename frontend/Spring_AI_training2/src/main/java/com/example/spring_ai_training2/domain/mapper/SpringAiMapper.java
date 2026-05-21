package com.example.spring_ai_training2.domain.mapper;

import com.example.spring_ai_training2.domain.dto.MessageDto;
import com.example.spring_ai_training2.domain.entity.Chat;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface SpringAiMapper {
    int insertMessage(@Param("dto") MessageDto messageDto,@Param("randomNumber") int randomNumber);

    List<Chat> showHistory(MessageDto dto, String userId);
}
