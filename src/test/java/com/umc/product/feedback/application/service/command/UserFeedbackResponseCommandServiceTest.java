package com.umc.product.feedback.application.service.command;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.umc.product.feedback.application.port.in.command.dto.SubmitUserFeedbackResponseCommand;
import com.umc.product.feedback.application.port.out.LoadUserFeedbackTemplatePort;
import com.umc.product.feedback.application.service.UserFeedbackAudienceResolver;
import com.umc.product.feedback.domain.UserFeedbackTemplate;
import com.umc.product.feedback.domain.enums.UserFeedbackContext;
import com.umc.product.feedback.domain.enums.UserFeedbackTargetType;
import com.umc.product.feedback.domain.exception.FeedbackDomainException;
import com.umc.product.feedback.domain.exception.FeedbackErrorCode;
import com.umc.product.form.application.port.in.command.ManageFormResponseUseCase;
import com.umc.product.form.application.port.in.command.dto.SubmitFormResponseCommand;

@ExtendWith(MockitoExtension.class)
class UserFeedbackResponseCommandServiceTest {

    private static final Long TEMPLATE_ID = 1L;
    private static final Long MEMBER_ID = 10L;

    @Mock
    LoadUserFeedbackTemplatePort loadUserFeedbackTemplatePort;
    @Mock
    ManageFormResponseUseCase manageFormResponseUseCase;
    @Mock
    UserFeedbackAudienceResolver audienceResolver;

    @InjectMocks
    UserFeedbackResponseCommandService sut;

    @Test
    @DisplayName("응답자 audience와 템플릿 targetType이 다르면 제출을 거부한다")
    void audience와_targetType이_다르면_제출을_거부한다() {
        given(loadUserFeedbackTemplatePort.getActiveById(TEMPLATE_ID))
            .willReturn(UserFeedbackTemplate.create(
                UserFeedbackContext.APPLICATION_MONITORING,
                UserFeedbackTargetType.ADMIN,
                100L
            ));
        given(audienceResolver.resolve(MEMBER_ID)).willReturn(Optional.of(UserFeedbackTargetType.NEW_CHALLENGER));

        assertThatThrownBy(() -> sut.submit(SubmitUserFeedbackResponseCommand.builder()
            .templateId(TEMPLATE_ID)
            .respondentMemberId(MEMBER_ID)
            .answers(List.of())
            .build()))
            .isInstanceOf(FeedbackDomainException.class)
            .extracting("baseCode")
            .isEqualTo(FeedbackErrorCode.USER_FEEDBACK_TEMPLATE_TARGET_MISMATCH);

        then(manageFormResponseUseCase).should(never()).submitImmediately(any(SubmitFormResponseCommand.class));
    }
}
