package com.umc.product.form.domain;

import java.time.Instant;

import com.umc.product.common.BaseEntity;
import com.umc.product.form.domain.enums.FormResponseStatus;
import com.umc.product.form.domain.exception.FormDomainException;
import com.umc.product.form.domain.exception.FormErrorCode;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@Builder
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "form_response")
public class FormResponse extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "form_id", nullable = false)
    private Form form;

    @Column(name = "respondent_member_id")
    private Long respondentMemberId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private FormResponseStatus status = FormResponseStatus.DRAFT;

    @Column(name = "submitted_at")
    private Instant submittedAt;

    @Column(name = "submitted_ip")
    private String submittedIp;

    @Column(name = "last_saved_at", nullable = false)
    private Instant lastSavedAt;

    /**
     * 익명 응답 인증용 access key 의 SHA-256 해시(hex 64자).
     * <p>
     * 기명 응답은 {@code null}. 익명 응답은 발급 시점에 {@code SecureTokenGenerator.sha256Hex(rawKey)} 로 저장.
     * UNIQUE — 익명 응답 간 hash 충돌 방지 (MySQL 은 NULL 여러 개 허용하므로 기명 여러 행은 문제 없음).
     */
    @Column(name = "response_access_key_hash", unique = true, length = 64)
    private String responseAccessKeyHash;

    public static FormResponse createDraft(Form form, Long respondentMemberId) {
        FormResponse fr = new FormResponse();
        fr.form = form;
        fr.respondentMemberId = respondentMemberId;
        fr.status = FormResponseStatus.DRAFT;
        fr.lastSavedAt = Instant.now();
        return fr;
    }

    /**
     * 익명 draft 생성. {@code respondentMemberId} 는 null 로 남고, {@code responseAccessKeyHash} 에 sha256(rawKey) 저장.
     * <p>
     * raw key 는 발급자(서비스 레이어)가 클라이언트/소비 도메인에 반환하며, 서버에는 저장하지 않는다.
     */
    public static FormResponse createAnonymousDraft(Form form, String responseAccessKeyHash) {
        FormResponse fr = new FormResponse();
        fr.form = form;
        fr.respondentMemberId = null;
        fr.responseAccessKeyHash = responseAccessKeyHash;
        fr.status = FormResponseStatus.DRAFT;
        fr.lastSavedAt = Instant.now();
        return fr;
    }

    public void submit(Instant submittedAt, String submittedIp) {
        if (this.status == FormResponseStatus.SUBMITTED) {
            return;
        }
        this.status = FormResponseStatus.SUBMITTED;
        this.submittedAt = submittedAt;
        this.submittedIp = submittedIp;
    }

    public void updateLastSavedAt(Instant now) {
        this.lastSavedAt = now;
    }

    /**
     * 익명 응답을 로그인 사용자에게 등록한다. {@code respondentMemberId} 와 {@code responseAccessKeyHash} 를 한 번에 갱신해
     * XOR CHECK({@code ck_form_response_identifier_xor}) 를 위반하는 중간 상태를 만들지 않는다.
     * <p>
     * 이미 기명 응답이면 재등록 금지 (방어) — {@link FormErrorCode#FORM_RESPONSE_ALREADY_CLAIMED}.
     * 상위 서비스에서도 사전 검증하지만 도메인 불변조건 방어 목적으로 함께 체크한다.
     */
    public void claimBy(Long memberId) {
        if (this.respondentMemberId != null) {
            throw new FormDomainException(FormErrorCode.FORM_RESPONSE_ALREADY_CLAIMED);
        }
        this.respondentMemberId = memberId;
        this.responseAccessKeyHash = null;
    }

}
