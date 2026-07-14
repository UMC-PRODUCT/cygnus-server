package com.umc.product.support;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

import com.umc.product.project.application.port.out.ScheduleMatchingRoundDeadlinePort;
import com.umc.product.project.domain.ProjectMatchingRound;

@TestConfiguration(proxyBeanMethods = false)
public class TestMatchingDeadlineSchedulerConfig {

    @Bean
    @Primary
    ScheduleMatchingRoundDeadlinePort testScheduleMatchingRoundDeadlinePort() {
        return new NoOpScheduleMatchingRoundDeadlinePort();
    }

    private static final class NoOpScheduleMatchingRoundDeadlinePort implements ScheduleMatchingRoundDeadlinePort {

        @Override
        public void schedule(ProjectMatchingRound round) {
        }

        @Override
        public void cancel(Long roundId) {
        }
    }
}
