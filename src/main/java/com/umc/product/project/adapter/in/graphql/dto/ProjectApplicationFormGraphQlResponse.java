package com.umc.product.project.adapter.in.graphql.dto;

import java.util.List;
import java.util.Set;

import com.umc.product.common.domain.enums.ChallengerPart;
import com.umc.product.form.domain.enums.QuestionType;

public record ProjectApplicationFormGraphQlResponse(
    Long projectId,
    Long applicationFormId,
    String title,
    String description,
    List<ProjectApplicationFormSectionGraphQlResponse> sections
) {
    public record ProjectApplicationFormSectionGraphQlResponse(
        Long sectionId,
        ProjectFormSectionType type,
        Set<ChallengerPart> allowedParts,
        String title,
        String description,
        long orderNo,
        List<ProjectApplicationFormQuestionGraphQlResponse> questions
    ) {
    }

    public record ProjectApplicationFormQuestionGraphQlResponse(
        Long questionId,
        QuestionType type,
        String title,
        String description,
        boolean required,
        long orderNo,
        List<ProjectApplicationFormOptionGraphQlResponse> options
    ) {
    }

    public record ProjectApplicationFormOptionGraphQlResponse(
        Long optionId,
        String content,
        long orderNo,
        boolean other,
        Long nextSectionId
    ) {
    }
}
