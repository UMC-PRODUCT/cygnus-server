package com.umc.product.notification.adapter.in.scheduler;

import static org.mockito.BDDMockito.then;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.umc.product.notification.application.port.in.ProcessFcmOutboxUseCase;

@ExtendWith(MockitoExtension.class)
@DisplayName("FcmOutboxScheduler")
class FcmOutboxSchedulerTest {

    @Mock
    ProcessFcmOutboxUseCase processFcmOutboxUseCase;

    @Test
    @DisplayName("스케줄마다 pending outbox 처리를 위임한다")
    void scheduled_processing_delegates() {
        FcmOutboxScheduler sut = new FcmOutboxScheduler(processFcmOutboxUseCase);

        sut.processPendingEvents();

        then(processFcmOutboxUseCase).should().process();
    }
}
