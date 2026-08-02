package com.umc.product.recruiting.adapter.out.persistence;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Import;

import com.umc.product.recruiting.domain.RecruitingInterviewSchedule;
import com.umc.product.recruiting.domain.RecruitingInterviewSession;
import com.umc.product.recruiting.domain.enums.RecruitingApplicationStatus;
import com.umc.product.recruiting.domain.enums.RecruitingInterviewMode;
import com.umc.product.support.PersistenceAdapterTest;

import jakarta.persistence.PersistenceException;

@PersistenceAdapterTest
@Import({
    RecruitingSeasonPersistenceAdapter.class,
    RecruitingRoundPersistenceAdapter.class,
    RecruitingApplicationFormPersistenceAdapter.class,
    RecruitingApplicationPersistenceAdapter.class,
    RecruitingApplicationQueryRepository.class
})
class RecruitingInterviewScheduleSessionPersistenceTest extends RecruitingPersistenceAdapterTestSupport {

    private static final Instant ROUND_START = Instant.parse("2026-08-11T00:00:00Z");
    private static final Instant ROUND_END = Instant.parse("2026-08-15T00:00:00Z");
    private static final Instant SLOT_START = Instant.parse("2026-08-12T01:00:00Z");
    private static final Instant SLOT_END = Instant.parse("2026-08-12T01:30:00Z");

    @Test
    @DisplayName("같은 세션과 시작 시각에는 확정 일정을 하나만 저장한다")
    void 같은_세션과_시작_시각에는_확정_일정을_하나만_저장한다() {
        RecruitingGraph firstGraph = persistApplicationGraph(
            91L, 901L, 1, "session:first", RecruitingApplicationStatus.INTERVIEW_ASSIGNED
        );
        RecruitingGraph secondGraph = persistApplicationGraph(
            92L, 902L, 1, "session:second", RecruitingApplicationStatus.INTERVIEW_ASSIGNED
        );
        RecruitingInterviewSession session = persistSession(firstGraph);
        em.persist(confirmedSchedule(firstGraph, session.getId()));
        em.flush();

        assertThatThrownBy(() -> em.persist(confirmedSchedule(secondGraph, session.getId())))
            .isInstanceOf(PersistenceException.class);
    }

    @Test
    @DisplayName("기존 확정 일정은 세션 연결 없이 저장할 수 있다")
    void 기존_확정_일정은_세션_연결_없이_저장할_수_있다() {
        RecruitingGraph graph = persistApplicationGraph(
            93L, 903L, 1, "session:legacy", RecruitingApplicationStatus.INTERVIEW_ASSIGNED
        );
        RecruitingInterviewSchedule schedule = RecruitingInterviewSchedule.requestAvailability(
            graph.application(),
            "카카오톡 @umc"
        );
        schedule.submitAvailability(700L);
        schedule.confirm(SLOT_START, SLOT_END, "온라인", "카카오톡 @umc");

        em.persist(schedule);

        assertThatCode(em::flush).doesNotThrowAnyException();
    }

    private RecruitingInterviewSession persistSession(RecruitingGraph graph) {
        RecruitingInterviewSession session = RecruitingInterviewSession.create(
            graph.round().getId(),
            "오전 면접",
            SLOT_START,
            Instant.parse("2026-08-12T03:00:00Z"),
            30,
            RecruitingInterviewMode.ONLINE,
            "https://meet.example.com/umc",
            ROUND_START,
            ROUND_END
        );
        em.persist(session);
        em.flush();
        return session;
    }

    private RecruitingInterviewSchedule confirmedSchedule(RecruitingGraph graph, Long sessionId) {
        RecruitingInterviewSchedule schedule = RecruitingInterviewSchedule.requestAvailability(
            graph.application(),
            "카카오톡 @umc"
        );
        schedule.submitAvailability(700L + graph.application().getId());
        schedule.confirm(sessionId, SLOT_START, SLOT_END, "온라인", "카카오톡 @umc");
        return schedule;
    }
}
