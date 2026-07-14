package com.umc.product.project.adapter.out.scheduler;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import com.umc.product.project.application.port.out.ScheduleMatchingRoundDeadlinePort;
import com.umc.product.project.domain.ProjectMatchingRound;

/**
 * 매칭 차수 마감 scheduler를 명시적으로 끈 환경에서 lifecycle service의 port 계약을 유지한다.
 */
@Component
@ConditionalOnProperty(
    name = "scheduler.matching-round-deadline.enabled",
    havingValue = "false"
)
public class DisabledMatchingRoundDeadlineScheduler implements ScheduleMatchingRoundDeadlinePort {

    @Override
    public void schedule(ProjectMatchingRound round) {
        // scheduler가 비활성화된 환경에서는 등록하지 않는다.
    }

    @Override
    public void cancel(Long roundId) {
        // scheduler가 비활성화된 환경에서는 취소할 task가 없다.
    }
}
