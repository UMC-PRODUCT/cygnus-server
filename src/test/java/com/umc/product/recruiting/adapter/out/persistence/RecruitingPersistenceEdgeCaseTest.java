package com.umc.product.recruiting.adapter.out.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import com.umc.product.recruiting.domain.enums.RecruitingApplicationFormStatus;

class RecruitingPersistenceEdgeCaseTest {

    @Test
    @DisplayName("빈 IN 조건은 repository를 호출하지 않고 빈 결과를 반환한다")
    void shortCircuitEmptyCollectionQueries() {
        RecruitingApplicationFormJpaRepository formRepository =
            mock(RecruitingApplicationFormJpaRepository.class);
        RecruitingApplicationEvaluationJpaRepository evaluationRepository =
            mock(RecruitingApplicationEvaluationJpaRepository.class);
        RecruitingRoundJpaRepository roundRepository = mock(RecruitingRoundJpaRepository.class);
        RecruitingApplicationFormPersistenceAdapter formAdapter =
            new RecruitingApplicationFormPersistenceAdapter(formRepository);
        RecruitingApplicationEvaluationPersistenceAdapter evaluationAdapter =
            new RecruitingApplicationEvaluationPersistenceAdapter(evaluationRepository);
        RecruitingRoundPersistenceAdapter roundAdapter = new RecruitingRoundPersistenceAdapter(roundRepository);

        assertThat(formAdapter.listByRoundIdsAndStatus(
            List.of(), RecruitingApplicationFormStatus.PUBLISHED
        )).isEmpty();
        assertThat(formAdapter.listByRoundIds(List.of())).isEmpty();
        assertThat(evaluationAdapter.listByApplicationIdsAndEvaluatorMemberId(null, 1L)).isEmpty();
        assertThat(roundAdapter.listBySeasonIds(List.of())).isEmpty();

        verifyNoInteractions(formRepository, evaluationRepository, roundRepository);
    }

    @Test
    @DisplayName("지원자 lock key는 기수와 회원 ID가 없으면 생성하지 않는다")
    void rejectApplicantLockWithoutRequiredScope() {
        RecruitingApplicantLockPersistenceAdapter adapter = new RecruitingApplicantLockPersistenceAdapter();

        assertThatThrownBy(() -> ReflectionTestUtils.invokeMethod(
            adapter,
            "lockKeys",
            null,
            1L,
            List.of("applicant@example.com")
        )).isInstanceOf(IllegalArgumentException.class);
    }
}
