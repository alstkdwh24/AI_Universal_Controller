package com.example.jo_gpt_program.gpt.repository.jpa;

import com.example.entitycom.entity.member.MemberPrompt;
import com.example.entitycom.entity.member.Members;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MemberPromptRepository extends JpaRepository<MemberPrompt, Long> {
    List<MemberPrompt> findByMember(Members member);
    Optional<MemberPrompt> findByMemberAndIsActiveTrue(Members member);
}
