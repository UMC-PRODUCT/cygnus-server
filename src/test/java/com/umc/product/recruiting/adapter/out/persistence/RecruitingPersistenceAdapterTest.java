package com.umc.product.recruiting.adapter.out.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.Import;

import com.umc.product.common.domain.enums.ChallengerTrack;
import com.umc.product.recruiting.application.port.out.dto.RecruitingApplicationSummaryRow;
import com.umc.product.recruiting.domain.RecruitingApplication;
import com.umc.product.recruiting.domain.RecruitingApplicationForm;
import com.umc.product.recruiting.domain.RecruitingEvaluationCriterion;
import com.umc.product.recruiting.domain.RecruitingEvaluationTemplate;
import com.umc.product.recruiting.domain.RecruitingInterviewAssignment;
import com.umc.product.recruiting.domain.RecruitingInterviewEvaluation;
import com.umc.product.recruiting.domain.RecruitingRound;
import com.umc.product.recruiting.domain.RecruitingSeason;
import com.umc.product.recruiting.domain.enums.RecruitingApplicationStatus;
import com.umc.product.support.PersistenceAdapterTest;

@PersistenceAdapterTest
@Import({
    RecruitingSeasonPersistenceAdapter.class,
    RecruitingRoundPersistenceAdapter.class,
    RecruitingApplicationFormPersistenceAdapter.class,
    RecruitingApplicationPersistenceAdapter.class,
    RecruitingApplicationQueryRepository.class,
    RecruitingEvaluationTemplatePersistenceAdapter.class,
    RecruitingInterviewAssignmentPersistenceAdapter.class,
    RecruitingInterviewEvaluationPersistenceAdapter.class
})
class RecruitingPersistenceAdapterTest {

    @Autowired
    TestEntityManager em;

    @Autowired
    RecruitingSeasonPersistenceAdapter seasonAdapter;

    @Autowired
    RecruitingRoundPersistenceAdapter roundAdapter;

    @Autowired
    RecruitingApplicationFormPersistenceAdapter formAdapter;

    @Autowired
    RecruitingApplicationPersistenceAdapter applicationAdapter;

    @Autowired
    RecruitingEvaluationTemplatePersistenceAdapter templateAdapter;

    @Autowired
    RecruitingInterviewAssignmentPersistenceAdapter assignmentAdapter;

    @Autowired
    RecruitingInterviewEvaluationPersistenceAdapter evaluationAdapter;

    private long fixtureSeed;

    @Test
    @DisplayName("모집_시즌_차수_폼_지원서를_저장하고_fetch_join_상세로_조회한다")
    void saveAndLoadRecruitingCoreGraphWithDetails() {
        // Given
        RecruitingSeason season = seasonAdapter.save(RecruitingSeason.create(1L, 10L));
        RecruitingRound round = roundAdapter.save(RecruitingRound.createRegular(season));
        RecruitingApplicationForm form = formAdapter.save(
            RecruitingApplicationForm.create(round, 100L, ChallengerTrack.WEB_PRODUCT_ENGINEER)
        );
        RecruitingApplication application = applicationAdapter.save(RecruitingApplication.createDraft(
            form,
            1_000L,
            200L,
            "member:200",
            "APP-001",
            "m***@umc.test"
        ));
        em.flush();
        em.clear();

        // When
        RecruitingApplication reloaded = applicationAdapter.getByIdWithDetails(application.getId());

        // Then
        assertThat(seasonAdapter.getById(season.getId()).getSchoolId()).isEqualTo(10L);
        assertThat(roundAdapter.listBySeasonId(season.getId()))
            .extracting(RecruitingRound::getId)
            .containsExactly(round.getId());
        assertThat(formAdapter.findByRoundIdAndFormId(round.getId(), 100L))
            .map(RecruitingApplicationForm::getId)
            .contains(form.getId());
        assertThat(applicationAdapter.findByApplicationNo("APP-001"))
            .map(RecruitingApplication::getId)
            .contains(application.getId());
        assertThat(reloaded.getApplicationForm().getRound().getSeason().getSchoolId()).isEqualTo(10L);
    }

    @Test
    @DisplayName("지원서_중복_및_재지원_차단_조회가_상태와_학교를_구분한다")
    void duplicateAndReapplicationQueriesRespectStatusAndSchool() {
        // Given
        RecruitingGraph submitted = persistApplicationGraph(
            1L,
            10L,
            1,
            "identity:block",
            "APP-BLOCK",
            RecruitingApplicationStatus.SUBMITTED
        );
        persistApplicationGraph(
            1L,
            11L,
            1,
            "identity:block",
            "APP-OTHER-SCHOOL",
            RecruitingApplicationStatus.SUBMITTED
        );
        persistApplicationGraph(
            1L,
            10L,
            2,
            "identity:failed",
            "APP-FAILED",
            RecruitingApplicationStatus.DOCUMENT_FAILED
        );
        persistApplicationGraph(
            1L,
            10L,
            3,
            "identity:passed",
            "APP-PASSED",
            RecruitingApplicationStatus.FINAL_PASSED
        );
        em.flush();
        em.clear();

        // When
        Optional<RecruitingApplication> activeSameRound =
            applicationAdapter.findActiveByRoundIdAndApplicantIdentityKey(
                submitted.round().getId(),
                "identity:block"
            );

        // Then
        assertThat(activeSameRound).isPresent();
        assertThat(applicationAdapter.existsBlockingApplicationByGisuIdAndApplicantIdentityKey(
            1L,
            "identity:block"
        )).isTrue();
        assertThat(applicationAdapter.existsBlockingApplicationByGisuIdAndDifferentSchoolIdAndApplicantIdentityKey(
            1L,
            10L,
            "identity:block"
        )).isTrue();
        assertThat(applicationAdapter.existsBlockingApplicationByGisuIdAndApplicantIdentityKey(
            1L,
            "identity:failed"
        )).isFalse();
        assertThat(applicationAdapter.existsFinalPassedByGisuIdAndApplicantIdentityKey(
            1L,
            "identity:passed"
        )).isTrue();
    }

    @Test
    @DisplayName("지원서_요약_행은_본문과_raw_email_없이_상태_필터로_조회된다")
    void searchSummaryRowsReturnsMinimalStatusRows() {
        // Given
        persistApplicationGraph(
            2L,
            20L,
            1,
            "identity:summary",
            "APP-SUMMARY",
            RecruitingApplicationStatus.SUBMITTED
        );
        persistApplicationGraph(
            2L,
            20L,
            2,
            "identity:failed-summary",
            "APP-FAILED-SUMMARY",
            RecruitingApplicationStatus.FINAL_FAILED
        );
        em.flush();
        em.clear();

        // When
        List<RecruitingApplicationSummaryRow> rows = applicationAdapter.searchSummaryRows(
            2L,
            20L,
            List.of(RecruitingApplicationStatus.SUBMITTED)
        );

        // Then
        assertThat(rows).hasSize(1);
        RecruitingApplicationSummaryRow row = rows.get(0);
        assertThat(row.applicationNo()).isEqualTo("APP-SUMMARY");
        assertThat(row.gisuId()).isEqualTo(2L);
        assertThat(row.schoolId()).isEqualTo(20L);
        assertThat(row.track()).isEqualTo(ChallengerTrack.WEB_PRODUCT_ENGINEER);
        assertThat(row.maskedEmail()).isEqualTo("a***@umc.test");
        assertThat(row.applicationStatus()).isEqualTo(RecruitingApplicationStatus.SUBMITTED);
    }

    @Test
    @DisplayName("면접_평가_템플릿_배정_평가를_저장하고_제출_평가만_조회한다")
    void saveAndLoadEvaluationGraph() {
        // Given
        RecruitingGraph graph = persistApplicationGraph(
            3L,
            30L,
            1,
            "identity:evaluation",
            "APP-EVALUATION",
            RecruitingApplicationStatus.DOCUMENT_PASSED
        );
        RecruitingEvaluationTemplate template = templateAdapter.saveTemplate(
            RecruitingEvaluationTemplate.createDefault(graph.form())
        );
        templateAdapter.saveCriteriaAll(RecruitingEvaluationCriterion.createDefaults(template));
        RecruitingInterviewAssignment assignment = assignmentAdapter.saveAssignment(RecruitingInterviewAssignment.assign(
            graph.application(),
            901L,
            Instant.parse("2026-07-02T01:00:00Z"),
            Instant.parse("2026-07-02T01:30:00Z"),
            "온라인"
        ));
        RecruitingInterviewEvaluation submitted = RecruitingInterviewEvaluation.createDraft(assignment, 901L);
        submitted.submit(5, "함께 일하고 싶은 지원자");
        evaluationAdapter.saveEvaluation(submitted);
        evaluationAdapter.saveEvaluation(RecruitingInterviewEvaluation.createDraft(assignment, 902L));
        em.flush();
        em.clear();

        // When
        List<RecruitingInterviewEvaluation> submittedEvaluations =
            evaluationAdapter.listSubmittedByApplicationId(graph.application().getId());

        // Then
        assertThat(templateAdapter.findActiveTemplateByApplicationFormId(graph.form().getId()))
            .map(RecruitingEvaluationTemplate::getId)
            .contains(template.getId());
        assertThat(templateAdapter.listCriteriaByTemplateId(template.getId()))
            .extracting(RecruitingEvaluationCriterion::getSortOrder)
            .containsExactly(1, 2, 3);
        assertThat(assignmentAdapter.findByApplicationIdAndInterviewerMemberId(
            graph.application().getId(),
            901L
        )).isPresent();
        assertThat(submittedEvaluations)
            .extracting(RecruitingInterviewEvaluation::getEvaluatorMemberId)
            .containsExactly(901L);
        assertThat(evaluationAdapter.existsSubmittedByApplicationIdAndEvaluatorMemberId(
            graph.application().getId(),
            902L
        )).isFalse();
    }

    private RecruitingGraph persistApplicationGraph(
        Long gisuId,
        Long schoolId,
        Integer roundNo,
        String applicantIdentityKey,
        String applicationNo,
        RecruitingApplicationStatus status
    ) {
        long seed = ++fixtureSeed;
        RecruitingSeason season = seasonAdapter.findByGisuIdAndSchoolId(gisuId, schoolId)
            .orElseGet(() -> em.persist(RecruitingSeason.create(gisuId, schoolId)));
        RecruitingRound round = em.persist(RecruitingRound.createAdditional(season, roundNo));
        RecruitingApplicationForm form = em.persist(RecruitingApplicationForm.create(
            round,
            10_000L + seed,
            ChallengerTrack.WEB_PRODUCT_ENGINEER
        ));
        RecruitingApplication application = RecruitingApplication.createDraft(
            form,
            20_000L + seed,
            30_000L + seed,
            applicantIdentityKey,
            applicationNo,
            "a***@umc.test"
        );
        moveToStatus(application, status);
        em.persist(application);
        return new RecruitingGraph(season, round, form, application);
    }

    private void moveToStatus(RecruitingApplication application, RecruitingApplicationStatus status) {
        if (status == RecruitingApplicationStatus.DRAFT) {
            return;
        }
        application.submit(1L);
        if (status == RecruitingApplicationStatus.SUBMITTED) {
            return;
        }
        if (status == RecruitingApplicationStatus.DOCUMENT_FAILED) {
            application.failDocument(1L, "서류 불합격");
            return;
        }
        application.passDocument(1L, "서류 합격");
        if (status == RecruitingApplicationStatus.DOCUMENT_PASSED) {
            return;
        }
        if (status == RecruitingApplicationStatus.INTERVIEW_SKIPPED) {
            application.skipInterview(1L, "면접 생략");
            return;
        }
        if (status == RecruitingApplicationStatus.FINAL_PASSED) {
            application.passFinal(1L, "최종 합격");
            return;
        }
        if (status == RecruitingApplicationStatus.FINAL_FAILED) {
            application.failFinal(1L, "최종 불합격");
            return;
        }
        throw new IllegalArgumentException("테스트에서 지원하지 않는 지원서 상태입니다: " + status);
    }

    private record RecruitingGraph(
        RecruitingSeason season,
        RecruitingRound round,
        RecruitingApplicationForm form,
        RecruitingApplication application
    ) {
    }
}
