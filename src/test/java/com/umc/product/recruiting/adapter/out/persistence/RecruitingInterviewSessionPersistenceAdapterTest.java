package com.umc.product.recruiting.adapter.out.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;

import com.umc.product.recruiting.domain.RecruitingInterviewSchedule;
import com.umc.product.recruiting.domain.RecruitingInterviewSession;
import com.umc.product.recruiting.domain.enums.RecruitingApplicationStatus;
import com.umc.product.recruiting.domain.enums.RecruitingInterviewMode;
import com.umc.product.recruiting.domain.exception.RecruitingDomainException;
import com.umc.product.support.PersistenceAdapterTest;

@PersistenceAdapterTest
@Import({
    RecruitingSeasonPersistenceAdapter.class,
    RecruitingRoundPersistenceAdapter.class,
    RecruitingApplicationFormPersistenceAdapter.class,
    RecruitingApplicationPersistenceAdapter.class,
    RecruitingApplicationQueryRepository.class,
    RecruitingInterviewSessionPersistenceAdapter.class,
    RecruitingInterviewSessionReferencePersistenceAdapter.class
})
class RecruitingInterviewSessionPersistenceAdapterTest extends RecruitingPersistenceAdapterTestSupport {

    private static final Instant ROUND_START = Instant.parse("2026-08-10T00:00:00Z");
    private static final Instant ROUND_END = Instant.parse("2026-08-11T00:00:00Z");

    @Autowired RecruitingInterviewSessionPersistenceAdapter sessionAdapter;
    @Autowired RecruitingInterviewSessionReferencePersistenceAdapter referenceAdapter;

    @Test
    @DisplayName("모집 차수 세션 목록은 시작 시각과 ID 오름차순으로 조회한다")
    void listSessionsByStartAndId() {
        RecruitingGraph graph = persistApplicationGraph(
            101L, 1001L, 1, "session:order", RecruitingApplicationStatus.SUBMITTED
        );
        RecruitingInterviewSession later = sessionAdapter.save(session(
            graph.round().getId(), "나중", ROUND_START.plusSeconds(3600)
        ));
        RecruitingInterviewSession firstSameTime = sessionAdapter.save(session(
            graph.round().getId(), "동시각 첫 번째", ROUND_START.plusSeconds(1800)
        ));
        RecruitingInterviewSession secondSameTime = sessionAdapter.save(session(
            graph.round().getId(), "동시각 두 번째", ROUND_START.plusSeconds(1800)
        ));
        em.flush();
        em.clear();

        assertThat(sessionAdapter.listByRoundId(graph.round().getId()))
            .extracting(RecruitingInterviewSession::getId)
            .containsExactly(firstSameTime.getId(), secondSameTime.getId(), later.getId());
    }

    @Test
    @DisplayName("시작 시각 범위로 조회하면 범위 밖 세션은 DB 단에서 제외된다")
    void listSessionsByRoundIdAndStartsAtRange() {
        RecruitingGraph graph = persistApplicationGraph(
            103L, 1003L, 1, "session:range", RecruitingApplicationStatus.SUBMITTED
        );
        RecruitingInterviewSession before = sessionAdapter.save(session(
            graph.round().getId(), "범위 이전", ROUND_START
        ));
        RecruitingInterviewSession inside = sessionAdapter.save(session(
            graph.round().getId(), "범위 내부", ROUND_START.plusSeconds(3600)
        ));
        RecruitingInterviewSession after = sessionAdapter.save(session(
            graph.round().getId(), "범위 이후", ROUND_START.plusSeconds(7200)
        ));
        em.flush();
        em.clear();

        assertThat(sessionAdapter.listByRoundIdAndStartsAtRange(
            graph.round().getId(),
            ROUND_START.plusSeconds(1800),
            ROUND_START.plusSeconds(5400)
        ))
            .extracting(RecruitingInterviewSession::getId)
            .containsExactly(inside.getId());
    }

    @Test
    @DisplayName("면접 세션을 hard delete 하면 더 이상 조회되지 않는다")
    void hardDeleteSession() {
        RecruitingGraph graph = persistApplicationGraph(
            102L, 1002L, 1, "session:delete", RecruitingApplicationStatus.SUBMITTED
        );
        RecruitingInterviewSession session = sessionAdapter.save(session(
            graph.round().getId(), "삭제", ROUND_START.plusSeconds(1800)
        ));
        em.flush();

        sessionAdapter.delete(session);
        em.flush();

        assertThatThrownBy(() -> sessionAdapter.getById(session.getId()))
            .isInstanceOf(RecruitingDomainException.class);
    }

    @Test
    @DisplayName("모집 차수를 삭제하기 전 세션을 일괄 삭제할 수 있다")
    void hardDeleteSessionsByRoundId() {
        RecruitingGraph graph = persistApplicationGraph(
            104L, 1004L, 1, "session:round-delete", RecruitingApplicationStatus.SUBMITTED
        );
        sessionAdapter.save(session(graph.round().getId(), "오전", ROUND_START.plusSeconds(1800)));
        sessionAdapter.save(session(graph.round().getId(), "오후", ROUND_START.plusSeconds(3600)));
        em.flush();

        sessionAdapter.deleteByRoundId(graph.round().getId());
        em.flush();
        em.clear();

        assertThat(sessionAdapter.listByRoundId(graph.round().getId())).isEmpty();
    }

    @Test
    @DisplayName("확정 후 취소된 일정도 세션 FK 참조로 감지한다")
    void detectConfirmedAndAnyScheduleReferences() {
        RecruitingGraph graph = persistApplicationGraph(
            103L, 1003L, 1, "session:reference", RecruitingApplicationStatus.INTERVIEW_ASSIGNED
        );
        RecruitingInterviewSession session = sessionAdapter.save(session(
            graph.round().getId(), "참조", ROUND_START.plusSeconds(1800)
        ));
        RecruitingInterviewSchedule schedule = RecruitingInterviewSchedule.requestAvailability(
            graph.application(), "카카오톡 @umc"
        );
        schedule.submitAvailability(900L);
        schedule.confirm(
            session.getId(),
            session.getStartsAt(),
            session.getStartsAt().plusSeconds(900),
            session.getLocation(),
            "카카오톡 @umc"
        );
        em.persist(schedule);
        em.flush();

        assertThat(referenceAdapter.existsConfirmedBySessionId(session.getId())).isTrue();
        schedule.cancel();
        em.flush();

        assertThat(referenceAdapter.existsConfirmedBySessionId(session.getId())).isFalse();
        assertThat(referenceAdapter.existsBySessionId(session.getId())).isTrue();
    }

    private RecruitingInterviewSession session(Long roundId, String name, Instant startsAt) {
        return RecruitingInterviewSession.create(
            roundId,
            name,
            startsAt,
            startsAt.plusSeconds(1800),
            15,
            RecruitingInterviewMode.ONLINE,
            "https://meet.example.com/room",
            ROUND_START,
            ROUND_END
        );
    }
}
