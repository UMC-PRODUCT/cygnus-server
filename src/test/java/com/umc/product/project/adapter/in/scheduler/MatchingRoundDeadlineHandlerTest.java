package com.umc.product.project.adapter.in.scheduler;

import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.umc.product.global.logging.OperationalMetrics;
import com.umc.product.project.application.port.in.command.AutoDecideProjectMatchingRoundUseCase;
import com.umc.product.project.application.port.in.command.AutoDecisionActor;

@ExtendWith(MockitoExtension.class)
class MatchingRoundDeadlineHandlerTest {

    @Mock
    AutoDecideProjectMatchingRoundUseCase autoDecideUseCase;

    @Mock
    OperationalMetrics operationalMetrics;

    @InjectMocks
    MatchingRoundDeadlineHandler sut;

    @Test
    void handleCallsAutoDecideWithSchedulerActor() {
        sut.handle(42L);

        then(autoDecideUseCase).should().autoDecide(42L, AutoDecisionActor.matchingRoundScheduler());
    }

    @Test
    void autoDecideFailureIsNotPropagated() {
        given(autoDecideUseCase.autoDecide(42L, AutoDecisionActor.matchingRoundScheduler()))
            .willThrow(new RuntimeException("boom"));

        sut.handle(42L);
        // 예외 미전파 — 호출이 정상 종료
    }
}
