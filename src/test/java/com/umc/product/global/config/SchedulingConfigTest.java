package com.umc.product.global.config;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.scheduling.TaskScheduler;

import com.umc.product.project.application.service.policy.MatchingDeadlineSchedulerConfig;

@DisplayName("SchedulingConfig")
class SchedulingConfigTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
        .withUserConfiguration(SchedulingConfig.class, MatchingDeadlineSchedulerConfig.class);

    @Test
    @DisplayName("전역 @Scheduled 작업용 taskScheduler와 project 매칭 전용 스케줄러를 분리한다")
    void scheduled_taskScheduler와_matchingDeadlineTaskScheduler를_분리한다() {
        contextRunner.run(context -> {
            TaskScheduler scheduledTaskScheduler = context.getBean("taskScheduler", TaskScheduler.class);
            TaskScheduler matchingDeadlineTaskScheduler =
                context.getBean("matchingDeadlineTaskScheduler", TaskScheduler.class);

            assertThat(scheduledTaskScheduler).isNotSameAs(matchingDeadlineTaskScheduler);
        });
    }

    @Test
    @DisplayName("project 매칭 데드라인 스케줄러가 비활성화되면 전용 스케줄러도 등록하지 않는다")
    void matchingDeadlineScheduler_비활성화시_전용_스케줄러를_등록하지_않는다() {
        contextRunner
            .withPropertyValues("scheduler.matching-round-deadline.enabled=false")
            .run(context -> {
                assertThat(context).hasBean("taskScheduler");
                assertThat(context).doesNotHaveBean("matchingDeadlineTaskScheduler");
            });
    }
}
