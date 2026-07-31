package com.umc.product.recruiting.application.service.command;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.spy;

import java.util.Set;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.umc.product.form.application.port.in.command.ManageFormUseCase;
import com.umc.product.recruiting.application.port.in.command.ValidateRecruitingApplicationFormUseCase;
import com.umc.product.recruiting.application.port.in.command.dto.CloseRecruitingApplicationFormCommand;
import com.umc.product.recruiting.application.port.in.command.dto.PublishRecruitingApplicationFormCommand;
import com.umc.product.recruiting.application.port.in.command.dto.UnpublishRecruitingApplicationFormCommand;
import com.umc.product.recruiting.application.port.out.LoadRecruitingApplicationFormPort;
import com.umc.product.recruiting.application.port.out.SaveRecruitingApplicationFormPort;
import com.umc.product.recruiting.domain.RecruitingApplicationForm;
import com.umc.product.recruiting.domain.RecruitingRound;
import com.umc.product.recruiting.domain.RecruitingSeason;
import com.umc.product.recruiting.domain.exception.RecruitingDomainException;
import com.umc.product.recruiting.domain.exception.RecruitingErrorCode;

@ExtendWith(MockitoExtension.class)
class RecruitingApplicationFormCommandServiceTest {

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
    @DisplayName("지원 Form 게시 전 섹션 정책 검증 seam을 호출한다")
    void validatePoliciesBeforePublish() {
        RecruitingApplicationForm form = spy(RecruitingApplicationForm.create(round(10L), 500L));
        ReflectionTestUtils.setField(form, "id", 100L);
        given(loadApplicationFormPort.getByIdForUpdate(100L)).willReturn(form);
        given(validateApplicationFormUseCase.validateForPublish(100L)).willReturn(Set.of());

        sut.publish(PublishRecruitingApplicationFormCommand.builder()
            .seasonId(1L)
            .applicationFormId(100L)
            .requesterMemberId(200L)
            .build());

        then(validateApplicationFormUseCase).should().validateForPublish(100L);
        InOrder lockBeforeStateCheck = inOrder(loadApplicationFormPort, form, manageFormUseCase);
        then(loadApplicationFormPort).should(lockBeforeStateCheck).getByIdForUpdate(100L);
        then(form).should(lockBeforeStateCheck).publish(Set.of());
        then(manageFormUseCase).should(lockBeforeStateCheck).publishForm(any());
        assertThat(form.getStatus().name()).isEqualTo("PUBLISHED");
    }

    @Test
    @DisplayName("지원 Form root lock을 획득한 뒤 마감 상태를 검사한다")
    void lockFormBeforeCloseStateCheck() {
        RecruitingApplicationForm form = RecruitingApplicationForm.create(round(10L), 500L);
        form.publish(form.getRound().getRecruitableTracks());
        form = spy(form);
        ReflectionTestUtils.setField(form, "id", 100L);
        given(loadApplicationFormPort.getByIdForUpdate(100L)).willReturn(form);

        sut.close(CloseRecruitingApplicationFormCommand.builder()
            .seasonId(1L)
            .applicationFormId(100L)
            .build());

        InOrder lockBeforeStateCheck = inOrder(loadApplicationFormPort, form, manageFormUseCase);
        then(loadApplicationFormPort).should(lockBeforeStateCheck).getByIdForUpdate(100L);
        then(form).should(lockBeforeStateCheck).close();
        then(manageFormUseCase).should(lockBeforeStateCheck).closeForm(any());
        assertThat(form.getStatus().name()).isEqualTo("CLOSED");
    }

    @Test
    @DisplayName("지원 Form 게시 취소는 실제 Form과 함께 DRAFT로 전환한다")
    void unpublishRecruitingAndActualForm() {
        RecruitingApplicationForm form = RecruitingApplicationForm.create(round(10L), 500L);
        form.publish(form.getRound().getRecruitableTracks());
        form = spy(form);
        ReflectionTestUtils.setField(form, "id", 100L);
        given(loadApplicationFormPort.getByIdForUpdate(100L)).willReturn(form);

        sut.unpublish(UnpublishRecruitingApplicationFormCommand.builder()
            .seasonId(1L)
            .applicationFormId(100L)
            .requesterMemberId(200L)
            .build());

        InOrder transitionOrder = inOrder(loadApplicationFormPort, form, manageFormUseCase);
        then(loadApplicationFormPort).should(transitionOrder).getByIdForUpdate(100L);
        then(form).should(transitionOrder).unpublish();
        then(manageFormUseCase).should(transitionOrder).unpublishForm(any());
        assertThat(form.getStatus().name()).isEqualTo("DRAFT");
    }

    @Test
    @DisplayName("다른 시즌의 지원 Form은 게시하지 않는다")
    void rejectPublishForFormInDifferentSeasonBeforeMutation() {
        RecruitingApplicationForm form = RecruitingApplicationForm.create(round(10L, 2L), 500L);
        ReflectionTestUtils.setField(form, "id", 100L);
        given(loadApplicationFormPort.getByIdForUpdate(100L)).willReturn(form);

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
        given(loadApplicationFormPort.getByIdForUpdate(100L)).willReturn(form);

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
