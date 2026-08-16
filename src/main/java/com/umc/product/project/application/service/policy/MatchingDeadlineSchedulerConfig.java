package com.umc.product.project.application.service.policy;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

/**
 * 매칭 차수 종료 시점에 자동 선발을 트리거하는 동적 스케줄러용 {@link TaskScheduler} 설정.
 * <p>
 * 본 도메인 전용 풀로 분리하여 다른 도메인의 {@code @Scheduled} 풀과 자원이 섞이지 않도록 한다.
 */
@Configuration
@ConditionalOnProperty(
    name = "scheduler.matching-round-deadline.enabled",
    havingValue = "true",
    matchIfMissing = true
)
public class MatchingDeadlineSchedulerConfig {

    @Bean(name = "matchingDeadlineTaskScheduler", destroyMethod = "shutdown")
    public TaskScheduler matchingDeadlineTaskScheduler() {
        ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
        scheduler.setPoolSize(2);
        scheduler.setThreadNamePrefix("matching-deadline-");
        scheduler.setWaitForTasksToCompleteOnShutdown(true);
        scheduler.initialize();
        return scheduler;
    }
}
