package com.umc.product.project.adapter.in.graphql.dto;

import java.util.List;
import java.util.Set;

import com.umc.product.common.domain.enums.ChallengerPart;
import com.umc.product.form.domain.enums.QuestionType;
import com.umc.product.project.application.port.in.query.dto.ApplicationFormInfo;
import com.umc.product.project.domain.enums.FormSectionType;

public record ProjectApplicationFormGraphQlResponse(
    Long projectId,
    Long applicationFormId,
    String title,
    String description,
    List<ProjectApplicationFormSectionGraphQlResponse> sections
) {
    public static ProjectApplicationFormGraphQlResponse from(ApplicationFormInfo info) {
        return new ProjectApplicationFormGraphQlResponse(
            info.projectId(),
            info.applicationFormId(),
            info.title(),
            info.description(),
            info.sections().stream().map(ProjectApplicationFormSectionGraphQlResponse::from).toList()
        );
    }

    public record ProjectApplicationFormSectionGraphQlResponse(
        Long sectionId,
        FormSectionType type,
        Set<ChallengerPart> allowedParts,
        String title,
        String description,
        long orderNo,
        List<ProjectFormQuestionGraphQlResponse> questions
    ) {
        public static ProjectApplicationFormSectionGraphQlResponse from(ApplicationFormInfo.SectionInfo info) {
            return new ProjectApplicationFormSectionGraphQlResponse(
                info.sectionId(),
                info.type(),
                info.allowedParts(),
                info.title(),
                info.description(),
                info.orderNo(),
                info.questions().stream().map(ProjectFormQuestionGraphQlResponse::from).toList()
            );
        }
    }

    public record ProjectFormQuestionGraphQlResponse(
        Long questionId,
        QuestionType type,
        String title,
        String description,
        boolean required,
        long orderNo,
        List<ProjectFormOptionGraphQlResponse> options
    ) {
        public static ProjectFormQuestionGraphQlResponse from(ApplicationFormInfo.QuestionInfo info) {
            return new ProjectFormQuestionGraphQlResponse(
                info.questionId(),
                info.type(),
                info.title(),
                info.description(),
                info.isRequired(),
                info.orderNo(),
                info.options().stream().map(ProjectFormOptionGraphQlResponse::from).toList()
            );
        }
    }

    public record ProjectFormOptionGraphQlResponse(
        Long optionId,
        String content,
        long orderNo,
        boolean other
    ) {
        public static ProjectFormOptionGraphQlResponse from(ApplicationFormInfo.OptionInfo info) {
            return new ProjectFormOptionGraphQlResponse(
                info.optionId(),
                info.content(),
                info.orderNo(),
                info.isOther()
            );
        }
    }
}
