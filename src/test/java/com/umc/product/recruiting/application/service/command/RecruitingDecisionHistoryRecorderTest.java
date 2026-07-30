package com.umc.product.recruiting.application.service.command;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.umc.product.authorization.application.port.in.query.ListChallengerRoleUseCase;
import com.umc.product.authorization.application.port.in.query.dto.ChallengerRoleInfo;
import com.umc.product.common.domain.enums.ChallengerRoleType;
import com.umc.product.common.domain.enums.ChallengerTrack;
import com.umc.product.common.domain.enums.OrganizationType;
import com.umc.product.recruiting.application.port.out.SaveRecruitingDecisionHistoryPort;
import com.umc.product.recruiting.domain.RecruitingApplicantEmail;
import com.umc.product.recruiting.domain.RecruitingApplicantProfile;
import com.umc.product.recruiting.domain.RecruitingApplication;
import com.umc.product.recruiting.domain.RecruitingApplicationForm;
import com.umc.product.recruiting.domain.RecruitingDecisionHistory;
import com.umc.product.recruiting.domain.RecruitingRound;
import com.umc.product.recruiting.domain.RecruitingRoundConfiguration;
import com.umc.product.recruiting.domain.RecruitingSeason;
import com.umc.product.recruiting.domain.enums.RecruitingApplicationStatus;

@ExtendWith(MockitoExtension.class)
class RecruitingDecisionHistoryRecorderTest {

    private static final Long GISU_ID = 1L;
    private static final Long SCHOOL_ID = 10L;
    private static final Long DECIDER_MEMBER_ID = 77L;

    @Mock
    ListChallengerRoleUseCase listChallengerRoleUseCase;
    @Mock
    SaveRecruitingDecisionHistoryPort saveDecisionHistoryPort;
    @InjectMocks
    RecruitingDecisionHistoryRecorder sut;

    @Test
    @DisplayName("지원서 학교의 회장단 직위를 최우선으로 스냅샷한다")
    void snapshotPrefersSchoolCoreOfApplicationSchool() {
        RecruitingApplication application = finalPassedApplication();
        given(listChallengerRoleUseCase.listByMemberIdAndGisuId(DECIDER_MEMBER_ID, GISU_ID)).willReturn(List.of(
            roleOf(ChallengerRoleType.SCHOOL_VICE_PRESIDENT, OrganizationType.SCHOOL, SCHOOL_ID),
            roleOf(ChallengerRoleType.SCHOOL_PRESIDENT, OrganizationType.SCHOOL, SCHOOL_ID),
            roleOf(ChallengerRoleType.CENTRAL_PRESIDENT, OrganizationType.CENTRAL, 999L)
        ));

        sut.record(application, DECIDER_MEMBER_ID);

        RecruitingDecisionHistory history = savedHistory();
        assertThat(history.getDeciderRoleType()).isEqualTo(ChallengerRoleType.SCHOOL_PRESIDENT);
        assertThat(history.getDecisionStatus()).isEqualTo(RecruitingApplicationStatus.FINAL_PASSED);
        assertThat(history.getDecidedByMemberId()).isEqualTo(DECIDER_MEMBER_ID);
        assertThat(history.getDecidedAt()).isEqualTo(application.getStatusChangedAt());
    }

    @Test
    @DisplayName("타 학교 회장단 직위는 판정 근거가 아니므로 무시하고 중앙 직위를 스냅샷한다")
    void snapshotFallsBackToCentralRoleWhenSchoolCoreIsForOtherSchool() {
        RecruitingApplication application = finalPassedApplication();
        given(listChallengerRoleUseCase.listByMemberIdAndGisuId(DECIDER_MEMBER_ID, GISU_ID)).willReturn(List.of(
            roleOf(ChallengerRoleType.SCHOOL_PRESIDENT, OrganizationType.SCHOOL, 99L),
            roleOf(ChallengerRoleType.CENTRAL_EDUCATION_TEAM_MEMBER, OrganizationType.CENTRAL, 999L)
        ));

        sut.record(application, DECIDER_MEMBER_ID);

        assertThat(savedHistory().getDeciderRoleType()).isEqualTo(ChallengerRoleType.CENTRAL_EDUCATION_TEAM_MEMBER);
    }

    @Test
    @DisplayName("기수 내 역할이 없는 SUPER_ADMIN 판정자는 직위 없이 기록한다")
    void snapshotWithoutRoleWhenDeciderHasNoChallengerRole() {
        RecruitingApplication application = finalPassedApplication();
        given(listChallengerRoleUseCase.listByMemberIdAndGisuId(DECIDER_MEMBER_ID, GISU_ID)).willReturn(List.of());

        sut.record(application, DECIDER_MEMBER_ID);

        assertThat(savedHistory().getDeciderRoleType()).isNull();
    }

    private RecruitingDecisionHistory savedHistory() {
        ArgumentCaptor<RecruitingDecisionHistory> captor = ArgumentCaptor.forClass(RecruitingDecisionHistory.class);
        then(saveDecisionHistoryPort).should().save(captor.capture());
        return captor.getValue();
    }

    private ChallengerRoleInfo roleOf(
        ChallengerRoleType roleType,
        OrganizationType organizationType,
        Long organizationId
    ) {
        return ChallengerRoleInfo.builder()
            .roleType(roleType)
            .organizationType(organizationType)
            .organizationId(organizationId)
            .gisuId(GISU_ID)
            .build();
    }

    private RecruitingApplication finalPassedApplication() {
        RecruitingSeason season = RecruitingSeason.create(GISU_ID, SCHOOL_ID);
        RecruitingRound round = RecruitingRound.createRegular(
            season,
            RecruitingRoundConfiguration.of(
                List.of(ChallengerTrack.WEB_PRODUCT_ENGINEER),
                false,
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
        RecruitingApplicationForm form = RecruitingApplicationForm.create(round, 500L);
        form.publish(round.getRecruitableTracks());
        RecruitingApplication application = RecruitingApplication.createMemberDraft(
            form,
            700L,
            200L,
            RecruitingApplicantProfile.create(
                round,
                "홍길동",
                RecruitingApplicantEmail.from("applicant@example.com"),
                ChallengerTrack.WEB_PRODUCT_ENGINEER,
                null
            ),
            "A1B2C3"
        );
        ReflectionTestUtils.setField(application, "id", 900L);
        application.submit(200L);
        application.skipInterview(1L, "면접 미진행");
        application.passFinal(DECIDER_MEMBER_ID, "최종 합격", ChallengerTrack.WEB_PRODUCT_ENGINEER);
        return application;
    }
}
