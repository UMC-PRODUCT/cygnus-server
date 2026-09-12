package com.umc.product.project.adapter.out.scheduler;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import com.umc.product.project.application.port.out.ScheduleMatchingRoundDeadlinePort;
import com.umc.product.project.domain.ProjectMatchingRound;

import lombok.extern.slf4j.Slf4j;

/**
 * {@code scheduler.matching-round-deadline.enabled=false} 일 때
 * {@link MatchingRoundDeadlineScheduler} 대신 주입되는 null object 구현.
 * <p>
 * {@link ScheduleMatchingRoundDeadlinePort} 는 {@code ProjectMatchingRoundCommandService} 가
 * 생성자로 요구하는 필수 의존이다. 실제 스케줄러만 {@code @ConditionalOnProperty} 로 끄면
 * 주입 대상이 사라져 컨텍스트 기동 자체가 실패하므로, off 스위치를 실제로 쓸 수 있도록
 * no-op 대체 구현을 함께 등록한다.
 * <p>
 * 두 구현의 조건은 서로 배타적이다. 실제 스케줄러는 {@code true} 또는 property 미설정에서,
 * 본 구현은 {@code false} 에서만 등록된다.
 * <p>
 * 스케줄을 등록하지 않으므로 결정 마감 시점의 자동 선발이 발화하지 않는다.
 * 운영 환경에서 끄는 경우 자동 선발은 운영진 수동 호출로만 실행된다.
 */
@Component
@ConditionalOnProperty(
    name = "scheduler.matching-round-deadline.enabled",
    havingValue = "false"
)
@Slf4j
public class NoOpMatchingRoundDeadlineScheduler implements ScheduleMatchingRoundDeadlinePort {

    public NoOpMatchingRoundDeadlineScheduler() {
        log.info("matching round deadline scheduler is disabled: auto decision will not be triggered by schedule");
    }

    @Override
    public void schedule(ProjectMatchingRound round) {
        // no-op: 스케줄러 비활성화 상태에서는 마감 task 를 등록하지 않는다.
    }

    @Override
    public void cancel(Long roundId) {
        // no-op: 등록된 task 가 없으므로 취소할 대상도 없다.
    }
}
