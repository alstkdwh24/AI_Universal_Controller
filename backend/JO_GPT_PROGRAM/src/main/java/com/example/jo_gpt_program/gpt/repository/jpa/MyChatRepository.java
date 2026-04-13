package com.example.jo_gpt_program.gpt.repository.jpa;

import com.example.entitycom.entity.member.Members;
import com.example.entitycom.entity.member.MyChat;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository("myChatRepository")
public interface MyChatRepository extends JpaRepository <MyChat, Long>{


    List<MyChat> findMyChatByMember(Members members);
}
