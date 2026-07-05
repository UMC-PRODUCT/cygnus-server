package com.umc.product.feedback.adapter.in.web.dto.response;

import java.util.List;

import com.umc.product.feedback.application.port.in.query.dto.UserFeedbackTemplateInfo;
import com.umc.product.feedback.domain.enums.UserFeedbackContext;
import com.umc.product.feedback.domain.enums.UserFeedbackTargetType;
import com.umc.product.form.application.port.in.query.dto.FormWithStructureInfo;
import com.umc.product.form.domain.enums.QuestionType;

import lombok.Builder;

/**
 * 사용자 피드백 템플릿 조회 응답.
 * <p>
 * 활성 템플릿이 없을 경우 Controller가 {@code ApiResponse.result = null}로 반환합니다.
 */
@Builder
public record GetUserFeedbackTemplateResponse(
    Long templateId,
    UserFeedbackContext context,
    UserFeedbackTargetType targetType,
    FeedbackForm form
) {

    @Builder
    public record FeedbackForm(
        Long formId,
        String title,
        String description,
        List<FeedbackSection> sections
    ) {
        public static FeedbackForm from(FormWithStructureInfo form) {
            return FeedbackForm.builder()
                .formId(form.formId())
                .title(form.title())
                .description(form.description())
                .sections(form.sections().stream().map(FeedbackSection::from).toList())
                .build();
        }
    }

    @Builder
    public record FeedbackSection(
        Long sectionId,
        String title,
        String description,
        Long orderNo,
        List<FeedbackQuestion> questions
    ) {
        public static FeedbackSection from(FormWithStructureInfo.SectionWithQuestions section) {
            return FeedbackSection.builder()
                .sectionId(section.sectionId())
                .title(section.title())
                .description(section.description())
                .orderNo(section.orderNo())
                .questions(section.questions().stream().map(FeedbackQuestion::from).toList())
                .build();
        }
    }

    @Builder
    public record FeedbackQuestion(
        Long questionId,
        String title,
        String description,
        QuestionType type,
        boolean isRequired,
        Long orderNo,
        List<FeedbackOption> options
    ) {
        public static FeedbackQuestion from(FormWithStructureInfo.QuestionWithOptions question) {
            return FeedbackQuestion.builder()
                .questionId(question.questionId())
                .title(question.title())
                .description(question.description())
                .type(question.type())
                .isRequired(question.isRequired())
                .orderNo(question.orderNo())
                .options(question.options().stream().map(FeedbackOption::from).toList())
                .build();
        }
    }

    @Builder
    public record FeedbackOption(
        Long optionId,
        String content,
        Long orderNo,
        boolean isOther
    ) {
        public static FeedbackOption from(FormWithStructureInfo.Option option) {
            return FeedbackOption.builder()
                .optionId(option.optionId())
                .content(option.content())
                .orderNo(option.orderNo())
                .isOther(option.isOther())
                .build();
        }
    }

    public static GetUserFeedbackTemplateResponse from(UserFeedbackTemplateInfo info) {
        return GetUserFeedbackTemplateResponse.builder()
            .templateId(info.templateId())
            .context(info.context())
            .targetType(info.targetType())
            .form(FeedbackForm.from(info.form()))
            .build();
    }
}
