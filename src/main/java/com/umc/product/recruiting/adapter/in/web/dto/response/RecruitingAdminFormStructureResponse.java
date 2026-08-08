package com.umc.product.recruiting.adapter.in.web.dto.response;

import java.util.List;

import com.umc.product.common.domain.enums.ChallengerTrack;
import com.umc.product.form.domain.enums.QuestionType;
import com.umc.product.recruiting.application.port.in.query.dto.RecruitingAdminFormStructureInfo;
import com.umc.product.recruiting.domain.enums.RecruitingApplicationFormStatus;
import com.umc.product.recruiting.domain.enums.RecruitingFormSectionType;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "운영진 편집기용 지원 Form 전체 구조 응답")
public record RecruitingAdminFormStructureResponse(
    @Schema(description = "지원 Form 생성 여부. false면 나머지 값은 비어 있습니다.", example = "true")
    boolean exists,
    @Schema(description = "리크루팅 지원 Form ID", example = "50") Long applicationFormId,
    @Schema(description = "Form 모듈 Form ID", example = "100") Long formId,
    @Schema(description = "Form 제목") String title,
    @Schema(description = "Form 설명") String description,
    @Schema(description = "지원 Form 상태", example = "DRAFT") RecruitingApplicationFormStatus status,
    @Schema(description = "section 목록") List<SectionResponse> sections
) {

    public static RecruitingAdminFormStructureResponse from(RecruitingAdminFormStructureInfo info) {
        return new RecruitingAdminFormStructureResponse(
            info.exists(),
            info.applicationFormId(),
            info.formId(),
            info.title(),
            info.description(),
            info.status(),
            info.sections().stream().map(SectionResponse::from).toList()
        );
    }

    @Schema(description = "지원 Form section")
    public record SectionResponse(
        @Schema(description = "section ID", example = "300") Long sectionId,
        @Schema(description = "저장된 section을 가리키는 편집기 key", example = "section-300") String clientKey,
        @Schema(description = "section 제목") String title,
        @Schema(description = "section 설명") String description,
        @Schema(description = "section 순서", example = "1") Long orderNo,
        @Schema(description = "section 유형", example = "COMMON") RecruitingFormSectionType type,
        @Schema(description = "TRACK section의 모집 트랙. COMMON이면 null입니다.") ChallengerTrack track,
        @Schema(description = "question 목록") List<QuestionResponse> questions
    ) {

        private static SectionResponse from(RecruitingAdminFormStructureInfo.SectionInfo section) {
            return new SectionResponse(
                section.sectionId(),
                section.clientKey(),
                section.title(),
                section.description(),
                section.orderNo(),
                section.type(),
                section.track(),
                section.questions().stream().map(QuestionResponse::from).toList()
            );
        }
    }

    @Schema(description = "지원 Form question")
    public record QuestionResponse(
        @Schema(description = "question ID", example = "400") Long questionId,
        @Schema(description = "question 제목") String title,
        @Schema(description = "question 설명") String description,
        @Schema(description = "question 유형", example = "SHORT_ANSWER") QuestionType type,
        @Schema(description = "필수 응답 여부", example = "true") boolean required,
        @Schema(description = "question 순서", example = "1") Long orderNo,
        @Schema(description = "option 목록") List<OptionResponse> options
    ) {

        private static QuestionResponse from(RecruitingAdminFormStructureInfo.QuestionInfo question) {
            return new QuestionResponse(
                question.questionId(),
                question.title(),
                question.description(),
                question.type(),
                question.required(),
                question.orderNo(),
                question.options().stream().map(OptionResponse::from).toList()
            );
        }
    }

    @Schema(description = "지원 Form question option")
    public record OptionResponse(
        @Schema(description = "option ID", example = "500") Long optionId,
        @Schema(description = "option 내용") String content,
        @Schema(description = "option 순서", example = "1") Long orderNo,
        @Schema(description = "기타 option 여부", example = "false") boolean other,
        @Schema(description = "조건부 이동 대상 section ID") Long nextSectionId,
        @Schema(description = "조건부 이동 대상 section의 편집기 key", example = "section-300") String nextSectionKey
    ) {

        private static OptionResponse from(RecruitingAdminFormStructureInfo.OptionInfo option) {
            return new OptionResponse(
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
