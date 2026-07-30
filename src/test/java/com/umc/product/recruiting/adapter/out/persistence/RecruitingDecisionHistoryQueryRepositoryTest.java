package com.umc.product.recruiting.adapter.out.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.util.ReflectionTestUtils;

import com.umc.product.common.domain.enums.ChallengerRoleType;
import com.umc.product.common.domain.enums.ChallengerTrack;
import com.umc.product.recruiting.application.port.out.dto.RecruitingDecisionHistoryRow;
import com.umc.product.recruiting.application.port.out.dto.RecruitingDecisionHistorySearchCondition;
import com.umc.product.recruiting.domain.RecruitingApplicantEmail;
import com.umc.product.recruiting.domain.RecruitingApplicantProfile;
import com.umc.product.recruiting.domain.RecruitingApplication;
import com.umc.product.recruiting.domain.RecruitingApplicationForm;
import com.umc.product.recruiting.domain.RecruitingDecisionHistory;
import com.umc.product.recruiting.domain.RecruitingRound;
import com.umc.product.recruiting.domain.RecruitingRoundConfiguration;
import com.umc.product.recruiting.domain.RecruitingSeason;
import com.umc.product.recruiting.domain.enums.RecruitingApplicationStatus;
import com.umc.product.support.PersistenceAdapterTest;

@PersistenceAdapterTest
@Import({
    RecruitingDecisionHistoryPersistenceAdapter.class,
    RecruitingDecisionHistoryQueryRepository.class,
    RecruitingApplicationPersistenceAdapter.class,
    RecruitingApplicationQueryRepository.class
})
@DisplayName("RecruitingDecisionHistoryQueryRepository")
class RecruitingDecisionHistoryQueryRepositoryTest {

    private static final Long GISU_ID = 1L;
    private static final Long HANYANG_SCHOOL_ID = 10L;
    private static final Long SOONGSIL_SCHOOL_ID = 20L;

    @Autowired
    TestEntityManager em;

    @Autowired
    RecruitingDecisionHistoryPersistenceAdapter decisionHistoryAdapter;

    @Autowired
    RecruitingApplicationPersistenceAdapter applicationAdapter;

    private long seed;

    @Test
    @DisplayName("등록 전이로 statusChanged 슬롯이 덮어써져도 판정 이력은 보존된다")
    void decisionHistorySurvivesRegistrationTransition() {
        RecruitingApplication application = persistApplication(HANYANG_SCHOOL_ID, "박유엠");
        application.skipInterview(700L, "면접 미진행");
        application.passFinal(700L, "최종 합격", ChallengerTrack.WEB_PRODUCT_ENGINEER);
        decisionHistoryAdapter.save(
            RecruitingDecisionHistory.create(application, 700L, ChallengerRoleType.SCHOOL_PRESIDENT)
        );
        Instant decidedAt = application.getStatusChangedAt();
        applicationAdapter.save(application);
        em.flush();

        // 중앙 운영진이 등록을 처리하면 판정 축과 공유하는 슬롯이 등록자·등록 시각으로 덮어써진다.
        application.markRegistrationReady(999L);
        application.register(999L);
        applicationAdapter.save(application);
        em.flush();
        em.clear();

        assertThat(applicationAdapter.getById(application.getId()).getStatusChangedMemberId()).isEqualTo(999L);
        Page<RecruitingDecisionHistoryRow> result = decisionHistoryAdapter.searchRows(
            condition().build(),
            PageRequest.of(0, 20)
        );
        assertThat(result.getContent()).singleElement().satisfies(row -> {
            assertThat(row.decidedByMemberId()).isEqualTo(700L);
            // Postgres timestamptz는 마이크로초까지만 저장하므로 나노초 정밀도의 in-memory 값과 오차를 허용한다.
            assertThat(row.decidedAt()).isCloseTo(decidedAt, within(1, ChronoUnit.MICROS));
            assertThat(row.decisionStatus()).isEqualTo(RecruitingApplicationStatus.FINAL_PASSED);
            assertThat(row.deciderRoleType()).isEqualTo(ChallengerRoleType.SCHOOL_PRESIDENT);
        });
    }

    @Test
    @DisplayName("학교·평가결과 필터와 지원자 이름 부분일치로 검색한다")
    void searchesBySchoolResultAndApplicantName() {
        persistDecision(HANYANG_SCHOOL_ID, "박유엠", RecruitingApplicationStatus.FINAL_PASSED, 700L);
        persistDecision(HANYANG_SCHOOL_ID, "김철수", RecruitingApplicationStatus.DOCUMENT_FAILED, 700L);
        persistDecision(SOONGSIL_SCHOOL_ID, "박유엠", RecruitingApplicationStatus.FINAL_PASSED, 701L);
        em.flush();
        em.clear();

        Page<RecruitingDecisionHistoryRow> result = decisionHistoryAdapter.searchRows(
            condition()
                .schoolIds(Set.of(HANYANG_SCHOOL_ID))
                .decisionStatuses(Set.of(RecruitingApplicationStatus.FINAL_PASSED))
                .searchName("박유")
                .build(),
            PageRequest.of(0, 20)
        );

        assertThat(result.getContent()).singleElement()
            .satisfies(row -> assertThat(row.applicantName()).isEqualTo("박유엠"));
    }

    @Test
    @DisplayName("담당자별 정렬은 최초 판정 시각 순으로 그룹을 배치하고 그룹 내부는 최신순으로 정렬한다")
    void groupByDeciderOrdersByFirstDecisionThenLatestWithinGroup() {
        // 담당자 700: 08:00, 10:00 판정 / 담당자 701: 09:00 판정
        RecruitingDecisionHistory decisionA =
            persistDecision(HANYANG_SCHOOL_ID, "지원자A", RecruitingApplicationStatus.FINAL_PASSED, 700L);
        RecruitingDecisionHistory decisionB =
            persistDecision(HANYANG_SCHOOL_ID, "지원자B", RecruitingApplicationStatus.FINAL_FAILED, 701L);
        RecruitingDecisionHistory decisionC =
            persistDecision(HANYANG_SCHOOL_ID, "지원자C", RecruitingApplicationStatus.FINAL_PASSED, 700L);
        ReflectionTestUtils.setField(decisionA, "decidedAt", Instant.parse("2026-07-01T08:00:00Z"));
        ReflectionTestUtils.setField(decisionB, "decidedAt", Instant.parse("2026-07-01T09:00:00Z"));
        ReflectionTestUtils.setField(decisionC, "decidedAt", Instant.parse("2026-07-01T10:00:00Z"));
        em.flush();
        em.clear();

        List<RecruitingDecisionHistoryRow> rows = decisionHistoryAdapter.searchRows(
            condition().latestFirst(true).groupByDecider(true).build(),
            PageRequest.of(0, 20)
        ).getContent();

        // 담당자 700의 최초 판정(08:00)이 701(09:00)보다 앞서므로 700 그룹 먼저, 그룹 내부는 최신(10:00)이 먼저.
        assertThat(rows).extracting(RecruitingDecisionHistoryRow::decidedByMemberId)
            .containsExactly(700L, 700L, 701L);
        assertThat(rows).extracting(RecruitingDecisionHistoryRow::applicantName)
            .containsExactly("지원자C", "지원자A", "지원자B");
    }

    @Test
    @DisplayName("unpaged 요청(CSV 다운로드)도 offset/limit 없이 전체 행을 반환한다")
    void searchRowsSupportsUnpaged() {
        persistDecision(HANYANG_SCHOOL_ID, "지원자A", RecruitingApplicationStatus.FINAL_PASSED, 700L);
        persistDecision(HANYANG_SCHOOL_ID, "지원자B", RecruitingApplicationStatus.FINAL_FAILED, 700L);
        em.flush();
        em.clear();

        Page<RecruitingDecisionHistoryRow> result = decisionHistoryAdapter.searchRows(
            condition().build(),
            Pageable.unpaged()
        );

        assertThat(result.getContent()).hasSize(2);
    }

    private RecruitingDecisionHistorySearchCondition.RecruitingDecisionHistorySearchConditionBuilder condition() {
        return RecruitingDecisionHistorySearchCondition.builder()
            .gisuId(GISU_ID)
            .latestFirst(true);
    }

    private RecruitingDecisionHistory persistDecision(
        Long schoolId,
        String applicantName,
        RecruitingApplicationStatus status,
        Long deciderMemberId
    ) {
        RecruitingApplication application = persistApplication(schoolId, applicantName);
        moveToDecision(application, status, deciderMemberId);
        applicationAdapter.save(application);
        return decisionHistoryAdapter.save(RecruitingDecisionHistory.create(
            application,
            deciderMemberId,
            ChallengerRoleType.SCHOOL_PRESIDENT
        ));
    }

    private void moveToDecision(RecruitingApplication application, RecruitingApplicationStatus status, Long memberId) {
        if (status == RecruitingApplicationStatus.DOCUMENT_FAILED) {
            application.failDocument(memberId, "서류 불합격");
            return;
        }
        application.skipInterview(memberId, "면접 미진행");
        if (status == RecruitingApplicationStatus.FINAL_PASSED) {
            application.passFinal(memberId, "최종 합격", ChallengerTrack.WEB_PRODUCT_ENGINEER);
        } else {
            application.failFinal(memberId, "최종 불합격");
        }
    }

    private RecruitingApplication persistApplication(Long schoolId, String applicantName) {
        long current = ++seed;
        RecruitingSeason season = em.getEntityManager()
            .createQuery(
                "select s from RecruitingSeason s where s.gisuId = :gisu and s.schoolId = :school",
                RecruitingSeason.class
            )
            .setParameter("gisu", GISU_ID)
            .setParameter("school", schoolId)
            .getResultList()
            .stream()
            .findFirst()
            .orElseGet(() -> em.persist(RecruitingSeason.create(GISU_ID, schoolId)));
        RecruitingRound round = em.persist(
            RecruitingRound.createAdditional(season, (int) current + 1, configuration())
        );
        RecruitingApplicationForm form = em.persist(RecruitingApplicationForm.create(round, 10_000L + current));
        RecruitingApplication application = RecruitingApplication.createMemberDraft(
            form,
            20_000L + current,
            30_000L + current,
            RecruitingApplicantProfile.create(
                round,
                applicantName,
                RecruitingApplicantEmail.from("applicant" + current + "@example.com"),
                ChallengerTrack.WEB_PRODUCT_ENGINEER,
                null
            ),
            String.format("%06d", current)
        );
        application.submit(application.getApplicantMemberId());
        return em.persist(application);
    }

    private RecruitingRoundConfiguration configuration() {
        return RecruitingRoundConfiguration.of(
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
        );
    }
}
