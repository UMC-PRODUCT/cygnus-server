package com.umc.product.member.domain;

import com.umc.product.common.BaseEntity;
import com.umc.product.common.domain.enums.MemberStatus;
import com.umc.product.member.domain.exception.MemberDomainException;
import com.umc.product.member.domain.exception.MemberErrorCode;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "member")
public class Member extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 30) // 한글 10자까지 고려
    private String name;

    @Column(nullable = false, length = 20) // 회원가입 입력은 한글 1~6자
    private String nickname;

    @Column(nullable = false, length = 100, unique = true)
    private String email;

    // DelegatingPasswordEncoder 의 "{id}encoded" prefix 를 포함한 단일 해시 컬럼
    @Column(name = "password_hash", length = 255)
    private String passwordHash;

    @Column(name = "school_id")
    private Long schoolId;  // ID 참조만 (organization 도메인 의존 방지)

    @Column(name = "profile_image_id")
    private String profileImageId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private MemberStatus status;

    // MemberProfile은 Member에서 단방향으로 조회합니다. MemberProfile에서 역으로 올라오는 경우는 없도록 합니다.
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(
        name = "profile_id",
        unique = true
    )
    private MemberProfile profile;

    @Builder
    private Member(String name, String nickname, String email, Long schoolId, String profileImageId) {
        this.name = name;
        this.nickname = nickname;
        this.email = email;
        this.schoolId = schoolId;
        this.profileImageId = profileImageId;
        this.status = MemberStatus.ACTIVE;
    }

    public static Member create(String name, String nickname, String email, Long schoolId, String profileImageId) {
        return Member.builder()
            .name(name)
            .nickname(nickname)
            .email(email)
            .schoolId(schoolId)
            .profileImageId(profileImageId)
            .build();
    }

    // Domain Logic: 프로필 업데이트
    public void updateProfile(String nickname, String profileImageId) {
        validateActive();
        if (nickname != null) {
            this.nickname = nickname;
        }

        if (profileImageId != null) {
            this.profileImageId = profileImageId;
        }
    }

    public void updateProfile(String profileImageId) {
        validateActive();
        this.profileImageId = profileImageId;
    }

    public void changeEmail(String email) {
        validateActive();
        this.email = email;
    }

    /**
     * 유효한 사용자인지 검증 (휴면 회원이 아닌지)
     */
    private void validateActive() {
        if (this.status != MemberStatus.ACTIVE) {
            throw new MemberDomainException(MemberErrorCode.MEMBER_NOT_ACTIVE);
        }
    }

    public void assignProfile(MemberProfile profile) {
        validateActive();
        this.profile = profile;
    }

    public void removeProfile() {
        this.profile = null;
    }

    /**
     * 이메일 기반 자격증명을 최초 등록한다. ADR-017 흐름.
     * <p>
     * email 은 이미 회원 생성 시점에 검증되어 저장되어 있으므로, 본 메서드는 비밀번호 등록만 수행한다.
     * 이미 비밀번호가 등록되어 있는 경우 변경 흐름({@link #changePassword})을 사용해야 하며, 여기서는 중복 등록을 막는다.
     * {@code encodedPassword} 는 반드시 DelegatingPasswordEncoder 로 인코딩된 "{id}encoded" 형식이어야 한다.
     * <p>
     * 비밀번호 형식 관련 검증은 CredentialPolicy 에서 사전에 수행되며, 본 메서드는 NULL CHECK 만 수행한다.
     */
    public void registerCredential(String encodedPassword) {
        validateActive();
        validatePassword(encodedPassword);

        if (this.passwordHash != null) {
            throw new MemberDomainException(MemberErrorCode.CREDENTIAL_ALREADY_REGISTERED);
        }
        this.passwordHash = encodedPassword;
    }

    /**
     * 비밀번호를 변경한다. 자격증명이 등록되어 있지 않으면 거부한다.
     * <p>
     * 로그인 성공 시점의 점진적 rehash(transparent rehash) 호출 경로에서도 사용된다.
     */
    public void changePassword(String encodedPassword) {
        validateActive();
        validatePassword(encodedPassword);

        if (!hasCredential()) {
            throw new MemberDomainException(MemberErrorCode.CREDENTIAL_NOT_REGISTERED);
        }
        this.passwordHash = encodedPassword;
    }

    private void validatePassword(String encodedPassword) {
        if (encodedPassword == null || encodedPassword.isBlank()) {
            throw new MemberDomainException(MemberErrorCode.INVALID_PASSWORD);
        }
    }

    public boolean hasCredential() {
        return this.passwordHash != null;
    }

    // TODO: 탈퇴 및 휴면 처리에 대한 도메인 로직은 추후 추가
}
