package com.umc.product.project.adapter.in.graphql.dto;

import java.util.List;
import java.util.Set;

import com.umc.product.common.domain.enums.ChallengerPart;
import com.umc.product.project.application.port.in.query.dto.ApplicationFormInfo;
import com.umc.product.project.domain.enums.FormSectionType;
import com.umc.product.form.domain.enums.QuestionType;

public record ProjectApplicationFormGraphQlResponse(
    Long projectId,
    Long applicationFormId,
    String title,
    String description,
    List<ApplicationFormSectionGraphQlResponse> sections
) {
    public static ProjectApplicationFormGraphQlResponse from(ApplicationFormInfo info) {
        return new ProjectApplicationFormGraphQlResponse(
            info.projectId(),
            info.applicationFormId(),
            info.title(),
            info.description(),
            info.sections().stream().map(ApplicationFormSectionGraphQlResponse::from).toList()
        );
    }

    public record ApplicationFormSectionGraphQlResponse(
        Long sectionId,
        FormSectionType type,
        Set<ChallengerPart> allowedParts,
        String title,
        String description,
        long orderNo,
        List<ApplicationFormQuestionGraphQlResponse> questions
    ) {
        public static ApplicationFormSectionGraphQlResponse from(ApplicationFormInfo.SectionInfo info) {
            return new ApplicationFormSectionGraphQlResponse(
                info.sectionId(),
                info.type(),
                info.allowedParts(),
                info.title(),
                info.description(),
                info.orderNo(),
                info.questions().stream().map(ApplicationFormQuestionGraphQlResponse::from).toList()
            );
        }
    }

    public record ApplicationFormQuestionGraphQlResponse(
        Long questionId,
        QuestionType type,
        String title,
        String description,
        boolean required,
        long orderNo,
        List<ApplicationFormOptionGraphQlResponse> options
    ) {
        public static ApplicationFormQuestionGraphQlResponse from(ApplicationFormInfo.QuestionInfo info) {
            return new ApplicationFormQuestionGraphQlResponse(
                info.questionId(),
                info.type(),
                info.title(),
                info.description(),
                info.isRequired(),
                info.orderNo(),
                info.options().stream().map(ApplicationFormOptionGraphQlResponse::from).toList()
            );
        }
    }

    public record ApplicationFormOptionGraphQlResponse(
        Long optionId,
        String content,
        long orderNo,
        boolean other
    ) {
        public static ApplicationFormOptionGraphQlResponse from(ApplicationFormInfo.OptionInfo info) {
            return new ApplicationFormOptionGraphQlResponse(
                info.optionId(),
                info.content(),
                info.orderNo(),
                info.isOther()
            );
        }
    }
}
