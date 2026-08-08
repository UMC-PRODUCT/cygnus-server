package com.umc.product.recruiting.adapter.out.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Import;

import com.umc.product.common.domain.enums.ChallengerTrack;
import com.umc.product.recruiting.application.port.out.dto.RecruitingApplicationSummaryRow;
import com.umc.product.recruiting.domain.RecruitingApplicantEmail;
import com.umc.product.recruiting.domain.RecruitingApplicantProfile;
import com.umc.product.recruiting.domain.RecruitingApplication;
import com.umc.product.recruiting.domain.RecruitingApplicationForm;
import com.umc.product.recruiting.domain.RecruitingRound;
import com.umc.product.recruiting.domain.RecruitingSeason;
import com.umc.product.recruiting.domain.enums.RecruitingApplicationStatus;
import com.umc.product.support.PersistenceAdapterTest;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;

@PersistenceAdapterTest
@Import({
    RecruitingSeasonPersistenceAdapter.class,
    RecruitingRoundPersistenceAdapter.class,
    RecruitingApplicationFormPersistenceAdapter.class,
    RecruitingApplicationPersistenceAdapter.class,
    RecruitingApplicationQueryRepository.class
})
class RecruitingPersistenceAdapterTest extends RecruitingPersistenceAdapterTestSupport {

    @Test
    @DisplayName("회원 ID로 본인 지원서를 최신순 조회하고 다른 회원 지원서는 제외한다")
    void listApplicationsByApplicantMemberId() {
        RecruitingSeason season = seasonAdapter.save(RecruitingSeason.create(1L, 10L));
        RecruitingApplication first = saveMemberApplication(season, 1, 200L, 1_001L, "A1B2C3");
        RecruitingApplication second = saveMemberApplication(season, 2, 200L, 1_002L, "D4E5F6");
        saveMemberApplication(season, 3, 201L, 1_003L, "G7H8I9");
        em.flush();
        em.clear();

        List<RecruitingApplication> result = applicationAdapter.listByApplicantMemberId(200L);

        assertThat(result)
            .extracting(RecruitingApplication::getId)
            .containsExactly(second.getId(), first.getId());
        assertThat(result)
            .allMatch(application -> application.getApplicationForm().getRound().getSeason().getId() != null);
    }

    @Test
    @DisplayName("모집_시즌_차수_폼_지원서를_저장하고_embedded_email_경로로_조회한다")
    void saveAndLoadRecruitingCoreGraphWithDetails() {
        RecruitingSeason season = seasonAdapter.save(RecruitingSeason.create(1L, 10L));
        RecruitingRound round = roundAdapter.save(RecruitingRound.createRegular(season, applicationConfiguration()));
        RecruitingApplicationForm form = formAdapter.save(RecruitingApplicationForm.create(round, 100L));
        RecruitingApplication application = applicationAdapter.save(RecruitingApplication.createMemberDraft(
            form,
            1_000L,
            200L,
            RecruitingApplicantProfile.create(
                round,
                "지원자",
                RecruitingApplicantEmail.from("applicant@example.com"),
                ChallengerTrack.WEB_PRODUCT_ENGINEER,
                null
            ),
            "A1B2C3"
        ));
        em.flush();
        em.clear();

        RecruitingApplication reloaded = applicationAdapter.getByIdWithDetails(application.getId());

        assertThat(seasonAdapter.getById(season.getId()).getSchoolId()).isEqualTo(10L);
        assertThat(roundAdapter.listBySeasonId(season.getId()))
            .extracting(RecruitingRound::getId)
            .containsExactly(round.getId());
        assertThat(formAdapter.findByRoundId(round.getId()))
            .map(RecruitingApplicationForm::getId)
            .contains(form.getId());
        assertThat(applicationAdapter.existsByApplicantEmailAndApplicationKey(
            "applicant@example.com",
            "A1B2C3"
        )).isTrue();
        assertThat(applicationAdapter.existsByRoundIdAndApplicantEmail(
            round.getId(),
            "applicant@example.com"
        )).isTrue();
        assertThat(applicationAdapter.existsByRoundIdAndApplicantEmailAndIdNot(
            round.getId(),
            "applicant@example.com",
            application.getId()
        )).isFalse();
        assertThat(applicationAdapter.existsByRoundIdAndApplicantEmailAndIdNot(
            round.getId(),
            "applicant@example.com",
            application.getId() + 1
        )).isTrue();
        assertThat(reloaded.getApplicationForm().getRound().getSeason().getSchoolId()).isEqualTo(10L);
    }

    @Test
    @DisplayName("익명 지원서는 email과 application key로 조회하고 Form raw key를 내부에 보존한다")
    void saveAndLoadAnonymousApplicationByCredential() {
        RecruitingSeason season = seasonAdapter.save(RecruitingSeason.create(1L, 10L));
        RecruitingRound round = roundAdapter.save(RecruitingRound.createRegular(season, applicationConfiguration()));
        RecruitingApplicationForm form = formAdapter.save(RecruitingApplicationForm.create(round, 100L));
        RecruitingApplication application = applicationAdapter.save(RecruitingApplication.createAnonymousDraft(
            form,
            1_000L,
            "raw-form-access-key",
            RecruitingApplicantProfile.create(
                round,
                "익명지원자",
                RecruitingApplicantEmail.from("anonymous@example.com"),
                ChallengerTrack.WEB_PRODUCT_ENGINEER,
                null
            ),
            "A1B2C3",
            3L,
            Instant.parse("2026-07-15T00:00:00Z")
        ));
        em.flush();
        em.clear();

        RecruitingApplication found = applicationAdapter.findByApplicantEmailAndApplicationKey(
            "anonymous@example.com",
            "A1B2C3"
        ).orElseThrow();

        assertThat(found.getId()).isEqualTo(application.getId());
        assertThat(found.isAnonymous()).isTrue();
        assertThat(found.getFormResponseAccessKey()).isEqualTo("raw-form-access-key");
    }

    @Test
    @DisplayName("지원서 mutation lock은 application root만 잠근 뒤 상세 연관을 별도 조회한다")
    void mutationLockTargetsApplicationRootBeforeLoadingDetails() {
        RecruitingGraph graph = persistApplicationGraph(
            7L,
            70L,
            1,
            "identity:root-lock",
            RecruitingApplicationStatus.SUBMITTED
        );
        em.flush();
        em.clear();
        ch.qos.logback.classic.Logger sqlLogger =
            (ch.qos.logback.classic.Logger)LoggerFactory.getLogger("org.hibernate.SQL");
        Level previousLevel = sqlLogger.getLevel();
        ListAppender<ILoggingEvent> appender = new ListAppender<>();
        appender.start();
        sqlLogger.addAppender(appender);
        sqlLogger.setLevel(Level.DEBUG);
        try {
            RecruitingApplication locked = applicationAdapter.getByIdWithDetailsForUpdate(
                graph.application().getId()
            );

            assertThat(locked.getRound().getSeason().getGisuId()).isEqualTo(7L);
            List<String> sql = appender.list.stream()
                .map(ILoggingEvent::getFormattedMessage)
                .map(statement -> statement.replaceAll("\\s+", " ").toLowerCase())
                .toList();
            String lockSql = sql.stream()
                .filter(statement -> statement.contains("for no key update") || statement.contains("for update"))
                .findFirst()
                .orElseThrow();
            assertThat(lockSql)
                .contains("from public.recruiting_application")
                .doesNotContain(" join ");
            assertThat(sql.stream().filter(statement -> statement.contains(" join ")))
                .anyMatch(statement -> !statement.contains("for no key update")
                    && !statement.contains("for update"));
        } finally {
            sqlLogger.detachAppender(appender);
            appender.stop();
            sqlLogger.setLevel(previousLevel);
        }
    }

    @Test
    @DisplayName("지원서_중복_및_재지원_차단_조회가_상태와_학교를_구분한다")
    void duplicateAndReapplicationQueriesRespectStatusAndSchool() {
        RecruitingGraph submitted = persistApplicationGraph(
            1L,
            10L,
            1,
            "identity:block",
            RecruitingApplicationStatus.SUBMITTED
        );
        persistApplicationGraph(1L, 11L, 1, "identity:block", RecruitingApplicationStatus.SUBMITTED);
        persistApplicationGraph(1L, 10L, 2, "identity:failed", RecruitingApplicationStatus.DOCUMENT_FAILED);
        persistApplicationGraph(1L, 10L, 3, "identity:passed", RecruitingApplicationStatus.FINAL_PASSED);
        em.flush();
        em.clear();

        boolean sameRoundEmail = applicationAdapter.existsByRoundIdAndApplicantEmail(
            submitted.round().getId(),
            "identity.block@example.com"
        );

        assertThat(sameRoundEmail).isTrue();
        assertThat(applicationAdapter.existsBlockingApplicationByGisuIdAndApplicant(
            1L,
            null,
            "identity.block@example.com",
            null
        )).isTrue();
        assertThat(applicationAdapter.existsBlockingApplicationByGisuIdAndDifferentSchoolIdAndApplicant(
            1L,
            10L,
            null,
            "identity.block@example.com",
            null
        )).isTrue();
        assertThat(applicationAdapter.existsBlockingApplicationByGisuIdAndApplicant(
            1L,
            null,
            "identity.failed@example.com",
            null
        )).isFalse();
        assertThat(applicationAdapter.existsFinalPassedByGisuIdAndApplicant(
            1L,
            null,
            "identity.passed@example.com",
            null
        )).isTrue();
    }

    @Test
    @DisplayName("쿼터 사용량은 Season 전체 차수의 READY와 REGISTERED를 합산한다")
    void countQuotaUsageAcrossAllRoundsInSeason() {
        RecruitingGraph ready = persistApplicationGraph(
            3L,
            30L,
            1,
            "identity:ready",
            RecruitingApplicationStatus.FINAL_PASSED
        );
        RecruitingGraph registered = persistApplicationGraph(
            3L,
            30L,
            2,
            "identity:registered",
            RecruitingApplicationStatus.FINAL_PASSED
        );
        ready.application().markRegistrationReady(1L);
        registered.application().markRegistrationReady(1L);
        registered.application().register(1L);
        em.flush();
        em.clear();

        assertThat(applicationAdapter.countReservedOrRegisteredBySeasonIdAndTrack(
            ready.season().getId(),
            ChallengerTrack.WEB_PRODUCT_ENGINEER
        )).isEqualTo(2L);
    }

    @Test
    @DisplayName("지원서 요약 행은 CSV 마스킹용 이메일과 상태 필터로 조회된다")
    void searchSummaryRowsReturnsMinimalStatusRows() {
        persistApplicationGraph(2L, 20L, 1, "identity:summary", RecruitingApplicationStatus.SUBMITTED);
        persistApplicationGraph(2L, 20L, 2, "identity:failed-summary", RecruitingApplicationStatus.FINAL_FAILED);
        em.flush();
        em.clear();

        List<RecruitingApplicationSummaryRow> rows = applicationAdapter.searchSummaryRows(
            2L,
            20L,
            List.of(RecruitingApplicationStatus.SUBMITTED)
        );

        assertThat(rows).hasSize(1);
        RecruitingApplicationSummaryRow row = rows.get(0);
        assertThat(row.applicantName()).isEqualTo("identity:summary");
        assertThat(row.applicantEmail()).contains("@");
        assertThat(row.gisuId()).isEqualTo(2L);
        assertThat(row.schoolId()).isEqualTo(20L);
        assertThat(row.firstChoice()).isEqualTo(ChallengerTrack.WEB_PRODUCT_ENGINEER);
        assertThat(row.applicationStatus()).isEqualTo(RecruitingApplicationStatus.SUBMITTED);
    }

    private RecruitingApplication saveMemberApplication(
        RecruitingSeason season,
        int roundNo,
        Long memberId,
        Long formResponseId,
        String applicationKey
    ) {
        RecruitingRound round = roundAdapter.save(RecruitingRound.createAdditional(
            season,
            roundNo,
            applicationConfiguration()
        ));
        RecruitingApplicationForm form = formAdapter.save(RecruitingApplicationForm.create(
            round,
            10_000L + roundNo
        ));
        return applicationAdapter.save(RecruitingApplication.createMemberDraft(
            form,
            formResponseId,
            memberId,
            RecruitingApplicantProfile.create(
                round,
                "지원자" + memberId,
                RecruitingApplicantEmail.from("applicant" + memberId + "@example.com"),
                ChallengerTrack.WEB_PRODUCT_ENGINEER,
                null
            ),
            applicationKey
        ));
    }
}
