package com.umc.product.authorization.adapter.in.aspect;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.databind.JsonNode;
import com.umc.product.audit.adapter.out.persistence.AuditLogJpaRepository;
import com.umc.product.audit.domain.AuditAction;
import com.umc.product.audit.domain.AuditLog;
import com.umc.product.audit.domain.AuditOutcome;
import com.umc.product.audit.domain.AuditSource;
import com.umc.product.authorization.application.port.in.CheckPermissionUseCase;
import com.umc.product.authorization.domain.PermissionType;
import com.umc.product.authorization.domain.ResourcePermission;
import com.umc.product.authorization.domain.ResourceType;
import com.umc.product.global.security.MemberPrincipal;
import com.umc.product.support.IntegrationTestSupport;

import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanContext;
import io.opentelemetry.api.trace.TraceFlags;
import io.opentelemetry.api.trace.TraceState;
import io.opentelemetry.context.Scope;

@AutoConfigureMockMvc(addFilters = false)
@Import(AuthorizationFailureAuditIntegrationTest.AuthorizationAuditSurfaceController.class)
@DisplayName("접근 거부 HTTP-PostgreSQL 감사 통합 계약")
class AuthorizationFailureAuditIntegrationTest extends IntegrationTestSupport {

    private static final Long MEMBER_ID = 73L;
    private static final String RESOURCE_ID = "9001";
    private static final String RAW_AUTHORIZATION = "Bearer raw-authorization-token-secret";
    private static final String TRACE_ID = "fedcba9876543210fedcba9876543210";
    private static final String TRACEPARENT = "00-" + TRACE_ID + "-fedcba9876543210-01";
    private static final String SPAN_ID = "fedcba9876543210";
    private static final String TRUSTED_REMOTE_ADDRESS = "198.51.100.73";
    private static final String SPOOFED_FORWARDED_ADDRESS = "203.0.113.73";
    private static final String SPOOFED_REQUEST_ID = "client-controlled-request-id";

    @Autowired
    AuditLogJpaRepository auditLogJpaRepository;

    @MockitoBean
    CheckPermissionUseCase checkPermissionUseCase;

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
        MDC.clear();
    }

    @Test
    @DisplayName("인증된 접근 거부는 원 403과 리소스 감사 행을 남기고 Authorization 원문은 버린다")
    void authenticatedDenialRemainsForbiddenAndPersistsSanitizedAuditRow() throws Exception {
        // given
        authenticate(MEMBER_ID);
        given(checkPermissionUseCase.check(eq(MEMBER_ID), any(ResourcePermission.class)))
            .willReturn(false);

        // when & then
        MvcResult result;
        try (Scope ignored = serverTraceScope()) {
            result = mockMvc.perform(get("/test/task-6/resources/{resourceId}", RESOURCE_ID)
                    .header(HttpHeaders.AUTHORIZATION, RAW_AUTHORIZATION)
                    .header("traceparent", TRACEPARENT)
                    .header("X-Forwarded-For", SPOOFED_FORWARDED_ADDRESS)
                    .header("X-Request-Id", SPOOFED_REQUEST_ID)
                    .with(request -> {
                        request.setRemoteAddr(TRUSTED_REMOTE_ADDRESS);
                        return request;
                    }))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("AUTHORIZATION-0002"))
                .andReturn();
        }

        AuditLog auditLog = auditLogJpaRepository.findAll().getFirst();
        assertAccessDenied(auditLog, MEMBER_ID, RESOURCE_ID, responseTraceId(result));

        JsonNode details = objectMapper.readTree(auditLog.getDetails());
        assertThat(details.path("actor").path("memberId").asLong()).isEqualTo(MEMBER_ID);
        assertThat(details.path("target").path("type").asText())
            .isEqualTo(ResourceType.SCHEDULE.name());
        assertThat(details.path("target").path("id").asText()).isEqualTo(RESOURCE_ID);
        assertThat(details.path("context").path("permission").asText())
            .isEqualTo(PermissionType.EDIT.name());
        assertThat(auditLog.getDetails())
            .doesNotContainIgnoringCase("authorization")
            .doesNotContainIgnoringCase("token")
            .doesNotContain(RAW_AUTHORIZATION);
    }

    @Test
    @DisplayName("익명 접근 거부도 원 403을 유지하고 actor 없는 감사 행을 남긴다")
    void anonymousDenialRemainsForbiddenAndPersistsAuditRowWithoutActor() throws Exception {
        // when & then
        MvcResult result;
        try (Scope ignored = serverTraceScope()) {
            result = mockMvc.perform(get("/test/task-6/resources/{resourceId}", RESOURCE_ID)
                    .header("traceparent", TRACEPARENT)
                    .header("X-Forwarded-For", SPOOFED_FORWARDED_ADDRESS)
                    .header("X-Request-Id", SPOOFED_REQUEST_ID)
                    .with(request -> {
                        request.setRemoteAddr(TRUSTED_REMOTE_ADDRESS);
                        return request;
                    }))
                .andExpect(status().isForbidden())
                .andReturn();
        }

        List<AuditLog> auditLogs = auditLogJpaRepository.findAll();
        assertThat(auditLogs).hasSize(1);
        assertAccessDenied(auditLogs.getFirst(), null, RESOURCE_ID, responseTraceId(result));

        JsonNode details = objectMapper.readTree(auditLogs.getFirst().getDetails());
        assertThat(details.path("actor").isEmpty()).isTrue();
        assertThat(details.path("target").path("type").asText())
            .isEqualTo(ResourceType.SCHEDULE.name());
        assertThat(details.path("target").path("id").asText()).isEqualTo(RESOURCE_ID);
    }

    private void assertAccessDenied(
        AuditLog auditLog,
        Long actorMemberId,
        String resourceId,
        String responseTraceId
    ) {
        assertThat(auditLog.getDomain().name()).isEqualTo("AUTHORIZATION");
        assertThat(auditLog.getAction()).isEqualTo(AuditAction.ACCESS_DENIED);
        assertThat(auditLog.getTargetType()).isEqualTo(ResourceType.SCHEDULE.name());
        assertThat(auditLog.getTargetId()).isEqualTo(resourceId);
        assertThat(auditLog.getActorMemberId()).isEqualTo(actorMemberId);
        assertThat(auditLog.getOutcome()).isEqualTo(AuditOutcome.FAILURE);
        assertThat(auditLog.getSource()).isEqualTo(AuditSource.AUTHORIZATION_ASPECT);
        assertThat(auditLog.getIpAddress()).isEqualTo(TRUSTED_REMOTE_ADDRESS);
        assertThat(auditLog.getRequestId()).isEqualTo(responseTraceId);
        assertThat(auditLog.getTraceId()).isEqualTo(responseTraceId);
        assertThat(auditLog.getDetails()).isNotBlank();
        assertThat(auditLog.getDetails())
            .doesNotContain(SPOOFED_FORWARDED_ADDRESS, SPOOFED_REQUEST_ID);
    }

    private String responseTraceId(MvcResult result) {
        String responseTraceId = result.getResponse().getHeader("X-Trace-Id");
        assertThat(responseTraceId).isNotBlank();
        return responseTraceId;
    }

    private Scope serverTraceScope() {
        MDC.put("traceId", TRACE_ID);
        SpanContext context = SpanContext.create(
            TRACE_ID,
            SPAN_ID,
            TraceFlags.getSampled(),
            TraceState.getDefault()
        );
        return Span.wrap(context).makeCurrent();
    }

    private void authenticate(Long memberId) {
        MemberPrincipal principal = MemberPrincipal.builder()
            .memberId(memberId)
            .build();
        SecurityContextHolder.getContext().setAuthentication(
            new UsernamePasswordAuthenticationToken(
                principal,
                null,
                principal.getAuthorities()
            )
        );
    }

    @RestController
    static class AuthorizationAuditSurfaceController {

        @GetMapping("/test/task-6/resources/{resourceId}")
        @CheckAccess(
            resourceType = ResourceType.SCHEDULE,
            resourceId = "#resourceId",
            permission = PermissionType.EDIT,
            message = "수정 권한이 없습니다."
        )
        void denied(@PathVariable String resourceId) {
        }
    }
}
