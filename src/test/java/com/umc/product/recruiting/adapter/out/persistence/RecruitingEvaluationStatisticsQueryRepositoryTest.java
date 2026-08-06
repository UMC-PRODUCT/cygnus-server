package com.umc.product.recruiting.adapter.out.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.Import;

import com.umc.product.common.domain.enums.ChallengerTrack;
import com.umc.product.recruiting.application.port.out.dto.RecruitingEvaluationStatisticsRow;
import com.umc.product.recruiting.domain.RecruitingApplicantEmail;
import com.umc.product.recruiting.domain.RecruitingApplicantProfile;
import com.umc.product.recruiting.domain.RecruitingApplication;
import com.umc.product.recruiting.domain.RecruitingApplicationForm;
import com.umc.product.recruiting.domain.RecruitingRound;
import com.umc.product.recruiting.domain.RecruitingRoundConfiguration;
import com.umc.product.recruiting.domain.RecruitingSeason;
import com.umc.product.recruiting.domain.enums.RecruitingApplicationStatus;
import com.umc.product.support.PersistenceAdapterTest;

@PersistenceAdapterTest
@Import(RecruitingEvaluationStatisticsQueryRepository.class)
@DisplayName("RecruitingEvaluationStatisticsQueryRepository")
class RecruitingEvaluationStatisticsQueryRepositoryTest {

    @Autowired
    RecruitingEvaluationStatisticsQueryRepository sut;

    @Autowired
    TestEntityManager em;

    @Test
    @DisplayName("기수와 상태를 필터링하고 1지망 트랙 및 상태별로 집계한다")
    void listByGisuId는_기수와_상태를_필터링하고_1지망_트랙_및_상태별로_집계한다() {
        RecruitingSeason targetSeason = persistSeason(100L, 200L);
        RecruitingRound targetRound = em.persist(RecruitingRound.createRegular(targetSeason, configuration()));
        RecruitingApplicationForm targetForm = em.persist(RecruitingApplicationForm.create(targetRound, 1_001L));

        persistApplication(targetForm, targetRound, 1L, ChallengerTrack.PLAN, RecruitingApplicationStatus.SUBMITTED);
        persistApplication(targetForm, targetRound, 2L, ChallengerTrack.PLAN, RecruitingApplicationStatus.DOCUMENT_FAILED);
        persistApplication(targetForm, targetRound, 3L, ChallengerTrack.DESIGN, RecruitingApplicationStatus.CANCELLED);

        RecruitingSeason otherSeason = persistSeason(999L, 200L);
        RecruitingRound otherRound = em.persist(RecruitingRound.createRegular(otherSeason, configuration()));
        RecruitingApplicationForm otherForm = em.persist(RecruitingApplicationForm.create(otherRound, 2_001L));
        persistApplication(otherForm, otherRound, 4L, ChallengerTrack.PLAN, RecruitingApplicationStatus.SUBMITTED);

        em.flush();
        em.clear();

        List<RecruitingEvaluationStatisticsRow> result = sut.listByGisuId(100L);

        assertThat(result)
            .extracting(
                RecruitingEvaluationStatisticsRow::schoolId,
                RecruitingEvaluationStatisticsRow::track,
                RecruitingEvaluationStatisticsRow::status,
                RecruitingEvaluationStatisticsRow::count
            )
            .containsExactlyInAnyOrder(
                tuple(200L, ChallengerTrack.PLAN,
                    RecruitingApplicationStatus.SUBMITTED, 1L),
                tuple(200L, ChallengerTrack.PLAN,
                    RecruitingApplicationStatus.DOCUMENT_FAILED, 1L)
            );
    }

    private RecruitingSeason persistSeason(Long gisuId, Long schoolId) {
        return em.persist(RecruitingSeason.create(gisuId, schoolId));
    }

    private void persistApplication(
        RecruitingApplicationForm form,
        RecruitingRound round,
        Long seed,
        ChallengerTrack track,
        RecruitingApplicationStatus status
    ) {
        RecruitingApplication application = RecruitingApplication.createMemberDraft(
            form,
            10_000L + seed,
            20_000L + seed,
            RecruitingApplicantProfile.create(
                round,
                "지원자" + seed,
                RecruitingApplicantEmail.from("applicant" + seed + "@example.com"),
                track,
                null
            ),
            String.format("A%05d", seed)
        );
        if (status != RecruitingApplicationStatus.DRAFT) {
            application.submit(application.getApplicantMemberId());
        }
        if (status == RecruitingApplicationStatus.CANCELLED) {
            application.cancel(application.getApplicantMemberId(), "테스트 취소");
        } else if (status == RecruitingApplicationStatus.DOCUMENT_FAILED) {
            application.failDocument(999L, "테스트 서류 불합격");
        }
        em.persist(application);
    }

    private RecruitingRoundConfiguration configuration() {
        return RecruitingRoundConfiguration.of(
            List.of(
                ChallengerTrack.PLAN,
                ChallengerTrack.DESIGN,
                ChallengerTrack.WEB_PRODUCT_ENGINEER,
                ChallengerTrack.MOBILE_PRODUCT_ENGINEER
            ),
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
