package com.umc.product.recruiting.adapter.out.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;

import com.umc.product.common.domain.enums.ChallengerTrack;
import com.umc.product.recruiting.domain.RecruitingApplicationForm;
import com.umc.product.recruiting.domain.RecruitingFormSectionPolicy;
import com.umc.product.recruiting.domain.RecruitingRound;
import com.umc.product.recruiting.domain.RecruitingRoundConfiguration;
import com.umc.product.recruiting.domain.RecruitingSeason;
import com.umc.product.support.PersistenceAdapterTest;

@PersistenceAdapterTest
@Import(RecruitingFormSectionPolicyPersistenceAdapter.class)
class RecruitingFormSectionPolicyPersistenceAdapterTest {

    @Autowired
    TestEntityManager em;

    @Autowired
    RecruitingSeasonJpaRepository seasonRepository;

    @Autowired
    RecruitingRoundJpaRepository roundRepository;

    @Autowired
    RecruitingApplicationFormJpaRepository formRepository;

    @Autowired
    RecruitingFormSectionPolicyPersistenceAdapter policyAdapter;

    @Test
    @DisplayName("지원 Form의 COMMON과 TRACK 섹션 정책을 저장하고 조회한다")
    void saveAndListPolicies() {
        RecruitingApplicationForm form = persistedForm();
        policyAdapter.save(RecruitingFormSectionPolicy.createCommon(form, 10L));
        policyAdapter.save(RecruitingFormSectionPolicy.createTrack(form, 11L, ChallengerTrack.PLAN));

        assertThat(policyAdapter.listByApplicationFormId(form.getId()))
            .extracting(RecruitingFormSectionPolicy::getFormSectionId)
            .containsExactly(10L, 11L);
    }

    @Test
    @DisplayName("같은 Form section ID에는 하나의 정책만 저장할 수 있다")
    void rejectDuplicateFormSectionPolicy() {
        RecruitingApplicationForm form = persistedForm();
        policyAdapter.save(RecruitingFormSectionPolicy.createCommon(form, 10L));
        em.flush();

        assertThatThrownBy(() -> {
            policyAdapter.save(RecruitingFormSectionPolicy.createTrack(form, 10L, ChallengerTrack.PLAN));
            em.flush();
        }).isInstanceOf(DataIntegrityViolationException.class);
    }

    private RecruitingApplicationForm persistedForm() {
        RecruitingSeason season = seasonRepository.save(RecruitingSeason.create(1L, 10L));
        RecruitingRound round = roundRepository.save(RecruitingRound.createRegular(
            season,
            RecruitingRoundConfiguration.of(
                List.of(ChallengerTrack.PLAN),
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
        ));
        return formRepository.save(RecruitingApplicationForm.create(round, 100L));
    }
}
