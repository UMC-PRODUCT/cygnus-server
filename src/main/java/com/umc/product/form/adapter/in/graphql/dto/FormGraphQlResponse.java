package com.umc.product.form.adapter.in.graphql.dto;

import java.time.Instant;
import java.util.List;

import com.umc.product.form.application.port.in.query.dto.FormWithStructureInfo;
import com.umc.product.form.domain.enums.FormStatus;
import com.umc.product.form.domain.enums.QuestionType;

public record FormGraphQlResponse(
    Long formId,
    Long createdMemberId,
    String title,
    String description,
    FormStatus status,
    boolean anonymous,
    boolean allowDuplicateResponses,
    Instant createdAt,
    Instant updatedAt,
    List<Section> sections
) {

    public static FormGraphQlResponse from(FormWithStructureInfo info) {
        return new FormGraphQlResponse(
            info.formId(),
            info.createdMemberId(),
            info.title(),
            info.description(),
            info.status(),
            info.isAnonymous(),
            info.allowDuplicateResponses(),
            info.createdAt(),
            info.updatedAt(),
            info.sections().stream().map(Section::from).toList()
        );
    }

    public record Section(
        Long sectionId,
        String title,
        String description,
        Long orderNo,
        List<Question> questions
    ) {

        private static Section from(FormWithStructureInfo.SectionWithQuestions info) {
            return new Section(
                info.sectionId(),
                info.title(),
                info.description(),
                info.orderNo(),
                info.questions().stream().map(Question::from).toList()
            );
        }
    }

    public record Question(
        Long questionId,
        QuestionType type,
        String title,
        String description,
        boolean required,
        Long orderNo,
        List<Option> options
    ) {

        private static Question from(FormWithStructureInfo.QuestionWithOptions info) {
            return new Question(
                info.questionId(),
                info.type(),
                info.title(),
                info.description(),
                info.isRequired(),
                info.orderNo(),
                info.options().stream().map(Option::from).toList()
            );
        }
    }

    public record Option(
        Long optionId,
        String content,
        Long orderNo,
        boolean other,
        Long nextSectionId
    ) {

        private static Option from(FormWithStructureInfo.Option info) {
            return new Option(
                info.optionId(),
                info.content(),
                info.orderNo(),
                info.isOther(),
                info.nextSectionId()
            );
        }
    }
}
