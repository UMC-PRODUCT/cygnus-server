package com.umc.product.recruiting.adapter.in.graphql.dto;

import java.time.Instant;
import java.util.List;

import com.umc.product.form.domain.enums.FormStatus;
import com.umc.product.form.domain.enums.QuestionType;

public record RecruitingApplicationFormStructureGraphQlResponse(
    Long formId,
    Long createdMemberId,
    String title,
    String description,
    FormStatus status,
    boolean anonymous,
    boolean allowDuplicateResponses,
    Instant createdAt,
    Instant updatedAt,
    List<RecruitingFormSectionGraphQlResponse> sections
) {
    public record RecruitingFormSectionGraphQlResponse(
        Long sectionId,
        String title,
        String description,
        Long orderNo,
        List<RecruitingFormQuestionGraphQlResponse> questions
    ) {
    }

    public record RecruitingFormQuestionGraphQlResponse(
        Long questionId,
        String title,
        String description,
        QuestionType type,
        boolean required,
        Long orderNo,
        List<RecruitingFormQuestionOptionGraphQlResponse> options
    ) {
    }

    public record RecruitingFormQuestionOptionGraphQlResponse(
        Long optionId,
        String content,
        Long orderNo,
        boolean other,
        Long nextSectionId
    ) {
    }
}
