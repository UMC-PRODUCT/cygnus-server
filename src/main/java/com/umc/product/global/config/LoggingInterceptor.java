package com.umc.product.global.config;

import org.slf4j.MDC;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.HandlerMapping;

import com.umc.product.global.client.ClientDeviceType;
import com.umc.product.global.client.ClientEnvironment;
import com.umc.product.global.client.ClientRequestClassifier;
import com.umc.product.global.client.ClientRequestContext;
import com.umc.product.global.client.ClientServiceType;
import com.umc.product.global.logging.OperationalMetrics;
import com.umc.product.global.security.MemberPrincipal;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * HTTP 요청/응답 로깅 인터셉터 (ADR-016 구조화 로그 기반)
 *
 * <p>요청 진입 시점에 {@code method} / {@code path} 를 MDC 에 push 하고, 응답 완료 시점에
 * {@code uriTemplate} / {@code statusCode} / {@code durationMs} / {@code queryCount} /
 * {@code queryTimeMs} / {@code clientIp} 를 MDC 에 채워 {@code api_request_completed} 이벤트 한 줄을 남긴다.
 *
 * <p>단일 요청 식별자는 Micrometer Tracing 이 자동으로 MDC 에 넣는 {@code traceId} 를 그대로 재사용한다.
 * 별도의 UUID 기반 requestId 는 발급하지 않는다 — FE 등 외부 시스템이 이미 {@code X-Trace-Id} 응답 헤더로
 * 알고 있는 식별자를 변경하지 않기 위해서다.
 *
 * <p>ThreadLocal 누수를 막기 위해 {@link #afterCompletion} 의 finally 블록에서 반드시
 * {@link QueryStatsHolder#clear()} 와 {@link MDC#clear()} 를 호출한다 — 조기 반환 / 예외 경로 모두 포함.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class LoggingInterceptor implements HandlerInterceptor {

    private static final String START_TIME_ATTR = "startTime";
    private static final String CLIENT_IP_ATTR = "clientIp";
    public static final String CLIENT_REQUEST_CONTEXT_ATTR = "clientRequestContext";

    private static final String TRACE_ID_HEADER = "X-Trace-Id";

    // ===== MDC keys (ADR-016 §MDC 키 표준) =====
    private static final String MDC_MEMBER_ID = "memberId";
    private static final String MDC_CLIENT_TYPE = "clientType";
    private static final String MDC_METHOD = "method";
    private static final String MDC_PATH = "path";
    private static final String MDC_URI_TEMPLATE = "uriTemplate";
    private static final String MDC_STATUS_CODE = "statusCode";
    private static final String MDC_DURATION_MS = "durationMs";
    private static final String MDC_QUERY_COUNT = "queryCount";
    private static final String MDC_QUERY_TIME_MS = "queryTimeMs";
    private static final String MDC_CLIENT_IP = "clientIp";
    private static final String MDC_EVENT = "event";
    private static final String MDC_EXCEPTION = "exception";
    private static final String MDC_TRACE_ID = "traceId";
    private static final String MDC_CLIENT_SERVICE = "clientService";
    private static final String MDC_CLIENT_DEVICE = "clientDevice";
    private static final String MDC_CLIENT_ENVIRONMENT = "clientEnvironment";
    private static final String MDC_CLIENT_CONTEXT_SOURCE = "clientContextSource";

    private static final String EVENT_REQUEST_STARTED = "api_request_started";
    private static final String EVENT_REQUEST_COMPLETED = "api_request_completed";
    private static final String UNKNOWN = "UNKNOWN";
    private static final String SECURITY_DOMAIN_AUTHENTICATION = "AUTHENTICATION";
    private static final String SECURITY_OPERATION_CLIENT_CONTEXT_MISMATCH = "CLIENT_CONTEXT_MISMATCH";
    private static final String SECURITY_RESULT_DETECTED = "detected";

    private final ClientRequestClassifier clientRequestClassifier;
    private final OperationalMetrics operationalMetrics;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {

        // 경과 시간 측정은 monotonic clock 인 nanoTime 을 사용한다.
        // Instant.now / currentTimeMillis 는 NTP 동기화 등 wall-clock 보정 시
        // 뒤로 점프할 수 있어 durationMs 가 음수/왜곡으로 찍힌다.
        request.setAttribute(START_TIME_ATTR, System.nanoTime());
        request.setAttribute(CLIENT_IP_ATTR, extractClientIp(request));

        QueryStatsHolder.init();

        MDC.put(MDC_METHOD, request.getMethod());
        MDC.put(MDC_PATH, request.getRequestURI());
        MDC.put(MDC_CLIENT_TYPE, UNKNOWN);

        // Micrometer Tracing 이 채워 둔 traceId 를 FE 디버깅용으로 응답 헤더에 노출.
        // 별도 requestId UUID 는 발급하지 않는다 (FE 호환성 — X-Trace-Id 가 이미 사용 중).
        String traceId = MDC.get(MDC_TRACE_ID);
        if (traceId != null) {
            response.setHeader(TRACE_ID_HEADER, traceId);
        }

        // 인증된 사용자라면 memberId / clientType 을 MDC 에 등록.
        // clientType 이 없으면 UNKNOWN 으로 남겨 메트릭에서 누락 트래픽을 집계할 수 있게 한다.
        MemberPrincipal memberPrincipal = putMemberContextToMdcIfAuthenticated();
        ClientRequestContext clientRequestContext = clientRequestClassifier.classify(request, memberPrincipal);
        request.setAttribute(CLIENT_REQUEST_CONTEXT_ATTR, clientRequestContext);
        putClientRequestContextToMdc(clientRequestContext);
        if (clientRequestContext.mismatched()) {
            operationalMetrics.recordSecurityEvent(
                SECURITY_DOMAIN_AUTHENTICATION,
                SECURITY_OPERATION_CLIENT_CONTEXT_MISMATCH,
                SECURITY_RESULT_DETECTED
            );
        }

        // ADR-016 §MDC 키 표준: api_request_completed 와 동일하게 event 필드를 채워
        // LogQL `| json | event="api_request_started"` 필터링이 가능하도록 한다.
        // 단, event 는 해당 로그 라인에만 묶여야 하므로 (이후 컨트롤러/서비스 로그 오염 방지)
        // 로깅 직후 즉시 제거한다.
        MDC.put(MDC_EVENT, EVENT_REQUEST_STARTED);
        try {
            log.info(EVENT_REQUEST_STARTED);
        } finally {
            MDC.remove(MDC_EVENT);
        }
        return true;
    }

    /**
     * SecurityContextHolder 에서 인증된 {@link MemberPrincipal} 을 조회해
     * MDC {@code memberId} 와 {@code clientType} 을 채운다.
     *
     * <p>서비스 도메인 명명을 그대로 사용 (`userId` 가 아니라 `memberId`).
     *
     * <p>{@code clientType} 은 로그인 시 클라이언트가 선택적으로 전달한 플랫폼 정보를
     * AT claim 으로 받아온 값이다. 도입 이전 발급 토큰 / 누락된 클라이언트는 UNKNOWN 으로 남겨
     * Loki/대시보드에서 미설정 트래픽을 구분 집계할 수 있게 한다.
     *
     * <p>Filter 가 아니라 Interceptor 에서 채우는 이유:
     * <ul>
     *     <li>MDC 의 lifecycle (put / clear) 이 Interceptor 한 곳에 모여 누수 위험이 줄어든다.</li>
     *     <li>인증 책임은 Filter, 로그 컨텍스트 책임은 Interceptor 로 분리된다.</li>
     * </ul>
     */
    private MemberPrincipal putMemberContextToMdcIfAuthenticated() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return null;
        }
        if (authentication.getPrincipal() instanceof MemberPrincipal memberPrincipal) {
            MDC.put(MDC_MEMBER_ID, String.valueOf(memberPrincipal.getMemberId()));
            if (memberPrincipal.getClientType() != null) {
                MDC.put(MDC_CLIENT_TYPE, memberPrincipal.getClientType().name());
            }
            return memberPrincipal;
        }
        return null;
    }

    @Override
    public void afterCompletion(
        HttpServletRequest request, HttpServletResponse response,
        Object handler, Exception ex
    ) {
        try {
            Long startNanos = (Long) request.getAttribute(START_TIME_ATTR);
            if (startNanos == null) {
                return;
            }

            long durationMs = (System.nanoTime() - startNanos) / 1_000_000L;
            long queryCount = QueryStatsHolder.getQueryCount();
            long queryTimeMs = QueryStatsHolder.getTotalTimeMs();

            // uriTemplate: Spring 이 매칭한 패턴 (예: /forms/{formId}/answers).
            // path 가 가변이라 dashboard 의 P95/P99 집계가 불가능했던 문제를 해결한다.
            Object bestMatchingPattern = request.getAttribute(
                HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE
            );
            String uriTemplate = bestMatchingPattern instanceof String s ? s : "UNKNOWN";

            String clientIp = (String) request.getAttribute(CLIENT_IP_ATTR);

            MDC.put(MDC_EVENT, EVENT_REQUEST_COMPLETED);
            MDC.put(MDC_URI_TEMPLATE, uriTemplate);
            MDC.put(MDC_STATUS_CODE, String.valueOf(response.getStatus()));
            MDC.put(MDC_DURATION_MS, String.valueOf(durationMs));
            MDC.put(MDC_QUERY_COUNT, String.valueOf(queryCount));
            MDC.put(MDC_QUERY_TIME_MS, String.valueOf(queryTimeMs));
            if (clientIp != null) {
                MDC.put(MDC_CLIENT_IP, clientIp);
            }

            ClientRequestContext clientRequestContext = getClientRequestContext(request);
            putClientRequestContextToMdc(clientRequestContext);
            operationalMetrics.recordClientRequest(
                clientRequestContext.serviceType(),
                clientRequestContext.deviceType(),
                clientRequestContext.environment(),
                clientRequestContext.source(),
                statusFamily(response.getStatus())
            );

            // 로컬 콘솔 패턴은 MDC 키를 렌더링하지 않으므로 (traceId 만 노출),
            // 메시지 본문에 핵심 메트릭을 함께 실어 로컬에서도 한 줄로 가시화한다.
            // JSON 어펜더(CONSOLE_JSON / LOKI)는 <mdc/> 프로바이더로 동일 값을 별도 필드로 직렬화한다.
            String summary = String.format(
                "%s status=%d durationMs=%d queryCount=%d queryTimeMs=%d",
                EVENT_REQUEST_COMPLETED, response.getStatus(), durationMs, queryCount, queryTimeMs
            );

            if (ex != null) {
                // ADR-016 §민감 필드 정책: throwable 을 log.error 에 넘기면 JSON layout 의
                // <stackTrace> 프로바이더가 전체 스택을 직렬화하고, 예외 메시지에 토큰 / 사용자
                // 입력이 섞여 stdout / Loki 로 노출될 수 있다. 본 이벤트는 dashboard 집계용
                // 메타 (exception 클래스명) 만 MDC 로 남기고, 스택 자체는 전역 예외 핸들러 /
                // 컨트롤러 등 책임 지점의 로그에 맡긴다.
                MDC.put(MDC_EXCEPTION, ex.getClass().getSimpleName());
                log.error(summary);
            } else {
                log.info(summary);
            }
        } finally {
            // ThreadLocal 누수 방지: 조기 반환 / try 블록 내 예외 경로 모두에서
            // QueryStatsHolder 와 MDC 를 반드시 비운다 (톰캣 스레드 재사용 대응).
            QueryStatsHolder.clear();
            MDC.clear();
        }
    }

    private String extractClientIp(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");

        if (xForwardedFor != null && !xForwardedFor.isBlank()) {
            // 다중 프록시 경유 시 첫 번째 IP가 실제 클라이언트 IP
            return xForwardedFor.split(",")[0].trim();
        }

        return request.getRemoteAddr();
    }

    private void putClientRequestContextToMdc(ClientRequestContext context) {
        MDC.put(MDC_CLIENT_SERVICE, context.serviceType().name());
        MDC.put(MDC_CLIENT_DEVICE, context.deviceType().name());
        MDC.put(MDC_CLIENT_ENVIRONMENT, context.environment().name());
        MDC.put(MDC_CLIENT_CONTEXT_SOURCE, context.source());
    }

    private ClientRequestContext getClientRequestContext(HttpServletRequest request) {
        Object context = request.getAttribute(CLIENT_REQUEST_CONTEXT_ATTR);
        if (context instanceof ClientRequestContext clientRequestContext) {
            return clientRequestContext;
        }
        return new ClientRequestContext(
            ClientServiceType.UNKNOWN,
            ClientDeviceType.UNKNOWN,
            ClientEnvironment.UNKNOWN,
            "unknown",
            false
        );
    }

    private String statusFamily(int status) {
        if (status < 100 || status > 599) {
            return UNKNOWN;
        }
        return (status / 100) + "xx";
    }

}
