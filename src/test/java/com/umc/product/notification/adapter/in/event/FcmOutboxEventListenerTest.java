package com.umc.product.notification.adapter.in.event;

import static org.mockito.BDDMockito.then;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.umc.product.global.config.FcmProperties;
import com.umc.product.notification.application.port.in.ProcessFcmOutboxUseCase;
import com.umc.product.notification.domain.FcmOutboxEvent;

@ExtendWith(MockitoExtension.class)
@DisplayName("FcmOutboxEventListener")
class FcmOutboxEventListenerTest {

    @Mock
    ProcessFcmOutboxUseCase processFcmOutboxUseCase;

    @Test
    @DisplayName("FCM이 활성화되면 commit 이후 outbox를 즉시 처리한다")
    void enabled_processes_outbox() {
        FcmOutboxEventListener sut = new FcmOutboxEventListener(
            new FcmProperties(true, true), processFcmOutboxUseCase
        );

        sut.handleFcmOutboxEvent(FcmOutboxEvent.create());

        then(processFcmOutboxUseCase).should().process();
    }

    @Test
    @DisplayName("FCM이 비활성화되면 outbox 처리를 생략한다")
    void disabled_skips_outbox() {
        FcmOutboxEventListener sut = new FcmOutboxEventListener(
            new FcmProperties(false, true), processFcmOutboxUseCase
        );

        sut.handleFcmOutboxEvent(FcmOutboxEvent.create());

        then(processFcmOutboxUseCase).shouldHaveNoInteractions();
    }
}
