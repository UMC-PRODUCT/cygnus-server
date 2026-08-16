package com.umc.product.global.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.servlet.HandlerMapping;

import com.umc.product.common.domain.enums.ClientType;
import com.umc.product.global.client.ClientContextProperties;
import com.umc.product.global.client.ClientOriginRegistry;
import com.umc.product.global.client.ClientRequestClassifier;
import com.umc.product.global.logging.OperationalMetrics;
import com.umc.product.global.security.MemberPrincipal;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;

/**
 * ADR-016 의 LoggingInterceptor MDC 라이프사이클을 검증한다.
 *
 * <p>Spring Context 를 띄우지 않는 순수 단위 테스트로, preHandle 시점의 MDC put
 * 과 afterCompletion 시점의 finally MDC.clear 가 보장되는지 확인한다.
 *
 * <p>JSON 어펜더가 실제로 직렬화하는 MDC 스냅샷은 ILoggingEvent.getMDCPropertyMap() 로
 * 캡처해서 검증한다 (실제 운영에서는 logstash-logback-encoder 가 같은 map 을 사용).
 */
class LoggingInterceptorTest {

    private final LoggingInterceptor interceptor = new LoggingInterceptor(
        new ClientRequestClassifier(new ClientOriginRegistry(new ClientContextProperties(List.of()))),
        new OperationalMetrics(new SimpleMeterRegistry())
    );

    private Logger interceptorLogger;
    private ListAppender<ILoggingEvent> listAppender;

    @BeforeEach
    void setUp() {
        interceptorLogger = (Logger) LoggerFactory.getLogger(LoggingInterceptor.class);
        listAppender = new ListAppender<>();
        listAppender.start();
        interceptorLogger.addAppender(listAppender);
        interceptorLogger.setLevel(Level.INFO);
    }

    @AfterEach
    void tearDown() {
        interceptorLogger.detachAppender(listAppender);
        // 테스트 간 MDC / SecurityContext 누수 방지
        MDC.clear();
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("preHandle 시 method / path 가 MDC 에 들어가고 traceId 가 있으면 X-Trace-Id 헤더가 채워진다")
    void preHandle_MDC_등록_성공() {
        // given — Micrometer Tracing 이 채웠을 traceId 를 시뮬레이션
        MDC.put("traceId", "test-trace-abc123");
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/forms/123/answers");
        MockHttpServletResponse response = new MockHttpServletResponse();

        // when
        boolean result = interceptor.preHandle(request, response, new Object());

        // then
        assertThat(result).isTrue();
        assertThat(MDC.get("method")).isEqualTo("GET");
        assertThat(MDC.get("path")).isEqualTo("/forms/123/answers");
        assertThat(response.getHeader("X-Trace-Id")).isEqualTo("test-trace-abc123");
        // 별도 requestId UUID 는 발급하지 않는다 (FE 호환성)
        assertThat(MDC.get("requestId")).isNull();
        assertThat(response.getHeader("X-Request-Id")).isNull();
    }

    @Test
    @DisplayName("traceId 가 없으면 X-Trace-Id 헤더도 비어 있어야 한다")
    void preHandle_traceId_없을_때_헤더_미설정() {
        // given — traceId 미주입 상태 (테스트 환경에는 Micrometer Tracing 가 없을 수 있다)
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/forms");
        MockHttpServletResponse response = new MockHttpServletResponse();

        // when
        interceptor.preHandle(request, response, new Object());

        // then
        assertThat(response.getHeader("X-Trace-Id")).isNull();
    }

    @Test
    @DisplayName("afterCompletion 의 finally 에서 MDC 가 반드시 비워진다")
    void afterCompletion_MDC_누수_방지() {
        // given
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/forms/123");
        MockHttpServletResponse response = new MockHttpServletResponse();
        interceptor.preHandle(request, response, new Object());

        // pre-condition: MDC 에 method 등 값이 들어있다
        assertThat(MDC.get("method")).isEqualTo("GET");

        // when
        interceptor.afterCompletion(request, response, new Object(), null);

        // then
        assertThat(MDC.getCopyOfContextMap()).isNullOrEmpty();
    }

    @Test
    @DisplayName("api_request_completed 로그의 MDC 스냅샷에 uriTemplate / statusCode / durationMs 가 포함된다")
    void afterCompletion_응답_필드_MDC_등록() {
        // given
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/forms/123/answers");
        request.setAttribute(
            HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE,
            "/forms/{formId}/answers"
        );
        MockHttpServletResponse response = new MockHttpServletResponse();
        response.setStatus(200);
        interceptor.preHandle(request, response, new Object());

        // when
        interceptor.afterCompletion(request, response, new Object(), null);

        // then — 로그 이벤트의 MDC 스냅샷을 캡처해서 확인
        Map<String, String> snapshot = findEventMdc("api_request_completed");
        assertThat(snapshot).isNotNull();
        assertThat(snapshot).containsEntry("event", "api_request_completed");
        assertThat(snapshot).containsEntry("uriTemplate", "/forms/{formId}/answers");
        assertThat(snapshot).containsEntry("statusCode", "200");
        assertThat(snapshot).containsKey("durationMs");
        assertThat(MDC.getCopyOfContextMap()).isNullOrEmpty();
    }

    @Test
    @DisplayName("preHandle 이 호출되지 않은 상태에서 afterCompletion 이 호출되어도 안전하게 종료된다")
    void afterCompletion_startTime_없을_때_안전_종료() {
        // given
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/forms/123");
        MockHttpServletResponse response = new MockHttpServletResponse();

        // when / then — 예외 없이 통과
        interceptor.afterCompletion(request, response, new Object(), null);
        assertThat(MDC.getCopyOfContextMap()).isNullOrEmpty();
    }

    @Test
    @DisplayName("인증된 MemberPrincipal 의 clientType 이 없으면 MDC clientType 을 UNKNOWN 으로 채운다")
    void preHandle_인증된_사용자_clientType_UNKNOWN_등록() {
        // given
        MemberPrincipal principal = new MemberPrincipal(42L);
        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
            principal, null, Collections.emptyList()
        );
        SecurityContextHolder.getContext().setAuthentication(auth);

        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/forms/123");
        MockHttpServletResponse response = new MockHttpServletResponse();

        // when
        interceptor.preHandle(request, response, new Object());

        // then
        assertThat(MDC.get("memberId")).isEqualTo("42");
        assertThat(MDC.get("clientType")).isEqualTo("UNKNOWN");
        // 서비스 도메인 명명을 사용하므로 userId 는 사용하지 않는다
        assertThat(MDC.get("userId")).isNull();
    }

    @Test
    @DisplayName("인증된 MemberPrincipal 의 clientType 이 있으면 해당 값을 MDC 에 채운다")
    void preHandle_인증된_사용자_clientType_MDC_등록() {
        // given
        MemberPrincipal principal = new MemberPrincipal(42L, ClientType.IOS);
        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
            principal, null, Collections.emptyList()
        );
        SecurityContextHolder.getContext().setAuthentication(auth);

        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/forms/123");
        MockHttpServletResponse response = new MockHttpServletResponse();

        // when
        interceptor.preHandle(request, response, new Object());

        // then
        assertThat(MDC.get("memberId")).isEqualTo("42");
        assertThat(MDC.get("clientType")).isEqualTo("IOS");
    }

    @Test
    @DisplayName("익명 사용자는 MDC memberId 가 채워지지 않는다")
    void preHandle_익명_사용자_memberId_MDC_미등록() {
        // given — SecurityContext 가 비어있는 상태
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/forms/123");
        MockHttpServletResponse response = new MockHttpServletResponse();

        // when
        interceptor.preHandle(request, response, new Object());

        // then
        assertThat(MDC.get("memberId")).isNull();
        assertThat(MDC.get("clientType")).isEqualTo("UNKNOWN");
    }

    @Test
    @DisplayName("X-Forwarded-For 헤더가 있으면 첫 번째 IP 가 clientIp 로 채워진다")
    void clientIp_X_Forwarded_For_우선() {
        // given
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/forms");
        request.addHeader("X-Forwarded-For", "203.0.113.7, 10.0.0.1");
        MockHttpServletResponse response = new MockHttpServletResponse();
        interceptor.preHandle(request, response, new Object());

        // when
        interceptor.afterCompletion(request, response, new Object(), null);

        // then
        Map<String, String> snapshot = findEventMdc("api_request_completed");
        assertThat(snapshot).isNotNull();
        assertThat(snapshot).containsEntry("clientIp", "203.0.113.7");
    }

    private Map<String, String> findEventMdc(String message) {
        // api_request_completed 메시지는 로컬 콘솔 가시화를 위해 본문 뒤에
        // " status=... durationMs=... queryCount=... queryTimeMs=..." 가 따라붙으므로
        // startsWith 로 매칭한다. (event MDC 키는 별도로 검증한다)
        List<ILoggingEvent> events = listAppender.list;
        for (ILoggingEvent event : events) {
            if (event.getMessage() != null && event.getMessage().startsWith(message)) {
                return event.getMDCPropertyMap();
            }
        }
        return null;
    }
}
