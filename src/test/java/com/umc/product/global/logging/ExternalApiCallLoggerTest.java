package com.umc.product.global.logging;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;

import net.logstash.logback.argument.StructuredArgument;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;

/**
 * ExternalApiCallLogger 가 external_api_called 이벤트를 통일된 스키마로 남기는지 검증한다.
 *
 * <p>핵심 invariant:
 * <ul>
 *     <li>성공 시: INFO, result=SUCCESS, durationMs 포함</li>
 *     <li>실패 시: WARN, result=FAILURE, errorClass 포함, 예외 그대로 재던짐</li>
 *     <li>예외 메시지는 어떤 필드에도 노출되지 않는다 (민감정보 정책)</li>
 * </ul>
 */
class ExternalApiCallLoggerTest {

    private Logger externalApiLogger;
    private ListAppender<ILoggingEvent> listAppender;

    @BeforeEach
    void setUp() {
        ExternalApiCallLogger.setOperationalMetrics(null);
        externalApiLogger = (Logger) LoggerFactory.getLogger("external_api");
        listAppender = new ListAppender<>();
        listAppender.start();
        externalApiLogger.addAppender(listAppender);
        externalApiLogger.setLevel(Level.INFO);
    }

    @AfterEach
    void tearDown() {
        externalApiLogger.detachAppender(listAppender);
        ExternalApiCallLogger.setOperationalMetrics(null);
    }

    @Test
    @DisplayName("성공 호출은 INFO + result=SUCCESS + durationMs 로 기록되고 반환값을 그대로 돌려준다")
    void measure_성공_INFO_로깅() {
        // when
        String result = ExternalApiCallLogger.measure(
            "GITHUB",
            "FETCH_PULL_REQUESTS",
            () -> "pr-list"
        );

        // then
        assertThat(result).isEqualTo("pr-list");

        ILoggingEvent event = onlyEvent();
        assertThat(event.getLevel()).isEqualTo(Level.INFO);
        assertThat(event.getMessage()).isEqualTo("external_api_called");
        assertThat(kvOf(event, "provider")).isEqualTo("GITHUB");
        assertThat(kvOf(event, "operation")).isEqualTo("FETCH_PULL_REQUESTS");
        assertThat(kvOf(event, "result")).isEqualTo("SUCCESS");
        assertThat(event.getArgumentArray()).anyMatch(arg -> arg.toString().contains("durationMs"));
    }

    @Test
    @DisplayName("RuntimeException 발생 시 WARN + result=FAILURE + errorClass 가 기록되고 예외는 재던져진다")
    void measure_실패_WARN_로깅_및_예외_재던지기() {
        // given
        IllegalStateException boom = new IllegalStateException("Bearer ya29.SECRET should not be logged");

        // when / then
        assertThatThrownBy(() ->
            ExternalApiCallLogger.measure("OPENAI", "CHAT_COMPLETION", () -> {
                throw boom;
            })
        ).isSameAs(boom);

        ILoggingEvent event = onlyEvent();
        assertThat(event.getLevel()).isEqualTo(Level.WARN);
        assertThat(event.getMessage()).isEqualTo("external_api_called");
        assertThat(kvOf(event, "provider")).isEqualTo("OPENAI");
        assertThat(kvOf(event, "operation")).isEqualTo("CHAT_COMPLETION");
        assertThat(kvOf(event, "result")).isEqualTo("FAILURE");
        assertThat(kvOf(event, "errorClass")).isEqualTo("IllegalStateException");

        // 민감정보 정책: 예외 메시지가 어떤 필드에도 포함되지 않는다
        for (Object arg : event.getArgumentArray()) {
            assertThat(arg.toString()).doesNotContain("ya29.SECRET");
        }
    }

    private ILoggingEvent onlyEvent() {
        List<ILoggingEvent> events = listAppender.list;
        assertThat(events).hasSize(1);
        return events.get(0);
    }

    /**
     * StructuredArgument 의 key={@code key} 인 값을 string 으로 꺼낸다.
     * logstash-logback-encoder 의 {@code kv(...)} 는 toString 이 {@code key=value} 형식이다.
     */
    private String kvOf(ILoggingEvent event, String key) {
        for (Object arg : event.getArgumentArray()) {
            if (arg instanceof StructuredArgument structured) {
                String s = structured.toString();
                String prefix = key + "=";
                if (s.startsWith(prefix)) {
                    return s.substring(prefix.length());
                }
            }
        }
        return null;
    }
}
