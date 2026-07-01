package com.umc.product.recruiting.application.service.command;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.umc.product.authorization.application.port.in.query.GetChallengerRoleUseCase;
import com.umc.product.challenger.application.port.in.command.ManageChallengerUseCase;
import com.umc.product.challenger.application.port.in.command.dto.CreateChallengerCommand;
import com.umc.product.common.domain.enums.ChallengerTrack;
import com.umc.product.recruiting.application.port.in.command.dto.ConfirmRecruitingRegistrationCommand;
import com.umc.product.recruiting.application.port.in.command.dto.DecideRecruitingDocumentCommand;
import com.umc.product.recruiting.application.port.in.command.dto.DecideRecruitingFinalCommand;
import com.umc.product.recruiting.application.port.in.command.dto.RecruitingDecisionStatus;
import com.umc.product.recruiting.application.port.out.LoadRecruitingApplicationPort;
import com.umc.product.recruiting.application.port.out.SaveRecruitingApplicationPort;
import com.umc.product.recruiting.domain.RecruitingApplication;
import com.umc.product.recruiting.domain.RecruitingApplicationForm;
import com.umc.product.recruiting.domain.RecruitingRound;
import com.umc.product.recruiting.domain.RecruitingSeason;
import com.umc.product.recruiting.domain.enums.RecruitingApplicationRegistrationStatus;
import com.umc.product.recruiting.domain.enums.RecruitingApplicationStatus;
import com.umc.product.recruiting.domain.exception.RecruitingDomainException;
import com.umc.product.recruiting.domain.exception.RecruitingErrorCode;

@ExtendWith(MockitoExtension.class)
class RecruitingDecisionCommandServiceTest {

    @Mock
    LoadRecruitingApplicationPort loadApplicationPort;

    @Mock
    SaveRecruitingApplicationPort saveApplicationPort;

    @Mock
    ManageChallengerUseCase manageChallengerUseCase;

    @Mock
    GetChallengerRoleUseCase getChallengerRoleUseCase;

    @InjectMocks
    RecruitingDecisionCommandService sut;

    @Test
    @DisplayName("서류_합격_결정은_SUBMITTED_지원서를_DOCUMENT_PASSED로_변경한다")
    void passDocumentChangesStatus() {
        // Given
        RecruitingApplication application = submittedApplication();
        given(loadApplicationPort.getByIdWithDetails(900L)).willReturn(application);

        // When
        sut.decideDocument(DecideRecruitingDocumentCommand.builder()
            .applicationId(900L)
            .decision(RecruitingDecisionStatus.PASS)
            .decidedByMemberId(1L)
            .reason("서류 합격")
            .build());

        // Then
        assertThat(application.getStatus()).isEqualTo(RecruitingApplicationStatus.DOCUMENT_PASSED);
        then(saveApplicationPort).should().save(application);
    }

    @Test
    @DisplayName("최종_합격_결정은_FINAL_PASSED와_등록_READY를_분리해서_기록한다")
    void passFinalMarksRegistrationReady() {
        // Given
        RecruitingApplication application = documentPassedApplication();
        given(loadApplicationPort.getByIdWithDetails(900L)).willReturn(application);
        given(loadApplicationPort.existsFinalPassedByGisuIdAndApplicantIdentityKeyAndIdNot(
            1L,
            "identity:1",
            900L
        )).willReturn(false);

        // When
        sut.decideFinal(DecideRecruitingFinalCommand.builder()
            .applicationId(900L)
            .decision(RecruitingDecisionStatus.PASS)
            .decidedByMemberId(1L)
            .reason("최종 합격")
            .build());

        // Then
        assertThat(application.getStatus()).isEqualTo(RecruitingApplicationStatus.FINAL_PASSED);
        assertThat(application.getRegistrationStatus()).isEqualTo(RecruitingApplicationRegistrationStatus.READY);
        then(saveApplicationPort).should().save(application);
    }

    @Test
    @DisplayName("같은_기수에서_이미_최종_합격한_지원자가_있으면_다른_지원서를_최종_합격시킬_수_없다")
    void passFinalRejectsDuplicateFinalPass() {
        // Given
        RecruitingApplication application = documentPassedApplication();
        given(loadApplicationPort.getByIdWithDetails(900L)).willReturn(application);
        given(loadApplicationPort.existsFinalPassedByGisuIdAndApplicantIdentityKeyAndIdNot(
            1L,
            "identity:1",
            900L
        )).willReturn(true);

        // When & Then
        assertThatThrownBy(() -> sut.decideFinal(DecideRecruitingFinalCommand.builder()
            .applicationId(900L)
            .decision(RecruitingDecisionStatus.PASS)
            .decidedByMemberId(1L)
            .build()))
            .isInstanceOf(RecruitingDomainException.class)
            .extracting("baseCode")
            .isEqualTo(RecruitingErrorCode.RECRUITING_APPLICATION_FINAL_PASS_ALREADY_EXISTS);
        then(saveApplicationPort).should(never()).save(any());
    }

    @Test
    @DisplayName("최종_등록_확정은_중앙운영사무국_총괄단만_가능하고_track으로_챌린저를_생성한다")
    void confirmRegistrationCreatesChallengerWithTrack() {
        // Given
        RecruitingApplication application = finalPassedReadyApplication();
        given(loadApplicationPort.getByIdWithDetails(900L)).willReturn(application);
        given(getChallengerRoleUseCase.isCentralCoreInGisu(1L, 1L)).willReturn(true);
        given(manageChallengerUseCase.createChallenger(any())).willReturn(1000L);

        // When
        sut.confirmRegistration(ConfirmRecruitingRegistrationCommand.builder()
            .applicationId(900L)
            .executorMemberId(1L)
            .build());

        // Then
        ArgumentCaptor<CreateChallengerCommand> captor = ArgumentCaptor.forClass(CreateChallengerCommand.class);
        then(manageChallengerUseCase).should().createChallenger(captor.capture());
        assertThat(captor.getValue().memberId()).isEqualTo(200L);
        assertThat(captor.getValue().gisuId()).isEqualTo(1L);
        assertThat(captor.getValue().track()).isEqualTo(ChallengerTrack.WEB_PRODUCT_ENGINEER);
        assertThat(captor.getValue().part()).isNull();
        assertThat(application.getRegistrationStatus()).isEqualTo(RecruitingApplicationRegistrationStatus.REGISTERED);
        then(saveApplicationPort).should().save(application);
    }

    @Test
    @DisplayName("중앙운영사무국_총괄단이_아니면_최종_등록을_확정할_수_없다")
    void confirmRegistrationRejectsNonCentralCore() {
        // Given
        RecruitingApplication application = finalPassedReadyApplication();
        given(loadApplicationPort.getByIdWithDetails(900L)).willReturn(application);
        given(getChallengerRoleUseCase.isCentralCoreInGisu(1L, 1L)).willReturn(false);
        given(getChallengerRoleUseCase.isSuperAdmin(1L)).willReturn(false);

        // When & Then
        assertThatThrownBy(() -> sut.confirmRegistration(ConfirmRecruitingRegistrationCommand.builder()
            .applicationId(900L)
            .executorMemberId(1L)
            .build()))
            .isInstanceOf(RecruitingDomainException.class)
            .extracting("baseCode")
            .isEqualTo(RecruitingErrorCode.RECRUITING_REGISTRATION_FORBIDDEN);
        then(manageChallengerUseCase).should(never()).createChallenger(any());
    }

    private RecruitingApplication finalPassedReadyApplication() {
        RecruitingApplication application = documentPassedApplication();
        application.passFinal(1L, "최종 합격");
        application.markRegistrationReady(1L);
        return application;
    }

    private RecruitingApplication documentPassedApplication() {
        RecruitingApplication application = submittedApplication();
        application.passDocument(1L, "서류 합격");
        return application;
    }

    private RecruitingApplication submittedApplication() {
        RecruitingApplication application = RecruitingApplication.createDraft(
            applicationForm(),
            700L,
            200L,
            "identity:1",
            "APP-001",
            "a***@umc.test"
        );
        ReflectionTestUtils.setField(application, "id", 900L);
        application.submit(200L);
        return application;
    }

    private RecruitingApplicationForm applicationForm() {
        RecruitingApplicationForm form = RecruitingApplicationForm.create(
            regularRound(),
            500L,
            ChallengerTrack.WEB_PRODUCT_ENGINEER
        );
        ReflectionTestUtils.setField(form, "id", 100L);
        form.publish();
        return form;
    }

    private RecruitingRound regularRound() {
        RecruitingSeason season = RecruitingSeason.create(1L, 10L);
        ReflectionTestUtils.setField(season, "id", 1L);
        RecruitingRound round = RecruitingRound.createRegular(season);
        ReflectionTestUtils.setField(round, "id", 10L);
        return round;
    }
}
