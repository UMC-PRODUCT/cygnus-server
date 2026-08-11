package com.umc.product.project.adapter.in.graphql.dto;

import java.util.List;
import java.util.Set;

import com.umc.product.common.domain.enums.ChallengerPart;
import com.umc.product.form.domain.enums.QuestionType;
import com.umc.product.global.graphql.relay.GlobalId;
import com.umc.product.global.graphql.relay.GlobalIdTypes;
import com.umc.product.project.application.port.in.query.dto.ApplicationFormInfo;
import com.umc.product.project.domain.enums.FormSectionType;

public record ProjectApplicationFormGraphQlResponse(
    String projectId,
    String applicationFormId,
    String title,
    String description,
    List<ApplicationFormSectionGraphQlResponse> sections
) {
    public static ProjectApplicationFormGraphQlResponse from(ApplicationFormInfo info) {
        return new ProjectApplicationFormGraphQlResponse(
            GlobalId.encode(GlobalIdTypes.PROJECT, info.projectId()),
            GlobalId.encode(GlobalIdTypes.FORM, info.applicationFormId()),
            info.title(),
            info.description(),
            info.sections().stream().map(ApplicationFormSectionGraphQlResponse::from).toList()
        );
    }

    public record ApplicationFormSectionGraphQlResponse(
        String sectionId,
        FormSectionType type,
        Set<ChallengerPart> allowedParts,
        String title,
        String description,
        long orderNo,
        List<ApplicationFormQuestionGraphQlResponse> questions
    ) {
        public static ApplicationFormSectionGraphQlResponse from(ApplicationFormInfo.SectionInfo info) {
            return new ApplicationFormSectionGraphQlResponse(
                GlobalId.encode(GlobalIdTypes.FORM_SECTION, info.sectionId()),
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
        String questionId,
        QuestionType type,
        String title,
        String description,
        boolean required,
        long orderNo,
        List<ApplicationFormOptionGraphQlResponse> options
    ) {
        public static ApplicationFormQuestionGraphQlResponse from(ApplicationFormInfo.QuestionInfo info) {
            return new ApplicationFormQuestionGraphQlResponse(
                GlobalId.encode(GlobalIdTypes.FORM_QUESTION, info.questionId()),
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
        String optionId,
        String content,
        long orderNo,
        boolean other
    ) {
        public static ApplicationFormOptionGraphQlResponse from(ApplicationFormInfo.OptionInfo info) {
            return new ApplicationFormOptionGraphQlResponse(
                GlobalId.encode(GlobalIdTypes.FORM_OPTION, info.optionId()),
                info.content(),
                info.orderNo(),
                info.isOther()
            );
        }
    }
}
