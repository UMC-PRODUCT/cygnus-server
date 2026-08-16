package com.umc.product.recruiting.adapter.in.graphql.dto;

import java.util.List;

import com.umc.product.common.domain.enums.ChallengerTrack;
import com.umc.product.form.domain.enums.QuestionType;
import com.umc.product.recruiting.application.port.in.query.dto.RecruitingAdminFormStructureInfo;
import com.umc.product.recruiting.domain.enums.RecruitingApplicationFormStatus;
import com.umc.product.recruiting.domain.enums.RecruitingFormSectionType;

public record RecruitingAdminFormStructureGraphQlResponse(
    boolean exists,
    Long applicationFormId,
    Long formId,
    String title,
    String description,
    RecruitingApplicationFormStatus status,
    List<SectionGraphQlResponse> sections
) {

    public static RecruitingAdminFormStructureGraphQlResponse from(RecruitingAdminFormStructureInfo info) {
        return new RecruitingAdminFormStructureGraphQlResponse(
            info.exists(),
            info.applicationFormId(),
            info.formId(),
            info.title(),
            info.description(),
            info.status(),
            info.sections().stream().map(SectionGraphQlResponse::from).toList()
        );
    }

    public record SectionGraphQlResponse(
        Long sectionId,
        String clientKey,
        String title,
        String description,
        Long orderNo,
        RecruitingFormSectionType type,
        ChallengerTrack track,
        List<QuestionGraphQlResponse> questions
    ) {

        private static SectionGraphQlResponse from(RecruitingAdminFormStructureInfo.SectionInfo section) {
            return new SectionGraphQlResponse(
                section.sectionId(),
                section.clientKey(),
                section.title(),
                section.description(),
                section.orderNo(),
                section.type(),
                section.track(),
                section.questions().stream().map(QuestionGraphQlResponse::from).toList()
            );
        }
    }

    public record QuestionGraphQlResponse(
        Long questionId,
        String title,
        String description,
        QuestionType type,
        boolean required,
        Long orderNo,
        List<OptionGraphQlResponse> options
    ) {

        private static QuestionGraphQlResponse from(RecruitingAdminFormStructureInfo.QuestionInfo question) {
            return new QuestionGraphQlResponse(
                question.questionId(),
                question.title(),
                question.description(),
                question.type(),
                question.required(),
                question.orderNo(),
                question.options().stream().map(OptionGraphQlResponse::from).toList()
            );
        }
    }

    public record OptionGraphQlResponse(
        Long optionId,
        String content,
        Long orderNo,
        boolean other,
        Long nextSectionId,
        String nextSectionKey
    ) {

        private static OptionGraphQlResponse from(RecruitingAdminFormStructureInfo.OptionInfo option) {
            return new OptionGraphQlResponse(
                option.optionId(),
                option.content(),
                option.orderNo(),
                option.other(),
                option.nextSectionId(),
                option.nextSectionKey()
            );
        }
    }
}
