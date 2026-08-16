package com.umc.product.project.adapter.out.scheduler;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.ScheduledFuture;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.test.util.ReflectionTestUtils;

import com.umc.product.global.observability.ScheduledTaskTracer;
import com.umc.product.project.adapter.in.scheduler.MatchingRoundDeadlineHandler;
import com.umc.product.project.application.port.out.LoadProjectMatchingRoundPort;
import com.umc.product.project.domain.ProjectMatchingRound;
import com.umc.product.project.domain.enums.MatchingPhase;
import com.umc.product.project.domain.enums.MatchingType;

import io.micrometer.tracing.Tracer;

@ExtendWith(MockitoExtension.class)
class MatchingRoundDeadlineSchedulerTest {

    private static final Duration TEST_DEADLINE_BUFFER = Duration.ofMinutes(3);

    @Mock
    TaskScheduler taskScheduler;
    @Mock
    MatchingRoundDeadlineHandler handler;
    @Mock
    LoadProjectMatchingRoundPort loadProjectMatchingRoundPort;
    @Mock
    ScheduledFuture<?> scheduledFuture;
    @Mock
    ScheduledFuture<?> scheduledFuture2;

    MatchingRoundDeadlineScheduler sut;

    @BeforeEach
    void setUp() {
        sut = new MatchingRoundDeadlineScheduler(
            taskScheduler,
            handler,
            loadProjectMatchingRoundPort,
            new MatchingRoundDeadlineSchedulerProperties(TEST_DEADLINE_BUFFER.toMinutes()),
            // NOOP Tracer 기반 — trace() 래퍼는 task 를 그대로 실행하므로 스케줄러 동작 검증에 영향이 없다.
            new ScheduledTaskTracer(Tracer.NOOP)
        );
    }

    @Nested
    class schedule {

        @Test
        void deadline_buffer_적용된_시점에_task가_등록된다() {
            ProjectMatchingRound round = roundWithId(1L);
            given(taskScheduler.schedule(any(Runnable.class), any(Instant.class)))
                .willAnswer(invocation -> scheduledFuture);

            sut.schedule(round);

            assertThat(sut.isScheduled(1L)).isTrue();
            Instant expectedRunAt = round.getDecisionDeadline().plus(TEST_DEADLINE_BUFFER);
            then(taskScheduler).should().schedule(any(Runnable.class), eq(expectedRunAt));
        }

        @Test
        void 동일_id_재등록시_기존_task는_취소되고_새_task가_등록된다() {
            ProjectMatchingRound round = roundWithId(1L);
            given(taskScheduler.schedule(any(Runnable.class), any(Instant.class)))
                .willAnswer(invocation -> scheduledFuture)
                .willAnswer(invocation -> scheduledFuture2);

            sut.schedule(round);
            sut.schedule(round);

            then(scheduledFuture).should().cancel(false);
            then(taskScheduler).should(times(2)).schedule(any(Runnable.class), any(Instant.class));
        }

        @Test
        void 등록된_task가_실행되면_handler에_위임한다() {
            ProjectMatchingRound round = roundWithId(1L);
            ArgumentCaptor<Runnable> taskCaptor = ArgumentCaptor.forClass(Runnable.class);
            given(taskScheduler.schedule(taskCaptor.capture(), any(Instant.class)))
                .willAnswer(invocation -> scheduledFuture);

            sut.schedule(round);
            taskCaptor.getValue().run();

            then(handler).should().handle(1L);
            assertThat(sut.isScheduled(1L)).isFalse();
        }

        @Test
        void handler_예외가_상위로_전파되어도_등록은_제거된다() {
            ProjectMatchingRound round = roundWithId(1L);
            ArgumentCaptor<Runnable> taskCaptor = ArgumentCaptor.forClass(Runnable.class);
            given(taskScheduler.schedule(taskCaptor.capture(), any(Instant.class)))
                .willAnswer(invocation -> scheduledFuture);
            willThrow(new RuntimeException("boom")).given(handler).handle(1L);

            sut.schedule(round);
            try {
                taskCaptor.getValue().run();
            } catch (RuntimeException ignored) {
                // 등록 제거 보장이 본 케이스의 검증 대상
            }

            assertThat(sut.isScheduled(1L)).isFalse();
        }
    }

    @Nested
    class cancel {

        @Test
        void 등록되지_않은_id면_no_op() {
            sut.cancel(999L);

            then(scheduledFuture).should(never()).cancel(any(boolean.class));
        }

        @Test
        void 등록된_id면_task_취소하고_제거된다() {
            ProjectMatchingRound round = roundWithId(1L);
            given(taskScheduler.schedule(any(Runnable.class), any(Instant.class)))
                .willAnswer(invocation -> scheduledFuture);
            sut.schedule(round);

            sut.cancel(1L);

            then(scheduledFuture).should().cancel(false);
            assertThat(sut.isScheduled(1L)).isFalse();
        }
    }

    @Nested
    class recoverPendingTasks {

        @Test
        void 부팅_시_미처리_round_모두_재등록된다() {
            ProjectMatchingRound round1 = roundWithId(1L);
            ProjectMatchingRound round2 = roundWithId(2L);
            given(loadProjectMatchingRoundPort.listAllNotAutoDecided())
                .willReturn(List.of(round1, round2));
            given(taskScheduler.schedule(any(Runnable.class), any(Instant.class)))
                .willAnswer(invocation -> scheduledFuture);

            sut.recoverPendingTasks();

            assertThat(sut.isScheduled(1L)).isTrue();
            assertThat(sut.isScheduled(2L)).isTrue();
            then(taskScheduler).should(times(2)).schedule(any(Runnable.class), any(Instant.class));
        }

        @Test
        void 미처리_round가_없으면_등록되는_task도_없다() {
            given(loadProjectMatchingRoundPort.listAllNotAutoDecided()).willReturn(List.of());

            sut.recoverPendingTasks();

            then(taskScheduler).should(never()).schedule(any(Runnable.class), any(Instant.class));
        }
    }

    private ProjectMatchingRound roundWithId(Long id) {
        ProjectMatchingRound round = ProjectMatchingRound.create(
            "테스트 매칭 " + id, null,
            MatchingType.PLAN_DESIGN, MatchingPhase.FIRST, 1L,
            Instant.now().plusSeconds(86_400),
            Instant.now().plusSeconds(172_800),
            Instant.now().plusSeconds(259_200)
        );
        ReflectionTestUtils.setField(round, "id", id);
        return round;
    }
}
