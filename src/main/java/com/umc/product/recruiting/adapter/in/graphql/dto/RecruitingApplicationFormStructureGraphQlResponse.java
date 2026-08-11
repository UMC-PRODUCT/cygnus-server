package com.umc.product.recruiting.adapter.in.graphql.dto;

import java.time.Instant;
import java.util.List;

import com.umc.product.form.application.port.in.query.dto.FormWithStructureInfo;
import com.umc.product.form.domain.enums.FormStatus;
import com.umc.product.form.domain.enums.QuestionType;
import com.umc.product.global.graphql.relay.GlobalId;
import com.umc.product.global.graphql.relay.GlobalIdTypes;

public record RecruitingApplicationFormStructureGraphQlResponse(
    String formId,
    String createdMemberId,
    String title,
    String description,
    FormStatus status,
    boolean anonymous,
    boolean allowDuplicateResponses,
    Instant createdAt,
    Instant updatedAt,
    List<SectionGraphQlResponse> sections
) {

    public static RecruitingApplicationFormStructureGraphQlResponse from(FormWithStructureInfo info) {
        return new RecruitingApplicationFormStructureGraphQlResponse(
            GlobalId.encode(GlobalIdTypes.FORM, info.formId()),
            GlobalId.encode(GlobalIdTypes.MEMBER, info.createdMemberId()),
            info.title(),
            info.description(),
            info.status(),
            info.isAnonymous(),
            info.allowDuplicateResponses(),
            info.createdAt(),
            info.updatedAt(),
            info.sections().stream().map(SectionGraphQlResponse::from).toList()
        );
    }

    public record SectionGraphQlResponse(
        String sectionId,
        String title,
        String description,
        Long orderNo,
        List<QuestionGraphQlResponse> questions
    ) {

        private static SectionGraphQlResponse from(FormWithStructureInfo.SectionWithQuestions section) {
            return new SectionGraphQlResponse(
                GlobalId.encode(GlobalIdTypes.FORM_SECTION, section.sectionId()),
                section.title(),
                section.description(),
                section.orderNo(),
                section.questions().stream().map(QuestionGraphQlResponse::from).toList()
            );
        }
    }

    public record QuestionGraphQlResponse(
        String questionId,
        String title,
        String description,
        QuestionType type,
        boolean required,
        Long orderNo,
        List<OptionGraphQlResponse> options
    ) {

        private static QuestionGraphQlResponse from(FormWithStructureInfo.QuestionWithOptions question) {
            return new QuestionGraphQlResponse(
                GlobalId.encode(GlobalIdTypes.FORM_QUESTION, question.questionId()),
                question.title(),
                question.description(),
                question.type(),
                question.isRequired(),
                question.orderNo(),
                question.options().stream().map(OptionGraphQlResponse::from).toList()
            );
        }
    }

    public record OptionGraphQlResponse(
        String optionId,
        String content,
        Long orderNo,
        boolean other,
        String nextSectionId
    ) {

        private static OptionGraphQlResponse from(FormWithStructureInfo.Option option) {
            return new OptionGraphQlResponse(
                GlobalId.encode(GlobalIdTypes.FORM_OPTION, option.optionId()),
                option.content(),
                option.orderNo(),
                option.isOther(),
                option.nextSectionId() == null
                    ? null
                    : GlobalId.encode(GlobalIdTypes.FORM_SECTION, option.nextSectionId())
            );
        }
    }
}
