package com.umc.product.recruiting.adapter.in.graphql.converter;

import com.umc.product.form.application.port.in.query.dto.FormWithStructureInfo;
import com.umc.product.recruiting.adapter.in.graphql.dto.RecruitingApplicationFormStructureGraphQlResponse;
import com.umc.product.recruiting.adapter.in.graphql.dto.RecruitingApplicationFormStructureGraphQlResponse.RecruitingFormQuestionGraphQlResponse;
import com.umc.product.recruiting.adapter.in.graphql.dto.RecruitingApplicationFormStructureGraphQlResponse.RecruitingFormQuestionOptionGraphQlResponse;
import com.umc.product.recruiting.adapter.in.graphql.dto.RecruitingApplicationFormStructureGraphQlResponse.RecruitingFormSectionGraphQlResponse;

public final class RecruitingApplicationFormStructureGraphQlConverter {

    private RecruitingApplicationFormStructureGraphQlConverter() {
    }

    public static RecruitingApplicationFormStructureGraphQlResponse from(FormWithStructureInfo info) {
        return new RecruitingApplicationFormStructureGraphQlResponse(
            info.formId(),
            info.createdMemberId(),
            info.title(),
            info.description(),
            info.status(),
            info.isAnonymous(),
            info.allowDuplicateResponses(),
            info.createdAt(),
            info.updatedAt(),
            info.sections().stream().map(RecruitingApplicationFormStructureGraphQlConverter::sectionFrom).toList()
        );
    }

    private static RecruitingFormSectionGraphQlResponse sectionFrom(
        FormWithStructureInfo.SectionWithQuestions section
    ) {
        return new RecruitingFormSectionGraphQlResponse(
            section.sectionId(),
            section.title(),
            section.description(),
            section.orderNo(),
            section.questions().stream()
                .map(RecruitingApplicationFormStructureGraphQlConverter::questionFrom)
                .toList()
        );
    }

    private static RecruitingFormQuestionGraphQlResponse questionFrom(
        FormWithStructureInfo.QuestionWithOptions question
    ) {
        return new RecruitingFormQuestionGraphQlResponse(
            question.questionId(),
            question.title(),
            question.description(),
            question.type(),
            question.isRequired(),
            question.orderNo(),
            question.options().stream()
                .map(RecruitingApplicationFormStructureGraphQlConverter::optionFrom)
                .toList()
        );
    }

    private static RecruitingFormQuestionOptionGraphQlResponse optionFrom(FormWithStructureInfo.Option option) {
        return new RecruitingFormQuestionOptionGraphQlResponse(
            option.optionId(),
            option.content(),
            option.orderNo(),
            option.isOther(),
            option.nextSectionId()
        );
    }
}
