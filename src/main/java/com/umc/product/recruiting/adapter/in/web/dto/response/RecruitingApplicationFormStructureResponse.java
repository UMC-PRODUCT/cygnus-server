package com.umc.product.recruiting.adapter.in.web.dto.response;

import java.time.Instant;
import java.util.List;

import com.umc.product.form.application.port.in.query.dto.FormWithStructureInfo;
import com.umc.product.form.domain.enums.FormStatus;
import com.umc.product.form.domain.enums.QuestionType;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "지원자가 선택한 트랙 범위의 리크루팅 Form 구조")
public record RecruitingApplicationFormStructureResponse(
    @Schema(description = "Form ID") Long formId,
    @Schema(description = "Form 생성 회원 ID") Long createdMemberId,
    @Schema(description = "모집명으로 사용하는 Form 제목") String title,
    @Schema(description = "Form 설명") String description,
    @Schema(description = "Form 상태") FormStatus status,
    @Schema(description = "익명 응답 허용 여부") boolean anonymous,
    @Schema(description = "중복 응답 허용 여부") boolean allowDuplicateResponses,
    @Schema(description = "Form 생성 시각") Instant createdAt,
    @Schema(description = "Form 수정 시각") Instant updatedAt,
    @Schema(description = "선택 트랙 범위에 포함된 section 목록") List<SectionResponse> sections
) {

    public static RecruitingApplicationFormStructureResponse from(FormWithStructureInfo info) {
        return new RecruitingApplicationFormStructureResponse(
            info.formId(),
            info.createdMemberId(),
            info.title(),
            info.description(),
            info.status(),
            info.isAnonymous(),
            info.allowDuplicateResponses(),
            info.createdAt(),
            info.updatedAt(),
            info.sections().stream().map(SectionResponse::from).toList()
        );
    }

    @Schema(description = "Form section")
    public record SectionResponse(
        Long sectionId,
        String title,
        String description,
        Long orderNo,
        List<QuestionResponse> questions
    ) {

        private static SectionResponse from(FormWithStructureInfo.SectionWithQuestions section) {
            return new SectionResponse(
                section.sectionId(),
                section.title(),
                section.description(),
                section.orderNo(),
                section.questions().stream().map(QuestionResponse::from).toList()
            );
        }
    }

    @Schema(description = "Form question")
    public record QuestionResponse(
        Long questionId,
        String title,
        String description,
        QuestionType type,
        boolean required,
        Long orderNo,
        List<OptionResponse> options
    ) {

        private static QuestionResponse from(FormWithStructureInfo.QuestionWithOptions question) {
            return new QuestionResponse(
                question.questionId(),
                question.title(),
                question.description(),
                question.type(),
                question.isRequired(),
                question.orderNo(),
                question.options().stream().map(OptionResponse::from).toList()
            );
        }
    }

    @Schema(description = "Form question option")
    public record OptionResponse(
        Long optionId,
        String content,
        Long orderNo,
        boolean other,
        @Schema(description = "조건부 이동 대상 section ID") Long nextSectionId
    ) {

        private static OptionResponse from(FormWithStructureInfo.Option option) {
            return new OptionResponse(
                option.optionId(),
                option.content(),
                option.orderNo(),
                option.isOther(),
                option.nextSectionId()
            );
        }
    }
}
