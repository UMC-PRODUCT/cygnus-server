package com.umc.product.support.isolation;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.scheduling.config.TaskManagementConfigUtils;

import com.umc.product.project.adapter.out.scheduler.MatchingRoundDeadlineScheduler;
import com.umc.product.project.adapter.out.scheduler.NoOpMatchingRoundDeadlineScheduler;
import com.umc.product.project.application.port.out.ScheduleMatchingRoundDeadlinePort;
import com.umc.product.support.IntegrationTestSupport;

/**
 * test 프로필에서 DB 에 쓰는 백그라운드 스케줄러가 하나도 살아 있지 않음을 고정한다.
 * <p>
 * {@link DatabaseIsolation} 은 각 테스트 종료 후 전체 테이블을 TRUNCATE 한다. 테스트가
 * 제어하지 못하는 스레드가 같은 DB 에 쓰고 있으면 TRUNCATE 와 락이 겹치거나, TRUNCATE 이후
 * 커밋된 행이 다음 테스트로 새어 나가 전체 실행에서만 재현되는 간헐 실패가 된다.
 * <p>
 * 이 테스트가 깨졌다면 스케줄러를 다시 켠 것이다. 켜야 한다면 해당 테스트에서만
 * {@code @TestPropertySource} 로 국소 활성화하고, 발화한 task 가 끝날 때까지 기다린 뒤
 * 종료해야 한다.
 */
@DisplayName("test 프로필 백그라운드 스케줄러 격리")
class BackgroundSchedulerIsolationTest extends IntegrationTestSupport {

    @Autowired
    private ApplicationContext applicationContext;

    @Autowired
    private ScheduleMatchingRoundDeadlinePort scheduleMatchingRoundDeadlinePort;

    @Test
    @DisplayName("@Scheduled 를 구동하는 스케줄링 인프라가 등록되지 않는다")
    void scheduling_인프라가_등록되지_않는다() {
        assertThat(applicationContext.containsBean(
            TaskManagementConfigUtils.SCHEDULED_ANNOTATION_PROCESSOR_BEAN_NAME)).isFalse();
        assertThat(applicationContext.containsBean("taskScheduler")).isFalse();
    }

    @Test
    @DisplayName("매칭 차수 마감 전용 스케줄러 풀을 등록하지 않는다")
    void 매칭_마감_전용_스케줄러_풀을_등록하지_않는다() {
        assertThat(applicationContext.containsBean("matchingDeadlineTaskScheduler")).isFalse();
        assertThat(applicationContext.getBeanNamesForType(MatchingRoundDeadlineScheduler.class)).isEmpty();
    }

    @Test
    @DisplayName("매칭 차수 마감 Port 는 no-op 구현이 주입된다")
    void 매칭_마감_port_는_no_op_구현이_주입된다() {
        assertThat(scheduleMatchingRoundDeadlinePort).isInstanceOf(NoOpMatchingRoundDeadlineScheduler.class);
    }
}
