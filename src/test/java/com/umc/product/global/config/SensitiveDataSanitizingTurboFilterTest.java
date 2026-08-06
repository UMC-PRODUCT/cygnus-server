package com.umc.product.global.config;

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

class SensitiveDataSanitizingTurboFilterTest {

    private static final String PROBE_EMAIL = "log-probe@example.invalid";
    private static final String PROBE_KEY = "L0G8K2";
    private static final String CONSTRAINT_NAME = "uk_recruiting_application_email_key";

    private LoggerContext context;
    private Logger logger;
    private ListAppender<ILoggingEvent> appender;

    @BeforeEach
    void setUp() {
        context = new LoggerContext();
        SensitiveDataSanitizingTurboFilter filter = new SensitiveDataSanitizingTurboFilter();
        filter.setContext(context);
        filter.start();
        context.addTurboFilter(filter);

        logger = context.getLogger("security-db-error-test");
        logger.setAdditive(false);
        logger.setLevel(Level.ERROR);

        appender = new ListAppender<>();
        appender.setContext(context);
        appender.start();
        logger.addAppender(appender);
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
        DataIntegrityViolationException exception = new DataIntegrityViolationException(
            "지원서 저장 중 duplicate constraint",
            sqlException
        );

        logger.error("DB persistence failed: {}", exception.getMessage(), exception);

        assertThat(appender.list).hasSize(1);
        ILoggingEvent event = appender.list.getFirst();
        String output = event.getFormattedMessage() + "\n" + throwableText(event.getThrowableProxy());
        assertThat(output)
            .contains(
                DataIntegrityViolationException.class.getName(),
                SQLException.class.getName(),
                "sqlState=23505",
                "vendorCode=0",
                "constraint=" + CONSTRAINT_NAME,
                "[REDACTED]"
            )
            .doesNotContain(PROBE_EMAIL, PROBE_KEY, "=(" + PROBE_EMAIL);
        assertThat(exception.getMostSpecificCause().getMessage()).contains(PROBE_EMAIL, PROBE_KEY);
    }

    @Test
    @DisplayName("민감값을 정제한 뒤에도 로그 format과 parameter 배열을 보존한다")
    void 정제된_로그_parameter_보존() {
        logger.error("Rejected email: {}", PROBE_EMAIL);

        assertThat(appender.list).hasSize(1);
        ILoggingEvent event = appender.list.getFirst();
        assertThat(event.getMessage()).isEqualTo("Rejected email: {}");
        assertThat(event.getArgumentArray()).containsExactly("[REDACTED]");
        assertThat(event.getFormattedMessage()).isEqualTo("Rejected email: [REDACTED]");
    }

    private String throwableText(IThrowableProxy throwable) {
        if (throwable == null) {
            return "";
        }
        return throwable.getClassName() + ": " + throwable.getMessage() + "\n" + throwableText(throwable.getCause());
    }
}
