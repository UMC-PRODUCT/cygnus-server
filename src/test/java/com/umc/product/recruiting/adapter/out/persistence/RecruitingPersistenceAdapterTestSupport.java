package com.umc.product.recruiting.adapter.out.persistence;

import java.time.Instant;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;

import com.umc.product.common.domain.enums.ChallengerTrack;
import com.umc.product.recruiting.domain.RecruitingApplicantEmail;
import com.umc.product.recruiting.domain.RecruitingApplicantProfile;
import com.umc.product.recruiting.domain.RecruitingApplication;
import com.umc.product.recruiting.domain.RecruitingApplicationForm;
import com.umc.product.recruiting.domain.RecruitingRound;
import com.umc.product.recruiting.domain.RecruitingRoundConfiguration;
import com.umc.product.recruiting.domain.RecruitingSeason;
import com.umc.product.recruiting.domain.enums.RecruitingApplicationStatus;

abstract class RecruitingPersistenceAdapterTestSupport {

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

    private long fixtureSeed;

    RecruitingGraph persistApplicationGraph(
        Long gisuId,
        Long schoolId,
        Integer roundNo,
        String applicantName,
        RecruitingApplicationStatus status
    ) {
        long seed = ++fixtureSeed;
        RecruitingSeason season = seasonAdapter.findByGisuIdAndSchoolId(gisuId, schoolId)
            .orElseGet(() -> em.persist(RecruitingSeason.create(gisuId, schoolId)));
        RecruitingRound round = em.persist(RecruitingRound.createAdditional(
            season,
            roundNo,
            applicationConfiguration()
        ));
        RecruitingApplicationForm form = em.persist(RecruitingApplicationForm.create(round, 10_000L + seed));
        RecruitingApplication application = RecruitingApplication.createMemberDraft(
            form,
            20_000L + seed,
            30_000L + seed,
            RecruitingApplicantProfile.create(
                round,
                applicantName,
                RecruitingApplicantEmail.from(applicantName.replace(':', '.') + "@example.com"),
                ChallengerTrack.WEB_PRODUCT_ENGINEER,
                null
            ),
            String.format("%06d", seed)
        );
        moveToStatus(application, status);
        em.persist(application);
        return new RecruitingGraph(season, round, form, application);
    }

    RecruitingRoundConfiguration applicationConfiguration() {
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

    private void moveToStatus(RecruitingApplication application, RecruitingApplicationStatus status) {
        if (status == RecruitingApplicationStatus.DRAFT) {
            return;
        }
        application.submit(application.getApplicantMemberId());
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
            application.passFinal(1L, "최종 합격", application.getFirstChoice());
            return;
        }
        if (status == RecruitingApplicationStatus.FINAL_FAILED) {
            application.failFinal(1L, "최종 불합격");
            return;
        }
        throw new IllegalArgumentException("테스트에서 지원하지 않는 지원서 상태입니다: " + status);
    }

    record RecruitingGraph(
        RecruitingSeason season,
        RecruitingRound round,
        RecruitingApplicationForm form,
        RecruitingApplication application
    ) {
    }
}
