package com.umc.product.form.domain;

import com.umc.product.common.BaseEntity;
import com.umc.product.form.domain.enums.FormStatus;
import com.umc.product.form.domain.exception.FormDomainException;
import com.umc.product.form.domain.exception.FormErrorCode;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Builder
@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "form")
public class Form extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "created_member_id", nullable = false)
    private Long createdMemberId;

    @Column(nullable = false)
    private String title;

    @Column(length = 500)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private FormStatus status;

    @Column(name = "is_anonymous", nullable = false)
    private boolean isAnonymous;

    @Column(name = "allow_duplicate_responses", nullable = false)
    private boolean allowDuplicateResponses;

    /**
     * Draft 생성을 위해서는 제목은 필수로 입력하여야 합니다.
     */
    public static Form createDraft(String title, Long createdMemberId) {
        return createDraft(title, createdMemberId, false);
    }

    public static Form createDraft(String title, Long createdMemberId, boolean allowDuplicateResponses) {
        return createDraft(title, createdMemberId, null, allowDuplicateResponses);
    }

    public static Form createDraft(
        String title,
        Long createdMemberId,
        String description,
        boolean allowDuplicateResponses
    ) {
        Form form = new Form();

        form.title = title;
        form.createdMemberId = createdMemberId;
        form.description = description;
        form.status = FormStatus.DRAFT;
        form.allowDuplicateResponses = allowDuplicateResponses;

        return form;
    }

    public static Form createPublished(Long createdMemberId, String title, boolean isAnonymous) {
        return createPublished(createdMemberId, title, isAnonymous, false);
    }

    public static Form createPublished(
        Long createdMemberId,
        String title,
        boolean isAnonymous,
        boolean allowDuplicateResponses
    ) {
        Form form = new Form();
        form.createdMemberId = createdMemberId;
        form.title = title;
        form.isAnonymous = isAnonymous;
        form.status = FormStatus.PUBLISHED;
        form.allowDuplicateResponses = allowDuplicateResponses;

        return form;
    }

    public boolean isPublished() {
        return this.status == FormStatus.PUBLISHED;
    }

    public void publish() {
        if (isPublished()) {
            throw new FormDomainException(FormErrorCode.FORM_ALREADY_PUBLISHED);
        }
        validateStatus(FormStatus.DRAFT);
        this.status = FormStatus.PUBLISHED;
    }

    public void unpublish() {
        validateStatus(FormStatus.PUBLISHED);
        this.status = FormStatus.DRAFT;
    }

    public void close() {
        validateStatus(FormStatus.PUBLISHED);
        this.status = FormStatus.CLOSED;
    }

    /**
     * 폼 메타데이터 부분 업데이트.
     * null 인 필드는 기존 값 유지. 임시저장 단계에서 어느 필드든 부분 변경 가능하도록 모든 파라미터 nullable.
     */
    public void update(String title, String description, Boolean isAnonymous) {
        update(title, description, isAnonymous, null);
    }

    public void update(String title, String description, Boolean isAnonymous, Boolean allowDuplicateResponses) {
        update(title, description, isAnonymous, allowDuplicateResponses, false);
    }

    public void update(
        String title,
        String description,
        Boolean isAnonymous,
        Boolean allowDuplicateResponses,
        boolean clearDescription
    ) {
        if (title != null) {
            this.title = title;
        }
        if (clearDescription) {
            this.description = null;
        } else if (description != null) {
            this.description = description;
        }
        if (isAnonymous != null) {
            this.isAnonymous = isAnonymous;
        }
        if (allowDuplicateResponses != null) {
            this.allowDuplicateResponses = allowDuplicateResponses;
        }
    }

    private void validateStatus(FormStatus expectedStatus) {
        if (status != expectedStatus) {
            throw new FormDomainException(FormErrorCode.FORM_INVALID_TRANSITION);
        }
    }
}
