package com.umc.product.recruiting.application.service.command;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.umc.product.common.domain.enums.ChallengerTrack;
import com.umc.product.recruiting.application.port.in.command.dto.CloseRecruitingApplicationFormCommand;
import com.umc.product.recruiting.application.port.in.command.dto.LinkRecruitingApplicationFormCommand;
import com.umc.product.recruiting.application.port.in.command.dto.PublishRecruitingApplicationFormCommand;
import com.umc.product.recruiting.application.port.out.LoadRecruitingApplicationFormPort;
import com.umc.product.recruiting.application.port.out.LoadRecruitingRoundPort;
import com.umc.product.recruiting.application.port.out.SaveRecruitingApplicationFormPort;
import com.umc.product.recruiting.domain.RecruitingApplicationForm;
import com.umc.product.recruiting.domain.RecruitingRound;
import com.umc.product.recruiting.domain.RecruitingSeason;
import com.umc.product.recruiting.domain.enums.RecruitingApplicationFormStatus;
import com.umc.product.recruiting.domain.exception.RecruitingDomainException;
import com.umc.product.recruiting.domain.exception.RecruitingErrorCode;
import com.umc.product.survey.application.port.in.command.ManageFormUseCase;
import com.umc.product.survey.application.port.in.command.dto.PublishFormCommand;

@ExtendWith(MockitoExtension.class)
class RecruitingApplicationFormCommandServiceTest {

    @Mock
    LoadRecruitingRoundPort loadRoundPort;

    @Mock
    LoadRecruitingApplicationFormPort loadApplicationFormPort;

    @Mock
    SaveRecruitingApplicationFormPort saveApplicationFormPort;

    @Mock
    ManageFormUseCase manageFormUseCase;

    @InjectMocks
    RecruitingApplicationFormCommandService sut;

    @Test
    @DisplayName("모집_차수에_아직_연결되지_않은_form을_지원_폼으로_연결한다")
    void linkApplicationFormWhenUniqueInRound() {
        // Given
        RecruitingRound round = regularRound(10L);
        LinkRecruitingApplicationFormCommand command = LinkRecruitingApplicationFormCommand.builder()
            .roundId(10L)
            .formId(500L)
            .track(ChallengerTrack.WEB_PRODUCT_ENGINEER)
            .build();
        given(loadRoundPort.getById(10L)).willReturn(round);
        given(loadApplicationFormPort.findByRoundIdAndFormId(10L, 500L)).willReturn(Optional.empty());
        given(saveApplicationFormPort.save(any())).willAnswer(invocation -> {
            RecruitingApplicationForm form = invocation.getArgument(0);
            ReflectionTestUtils.setField(form, "id", 100L);
            return form;
        });

        // When
        Long applicationFormId = sut.link(command);

        // Then
        assertThat(applicationFormId).isEqualTo(100L);
        ArgumentCaptor<RecruitingApplicationForm> captor = ArgumentCaptor.forClass(RecruitingApplicationForm.class);
        then(saveApplicationFormPort).should().save(captor.capture());
        assertThat(captor.getValue().getFormId()).isEqualTo(500L);
        assertThat(captor.getValue().getTrack()).isEqualTo(ChallengerTrack.WEB_PRODUCT_ENGINEER);
    }

    @Test
    @DisplayName("같은_모집_차수에_이미_연결된_form은_다시_연결할_수_없다")
    void linkApplicationFormRejectsDuplicateRoundForm() {
        // Given
        RecruitingRound round = regularRound(10L);
        RecruitingApplicationForm existing = RecruitingApplicationForm.create(
            round,
            500L,
            ChallengerTrack.WEB_PRODUCT_ENGINEER
        );
        given(loadApplicationFormPort.findByRoundIdAndFormId(10L, 500L)).willReturn(Optional.of(existing));

        LinkRecruitingApplicationFormCommand command = LinkRecruitingApplicationFormCommand.builder()
            .roundId(10L)
            .formId(500L)
            .track(ChallengerTrack.WEB_PRODUCT_ENGINEER)
            .build();

        // When & Then
        assertThatThrownBy(() -> sut.link(command))
            .isInstanceOf(RecruitingDomainException.class)
            .extracting("baseCode")
            .isEqualTo(RecruitingErrorCode.RECRUITING_APPLICATION_FORM_ALREADY_EXISTS);
        then(saveApplicationFormPort).should(never()).save(any());
    }

    @Test
    @DisplayName("지원_폼_게시시_survey_form을_publish하고_recruiting_form도_PUBLISHED로_변경한다")
    void publishApplicationFormDelegatesSurveyPublish() {
        // Given
        RecruitingApplicationForm form = applicationForm(100L, 500L);
        given(loadApplicationFormPort.getById(100L)).willReturn(form);

        // When
        sut.publish(PublishRecruitingApplicationFormCommand.builder()
            .applicationFormId(100L)
            .requesterMemberId(900L)
            .build());

        // Then
        ArgumentCaptor<PublishFormCommand> captor = ArgumentCaptor.forClass(PublishFormCommand.class);
        then(manageFormUseCase).should().publishForm(captor.capture());
        assertThat(captor.getValue().formId()).isEqualTo(500L);
        assertThat(captor.getValue().requesterMemberId()).isEqualTo(900L);
        assertThat(form.getStatus()).isEqualTo(RecruitingApplicationFormStatus.PUBLISHED);
        then(saveApplicationFormPort).should().save(form);
    }

    @Test
    @DisplayName("지원_폼_닫기는_recruiting_form_상태만_CLOSED로_변경한다")
    void closeApplicationFormChangesRecruitingStatusOnly() {
        // Given
        RecruitingApplicationForm form = applicationForm(100L, 500L);
        form.publish();
        given(loadApplicationFormPort.getById(100L)).willReturn(form);

        // When
        sut.close(CloseRecruitingApplicationFormCommand.builder()
            .applicationFormId(100L)
            .build());

        // Then
        assertThat(form.getStatus()).isEqualTo(RecruitingApplicationFormStatus.CLOSED);
        then(manageFormUseCase).should(never()).publishForm(any());
        then(saveApplicationFormPort).should().save(form);
    }

    private RecruitingApplicationForm applicationForm(Long applicationFormId, Long formId) {
        RecruitingApplicationForm form = RecruitingApplicationForm.create(
            regularRound(10L),
            formId,
            ChallengerTrack.WEB_PRODUCT_ENGINEER
        );
        ReflectionTestUtils.setField(form, "id", applicationFormId);
        return form;
    }

    private RecruitingRound regularRound(Long id) {
        RecruitingSeason season = RecruitingSeason.create(1L, 10L);
        ReflectionTestUtils.setField(season, "id", 1L);
        RecruitingRound round = RecruitingRound.createRegular(season);
        ReflectionTestUtils.setField(round, "id", id);
        return round;
    }
}
