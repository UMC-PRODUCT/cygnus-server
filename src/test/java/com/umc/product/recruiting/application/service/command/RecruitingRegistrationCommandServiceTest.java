package com.umc.product.recruiting.application.service.command;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.umc.product.authorization.application.port.in.query.GetChallengerRoleUseCase;
import com.umc.product.challenger.application.port.in.command.AddChallengerTrackUseCase;
import com.umc.product.challenger.application.port.in.command.dto.AddChallengerTrackCommand;
import com.umc.product.common.domain.enums.ChallengerTrack;
import com.umc.product.recruiting.application.port.in.command.dto.CancelRecruitingRegistrationCommand;
import com.umc.product.recruiting.application.port.in.command.dto.ConfirmRecruitingRegistrationCommand;
import com.umc.product.recruiting.application.port.in.command.dto.PrepareRecruitingRegistrationCommand;
import com.umc.product.recruiting.application.port.out.LoadRecruitingApplicationPort;
import com.umc.product.recruiting.application.port.out.LoadRecruitingSeasonTrackQuotaPort;
import com.umc.product.recruiting.application.port.out.SaveRecruitingApplicationPort;
import com.umc.product.recruiting.domain.RecruitingApplicantEmail;
import com.umc.product.recruiting.domain.RecruitingApplicantProfile;
import com.umc.product.recruiting.domain.RecruitingApplication;
import com.umc.product.recruiting.domain.RecruitingApplicationForm;
import com.umc.product.recruiting.domain.RecruitingRound;
import com.umc.product.recruiting.domain.RecruitingRoundConfiguration;
import com.umc.product.recruiting.domain.RecruitingSeason;
import com.umc.product.recruiting.domain.RecruitingSeasonTrackQuota;
import com.umc.product.recruiting.domain.enums.RecruitingApplicationRegistrationStatus;
import com.umc.product.recruiting.domain.exception.RecruitingDomainException;
import com.umc.product.recruiting.domain.exception.RecruitingErrorCode;

@ExtendWith(MockitoExtension.class)
class RecruitingRegistrationCommandServiceTest {

    @Mock
    LoadRecruitingApplicationPort loadApplicationPort;
    @Mock
    SaveRecruitingApplicationPort saveApplicationPort;
    @Mock
    LoadRecruitingSeasonTrackQuotaPort loadQuotaPort;
    @Mock
    AddChallengerTrackUseCase addChallengerTrackUseCase;
    @Mock
    GetChallengerRoleUseCase getChallengerRoleUseCase;

    RecruitingRegistrationCommandService sut;

    @BeforeEach
    void setUp() {
        sut = new RecruitingRegistrationCommandService(
            loadApplicationPort,
            saveApplicationPort,
            loadQuotaPort,
            addChallengerTrackUseCase,
            getChallengerRoleUseCase
        );
    }

    @Test
    @DisplayName("중앙 총괄단은 quota row를 잠근 뒤 Season 전체 사용량이 남으면 READY를 예약한다")
    void centralCorePreparesRegistrationWithQuotaLock() {
        RecruitingApplication application = finalPassedApplication();
        RecruitingSeasonTrackQuota quota = quota(application, 1);
        given(loadApplicationPort.getByIdWithDetailsForUpdate(900L)).willReturn(application);
        given(getChallengerRoleUseCase.isCentralCoreInGisu(1L, 1L)).willReturn(true);
        given(loadQuotaPort.getBySeasonIdAndTrackForUpdate(1L, ChallengerTrack.DESIGN)).willReturn(quota);
        given(loadApplicationPort.countReservedOrRegisteredBySeasonIdAndTrack(1L, ChallengerTrack.DESIGN))
            .willReturn(0L);

        sut.prepareRegistration(PrepareRecruitingRegistrationCommand.of(900L, 1L));

        assertThat(application.getRegistrationStatus()).isEqualTo(RecruitingApplicationRegistrationStatus.READY);
        then(saveApplicationPort).should().save(application);
    }

    @Test
    @DisplayName("학교 회장단은 등록 준비를 할 수 없다")
    void schoolCoreCannotPrepareRegistration() {
        RecruitingApplication application = finalPassedApplication();
        given(loadApplicationPort.getByIdWithDetailsForUpdate(900L)).willReturn(application);

        assertThatThrownBy(() -> sut.prepareRegistration(PrepareRecruitingRegistrationCommand.of(900L, 2L)))
            .isInstanceOf(RecruitingDomainException.class)
            .extracting("baseCode")
            .isEqualTo(RecruitingErrorCode.RECRUITING_REGISTRATION_FORBIDDEN);

        then(loadQuotaPort).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("READY와 REGISTERED 합계가 targetCount에 도달하면 추가 READY를 거부한다")
    void fullQuotaRejectsPreparation() {
        RecruitingApplication application = finalPassedApplication();
        given(loadApplicationPort.getByIdWithDetailsForUpdate(900L)).willReturn(application);
        given(getChallengerRoleUseCase.isCentralCoreInGisu(1L, 1L)).willReturn(true);
        given(loadQuotaPort.getBySeasonIdAndTrackForUpdate(1L, ChallengerTrack.DESIGN))
            .willReturn(quota(application, 1));
        given(loadApplicationPort.countReservedOrRegisteredBySeasonIdAndTrack(1L, ChallengerTrack.DESIGN))
            .willReturn(1L);

        assertThatThrownBy(() -> sut.prepareRegistration(PrepareRecruitingRegistrationCommand.of(900L, 1L)))
            .isInstanceOf(RecruitingDomainException.class)
            .extracting("baseCode")
            .isEqualTo(RecruitingErrorCode.RECRUITING_QUOTA_EXCEEDED);
    }

    @Test
    @DisplayName("READY 취소는 NOT_READY로 되돌려 예약 자리를 반환한다")
    void cancelReadyReturnsCapacity() {
        RecruitingApplication application = finalPassedApplication();
        application.markRegistrationReady(1L);
        given(loadApplicationPort.getByIdWithDetailsForUpdate(900L)).willReturn(application);
        given(getChallengerRoleUseCase.isCentralCoreInGisu(1L, 1L)).willReturn(true);

        sut.cancelRegistration(CancelRecruitingRegistrationCommand.of(900L, 1L));

        assertThat(application.getRegistrationStatus()).isEqualTo(RecruitingApplicationRegistrationStatus.NOT_READY);
        then(saveApplicationPort).should().save(application);
    }

    @Test
    @DisplayName("READY 등록 확정은 Challenger 공개 usecase로 신규 생성 또는 track 멱등 추가 후 REGISTERED가 된다")
    void confirmReadyRegistersChallengerTrack() {
        RecruitingApplication application = finalPassedApplication();
        application.markRegistrationReady(1L);
        given(loadApplicationPort.getByIdWithDetailsForUpdate(900L)).willReturn(application);
        given(getChallengerRoleUseCase.isCentralCoreInGisu(1L, 1L)).willReturn(true);

        sut.confirmRegistration(ConfirmRecruitingRegistrationCommand.builder()
            .applicationId(900L)
            .executorMemberId(1L)
            .build());

        ArgumentCaptor<AddChallengerTrackCommand> captor = ArgumentCaptor.forClass(AddChallengerTrackCommand.class);
        then(addChallengerTrackUseCase).should().addTrack(captor.capture());
        assertThat(captor.getValue()).isEqualTo(AddChallengerTrackCommand.of(200L, 1L, ChallengerTrack.DESIGN));
        assertThat(application.getRegistrationStatus()).isEqualTo(RecruitingApplicationRegistrationStatus.REGISTERED);
    }

    @Test
    @DisplayName("REGISTERED는 READY 취소로 되돌릴 수 없다")
    void registeredCannotBeCancelled() {
        RecruitingApplication application = finalPassedApplication();
        application.markRegistrationReady(1L);
        application.register(1L);
        given(loadApplicationPort.getByIdWithDetailsForUpdate(900L)).willReturn(application);
        given(getChallengerRoleUseCase.isCentralCoreInGisu(1L, 1L)).willReturn(true);

        assertThatThrownBy(() -> sut.cancelRegistration(CancelRecruitingRegistrationCommand.of(900L, 1L)))
            .isInstanceOf(RecruitingDomainException.class)
            .extracting("baseCode")
            .isEqualTo(RecruitingErrorCode.RECRUITING_APPLICATION_INVALID_TRANSITION);

        then(saveApplicationPort).should(never()).save(any());
    }

    private RecruitingSeasonTrackQuota quota(RecruitingApplication application, int targetCount) {
        return RecruitingSeasonTrackQuota.create(
            application.getRound().getSeason(),
            application.getAcceptedTrack(),
            targetCount
        );
    }

    private RecruitingApplication finalPassedApplication() {
        RecruitingSeason season = RecruitingSeason.create(1L, 10L);
        ReflectionTestUtils.setField(season, "id", 1L);
        RecruitingRound round = RecruitingRound.createRegular(season, configuration());
        ReflectionTestUtils.setField(round, "id", 10L);
        RecruitingApplicationForm form = RecruitingApplicationForm.create(round, 500L);
        RecruitingApplication application = RecruitingApplication.createMemberDraft(
            form,
            700L,
            200L,
            RecruitingApplicantProfile.create(
                round,
                "홍길동",
                RecruitingApplicantEmail.from("applicant@example.com"),
                ChallengerTrack.WEB_PRODUCT_ENGINEER,
                ChallengerTrack.DESIGN
            ),
            "A1B2C3"
        );
        ReflectionTestUtils.setField(application, "id", 900L);
        application.submit(200L);
        application.passDocument(1L, "서류 합격");
        application.passFinal(1L, "최종 합격", ChallengerTrack.DESIGN);
        return application;
    }

    private RecruitingRoundConfiguration configuration() {
        return RecruitingRoundConfiguration.of(
            List.of(ChallengerTrack.WEB_PRODUCT_ENGINEER, ChallengerTrack.DESIGN),
            true,
            Instant.parse("2026-08-01T00:00:00Z"),
            Instant.parse("2026-08-08T00:00:00Z"),
            Instant.parse("2026-08-10T00:00:00Z"),
            false,
            null,
            null,
            Instant.parse("2026-08-16T00:00:00Z"),
            null,
            null,
            null
        );
    }
}
