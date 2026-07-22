package com.umc.product.feedback.application.service.command;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.umc.product.feedback.application.port.in.command.dto.SubmitUserFeedbackResponseCommand;
import com.umc.product.feedback.application.port.out.LoadUserFeedbackTemplatePort;
import com.umc.product.feedback.domain.UserFeedbackTemplate;
import com.umc.product.feedback.domain.enums.UserFeedbackContext;
import com.umc.product.feedback.domain.enums.UserFeedbackTargetType;
import com.umc.product.form.application.port.in.command.ManageFormResponseUseCase;
import com.umc.product.form.application.port.in.command.dto.AnswerCommand;
import com.umc.product.form.application.port.in.command.dto.SubmitFormResponseCommand;

@ExtendWith(MockitoExtension.class)
class UserFeedbackResponseCommandServiceTest {

    @Mock
    LoadUserFeedbackTemplatePort loadPort;

    @Mock
    ManageFormResponseUseCase manageFormResponseUseCase;

    @Test
    @DisplayName("피드백 템플릿의 Form에 응답을 즉시 제출한다")
    void 피드백_템플릿의_Form에_응답을_즉시_제출한다() {
        // given
        UserFeedbackTemplate template = UserFeedbackTemplate.create(
            UserFeedbackContext.APPLICATION_SUBMITTED,
            UserFeedbackTargetType.NEW_CHALLENGER,
            200L
        );
        AnswerCommand answer = AnswerCommand.builder().questionId(1L).textValue("좋아요").build();
        SubmitUserFeedbackResponseCommand command = SubmitUserFeedbackResponseCommand.builder()
            .templateId(100L)
            .respondentMemberId(10L)
            .answers(List.of(answer))
            .build();
        given(loadPort.getById(100L)).willReturn(template);
        given(manageFormResponseUseCase.submitImmediately(org.mockito.ArgumentMatchers.any())).willReturn(300L);
        UserFeedbackResponseCommandService sut = new UserFeedbackResponseCommandService(
            loadPort,
            manageFormResponseUseCase
        );

        // when
        Long result = sut.submit(command);

        // then
        assertThat(result).isEqualTo(300L);
        verify(manageFormResponseUseCase).submitImmediately(SubmitFormResponseCommand.builder()
            .formId(200L)
            .respondentMemberId(10L)
            .answers(List.of(answer))
            .build());
    }
}
