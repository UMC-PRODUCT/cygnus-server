package com.umc.product.recruiting.adapter.out.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;

import com.umc.product.recruiting.domain.RecruitingInterviewSchedule;
import com.umc.product.recruiting.domain.enums.RecruitingApplicationStatus;
import com.umc.product.support.PersistenceAdapterTest;

@PersistenceAdapterTest
@Import({
    RecruitingSeasonPersistenceAdapter.class,
    RecruitingRoundPersistenceAdapter.class,
    RecruitingApplicationFormPersistenceAdapter.class,
    RecruitingApplicationPersistenceAdapter.class,
    RecruitingApplicationQueryRepository.class,
    RecruitingInterviewScheduleBoardQueryAdapter.class
})
class RecruitingInterviewScheduleBoardQueryAdapterTest extends RecruitingPersistenceAdapterTestSupport {

    @Autowired RecruitingInterviewScheduleBoardQueryAdapter adapter;

    @Test
    @DisplayName("모집 차수의 제출·확정 면접 일정과 지원자 식별 정보를 한 번에 조회한다")
    void listBoardRowsByRound() {
        RecruitingGraph included = persistApplicationGraph(
            201L, 2001L, 1, "board:included", RecruitingApplicationStatus.INTERVIEW_ASSIGNED
        );
        RecruitingGraph otherRound = persistApplicationGraph(
            201L, 2001L, 2, "board:other", RecruitingApplicationStatus.INTERVIEW_ASSIGNED
        );
        RecruitingInterviewSchedule submitted = RecruitingInterviewSchedule.requestAvailability(
            included.application(), "비공개 연락처"
        );
        submitted.submitAvailability(7001L);
        RecruitingInterviewSchedule requested = RecruitingInterviewSchedule.requestAvailability(
            otherRound.application(), "비공개 연락처"
        );
        em.persist(submitted);
        em.persist(requested);
        em.flush();
        em.clear();

        var rows = adapter.listByRoundId(included.round().getId());

        assertThat(rows).singleElement().satisfies(row -> {
            assertThat(row.applicationId()).isEqualTo(included.application().getId());
            assertThat(row.applicantName()).isEqualTo("board:included");
            assertThat(row.availabilityFormResponseId()).isEqualTo(7001L);
        });
    }
}
