package com.umc.product.notification.adapter.in.event;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import com.umc.product.notification.application.port.in.DeliverTemplateEmailUseCase;
import com.umc.product.notification.domain.EmailTemplateType;
import com.umc.product.notification.domain.TemplateEmailRequestedEvent;

@DisplayName("Template email 요청 event listener")
class TemplateEmailRequestedEventListenerTest {

    @Test
    @DisplayName("Spring event dispatch는 호출 thread에서 transaction 없이 동기 완료된다")
    void testCase001() {
        try (AnnotationConfigApplicationContext context =
                 new AnnotationConfigApplicationContext(ListenerTestConfiguration.class)) {
            ObservingDeliverTemplateEmailUseCase useCase =
                context.getBean(ObservingDeliverTemplateEmailUseCase.class);
            Thread callerThread = Thread.currentThread();

            context.publishEvent(event());

            assertThat(useCase.completed.getCount()).isZero();
            assertThat(useCase.invocationThread).isSameAs(callerThread);
            assertThat(useCase.transactionActive).isFalse();
        }
    }

    @Test
    @DisplayName("Spring event dispatch는 listener 예외를 호출자에게 그대로 전파한다")
    void testCase002() {
        try (AnnotationConfigApplicationContext context =
                 new AnnotationConfigApplicationContext(ListenerTestConfiguration.class)) {
            ObservingDeliverTemplateEmailUseCase useCase =
                context.getBean(ObservingDeliverTemplateEmailUseCase.class);
            IllegalStateException failure = new IllegalStateException("provider failed");
            useCase.failure = failure;
            ApplicationEventPublisher publisher = context;

            assertThatThrownBy(() -> publisher.publishEvent(event())).isSameAs(failure);
        }
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

    @Configuration(proxyBeanMethods = false)
    @EnableAsync
    static class ListenerTestConfiguration {

        @Bean
        ObservingDeliverTemplateEmailUseCase deliverTemplateEmailUseCase() {
            return new ObservingDeliverTemplateEmailUseCase();
        }

        @Bean
        TemplateEmailRequestedEventListener templateEmailRequestedEventListener(
            DeliverTemplateEmailUseCase deliverTemplateEmailUseCase
        ) {
            return new TemplateEmailRequestedEventListener(deliverTemplateEmailUseCase);
        }

        @Bean(name = "emailTaskExecutor")
        ThreadPoolTaskExecutor emailTaskExecutor() {
            ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
            executor.setCorePoolSize(1);
            executor.setMaxPoolSize(1);
            executor.setThreadNamePrefix("listener-proxy-test-");
            executor.initialize();
            return executor;
        }
    }

    static class ObservingDeliverTemplateEmailUseCase implements DeliverTemplateEmailUseCase {

        private final CountDownLatch completed = new CountDownLatch(1);
        private Thread invocationThread;
        private boolean transactionActive;
        private RuntimeException failure;

        @Override
        public void deliver(TemplateEmailRequestedEvent event) {
            invocationThread = Thread.currentThread();
            transactionActive = TransactionSynchronizationManager.isActualTransactionActive();
            completed.countDown();
            if (failure != null) {
                throw failure;
            }
        }
    }
}
