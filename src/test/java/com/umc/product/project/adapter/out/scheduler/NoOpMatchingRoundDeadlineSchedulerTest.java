package com.umc.product.project.adapter.out.scheduler;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import com.umc.product.project.application.port.out.ScheduleMatchingRoundDeadlinePort;

@DisplayName("NoOpMatchingRoundDeadlineScheduler")
class NoOpMatchingRoundDeadlineSchedulerTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
        .withUserConfiguration(NoOpMatchingRoundDeadlineScheduler.class);

    @Test
    @DisplayName("스케줄러를 끄면 ScheduleMatchingRoundDeadlinePort 의 no-op 구현이 대신 등록된다")
    void 스케줄러_비활성화시_no_op_port_구현을_등록한다() {
        contextRunner
            .withPropertyValues("scheduler.matching-round-deadline.enabled=false")
            .run(context -> assertThat(context)
                .hasSingleBean(ScheduleMatchingRoundDeadlinePort.class)
                .getBean(ScheduleMatchingRoundDeadlinePort.class)
                .isInstanceOf(NoOpMatchingRoundDeadlineScheduler.class));
    }

    @Test
    @DisplayName("스케줄러가 켜져 있으면 no-op 구현을 등록하지 않는다")
    void 스케줄러_활성화시_no_op_구현을_등록하지_않는다() {
        contextRunner
            .withPropertyValues("scheduler.matching-round-deadline.enabled=true")
            .run(context -> assertThat(context).doesNotHaveBean(NoOpMatchingRoundDeadlineScheduler.class));
    }

    @Test
    @DisplayName("property 미설정은 활성화로 간주하므로 no-op 구현을 등록하지 않는다")
    void property_미설정시_no_op_구현을_등록하지_않는다() {
        contextRunner.run(context ->
            assertThat(context).doesNotHaveBean(NoOpMatchingRoundDeadlineScheduler.class));
    }

    @Test
    @DisplayName("schedule 과 cancel 은 아무것도 하지 않고 예외 없이 통과한다")
    void schedule_과_cancel_은_예외_없이_통과한다() {
        NoOpMatchingRoundDeadlineScheduler sut = new NoOpMatchingRoundDeadlineScheduler();

        assertThatCode(() -> {
            sut.schedule(null);
            sut.cancel(1L);
        }).doesNotThrowAnyException();
    }
}
