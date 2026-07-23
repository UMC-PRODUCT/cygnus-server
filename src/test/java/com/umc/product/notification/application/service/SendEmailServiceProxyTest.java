package com.umc.product.notification.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionException;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.transaction.support.AbstractPlatformTransactionManager;
import org.springframework.transaction.support.DefaultTransactionStatus;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import com.umc.product.global.event.application.port.out.DomainEventPublisher;
import com.umc.product.global.event.application.port.out.dto.OutboxPublishResult;
import com.umc.product.global.event.domain.DomainEvent;
import com.umc.product.global.event.domain.EventOutboxStatus;
import com.umc.product.notification.application.port.in.SendEmailUseCase;
import com.umc.product.notification.application.port.in.dto.SendTemplateEmailCommand;
import com.umc.product.notification.application.port.in.dto.SendVerificationEmailCommand;
import com.umc.product.notification.application.port.in.dto.TemplateEmailRequestInfo;
import com.umc.product.notification.application.port.out.SendEmailPort;
import com.umc.product.notification.application.port.out.dto.EmailMessage;
import com.umc.product.notification.domain.EmailTemplateType;

@DisplayName("SendEmailService Spring proxy")
class SendEmailServiceProxyTest {

    private static final Instant AVAILABLE_AT = Instant.parse("2026-07-18T03:00:00.123456789Z");

    @Test
    @DisplayName("template email 요청은 호출 thread의 read-write transaction 안에서 publish한다")
    void templateEmailRequestPublishesInsideReadWriteTransaction() {
        try (AnnotationConfigApplicationContext context = context()) {
            SendEmailUseCase service = context.getBean(SendEmailUseCase.class);
            ObservingDomainEventPublisher publisher = context.getBean(ObservingDomainEventPublisher.class);
            Thread callerThread = Thread.currentThread();
            SendTemplateEmailCommand command = new SendTemplateEmailCommand(
                UUID.fromString("70000000-0000-0000-0000-000000000011"),
                "applicant@test.umc.local",
                EmailTemplateType.RECRUITMENT_FINAL_FAILED,
                Map.of("applicantName", "홍길동"),
                AVAILABLE_AT
            );

            TemplateEmailRequestInfo result = service.requestTemplateEmail(command);

            assertThat(publisher.invocationThread).isSameAs(callerThread);
            assertThat(publisher.transactionActive).isTrue();
            assertThat(publisher.transactionReadOnly).isFalse();
            assertThat(result.status()).isEqualTo(EventOutboxStatus.PENDING);
        }
    }

    @Test
    @DisplayName("verification email은 지정 executor에서 호출 반환 후 발송한다")
    void verificationEmailRunsOnNamedExecutorAfterInvocationReturns() throws Exception {
        try (AnnotationConfigApplicationContext context = context()) {
            TemplateEngine templateEngine = context.getBean(TemplateEngine.class);
            given(templateEngine.process(eq("email/verification"), any(Context.class)))
                .willReturn("<html>인증</html>");
            SendEmailUseCase service = context.getBean(SendEmailUseCase.class);
            BlockingSendEmailPort sendEmailPort = context.getBean(BlockingSendEmailPort.class);
            SendVerificationEmailCommand command = SendVerificationEmailCommand.builder()
                .to("member@test.umc.local")
                .verificationCode("123456")
                .build();
            AtomicReference<Thread> callerThread = new AtomicReference<>();

            try (ExecutorService caller = Executors.newSingleThreadExecutor()) {
                Future<?> invocation = caller.submit(() -> {
                    callerThread.set(Thread.currentThread());
                    service.sendVerificationEmail(command);
                });
                try {
                    assertThat(sendEmailPort.entered.await(3, TimeUnit.SECONDS)).isTrue();
                    assertThat(invocation.isDone()).isTrue();
                    assertThat(sendEmailPort.completed.getCount()).isOne();
                    assertThat(sendEmailPort.invocationThread).isNotSameAs(callerThread.get());
                    assertThat(sendEmailPort.invocationThread.getName()).startsWith("email-proxy-test-");
                } finally {
                    sendEmailPort.release.release();
                }
                assertThat(sendEmailPort.completed.await(3, TimeUnit.SECONDS)).isTrue();
                invocation.get(3, TimeUnit.SECONDS);
            }

            assertThat(sendEmailPort.message).isEqualTo(new EmailMessage(
                "noreply@test.umc.local",
                "UMC 테스트",
                "member@test.umc.local",
                "이메일 인증 코드: 123456",
                "<html>인증</html>"
            ));
        }
    }

    private AnnotationConfigApplicationContext context() {
        return new AnnotationConfigApplicationContext(ProxyTestConfiguration.class);
    }

    @Configuration(proxyBeanMethods = false)
    @EnableAsync
    @EnableTransactionManagement
    static class ProxyTestConfiguration {

        @Bean
        TemplateEngine templateEngine() {
            return mock(TemplateEngine.class);
        }

        @Bean
        BlockingSendEmailPort sendEmailPort() {
            return new BlockingSendEmailPort();
        }

        @Bean
        ObservingDomainEventPublisher domainEventPublisher() {
            return new ObservingDomainEventPublisher();
        }

        @Bean
        SendEmailService sendEmailService(
            TemplateEngine templateEngine,
            SendEmailPort sendEmailPort,
            DomainEventPublisher domainEventPublisher
        ) {
            return new SendEmailService(
                templateEngine,
                sendEmailPort,
                new EmailSenderProperties("noreply@test.umc.local", "UMC 테스트"),
                new EmailTemplateCatalog(List.of("https://university.neordinary.com")),
                domainEventPublisher
            );
        }

        @Bean
        PlatformTransactionManager transactionManager() {
            return new LocalTransactionManager();
        }

        @Bean(name = "taskExecutor")
        ThreadPoolTaskExecutor defaultTaskExecutor() {
            return executor("default-proxy-test-");
        }

        @Bean(name = "emailTaskExecutor")
        ThreadPoolTaskExecutor emailTaskExecutor() {
            return executor("email-proxy-test-");
        }

        private ThreadPoolTaskExecutor executor(String threadNamePrefix) {
            ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
            executor.setCorePoolSize(1);
            executor.setMaxPoolSize(1);
            executor.setThreadNamePrefix(threadNamePrefix);
            executor.initialize();
            return executor;
        }
    }

    static class BlockingSendEmailPort implements SendEmailPort {

        private final CountDownLatch entered = new CountDownLatch(1);
        private final CountDownLatch completed = new CountDownLatch(1);
        private final Semaphore release = new Semaphore(0);
        private Thread invocationThread;
        private EmailMessage message;

        @Override
        public void send(EmailMessage message) {
            this.invocationThread = Thread.currentThread();
            this.message = message;
            entered.countDown();
            release.acquireUninterruptibly();
            completed.countDown();
        }
    }

    static class ObservingDomainEventPublisher implements DomainEventPublisher {

        private Thread invocationThread;
        private boolean transactionActive;
        private boolean transactionReadOnly;

        @Override
        public OutboxPublishResult publishOnce(DomainEvent event, Instant availableAt) {
            invocationThread = Thread.currentThread();
            transactionActive = TransactionSynchronizationManager.isActualTransactionActive();
            transactionReadOnly = TransactionSynchronizationManager.isCurrentTransactionReadOnly();
            return new OutboxPublishResult(
                event.eventId(),
                EventOutboxStatus.PENDING,
                false,
                availableAt.truncatedTo(ChronoUnit.MICROS),
                availableAt.truncatedTo(ChronoUnit.MICROS)
            );
        }

        @Override
        public void publish(DomainEvent event) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void publishAll(Collection<? extends DomainEvent> events) {
            throw new UnsupportedOperationException();
        }
    }

    private static class LocalTransactionManager extends AbstractPlatformTransactionManager {

        @Override
        protected Object doGetTransaction() throws TransactionException {
            return new Object();
        }

        @Override
        protected void doBegin(Object transaction, TransactionDefinition definition) throws TransactionException {
        }

        @Override
        protected void doCommit(DefaultTransactionStatus status) throws TransactionException {
        }

        @Override
        protected void doRollback(DefaultTransactionStatus status) throws TransactionException {
        }

        @Override
        protected boolean isExistingTransaction(Object transaction) throws TransactionException {
            return false;
        }
    }
}
