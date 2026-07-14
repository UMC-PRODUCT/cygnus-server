package com.umc.product.audit.adapter.in.web;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.Properties;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.info.BuildProperties;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import com.umc.product.audit.adapter.out.persistence.AuditLogJpaRepository;
import com.umc.product.audit.domain.AuditAction;
import com.umc.product.audit.domain.AuditLog;
import com.umc.product.audit.domain.AuditLogEvent;
import com.umc.product.audit.domain.AuditOutcome;
import com.umc.product.audit.domain.AuditSource;
import com.umc.product.authorization.application.port.in.CheckPermissionUseCase;
import com.umc.product.global.exception.constant.Domain;
import com.umc.product.global.security.MemberPrincipal;
import com.umc.product.support.IntegrationTestSupport;

@AutoConfigureMockMvc(addFilters = false)
@DisplayName("관리자 감사 로그 조회 API 통합 계약")
class AuditLogControllerIntegrationTest extends IntegrationTestSupport {

    private static final Instant BASE_TIME = Instant.parse("2026-03-01T00:00:00Z");

    @Autowired
    AuditLogJpaRepository auditLogJpaRepository;

    @Autowired
    PlatformTransactionManager transactionManager;

    @MockitoBean
    CheckPermissionUseCase checkPermissionUseCase;

    @TestConfiguration
    static class BuildPropertiesTestConfig {

        @Bean
        BuildProperties buildProperties() {
            Properties properties = new Properties();
            properties.setProperty("version", "test");
            return new BuildProperties(properties);
        }
    }

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("신규 필터별 HTTP 조회는 실제 QueryDSL 조건과 확장 응답 필드를 반영한다")
    void 신규_필터별_HTTP_조회는_실제_QueryDSL_조건과_확장_응답_필드를_반영한다() throws Exception {
        // given
        authenticate(1L);
        given(checkPermissionUseCase.check(eq(1L), any())).willReturn(true);
        seedAuditLog(
            "Schedule",
            "schedule-10",
            AuditOutcome.FAILURE,
            AuditSource.AUTHORIZATION_ASPECT,
            "req_%_literal",
            "trace_' OR '1'='1",
            "{\"schemaVersion\":1,\"target\":{\"id\":\"schedule-10\"}}",
            BASE_TIME.plusSeconds(30)
        );
        seedAuditLog(
            "Schedule",
            "schedule-legacy",
            AuditOutcome.SUCCESS,
            AuditSource.ANNOTATION,
            null,
            null,
            null,
            BASE_TIME.plusSeconds(20)
        );
        seedAuditLog(
            "Member",
            "member-1",
            AuditOutcome.SUCCESS,
            AuditSource.EXPLICIT_RECORDER,
            "req-abc-literal",
            "trace-safe",
            "{\"schemaVersion\":1,\"target\":{\"id\":\"member-1\"}}",
            BASE_TIME.plusSeconds(10)
        );

        // when & then
        mockMvc.perform(get("/api/v1/audit/admin/audit-logs").param("targetType", "Schedule"))
            .andDo(print())
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.result.content.length()").value(2))
            .andExpect(jsonPath("$.result.content[0].targetId").value("schedule-10"))
            .andExpect(jsonPath("$.result.content[0].outcome").value("FAILURE"))
            .andExpect(jsonPath("$.result.content[0].source").value("AUTHORIZATION_ASPECT"))
            .andExpect(jsonPath("$.result.content[0].requestId").value("req_%_literal"))
            .andExpect(jsonPath("$.result.content[0].traceId").value("trace_' OR '1'='1"))
            .andExpect(jsonPath("$.result.content[0].details")
                .value("{\"target\": {\"id\": \"schedule-10\"}, \"schemaVersion\": 1}"))
            .andExpect(jsonPath("$.result.content[1].targetId").value("schedule-legacy"))
            .andExpect(jsonPath("$.result.content[1].details").doesNotExist());

        mockMvc.perform(get("/api/v1/audit/admin/audit-logs").param("targetId", "schedule-10"))
            .andDo(print())
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.result.content.length()").value(1))
            .andExpect(jsonPath("$.result.content[0].targetId").value("schedule-10"));

        mockMvc.perform(get("/api/v1/audit/admin/audit-logs").param("outcome", "FAILURE"))
            .andDo(print())
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.result.content.length()").value(1))
            .andExpect(jsonPath("$.result.content[0].targetId").value("schedule-10"));

        mockMvc.perform(get("/api/v1/audit/admin/audit-logs").param("source", "AUTHORIZATION_ASPECT"))
            .andDo(print())
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.result.content.length()").value(1))
            .andExpect(jsonPath("$.result.content[0].targetId").value("schedule-10"));

        mockMvc.perform(get("/api/v1/audit/admin/audit-logs").param("requestId", "req_%_literal"))
            .andDo(print())
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.result.content.length()").value(1))
            .andExpect(jsonPath("$.result.content[0].targetId").value("schedule-10"));

        mockMvc.perform(get("/api/v1/audit/admin/audit-logs").param("requestId", "req_%"))
            .andDo(print())
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.result.content.length()").value(0));

        mockMvc.perform(get("/api/v1/audit/admin/audit-logs").param("traceId", "trace_' OR '1'='1"))
            .andDo(print())
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.result.content.length()").value(1))
            .andExpect(jsonPath("$.result.content[0].targetId").value("schedule-10"));

        mockMvc.perform(get("/api/v1/audit/admin/audit-logs").param("traceId", "' OR '1'='1"))
            .andDo(print())
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.result.content.length()").value(0));

        mockMvc.perform(get("/api/v1/audit/admin/audit-logs")
                .param("targetType", "Schedule")
                .param("targetId", "schedule-10")
                .param("outcome", "FAILURE"))
            .andDo(print())
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.result.content.length()").value(1))
            .andExpect(jsonPath("$.result.content[0].targetType").value("Schedule"))
            .andExpect(jsonPath("$.result.content[0].targetId").value("schedule-10"))
            .andExpect(jsonPath("$.result.content[0].outcome").value("FAILURE"));
    }

    private void authenticate(Long memberId) {
        MemberPrincipal principal = MemberPrincipal.builder()
            .memberId(memberId)
            .build();
        SecurityContextHolder.getContext().setAuthentication(
            new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities())
        );
    }

    private void seedAuditLog(
        String targetType,
        String targetId,
        AuditOutcome outcome,
        AuditSource source,
        String requestId,
        String traceId,
        String detailsJson,
        Instant createdAt
    ) {
        new TransactionTemplate(transactionManager).executeWithoutResult(status -> {
            AuditLog auditLog = auditLogJpaRepository.save(AuditLog.from(
                AuditLogEvent.builder()
                    .occurredAt(createdAt)
                    .domain(Domain.SCHEDULE)
                    .action(AuditAction.UPDATE)
                    .targetType(targetType)
                    .targetId(targetId)
                    .actorMemberId(1L)
                    .description("감사 로그 조회 API 통합 테스트")
                    .outcome(outcome)
                    .source(source)
                    .requestId(requestId)
                    .traceId(traceId)
                    .build(),
                detailsJson,
                "203.0.113.100"
            ));
            entityManager.flush();
            entityManager
                .createNativeQuery("UPDATE audit_log SET created_at = :createdAt WHERE id = :id")
                .setParameter("createdAt", Timestamp.from(createdAt))
                .setParameter("id", auditLog.getId())
                .executeUpdate();
        });
    }
}
