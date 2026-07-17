package com.umc.product.feedback.application.service.command;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;

import com.umc.product.feedback.application.policy.FeedbackTemplateOwnerReferenceFactory;
import com.umc.product.feedback.application.port.in.command.dto.SubmitUserFeedbackResponseCommand;
import com.umc.product.feedback.application.port.out.LoadUserFeedbackTemplatePort;
import com.umc.product.feedback.domain.UserFeedbackTemplate;
import com.umc.product.feedback.domain.enums.UserFeedbackContext;
import com.umc.product.feedback.domain.enums.UserFeedbackTargetType;
import com.umc.product.form.application.port.in.FormActorContext;
import com.umc.product.form.application.port.in.command.ManageFormResponseUseCase;
import com.umc.product.form.application.port.in.command.dto.AnswerCommand;
import com.umc.product.form.application.port.in.command.dto.SubmitFormResponseCommand;

@DisplayName("UserFeedbackResponseCommandService")
class UserFeedbackResponseCommandServiceTest {

    @Test
    @DisplayName("loaded template 좌표와 server actor를 Form Port In에 그대로 전달한다")
    void forwardsTrustedOwnerAndActor() {
        LoadUserFeedbackTemplatePort templatePort = mock(LoadUserFeedbackTemplatePort.class);
        ManageFormResponseUseCase formUseCase = mock(ManageFormResponseUseCase.class);
        FeedbackTemplateOwnerReferenceFactory factory = new FeedbackTemplateOwnerReferenceFactory();
        UserFeedbackTemplate template = UserFeedbackTemplate.create(
            UserFeedbackContext.APPLICATION_SUBMITTED,
            UserFeedbackTargetType.NEW_CHALLENGER,
            42L
        );
        ReflectionTestUtils.setField(template, "id", 7L);
        given(templatePort.getById(7L)).willReturn(template);
        given(formUseCase.submitImmediately(
            org.mockito.ArgumentMatchers.eq(factory.create(7L, 42L)),
            org.mockito.ArgumentMatchers.eq(FormActorContext.authenticated(100L)),
            org.mockito.ArgumentMatchers.any(SubmitFormResponseCommand.class)
        )).willReturn(900L);

        UserFeedbackResponseCommandService sut = new UserFeedbackResponseCommandService(
            templatePort,
            formUseCase,
            factory
        );
        SubmitUserFeedbackResponseCommand command = SubmitUserFeedbackResponseCommand.builder()
            .templateId(7L)
            .answers(List.of(AnswerCommand.builder().questionId(1L).build()))
            .build();

        Long result = sut.submit(FormActorContext.authenticated(100L), command);

        assertThat(result).isEqualTo(900L);
        ArgumentCaptor<SubmitFormResponseCommand> commandCaptor = ArgumentCaptor.forClass(
            SubmitFormResponseCommand.class
        );
        verify(formUseCase).submitImmediately(
            org.mockito.ArgumentMatchers.eq(factory.create(7L, 42L)),
            org.mockito.ArgumentMatchers.eq(FormActorContext.authenticated(100L)),
            commandCaptor.capture()
        );
        assertThat(commandCaptor.getValue().formId()).isEqualTo(42L);
        assertThat(commandCaptor.getValue().answers()).hasSize(1);
    }
}
