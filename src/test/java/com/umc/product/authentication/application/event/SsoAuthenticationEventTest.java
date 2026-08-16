package com.umc.product.authentication.application.event;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionException;
import org.springframework.transaction.event.TransactionalEventListenerFactory;
import org.springframework.transaction.support.AbstractPlatformTransactionManager;
import org.springframework.transaction.support.DefaultTransactionStatus;
import org.springframework.transaction.support.TransactionTemplate;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.umc.product.authentication.adapter.in.event.SsoSecurityEventListener;
import com.umc.product.global.logging.OperationalMetrics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;

@DisplayName("SSO 인증 이벤트")
class SsoAuthenticationEventTest {

    private static final Long MEMBER_ID = 10L;
    private static final String CLIENT_ID = "backoffice";
    private static final String REDIRECT_URI = "https://backoffice.university.neordinary.com/auth/callback";

    @Test
    @DisplayName("SSO lifecycle 이벤트는 명시적인 eventType을 제공한다")
    void event_type_검증() {
        assertThat(SsoBrowserLoginCreatedEvent.of(MEMBER_ID, "EMAIL").eventType())
            .isEqualTo("authentication.sso.browser-login.created");
        assertThat(SsoAuthorizationCodeIssuedEvent.of(
            MEMBER_ID,
            CLIENT_ID,
            REDIRECT_URI,
            Instant.now().plusSeconds(300)
        ).eventType()).isEqualTo("authentication.sso.authorization-code.issued");
        assertThat(SsoAuthorizationCodeExchangedEvent.of(MEMBER_ID, CLIENT_ID, REDIRECT_URI).eventType())
            .isEqualTo("authentication.sso.authorization-code.exchanged");
        assertThat(SsoTokenIssuedEvent.of(MEMBER_ID, CLIENT_ID, "authorization_code").eventType())
            .isEqualTo("authentication.sso.token.issued");
    }

    @Test
    @DisplayName("SSO 교환/토큰 이벤트 payload에는 민감 값이 포함되지 않는다")
    void event_payload_민감값_제외() throws Exception {
        ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

        String exchangedPayload = objectMapper.writeValueAsString(
            SsoAuthorizationCodeExchangedEvent.of(MEMBER_ID, CLIENT_ID, REDIRECT_URI)
        );
        String issuedPayload = objectMapper.writeValueAsString(
            SsoTokenIssuedEvent.of(MEMBER_ID, CLIENT_ID, "authorization_code")
        );

        assertThat(exchangedPayload)
            .contains(CLIENT_ID, REDIRECT_URI)
            .doesNotContain("raw-code", "code-hash", "access-token", "refresh-token", "password");
        assertThat(issuedPayload)
            .contains(CLIENT_ID, "authorization_code")
            .doesNotContain("raw-code", "code-hash", REDIRECT_URI, "access-token", "refresh-token", "password");
    }

    @Test
    @DisplayName("SSO security event listener는 lifecycle 이벤트를 보안 메트릭으로 기록한다")
    void security_event_listener_metric_기록() {
        SimpleMeterRegistry registry = new SimpleMeterRegistry();
        SsoSecurityEventListener listener = new SsoSecurityEventListener(new OperationalMetrics(registry));

        listener.handle(SsoBrowserLoginCreatedEvent.of(MEMBER_ID, "EMAIL"));
        listener.handle(SsoAuthorizationCodeIssuedEvent.of(
            MEMBER_ID,
            CLIENT_ID,
            REDIRECT_URI,
            Instant.now().plusSeconds(300)
        ));
        listener.handle(SsoAuthorizationCodeExchangedEvent.of(MEMBER_ID, CLIENT_ID, REDIRECT_URI));
        listener.handle(SsoTokenIssuedEvent.of(MEMBER_ID, CLIENT_ID, "authorization_code"));

        assertSecurityMetric(registry, "SSO_BROWSER_LOGIN", "success");
        assertSecurityMetric(registry, "SSO_AUTHORIZATION_CODE", "issued");
        assertSecurityMetric(registry, "SSO_AUTHORIZATION_CODE", "exchanged");
        assertSecurityMetric(registry, "SSO_TOKEN", "issued");
    }

    @Test
    @DisplayName("SSO security event listener는 트랜잭션 커밋 후에만 보안 메트릭을 기록한다")
    void security_event_listener_after_commit_기록() {
        try (AnnotationConfigApplicationContext context =
                 new AnnotationConfigApplicationContext(EventListenerTestConfig.class)) {
            ApplicationEventPublisher publisher = context;
            TransactionTemplate tx = context.getBean(TransactionTemplate.class);
            SimpleMeterRegistry registry = context.getBean(SimpleMeterRegistry.class);

            tx.executeWithoutResult(status ->
                publisher.publishEvent(SsoTokenIssuedEvent.of(MEMBER_ID, CLIENT_ID, "authorization_code"))
            );

            assertSecurityMetric(registry, "SSO_TOKEN", "issued");

            assertThatThrownBy(() -> tx.executeWithoutResult(status -> {
                publisher.publishEvent(SsoBrowserLoginCreatedEvent.of(MEMBER_ID, "email"));
                throw new IllegalStateException("rollback");
            })).isInstanceOf(IllegalStateException.class);

            assertNoSecurityMetric(registry, "SSO_BROWSER_LOGIN", "success");
        }
    }

    private void assertSecurityMetric(SimpleMeterRegistry registry, String operation, String result) {
        assertThat(registry.get("operational.security.event.total")
            .tag("domain", "AUTHENTICATION")
            .tag("operation", operation)
            .tag("result", result)
            .counter()
            .count()).isEqualTo(1);
    }

    private void assertNoSecurityMetric(SimpleMeterRegistry registry, String operation, String result) {
        Counter counter = registry.find("operational.security.event.total")
            .tag("domain", "AUTHENTICATION")
            .tag("operation", operation)
            .tag("result", result)
            .counter();
        assertThat(counter).isNull();
    }

    @Configuration
    static class EventListenerTestConfig {

        @Bean
        SimpleMeterRegistry meterRegistry() {
            return new SimpleMeterRegistry();
        }

        @Bean
        OperationalMetrics operationalMetrics(SimpleMeterRegistry registry) {
            return new OperationalMetrics(registry);
        }

        @Bean
        SsoSecurityEventListener ssoSecurityEventListener(OperationalMetrics operationalMetrics) {
            return new SsoSecurityEventListener(operationalMetrics);
        }

        @Bean
        PlatformTransactionManager transactionManager() {
            return new LocalTransactionManager();
        }

        @Bean
        TransactionTemplate transactionTemplate(PlatformTransactionManager transactionManager) {
            return new TransactionTemplate(transactionManager);
        }

        @Bean
        TransactionalEventListenerFactory transactionalEventListenerFactory() {
            return new TransactionalEventListenerFactory();
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
