package com.umc.product.recruiting.application.service.command;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.umc.product.authorization.application.port.in.query.GetChallengerRoleUseCase;
import com.umc.product.common.domain.enums.ChallengerTrack;
import com.umc.product.recruiting.application.port.in.command.dto.DecideRecruitingDocumentCommand;
import com.umc.product.recruiting.application.port.in.command.dto.DecideRecruitingFinalCommand;
import com.umc.product.recruiting.application.port.in.command.dto.RecruitingDecisionStatus;
import com.umc.product.recruiting.application.port.out.LoadRecruitingApplicationPort;
import com.umc.product.recruiting.application.port.out.SaveRecruitingApplicationPort;
import com.umc.product.recruiting.domain.RecruitingApplicantEmail;
import com.umc.product.recruiting.domain.RecruitingApplicantProfile;
import com.umc.product.recruiting.domain.RecruitingApplication;
import com.umc.product.recruiting.domain.RecruitingApplicationForm;
import com.umc.product.recruiting.domain.RecruitingRound;
import com.umc.product.recruiting.domain.RecruitingRoundConfiguration;
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
    GetChallengerRoleUseCase getChallengerRoleUseCase;
    @Mock
    RecruitingConcurrencyLockService concurrencyLockService;
    @InjectMocks
    RecruitingDecisionCommandService sut;

    @Test
    @DisplayName("서류 합격 결정은 SUBMITTED 지원서를 DOCUMENT_PASSED로 변경한다")
    void passDocumentChangesStatus() {
        RecruitingApplication application = submittedApplication();
        given(concurrencyLockService.lockApplication(900L)).willReturn(application);

        sut.decideDocument(DecideRecruitingDocumentCommand.builder()
            .applicationId(900L)
            .decision(RecruitingDecisionStatus.PASS)
            .decidedByMemberId(1L)
            .reason("서류 합격")
            .build());

        assertThat(application.getStatus()).isEqualTo(RecruitingApplicationStatus.DOCUMENT_PASSED);
        then(saveApplicationPort).should().save(application);
    }

    @Test
    @DisplayName("학교 회장단은 지원한 트랙을 선택해 최종 합격시키고 등록 상태는 NOT_READY로 둔다")
    void schoolCoreDecidesFinalPassWithAcceptedTrack() {
        RecruitingApplication application = documentPassedApplication();
        given(concurrencyLockService.lockApplicantThenApplication(900L, List.of())).willReturn(application);
        given(getChallengerRoleUseCase.isSchoolCoreInGisu(1L, 1L, 10L)).willReturn(true);

        sut.decideFinal(finalPassCommand(1L, ChallengerTrack.DESIGN));

        assertThat(application.getStatus()).isEqualTo(RecruitingApplicationStatus.FINAL_PASSED);
        assertThat(application.getAcceptedTrack()).isEqualTo(ChallengerTrack.DESIGN);
        assertThat(application.getRegistrationStatus()).isEqualTo(RecruitingApplicationRegistrationStatus.NOT_READY);
        then(saveApplicationPort).should().save(application);
    }

    @Test
    @DisplayName("중앙 총괄단은 최종 합격을 판정할 수 있다")
    void centralCoreDecidesFinalPass() {
        RecruitingApplication application = documentPassedApplication();
        given(concurrencyLockService.lockApplicantThenApplication(900L, List.of())).willReturn(application);
        given(getChallengerRoleUseCase.isCentralCoreInGisu(1L, 1L)).willReturn(true);

        sut.decideFinal(finalPassCommand(1L, ChallengerTrack.WEB_PRODUCT_ENGINEER));

        assertThat(application.getStatus()).isEqualTo(RecruitingApplicationStatus.FINAL_PASSED);
    }

    @Test
    @DisplayName("평가자 whitelist만으로는 최종 판정을 할 수 없다")
    void evaluatorWhitelistCannotDecideFinal() {
        RecruitingApplication application = documentPassedApplication();
        given(concurrencyLockService.lockApplicantThenApplication(900L, List.of())).willReturn(application);

        assertThatThrownBy(() -> sut.decideFinal(finalPassCommand(99L, ChallengerTrack.DESIGN)))
            .isInstanceOf(RecruitingDomainException.class)
            .extracting("baseCode")
            .isEqualTo(RecruitingErrorCode.RECRUITING_FINAL_DECISION_FORBIDDEN);

        then(saveApplicationPort).should(never()).save(any());
    }

    @Test
    @DisplayName("최종 합격에는 1지망 또는 2지망 acceptedTrack이 필요하다")
    void finalPassRequiresAppliedTrack() {
        RecruitingApplication application = documentPassedApplication();
        given(concurrencyLockService.lockApplicantThenApplication(900L, List.of())).willReturn(application);
        given(getChallengerRoleUseCase.isCentralCoreInGisu(1L, 1L)).willReturn(true);

        assertThatThrownBy(() -> sut.decideFinal(finalPassCommand(1L, ChallengerTrack.MOBILE_PRODUCT_ENGINEER)))
            .isInstanceOf(RecruitingDomainException.class)
            .extracting("baseCode")
            .isEqualTo(RecruitingErrorCode.RECRUITING_APPLICATION_INVALID_ACCEPTED_TRACK);
    }

    @Test
    @DisplayName("같은 target gisu에서 이미 최종 합격한 지원자가 있으면 다른 학교 지원도 합격시킬 수 없다")
    void finalPassRejectsDuplicateAcrossSchools() {
        RecruitingApplication application = documentPassedApplication();
        given(concurrencyLockService.lockApplicantThenApplication(900L, List.of())).willReturn(application);
        given(getChallengerRoleUseCase.isCentralCoreInGisu(1L, 1L)).willReturn(true);
        given(loadApplicationPort.existsFinalPassedByGisuIdAndApplicant(
            1L,
            200L,
            "applicant@example.com",
            900L
        )).willReturn(true);

        assertThatThrownBy(() -> sut.decideFinal(finalPassCommand(1L, ChallengerTrack.DESIGN)))
            .isInstanceOf(RecruitingDomainException.class)
            .extracting("baseCode")
            .isEqualTo(RecruitingErrorCode.RECRUITING_APPLICATION_FINAL_PASS_ALREADY_EXISTS);
    }

    private DecideRecruitingFinalCommand finalPassCommand(Long memberId, ChallengerTrack acceptedTrack) {
        return DecideRecruitingFinalCommand.builder()
            .applicationId(900L)
            .decision(RecruitingDecisionStatus.PASS)
            .acceptedTrack(acceptedTrack)
            .decidedByMemberId(memberId)
            .reason("최종 합격")
            .build();
    }

    private RecruitingApplication documentPassedApplication() {
        RecruitingApplication application = submittedApplication();
        application.passDocument(1L, "서류 합격");
        return application;
    }

    private RecruitingApplication submittedApplication() {
        RecruitingApplicationForm form = applicationForm();
        RecruitingApplication application = RecruitingApplication.createMemberDraft(
            form,
            700L,
            200L,
            RecruitingApplicantProfile.create(
                form.getRound(),
                "홍길동",
                RecruitingApplicantEmail.from("applicant@example.com"),
                ChallengerTrack.WEB_PRODUCT_ENGINEER,
                ChallengerTrack.DESIGN
            ),
            "A1B2C3"
        );
        ReflectionTestUtils.setField(application, "id", 900L);
        application.submit(200L);
        return application;
    }

    private RecruitingApplicationForm applicationForm() {
        RecruitingSeason season = RecruitingSeason.create(1L, 10L);
        ReflectionTestUtils.setField(season, "id", 1L);
        RecruitingRound round = RecruitingRound.createRegular(
            season,
            RecruitingRoundConfiguration.of(
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
            )
        );
        ReflectionTestUtils.setField(round, "id", 10L);
        RecruitingApplicationForm form = RecruitingApplicationForm.create(round, 500L);
        ReflectionTestUtils.setField(form, "id", 100L);
        form.publish(form.getRound().getRecruitableTracks());
        return form;
    }
}
