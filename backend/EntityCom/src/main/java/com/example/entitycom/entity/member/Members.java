package com.example.entitycom.entity.member;


import com.example.entitycom.entity.chat.ShowChat;
import com.example.entitycom.entity.device.Devices;
import com.example.entitycom.entity.gpt.GPT;
import com.example.entitycom.entity.gpt.GPTSessions;
import com.example.entitycom.entity.log.CreateTimeLogs;
import com.example.entitycom.entity.token.MonthlyUsage;
import com.example.entitycom.entity.token.PlanLimits;
import com.example.entitycom.entity.token.UserTokens;
import com.example.entitycom.enums.Role;
import io.hypersistence.utils.hibernate.id.Tsid;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

//유저 아이디 엔터티
@Entity
@Table(name = "members")
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Members {
    @Id
    @Tsid
    @Column(name = "member_key", unique = true)
    private Long memberKey;

    @Column(name = "member_id", nullable = false )
    private String memberId;


    @Column(name = "name")
    private String name;

    @Column(name = "phone")
    private String phone;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role; // 역할 (ADMIN, USER, GUEST 등)

    @Column(name = "gender")
    private String gender;

    @Column(name = "age")
    private Integer age;

    @Column(name = "extra_settings", columnDefinition = "TEXT")
    private String extraSettings; //추가 설정 리스트 (개인 설정 리스트)

    //회원 정보를 참조하기 위한 조인
    @OneToOne(mappedBy = "member", cascade = CascadeType.ALL, orphanRemoval = true)
    private UserCredentials userCredentials;


    //SNS로그인을 참조하기 위한 조인
    @OneToOne(mappedBy = "member", cascade = CascadeType.ALL, orphanRemoval = true)
    private AuthProviders authProviders;


    //기기 정보를 참조하기 위한 조인
    @OneToOne(mappedBy = "member", cascade = CascadeType.ALL, orphanRemoval = true)
    private Devices devices;


    //GPT 대화내용을 참조하기 위한 조인
    @OneToMany(mappedBy = "member", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<GPT> gptList = new ArrayList<>();


    //GPT 세션 정보를 참조하기 위한 조인
    @OneToMany(mappedBy = "member", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<GPTSessions> gptSessionsList = new ArrayList<>();

    //월 토큰 사용량을 참조하기 위한 조인
    @OneToMany(mappedBy = "member", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<MonthlyUsage> monthlyUsageList = new ArrayList<>();

    //사용자의 토큰사용량 제한을 참조하기 위한 조인
    @OneToMany(mappedBy = "member", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<PlanLimits> planLimits = new ArrayList<>();

    //유저의 토큰 사용량을 참조하기 위한 조인
    @OneToMany(mappedBy = "member", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<UserTokens> userTokenList = new ArrayList<>();


    // 생성 시간 관련 조인
    @OneToOne(mappedBy = "member", cascade = CascadeType.ALL, orphanRemoval = true)
    private CreateTimeLogs createTimeLogs;

    @OneToMany(mappedBy = "members", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ShowChat> showChatList = new ArrayList<>();


    public void updateProfile(String name, String phone, String gender, Integer age) {
        if (name != null) this.name = name;
        if (phone != null) this.phone = phone;
        if (age != null) this.age = age;
        if (gender != null) this.gender = gender;
    }

    public void changeMemberId(String memberId) {
        if (memberId != null && !memberId.isEmpty()) {
            this.memberId = memberId;
        }
    }


    public void changeGender(String gender) {
        if (gender != null && !gender.isEmpty()) {
            this.gender = gender;
        }
    }

    public void changeAge(Integer age) {
        if (age != null) {
            this.age = age;
        }
    }


    public void changeName(String name) {
        if (name != null && !name.isEmpty()) {
            this.name = name;
        }
    }

    public void changePhone(String phone) {
        if (phone != null && !phone.isEmpty()) {
            this.phone = phone;
        }
    }

    public void changeRole(Role role) {
        if (role != null) {
            this.role = role;
        }
    }
//삽입할때는 Members 엔티티에서 저장한 후에, 자식 객체를 담아두는 것이 좋습니다. 이렇게 하면 연관관계가 명확해지고, 데이터 일관성을 유지할 수 있습니다.
    public void changeUserCredentials(UserCredentials userCredentials) {
        this.userCredentials = userCredentials;
        if (userCredentials != null && userCredentials.getMember() != this) {
            userCredentials.changeId(this);
        }
    }
}
