package com.umc.product.authentication.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

import com.fasterxml.jackson.databind.JsonNode;
import com.umc.product.audit.adapter.out.persistence.AuditLogJpaRepository;
import com.umc.product.audit.domain.AuditAction;
import com.umc.product.audit.domain.AuditLog;
import com.umc.product.audit.domain.AuditOutcome;
import com.umc.product.audit.domain.AuditSource;
import com.umc.product.support.IntegrationTestSupport;

@DisplayName("로그인 실패 HTTP-PostgreSQL 감사 통합 계약")
class AuthenticationFailureAuditIntegrationTest extends IntegrationTestSupport {

    private static final String MALFORMED_EMAIL = "malformed?token=login-token-secret";
    private static final String RAW_PASSWORD = "raw-password-secret";
    private static final String RAW_AUTHORIZATION = "Bearer raw-authorization-token-secret";
    private static final String TRACE_ID = "0123456789abcdef0123456789abcdef";
    private static final String TRACEPARENT = "00-" + TRACE_ID + "-0123456789abcdef-01";
    private static final String TRUSTED_REMOTE_ADDRESS = "198.51.100.44";
    private static final String SPOOFED_FORWARDED_ADDRESS = "203.0.113.99";
    private static final String SPOOFED_REQUEST_ID = "client-controlled-request-id";

    @Autowired
    AuditLogJpaRepository auditLogJpaRepository;

    @Test
    @DisplayName("반복된 형식 불명 자격증명은 원 401을 유지하고 롤백 뒤 실패 감사 행을 각각 남긴다")
    void malformedRepeatedLoginFailuresRemainUnauthorizedAndPersistAuditRows() throws Exception {
        // when & then
        List<String> responseTraceIds = List.of(performFailedLogin(), performFailedLogin());

        List<AuditLog> auditLogs = auditLogJpaRepository.findAll();
        assertThat(auditLogs).hasSize(2);
        assertThat(auditLogs).allSatisfy(auditLog -> {
            assertThat(auditLog.getDomain().name()).isEqualTo("AUTHENTICATION");
            assertThat(auditLog.getAction()).isEqualTo(AuditAction.LOGIN);
            assertThat(auditLog.getTargetType()).isEqualTo("MemberCredential");
            assertThat(auditLog.getTargetId()).isNull();
            assertThat(auditLog.getActorMemberId()).isNull();
            assertThat(auditLog.getOutcome()).isEqualTo(AuditOutcome.FAILURE);
            assertThat(auditLog.getSource()).isEqualTo(AuditSource.AUTHENTICATION_SERVICE);
            assertThat(auditLog.getIpAddress()).isEqualTo(TRUSTED_REMOTE_ADDRESS);
            assertThat(auditLog.getRequestId()).isIn(responseTraceIds);
            assertThat(auditLog.getTraceId()).isEqualTo(auditLog.getRequestId());
            assertThat(auditLog.getDetails()).isNotBlank();
            assertThat(auditLog.getDescription())
                .doesNotContain(MALFORMED_EMAIL)
                .doesNotContain(RAW_PASSWORD)
                .doesNotContain(RAW_AUTHORIZATION);
            assertThat(auditLog.getDetails())
                .doesNotContain(MALFORMED_EMAIL)
                .doesNotContain(RAW_PASSWORD)
                .doesNotContain(RAW_AUTHORIZATION)
                .doesNotContainIgnoringCase("email")
                .doesNotContainIgnoringCase("password")
                .doesNotContainIgnoringCase("token")
                .doesNotContainIgnoringCase("authorization")
                .doesNotContain(SPOOFED_FORWARDED_ADDRESS, SPOOFED_REQUEST_ID);
        });
        assertThat(auditLogs).extracting(AuditLog::getRequestId)
            .containsExactlyInAnyOrderElementsOf(responseTraceIds);

        JsonNode details = objectMapper.readTree(auditLogs.getFirst().getDetails());
        assertThat(details.path("schemaVersion").asInt()).isEqualTo(1);
        assertThat(details.path("actor").isEmpty()).isTrue();
        assertThat(details.path("target").path("type").asText())
            .isEqualTo("MemberCredential");
        assertThat(details.path("target").size()).isEqualTo(1);
        assertThat(details.path("context").isEmpty()).isTrue();
        assertThat(details.size()).isEqualTo(6);
    }

    private String performFailedLogin() throws Exception {
        String responseTraceId = mockMvc.perform(post("/api/v1/auth/login/email")
                .contentType(MediaType.APPLICATION_JSON)
                .header(HttpHeaders.AUTHORIZATION, RAW_AUTHORIZATION)
                .header("traceparent", TRACEPARENT)
                .header("X-Forwarded-For", SPOOFED_FORWARDED_ADDRESS)
                .header("X-Request-Id", SPOOFED_REQUEST_ID)
                .with(request -> {
                    request.setRemoteAddr(TRUSTED_REMOTE_ADDRESS);
                    return request;
                })
                .content("""
                    {
                      "email": "%s",
                      "password": "%s",
                      "clientType": "WEB"
                    }
                    """.formatted(MALFORMED_EMAIL, RAW_PASSWORD)))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.success").value(false))
            .andExpect(jsonPath("$.code").value("AUTHENTICATION-0022"))
            .andReturn()
            .getResponse()
            .getHeader("X-Trace-Id");
        assertThat(responseTraceId).isNotBlank();
        return responseTraceId;
    }
}
