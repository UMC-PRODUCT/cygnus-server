package com.umc.product.audit.adapter.in.aop;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.reflect.MethodSignature;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import com.umc.product.audit.application.port.in.annotation.Audited;
import com.umc.product.audit.domain.AuditLogEvent;
import com.umc.product.audit.domain.AuditOutcome;
import com.umc.product.audit.domain.AuditSource;
import com.umc.product.challenger.application.port.in.command.dto.CreateChallengerRecordCommand;
import com.umc.product.challenger.application.service.ChallengerRecordCommandService;
import com.umc.product.global.event.application.port.out.DomainEventPublisher;
import com.umc.product.global.event.domain.DomainEvent;
import com.umc.product.global.logging.AuditRequestContextProvider;
import com.umc.product.global.logging.OperationalMetrics;
import com.umc.product.global.security.MemberPrincipal;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanContext;
import io.opentelemetry.api.trace.TraceFlags;
import io.opentelemetry.api.trace.TraceState;
import io.opentelemetry.context.Scope;

class AuditAspectContractTest {

    private static final String TRACE_ID = "0123456789abcdef0123456789abcdef";
    private static final String SPAN_ID = "0123456789abcdef";

    private final List<AuditLogEvent> publishedEvents = new ArrayList<>();
    private AuditAspect sut;
    private MockHttpServletRequest request;
    private SimpleMeterRegistry meterRegistry;

    @BeforeEach
    void setUp() {
        DomainEventPublisher eventPublisher = new DomainEventPublisher() {
            @Override
            public void publish(DomainEvent event) {
                publishedEvents.add((AuditLogEvent) event);
            }

            @Override
            public void publishAll(Collection<? extends DomainEvent> events) {
                events.forEach(this::publish);
            }
        };
        meterRegistry = new SimpleMeterRegistry();
        sut = new AuditAspect(
            eventPublisher,
            new com.umc.product.audit.application.service.command.AuditLogRecordingMonitor(
                new OperationalMetrics(meterRegistry)
            ),
            new AuditRequestContextProvider()
        );

        MemberPrincipal principal = MemberPrincipal.builder().memberId(73L).build();
        SecurityContextHolder.getContext().setAuthentication(
            new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities())
        );

        MDC.clear();
        request = new MockHttpServletRequest();
        request.setRemoteAddr("192.0.2.10");
        request.addHeader("X-Forwarded-For", "198.51.100.7, 203.0.113.8");
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));
    }

    @AfterEach
    void tearDown() {
        RequestContextHolder.resetRequestAttributes();
        SecurityContextHolder.clearContext();
        MDC.clear();
    }

    @Test
    @DisplayName("기존 @Audited 성공 메서드는 감사 이벤트의 계약 필드를 전달한다")
    void 기존_Audited_성공_메서드는_감사_이벤트의_계약_필드를_전달한다() throws Exception {
        // given
        Method method = ChallengerRecordCommandService.class.getMethod(
            "create", CreateChallengerRecordCommand.class);
        Audited audited = method.getAnnotation(Audited.class);
        // when
        publish(method, audited, new Object[]{null}, 42L);

        // then
        assertThat(publishedEvents).hasSize(1);
        AuditLogEvent event = publishedEvents.getFirst();
        assertThat(event.domain()).isEqualTo(com.umc.product.global.exception.constant.Domain.CHALLENGER);
        assertThat(event.action()).isEqualTo(com.umc.product.audit.domain.AuditAction.CREATE);
        assertThat(event.targetType()).isEqualTo("ChallengerRecord");
        assertThat(event.targetId()).isEqualTo("42");
        assertThat(event.actorMemberId()).isEqualTo(73L);
        assertThat(event.ipAddress()).isEqualTo("192.0.2.10");
        assertThat(event.outcome()).isEqualTo(AuditOutcome.SUCCESS);
        assertThat(event.source()).isEqualTo(AuditSource.ANNOTATION);
        assertThat(event.requestId()).isNull();
        assertThat(event.traceId()).isNull();
        assertThat(event.occurredAt()).isNotNull();
    }

    @Test
    @DisplayName("MDC의 서버 traceId를 requestId로 사용하고 요청 헤더보다 우선한다")
    void MDC_traceId를_requestId로_사용하고_요청_헤더보다_우선한다() throws Exception {
        // given
        MDC.put("traceId", "server-request-id");
        request.addHeader("X-Request-Id", "spoofed-client-request-id");
        Method method = auditedFixtureMethod("record", String.class, String.class);

        SpanContext spanContext = SpanContext.create(
            TRACE_ID,
            SPAN_ID,
            TraceFlags.getSampled(),
            TraceState.getDefault()
        );

        // when
        try (Scope ignored = Span.wrap(spanContext).makeCurrent()) {
            publish(method, method.getAnnotation(Audited.class),
                new Object[]{"17", "정상 설명"}, null);
        }

        // then
        AuditLogEvent event = publishedEvents.getFirst();
        assertThat(event.requestId()).isEqualTo("server-request-id");
        assertThat(event.traceId()).isEqualTo(TRACE_ID);
        assertThat(event.outcome()).isEqualTo(AuditOutcome.SUCCESS);
        assertThat(event.source()).isEqualTo(AuditSource.ANNOTATION);
    }

    @Test
    @DisplayName("MDC 요청 식별자가 없으면 client X-Request-Id를 신뢰하지 않는다")
    void MDC_요청_식별자가_없으면_client_X_Request_Id를_무시한다() throws Exception {
        // given
        request.addHeader("X-Request-Id", "gateway-request-123");
        Method method = auditedFixtureMethod("record", String.class, String.class);

        // when
        publish(method, method.getAnnotation(Audited.class),
            new Object[]{"18", "헤더 fallback"}, null);

        // then
        AuditLogEvent event = publishedEvents.getFirst();
        assertThat(event.requestId()).isNull();
        assertThat(event.traceId()).isNull();
    }

    @Test
    @DisplayName("서버 MDC requestId는 100자까지만 허용하고 malformed 또는 oversized 값은 무시한다")
    void 서버_MDC_requestId_길이와_형식을_검증한다() throws Exception {
        // given
        Method method = auditedFixtureMethod("record", String.class, String.class);
        String maxLengthRequestId = "r".repeat(100);

        // when
        MDC.put("traceId", maxLengthRequestId);
        publish(method, method.getAnnotation(Audited.class),
            new Object[]{"19", "100자 경계"}, null);

        // then
        assertThat(publishedEvents.getFirst().requestId()).isEqualTo(maxLengthRequestId);

        for (String rejected : List.of(
            "r".repeat(101),
            "request id with spaces",
            "request-id\r\nforged-header"
        )) {
            publishedEvents.clear();
            MDC.put("traceId", rejected);
            request.addHeader("X-Request-Id", "spoofed-fallback");

            assertThatCode(() -> publish(method, method.getAnnotation(Audited.class),
                new Object[]{"20", "거부 경계"}, null))
                .doesNotThrowAnyException();

            assertThat(publishedEvents).hasSize(1);
            assertThat(publishedEvents.getFirst().requestId()).isNull();
        }
    }

    @Test
    @DisplayName("유효하지 않은 MDC 식별자가 있으면 spoofed 요청 헤더로 대체하지 않는다")
    void 유효하지_않은_MDC가_있으면_요청_헤더로_대체하지_않는다() throws Exception {
        // given
        MDC.put("traceId", "invalid canonical value");
        request.addHeader("X-Request-Id", "spoofed-but-valid-header");
        Method method = auditedFixtureMethod("record", String.class, String.class);

        // when
        publish(method, method.getAnnotation(Audited.class),
            new Object[]{"21", "canonical 오류"}, null);

        // then
        assertThat(publishedEvents.getFirst().requestId()).isNull();
    }

    @Test
    @DisplayName("SpEL의 bean 조회 시도는 비즈니스 요청을 깨지 않고 오류 로그를 남긴다")
    void SpEL의_bean_조회_시도는_비즈니스_요청을_깨지_않고_오류_로그를_남긴다() throws Exception {
        // given
        Method method = auditedFixtureMethod("queryInSpel");
        Logger logger = (Logger) LoggerFactory.getLogger(AuditAspect.class);
        ListAppender<ILoggingEvent> appender = new ListAppender<>();
        appender.start();
        logger.addAppender(appender);

        try {
            // when
            assertThatCode(() -> publish(method, method.getAnnotation(Audited.class),
                new Object[0], null))
                .doesNotThrowAnyException();
            assertThat(publishedEvents).isEmpty();
            assertThat(appender.list)
                .anySatisfy(event -> assertThat(event.getFormattedMessage())
                    .contains("감사 로그 이벤트 발행 중 오류")
                    .contains("queryInSpel")
                    .contains("phase=targetId-spel")
                    .contains("errorType=SpelEvaluationException"));
            assertThat(meterRegistry.get("operational.audit.log.failure.total")
                .tags(
                    "domain", "CHALLENGER",
                    "action", "CREATE",
                    "reason", "spel_evaluation"
                )
                .counter()
                .count()).isEqualTo(1D);
        } finally {
            logger.detachAppender(appender);
            appender.stop();
        }
    }

    @Test
    @DisplayName("description의 공격성 문자열은 details key나 감사 결과 필드로 승격되지 않는다")
    void description의_공격성_문자열은_details_key로_승격되지_않는다() throws Exception {
        // given
        String untrustedDescription =
            "requestId=attacker traceId=attacker outcome=FAILURE source=SYSTEM details[token]=expose";
        Method method = auditedFixtureMethod("record", String.class, String.class);

        // when
        publish(method, method.getAnnotation(Audited.class),
            new Object[]{"22", untrustedDescription}, null);

        // then
        AuditLogEvent event = publishedEvents.getFirst();
        assertThat(event.description()).isEqualTo(untrustedDescription);
        assertThat(event.details()).isEmpty();
        assertThat(event.outcome()).isEqualTo(AuditOutcome.SUCCESS);
        assertThat(event.source()).isEqualTo(AuditSource.ANNOTATION);
        assertThat(event.requestId()).isNull();
        assertThat(event.traceId()).isNull();
    }

    private Method auditedFixtureMethod(String name, Class<?>... parameterTypes) throws Exception {
        return AuditAspectAuditedFixture.class.getMethod(name, parameterTypes);
    }

    private void publish(Method method, Audited audited, Object[] args, Object result) {
        MethodSignature signature = mock(MethodSignature.class);
        given(signature.getMethod()).willReturn(method);
        given(signature.toShortString()).willReturn(
            method.getDeclaringClass().getSimpleName() + "." + method.getName() + "(..)"
        );
        JoinPoint joinPoint = mock(JoinPoint.class);
        given(joinPoint.getSignature()).willReturn(signature);
        given(joinPoint.getArgs()).willReturn(args);

        sut.publishAuditEvent(joinPoint, audited, result);
    }

}
