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

    @Test
    @DisplayName("비활성 레벨의 로그는 민감값이 있어도 출력하지 않는다")
    void 비활성_레벨_로그_억제() {
        logger.debug("Rejected email: {}", PROBE_EMAIL);
        logger.trace("Rejected email: {}", PROBE_EMAIL);
        logger.info("Rejected email: {}", PROBE_EMAIL);

        assertThat(appender.list).isEmpty();
    }

    @Test
    @DisplayName("레벨을 상속받는 로거에서도 비활성 레벨만 억제한다")
    void 상속_레벨_로그_억제() {
        // 운영 로거는 대부분 레벨 미지정 상태로 root에서 상속받는다.
        Logger root = context.getLogger(Logger.ROOT_LOGGER_NAME);
        root.setLevel(Level.ERROR);
        root.addAppender(appender);
        Logger inherited = context.getLogger("inherited-level-test");

        inherited.debug("Rejected email: {}", PROBE_EMAIL);
        assertThat(appender.list).isEmpty();

        inherited.error("Rejected email: {}", PROBE_EMAIL);
        assertThat(appender.list).hasSize(1);
    }

    @Test
    @DisplayName("민감값이 없는 로그는 그대로 출력한다")
    void 민감값_없는_로그_보존() {
        logger.error("plain message {}", "nothing-sensitive");

        assertThat(appender.list).hasSize(1);
        assertThat(appender.list.getFirst().getFormattedMessage())
            .isEqualTo("plain message nothing-sensitive");
    }

    private String throwableText(IThrowableProxy throwable) {
        if (throwable == null) {
            return "";
        }
        return throwable.getClassName() + ": " + throwable.getMessage() + "\n" + throwableText(throwable.getCause());
    }
}
