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

import com.umc.product.recruiting.application.port.in.command.ValidateRecruitingApplicationFormUseCase;
import com.umc.product.recruiting.application.port.in.command.dto.CloseRecruitingApplicationFormCommand;
import com.umc.product.recruiting.application.port.in.command.dto.LinkRecruitingApplicationFormCommand;
import com.umc.product.recruiting.application.port.in.command.dto.PublishRecruitingApplicationFormCommand;
import com.umc.product.recruiting.application.port.out.LoadRecruitingApplicationFormPort;
import com.umc.product.recruiting.application.port.out.LoadRecruitingRoundPort;
import com.umc.product.recruiting.application.port.out.SaveRecruitingApplicationFormPort;
import com.umc.product.recruiting.domain.RecruitingApplicationForm;
import com.umc.product.recruiting.domain.RecruitingRound;
import com.umc.product.recruiting.domain.RecruitingSeason;
import com.umc.product.recruiting.domain.exception.RecruitingDomainException;
import com.umc.product.recruiting.domain.exception.RecruitingErrorCode;
import com.umc.product.survey.application.port.in.command.ManageFormUseCase;

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

    @Mock
    ValidateRecruitingApplicationFormUseCase validateApplicationFormUseCase;

    @InjectMocks
    RecruitingApplicationFormCommandService sut;

    @Test
    @DisplayName("모집 차수에는 트랙 없이 하나의 지원 Form만 연결한다")
    void linkSingleFormToRound() {
        RecruitingRound round = round(10L);
        given(loadApplicationFormPort.findByRoundId(10L)).willReturn(Optional.empty());
        given(loadRoundPort.getById(10L)).willReturn(round);
        given(saveApplicationFormPort.save(any())).willAnswer(invocation -> {
            RecruitingApplicationForm form = invocation.getArgument(0);
            ReflectionTestUtils.setField(form, "id", 100L);
            return form;
        });

        Long result = sut.link(LinkRecruitingApplicationFormCommand.builder()
            .seasonId(1L)
            .roundId(10L)
            .formId(500L)
            .build());

        assertThat(result).isEqualTo(100L);
        ArgumentCaptor<RecruitingApplicationForm> captor =
            ArgumentCaptor.forClass(RecruitingApplicationForm.class);
        then(saveApplicationFormPort).should().save(captor.capture());
        assertThat(captor.getValue().getFormId()).isEqualTo(500L);
    }

    @Test
    @DisplayName("같은 모집 차수에 두 번째 지원 Form을 연결할 수 없다")
    void rejectSecondFormForRound() {
        given(loadRoundPort.getById(10L)).willReturn(round(10L));
        given(loadApplicationFormPort.findByRoundId(10L))
            .willReturn(Optional.of(RecruitingApplicationForm.create(round(10L), 400L)));

        assertThatThrownBy(() -> sut.link(LinkRecruitingApplicationFormCommand.builder()
            .seasonId(1L)
            .roundId(10L)
            .formId(500L)
            .build()))
            .isInstanceOf(RecruitingDomainException.class)
            .extracting("baseCode")
            .isEqualTo(RecruitingErrorCode.RECRUITING_APPLICATION_FORM_ALREADY_EXISTS);
        then(saveApplicationFormPort).should(never()).save(any());
    }

    @Test
    @DisplayName("지원 Form 게시 전 섹션 정책 검증 seam을 호출한다")
    void validatePoliciesBeforePublish() {
        RecruitingApplicationForm form = RecruitingApplicationForm.create(round(10L), 500L);
        ReflectionTestUtils.setField(form, "id", 100L);
        given(loadApplicationFormPort.getById(100L)).willReturn(form);

        sut.publish(PublishRecruitingApplicationFormCommand.builder()
            .seasonId(1L)
            .applicationFormId(100L)
            .requesterMemberId(200L)
            .build());

        then(validateApplicationFormUseCase).should().validateForPublish(100L);
        then(manageFormUseCase).should().publishForm(any());
        assertThat(form.getStatus().name()).isEqualTo("PUBLISHED");
    }

    @Test
    @DisplayName("다른 시즌의 차수에는 지원 Form을 연결하지 않는다")
    void rejectLinkForRoundInDifferentSeasonBeforeSave() {
        given(loadRoundPort.getById(10L)).willReturn(round(10L, 2L));

        assertThatThrownBy(() -> sut.link(LinkRecruitingApplicationFormCommand.builder()
            .seasonId(1L)
            .roundId(10L)
            .formId(500L)
            .build()))
            .isInstanceOf(RecruitingDomainException.class)
            .extracting("baseCode")
            .isEqualTo(RecruitingErrorCode.RECRUITING_ROUND_NOT_FOUND);

        then(loadApplicationFormPort).shouldHaveNoInteractions();
        then(saveApplicationFormPort).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("다른 시즌의 지원 Form은 게시하지 않는다")
    void rejectPublishForFormInDifferentSeasonBeforeMutation() {
        RecruitingApplicationForm form = RecruitingApplicationForm.create(round(10L, 2L), 500L);
        ReflectionTestUtils.setField(form, "id", 100L);
        given(loadApplicationFormPort.getById(100L)).willReturn(form);

        assertThatThrownBy(() -> sut.publish(PublishRecruitingApplicationFormCommand.builder()
            .seasonId(1L)
            .applicationFormId(100L)
            .requesterMemberId(200L)
            .build()))
            .isInstanceOf(RecruitingDomainException.class)
            .extracting("baseCode")
            .isEqualTo(RecruitingErrorCode.RECRUITING_APPLICATION_FORM_NOT_FOUND);

        then(validateApplicationFormUseCase).shouldHaveNoInteractions();
        then(manageFormUseCase).shouldHaveNoInteractions();
        then(saveApplicationFormPort).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("다른 시즌의 지원 Form은 마감하지 않는다")
    void rejectCloseForFormInDifferentSeasonBeforeMutation() {
        RecruitingApplicationForm form = RecruitingApplicationForm.create(round(10L, 2L), 500L);
        ReflectionTestUtils.setField(form, "id", 100L);
        given(loadApplicationFormPort.getById(100L)).willReturn(form);

        assertThatThrownBy(() -> sut.close(CloseRecruitingApplicationFormCommand.builder()
            .seasonId(1L)
            .applicationFormId(100L)
            .build()))
            .isInstanceOf(RecruitingDomainException.class)
            .extracting("baseCode")
            .isEqualTo(RecruitingErrorCode.RECRUITING_APPLICATION_FORM_NOT_FOUND);

        then(saveApplicationFormPort).shouldHaveNoInteractions();
    }

    private RecruitingRound round(Long id) {
        return round(id, 1L);
    }

    private RecruitingRound round(Long id, Long seasonId) {
        RecruitingSeason season = RecruitingSeason.create(1L, 10L);
        ReflectionTestUtils.setField(season, "id", seasonId);
        RecruitingRound round = RecruitingRound.createRegular(season);
        ReflectionTestUtils.setField(round, "id", id);
        return round;
    }
}
