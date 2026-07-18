package com.umc.product.notification.adapter.in.event;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.willAnswer;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.verify;

import java.lang.reflect.Method;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import com.umc.product.notification.application.port.in.DeliverTemplateEmailUseCase;
import com.umc.product.notification.domain.EmailTemplateType;
import com.umc.product.notification.domain.TemplateEmailRequestedEvent;

@DisplayName("Template email 요청 event listener")
@ExtendWith(MockitoExtension.class)
class TemplateEmailRequestedEventListenerTest {

    @Mock
    private DeliverTemplateEmailUseCase deliverTemplateEmailUseCase;

    @Test
    @DisplayName("일반 EventListener는 transaction 밖에서 동기 dispatch usecase에 위임한다")
    void 일반_listener가_transaction_밖에서_동기_위임한다() {
        AtomicBoolean transactionActive = new AtomicBoolean(true);
        TemplateEmailRequestedEvent event = event();
        willAnswer(invocation -> {
            transactionActive.set(TransactionSynchronizationManager.isActualTransactionActive());
            return null;
        }).given(deliverTemplateEmailUseCase).deliver(event);
        TemplateEmailRequestedEventListener listener =
            new TemplateEmailRequestedEventListener(deliverTemplateEmailUseCase);

        listener.handle(event);

        verify(deliverTemplateEmailUseCase).deliver(event);
        assertThat(transactionActive).isFalse();
    }

    @Test
    @DisplayName("dispatch 예외를 삼키지 않고 global relay까지 전파한다")
    void dispatch_예외를_그대로_전파한다() {
        TemplateEmailRequestedEvent event = event();
        IllegalStateException failure = new IllegalStateException("provider failed");
        willThrow(failure).given(deliverTemplateEmailUseCase).deliver(event);
        TemplateEmailRequestedEventListener listener =
            new TemplateEmailRequestedEventListener(deliverTemplateEmailUseCase);

        assertThatThrownBy(() -> listener.handle(event)).isSameAs(failure);
    }

    @Test
    @DisplayName("신규 listener에는 Async와 TransactionalEventListener가 없다")
    void listener_annotation_경계를_고정한다() throws Exception {
        Method handle = TemplateEmailRequestedEventListener.class.getMethod(
            "handle",
            TemplateEmailRequestedEvent.class
        );

        assertThat(handle.getAnnotation(EventListener.class)).isNotNull();
        assertThat(handle.getAnnotation(Async.class)).isNull();
        assertThat(handle.getAnnotation(TransactionalEventListener.class)).isNull();
    }

    private TemplateEmailRequestedEvent event() {
        return new TemplateEmailRequestedEvent(
            UUID.fromString("70000000-0000-0000-0000-000000000003"),
            Instant.parse("2026-07-18T00:00:00Z"),
            "applicant@test.umc.local",
            EmailTemplateType.RECRUITMENT_FINAL_FAILED,
            Map.of("applicantName", "홍길동")
        );
    }
}
