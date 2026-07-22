package com.umc.product.feedback;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.umc.product.feedback.adapter.in.web.dto.request.SubmitUserFeedbackResponseRequest;
import com.umc.product.feedback.adapter.in.web.dto.response.GetUserFeedbackTemplateResponse;
import com.umc.product.feedback.adapter.in.web.dto.response.UserFeedbackSubmitResponse;
import com.umc.product.feedback.application.port.in.query.dto.UserFeedbackTemplateInfo;
import com.umc.product.feedback.domain.UserFeedbackTemplate;
import com.umc.product.feedback.domain.enums.UserFeedbackContext;
import com.umc.product.feedback.domain.enums.UserFeedbackTargetType;
import com.umc.product.feedback.domain.exception.FeedbackDomainException;
import com.umc.product.feedback.domain.exception.FeedbackErrorCode;
import com.umc.product.form.application.port.in.query.dto.FormWithStructureInfo;
import com.umc.product.form.domain.enums.QuestionType;

class FeedbackDtoTest {

    @Test
    @DisplayName("피드백 템플릿은 생성 시 활성 상태와 참조 Form을 보존한다")
    void 피드백_템플릿은_생성_시_활성_상태와_참조_Form을_보존한다() {
        UserFeedbackTemplate template = UserFeedbackTemplate.create(
            UserFeedbackContext.APPLICATION_SUBMITTED,
            UserFeedbackTargetType.NEW_CHALLENGER,
            200L
        );

        assertThat(template.getContext()).isEqualTo(UserFeedbackContext.APPLICATION_SUBMITTED);
        assertThat(template.getTargetType()).isEqualTo(UserFeedbackTargetType.NEW_CHALLENGER);
        assertThat(template.getFormId()).isEqualTo(200L);
        assertThat(template.isActive()).isTrue();
    }

    @Test
    @DisplayName("제출 요청은 모든 answer 값을 command로 손실 없이 변환한다")
    void 제출_요청은_모든_answer_값을_command로_손실_없이_변환한다() {
        SubmitUserFeedbackResponseRequest request = new SubmitUserFeedbackResponseRequest(
            100L,
            List.of(new SubmitUserFeedbackResponseRequest.UserFeedbackAnswerItem(
                1L,
                "답변",
                List.of(2L),
                List.of("file-id")
            ))
        );

        var command = request.toCommand(10L);

        assertThat(command.templateId()).isEqualTo(100L);
        assertThat(command.respondentMemberId()).isEqualTo(10L);
        assertThat(command.answers()).singleElement().satisfies(answer -> {
            assertThat(answer.questionId()).isEqualTo(1L);
            assertThat(answer.textValue()).isEqualTo("답변");
            assertThat(answer.selectedOptionIds()).containsExactly(2L);
            assertThat(answer.fileIds()).containsExactly("file-id");
        });
    }

    @Test
    @DisplayName("Form 중첩 구조를 피드백 응답 구조로 변환한다")
    void Form_중첩_구조를_피드백_응답_구조로_변환한다() {
        UserFeedbackTemplateInfo info = UserFeedbackTemplateInfo.builder()
            .templateId(100L)
            .context(UserFeedbackContext.MATCHING_COMPLETED)
            .targetType(UserFeedbackTargetType.EXPERIENCED_CHALLENGER)
            .form(form())
            .build();

        GetUserFeedbackTemplateResponse response = GetUserFeedbackTemplateResponse.from(info);

        assertThat(response.templateId()).isEqualTo(100L);
        assertThat(response.form().formId()).isEqualTo(200L);
        assertThat(response.form().sections()).singleElement().satisfies(section -> {
            assertThat(section.sectionId()).isEqualTo(300L);
            assertThat(section.questions()).singleElement().satisfies(question -> {
                assertThat(question.questionId()).isEqualTo(400L);
                assertThat(question.options()).singleElement().satisfies(option -> {
                    assertThat(option.optionId()).isEqualTo(500L);
                    assertThat(option.isOther()).isTrue();
                });
            });
        });
    }

    @Test
    @DisplayName("제출 응답과 feedback 예외 code 계약을 보존한다")
    void 제출_응답과_feedback_예외_code_계약을_보존한다() {
        assertThat(UserFeedbackSubmitResponse.from(300L).formResponseId()).isEqualTo(300L);
        assertThat(FeedbackErrorCode.USER_FEEDBACK_TEMPLATE_NOT_FOUND.getCode()).isEqualTo("FEEDBACK-0001");
        assertThat(new FeedbackDomainException(FeedbackErrorCode.USER_FEEDBACK_TEMPLATE_NOT_FOUND))
            .extracting("baseCode")
            .isEqualTo(FeedbackErrorCode.USER_FEEDBACK_TEMPLATE_NOT_FOUND);
        assertThat(new FeedbackDomainException(
            FeedbackErrorCode.USER_FEEDBACK_TEMPLATE_NOT_FOUND,
            "custom"
        )).hasMessageContaining("custom");
    }

    private FormWithStructureInfo form() {
        return FormWithStructureInfo.builder()
            .formId(200L)
            .title("사용자 피드백")
            .description("설명")
            .sections(List.of(FormWithStructureInfo.SectionWithQuestions.builder()
                .sectionId(300L)
                .title("섹션")
                .description("섹션 설명")
                .orderNo(1L)
                .questions(List.of(FormWithStructureInfo.QuestionWithOptions.builder()
                    .questionId(400L)
                    .title("질문")
                    .description("질문 설명")
                    .type(QuestionType.RADIO)
                    .isRequired(true)
                    .orderNo(1L)
                    .options(List.of(FormWithStructureInfo.Option.builder()
                        .optionId(500L)
                        .content("기타")
                        .orderNo(1L)
                        .isOther(true)
                        .nextSectionId(null)
                        .build()))
                    .build()))
                .build()))
            .build();
    }
}
