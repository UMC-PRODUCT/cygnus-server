package com.umc.product.project.adapter.in.scheduler;

import java.time.Duration;
import java.time.Instant;

import org.springframework.stereotype.Component;

import com.umc.product.global.logging.OperationalMetrics;
import com.umc.product.project.application.authorization.rollout.ProjectAuthorizationSurface;
import com.umc.product.project.application.authorization.rollout.ProjectAuthorizationSurfaceBinding;
import com.umc.product.project.application.port.in.command.AutoDecideProjectMatchingRoundUseCase;
import com.umc.product.project.application.port.in.command.AutoDecisionActor;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 매칭 차수 결정 마감 시점에 트리거되어 자동 선발 UseCase 를 호출하는 driving adapter.
 * <p>
 * out-side {@link com.umc.product.project.adapter.out.scheduler.MatchingRoundDeadlineScheduler}
 * 가 {@link org.springframework.scheduling.TaskScheduler} 에 본 컴포넌트의 {@link #handle(Long)} 를 묶어 등록한다.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class MatchingRoundDeadlineHandler {

    private static final String JOB_NAME = "matching_round_deadline";

    private final AutoDecideProjectMatchingRoundUseCase autoDecideUseCase;
    private final OperationalMetrics operationalMetrics;

    /**
     * deadline 시점에 1회 호출되어 자동 선발을 실행한다. 예외는 swallow 하여 다음 task 진행을 막지 않는다.
     */
    @ProjectAuthorizationSurfaceBinding(ProjectAuthorizationSurface.SCHEDULER_MATCHING_ROUND_DEADLINE)
    public void handle(Long matchingRoundId) {
        Instant startedAt = Instant.now();
        try {
            autoDecideUseCase.autoDecide(matchingRoundId, AutoDecisionActor.matchingRoundScheduler());
            Duration duration = Duration.between(startedAt, Instant.now());
            operationalMetrics.recordBatchJob(JOB_NAME, "success", duration, 1);
        } catch (Exception e) {
            Duration duration = Duration.between(startedAt, Instant.now());
            operationalMetrics.recordBatchJob(JOB_NAME, "failure", duration, 0);
            log.error("batch job failed: jobName={}, matchingRoundId={}, durationMs={}, result={}, errorClass={}",
                JOB_NAME, matchingRoundId, duration.toMillis(), "failure", e.getClass().getSimpleName(), e);
        }
    }
}
