package com.umc.product.global.logging;

import static org.assertj.core.api.Assertions.assertThat;

import java.sql.SQLException;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.spi.IThrowableProxy;
import ch.qos.logback.core.read.ListAppender;

class SanitizingAppenderTest {

    private static final String PROBE_EMAIL = "log-probe@example.invalid";
    private static final String PROBE_KEY = "L0G8K2";
    private static final String CONSTRAINT_NAME = "uk_recruiting_application_email_key";

    private LoggerContext context;
    private Logger logger;
    private ListAppender<ILoggingEvent> appender;

    @BeforeEach
    void setUp() {
        context = new LoggerContext();

        appender = new ListAppender<>();
        appender.setContext(context);
        appender.setName("LIST");
        appender.start();

        SanitizingAppender sanitizing = new SanitizingAppender();
        sanitizing.setContext(context);
        sanitizing.setName("SANITIZED");
        sanitizing.addAppender(appender);
        sanitizing.start();

        logger = context.getLogger("security-db-error-test");
        logger.setAdditive(false);
        logger.setLevel(Level.ERROR);
        logger.addAppender(sanitizing);
    }

    @AfterEach
    void tearDown() {
        context.stop();
    }

    @Test
    @DisplayName("DataIntegrityViolation 로그는 PostgreSQL 진단 정보만 남기고 바인딩 값을 치환한다")
    void dataIntegrityViolation_로그_redaction() {
        SQLException sqlException = new SQLException(
            """
                ERROR: duplicate key value violates unique constraint "%s"
                  Detail: Key (applicant_email, application_key)=(%s, %s) already exists.
                """.formatted(CONSTRAINT_NAME, PROBE_EMAIL, PROBE_KEY),
            "23505",
            0
        );
        DataIntegrityViolationException error =
            new DataIntegrityViolationException("could not execute statement", sqlException);

        logger.error("지원서 저장 실패", error);

        ILoggingEvent event = appender.list.getFirst();
        String throwableText = throwableText(event.getThrowableProxy());
        assertThat(throwableText).doesNotContain(PROBE_EMAIL, PROBE_KEY);
    }

    @Test
    @DisplayName("메시지 파라미터의 민감값을 치환한다")
    void 파라미터_redaction() {
        logger.error("Rejected email: {}", PROBE_EMAIL);

        ILoggingEvent event = appender.list.getFirst();
        assertThat(event.getFormattedMessage()).isEqualTo("Rejected email: [REDACTED]");
        assertThat(event.getFormattedMessage()).doesNotContain(PROBE_EMAIL);
    }

    @Test
    @DisplayName("비활성 레벨의 로그는 민감값이 있어도 어펜더에 도달하지 않는다")
    void 비활성_레벨_로그_억제() {
        logger.debug("Rejected email: {}", PROBE_EMAIL);
        logger.trace("Rejected email: {}", PROBE_EMAIL);
        logger.info("Rejected email: {}", PROBE_EMAIL);

        assertThat(appender.list).isEmpty();
    }

    @Test
    @DisplayName("민감값이 없는 로그는 그대로 전달한다")
    void 민감값_없는_로그_보존() {
        logger.error("plain message {}", "nothing-sensitive");

        assertThat(appender.list).hasSize(1);
        assertThat(appender.list.getFirst().getFormattedMessage())
            .isEqualTo("plain message nothing-sensitive");
    }

    @Test
    @DisplayName("이벤트를 새로 만들지 않으므로 로거 이름과 레벨이 보존된다")
    void 이벤트_메타데이터_보존() {
        logger.error("Rejected email: {}", PROBE_EMAIL);

        ILoggingEvent event = appender.list.getFirst();
        assertThat(event.getLoggerName()).isEqualTo("security-db-error-test");
        assertThat(event.getLevel()).isEqualTo(Level.ERROR);
        assertThat(event.getThreadName()).isEqualTo(Thread.currentThread().getName());
    }

    @Test
    @DisplayName("예외 정제 후에도 스택트레이스와 클래스명이 원형을 유지한다")
    void 스택트레이스_보존() {
        IllegalStateException error = new IllegalStateException("email=" + PROBE_EMAIL);

        logger.error("작업 실패", error);

        IThrowableProxy proxy = appender.list.getFirst().getThrowableProxy();
        assertThat(proxy.getClassName()).isEqualTo(IllegalStateException.class.getName());
        assertThat(proxy.getStackTraceElementProxyArray()).isNotEmpty();
        assertThat(proxy.getMessage()).doesNotContain(PROBE_EMAIL).contains("[REDACTED]");
    }

    @Test
    @DisplayName("파라미터의 toString()이 던져도 로깅 호출이 깨지지 않는다")
    void toString_예외_격리() {
        Object hostile = new Object() {
            @Override
            public String toString() {
                throw new IllegalStateException("toString 실패");
            }
        };

        logger.error("value: {}", hostile);

        // logback 이 포매팅 단계에서 흡수하므로 호출자에게 전파되지 않는다.
        assertThat(appender.list).hasSize(1);
    }

    @Test
    @DisplayName("하위 어펜더가 없으면 시작하지 않고 오류를 남긴다")
    void 배선_누락_감지() {
        SanitizingAppender orphan = new SanitizingAppender();
        orphan.setContext(context);
        orphan.setName("ORPHAN");

        orphan.start();

        assertThat(orphan.isStarted()).isFalse();
        assertThat(context.getStatusManager().getCopyOfStatusList())
            .anyMatch(status -> status.getMessage().contains("ORPHAN"));
    }

    private String throwableText(IThrowableProxy throwable) {
        if (throwable == null) {
            return "";
        }
        StringBuilder text = new StringBuilder();
        for (IThrowableProxy current = throwable; current != null; current = current.getCause()) {
            text.append(current.getClassName()).append(' ').append(current.getMessage()).append('\n');
        }
        return text.toString();
    }
}
