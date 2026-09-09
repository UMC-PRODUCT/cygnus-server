package com.umc.product.global.logging;

import static net.logstash.logback.argument.StructuredArguments.kv;
import static org.assertj.core.api.Assertions.assertThat;

import java.sql.SQLException;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.Marker;
import org.slf4j.event.KeyValuePair;
import org.springframework.dao.DataIntegrityViolationException;

import net.logstash.logback.marker.SingleFieldAppendingMarker;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.spi.IThrowableProxy;
import ch.qos.logback.classic.spi.LoggerContextVO;
import ch.qos.logback.classic.spi.LoggingEvent;
import ch.qos.logback.classic.spi.ThrowableProxyUtil;
import ch.qos.logback.classic.util.LogbackMDCAdapter;
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
        // 어댑터가 없으면 LoggingEvent.getMDCPropertyMap() 이 NPE 를 낸다. 운영과 같은 경로를 태운다.
        context.setMDCAdapter(new LogbackMDCAdapter());

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
    @DisplayName("민감값이 없는 로그는 그대로 전달한다")
    void 민감값_없는_로그_보존() {
        logger.error("plain message {}", "nothing-sensitive");

        assertThat(appender.list).hasSize(1);
        assertThat(appender.list.getFirst().getFormattedMessage())
            .isEqualTo("plain message nothing-sensitive");
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
    @DisplayName("MDC 값의 민감정보를 치환한다")
    void mdc_redaction() {
        LoggingEvent event = event("요청 처리");
        event.setMDCPropertyMap(Map.of("path", "/members/" + PROBE_EMAIL, "statusCode", "200"));

        ILoggingEvent sanitized = SanitizedLoggingEvent.wrap(event);

        assertThat(sanitized.getMDCPropertyMap().get("path")).doesNotContain(PROBE_EMAIL).contains("[REDACTED]");
        assertThat(sanitized.getMDCPropertyMap().get("statusCode")).isEqualTo("200");
    }

    @Test
    @DisplayName("구조화 인자는 평문으로 평탄화하지 않고 필드를 유지한 채 치환한다")
    void 구조화_인자_필드_보존() {
        LoggingEvent event = new LoggingEvent(
            SanitizingAppenderTest.class.getName(),
            logger,
            Level.ERROR,
            "external_api_called",
            null,
            new Object[] {kv("provider", "KAKAO"), kv("email", PROBE_EMAIL)}
        );

        Object[] arguments = SanitizedLoggingEvent.wrap(event).getArgumentArray();

        assertThat(arguments[0]).isInstanceOf(SingleFieldAppendingMarker.class);
        assertThat(arguments[0].toString()).contains("KAKAO");

        assertThat(arguments[1]).isInstanceOf(SingleFieldAppendingMarker.class);
        assertThat(((SingleFieldAppendingMarker) arguments[1]).getFieldName()).isEqualTo("email");
        assertThat(arguments[1].toString()).doesNotContain(PROBE_EMAIL).contains("[REDACTED]");
    }

    @Test
    @DisplayName("KeyValuePair의 문자열 값만 치환하고 나머지 타입은 건드리지 않는다")
    void keyValuePair_redaction() {
        LoggingEvent event = event("요청 처리");
        event.setKeyValuePairs(List.of(
            new KeyValuePair("email", PROBE_EMAIL),
            new KeyValuePair("durationMs", 120L)
        ));

        List<KeyValuePair> pairs = SanitizedLoggingEvent.wrap(event).getKeyValuePairs();

        assertThat(pairs.get(0).value.toString()).doesNotContain(PROBE_EMAIL).contains("[REDACTED]");
        assertThat(pairs.get(1).value).isEqualTo(120L);
    }

    @Test
    @DisplayName("순환 원인 체인도 정제하고 logback 의 순환 표기를 유지한다")
    void 순환_원인_체인_정제() {
        RuntimeException outer = new RuntimeException("결제 실패: " + PROBE_EMAIL);
        RuntimeException inner = new RuntimeException("커넥션 실패");
        outer.initCause(inner);
        inner.initCause(outer);

        logger.error("작업 실패", outer);

        String rendered = ThrowableProxyUtil.asString(appender.list.getFirst().getThrowableProxy());
        assertThat(rendered).doesNotContain(PROBE_EMAIL);
        assertThat(rendered).contains("[CIRCULAR REFERENCE:").contains("[REDACTED]");
    }

    @Test
    @DisplayName("suppressed 예외의 민감값도 정제한다")
    void suppressed_예외_정제() {
        RuntimeException error = new RuntimeException("작업 실패");
        error.addSuppressed(new IllegalStateException("close 실패: " + PROBE_EMAIL));

        logger.error("작업 실패", error);

        IThrowableProxy proxy = appender.list.getFirst().getThrowableProxy();
        assertThat(proxy.getSuppressed()).hasSize(1);
        assertThat(ThrowableProxyUtil.asString(proxy)).doesNotContain(PROBE_EMAIL).contains("[REDACTED]");
    }

    @Test
    @DisplayName("접근자 하나가 실패해도 로그를 잃지 않고 해당 필드만 비운다")
    void 접근자_실패_시_이벤트_보존() {
        ILoggingEvent hostile = new HostileMdcEvent(event("정상 메시지"));

        ILoggingEvent sanitized = SanitizedLoggingEvent.wrap(hostile);

        // AppenderBase.doAppend() 가 예외를 삼키므로, 여기서 던지면 로그 한 줄이 조용히 사라진다.
        assertThat(sanitized.getFormattedMessage()).isEqualTo("정상 메시지");
        assertThat(sanitized.getMDCPropertyMap()).isEmpty();
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

    /** MDC 접근이 실패하는 이벤트. logback 이 MDC 어댑터를 갖추지 못한 상황을 재현한다. */
    private record HostileMdcEvent(ILoggingEvent delegate) implements ILoggingEvent {

        @Override
        public Map<String, String> getMDCPropertyMap() {
            throw new IllegalStateException("MDC 접근 실패");
        }

        @Override
        @SuppressWarnings("deprecation")
        public Map<String, String> getMdc() {
            return getMDCPropertyMap();
        }

        @Override
        public String getThreadName() {
            return delegate.getThreadName();
        }

        @Override
        public Level getLevel() {
            return delegate.getLevel();
        }

        @Override
        public String getMessage() {
            return delegate.getMessage();
        }

        @Override
        public Object[] getArgumentArray() {
            return delegate.getArgumentArray();
        }

        @Override
        public String getFormattedMessage() {
            return delegate.getFormattedMessage();
        }

        @Override
        public String getLoggerName() {
            return delegate.getLoggerName();
        }

        @Override
        public LoggerContextVO getLoggerContextVO() {
            return delegate.getLoggerContextVO();
        }

        @Override
        public IThrowableProxy getThrowableProxy() {
            return delegate.getThrowableProxy();
        }

        @Override
        public StackTraceElement[] getCallerData() {
            return delegate.getCallerData();
        }

        @Override
        public boolean hasCallerData() {
            return delegate.hasCallerData();
        }

        @Override
        public List<Marker> getMarkerList() {
            return delegate.getMarkerList();
        }

        @Override
        public long getTimeStamp() {
            return delegate.getTimeStamp();
        }

        @Override
        public int getNanoseconds() {
            return delegate.getNanoseconds();
        }

        @Override
        public long getSequenceNumber() {
            return delegate.getSequenceNumber();
        }

        @Override
        public List<KeyValuePair> getKeyValuePairs() {
            return delegate.getKeyValuePairs();
        }

        @Override
        public void prepareForDeferredProcessing() {
            delegate.prepareForDeferredProcessing();
        }
    }

    private LoggingEvent event(String message) {
        return new LoggingEvent(
            SanitizingAppenderTest.class.getName(), logger, Level.ERROR, message, null, null
        );
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
