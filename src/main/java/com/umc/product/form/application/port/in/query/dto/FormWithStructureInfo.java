package com.umc.product.form.application.port.in.query.dto;

import java.time.Instant;
import java.util.List;

import com.umc.product.form.domain.enums.FormStatus;
import com.umc.product.form.domain.enums.QuestionType;

import lombok.Builder;

/**
 * 폼의 전체 구조(섹션 -> 질문 -> 옵션)를 중첩 포함한 DTO.
 * 편집기 초기 로딩/응답자 UI에서 단일 호출로 전체 구조를 얻을 때 사용.
 */
@Builder
public record FormWithStructureInfo(
    Long formId,
    Long createdMemberId,
    String title,
    String description,
    FormStatus status,
    boolean isAnonymous,
    boolean allowDuplicateResponses,
    Instant createdAt,
    Instant updatedAt,
    List<SectionWithQuestions> sections
) {

    @Builder
    public record SectionWithQuestions(
        Long sectionId,
        String title,
        String description,
        Long orderNo,
        List<QuestionWithOptions> questions
    ) {
    }

    @Builder
    public record QuestionWithOptions(
        Long questionId,
        String title,
        String description,
        QuestionType type,
        boolean isRequired,
        Long orderNo,
        List<Option> options
    ) {
    }

    @Builder
    public record Option(
        Long optionId,
        String content,
        Long orderNo,
        boolean isOther,
        Long nextSectionId
    ) {
    }
}
