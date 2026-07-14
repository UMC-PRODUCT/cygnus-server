package com.umc.product.audit.adapter.out.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.umc.product.audit.application.service.command.AuditLogCommandService;
import com.umc.product.audit.application.service.command.AuditLogRecordingMonitor;
import com.umc.product.audit.domain.AuditAction;
import com.umc.product.audit.domain.AuditLog;
import com.umc.product.audit.domain.AuditLogEvent;
import com.umc.product.audit.domain.AuditOutcome;
import com.umc.product.audit.domain.AuditSource;
import com.umc.product.global.exception.constant.Domain;
import com.umc.product.support.PersistenceAdapterTest;

@PersistenceAdapterTest
@Import({
    AuditLogCommandService.class,
    AuditLogPersistenceAdapter.class,
    AuditLogQueryRepository.class,
    AuditLogPersistenceContractTest.ObjectMapperTestConfiguration.class
})
@DisplayName("감사 로그 저장 계약")
class AuditLogPersistenceContractTest {

    @Autowired
    TestEntityManager entityManager;

    @Autowired
    AuditLogJpaRepository auditLogJpaRepository;

    @Autowired
    AuditLogCommandService auditLogCommandService;

    @Autowired
    ObjectMapper objectMapper;

    @MockitoBean
    AuditLogRecordingMonitor recordingMonitor;

    @Test
    @DisplayName("감사 이벤트의 계약 필드와 생성 시각을 저장한다")
    void 감사_이벤트의_계약_필드와_생성_시각을_저장한다() {
        // given
        Instant beforeSave = Instant.now();
        AuditLogEvent event = AuditLogEvent.builder()
            .occurredAt(Instant.parse("2026-01-01T00:00:00Z"))
            .domain(Domain.CHALLENGER)
            .action(AuditAction.CREATE)
            .targetType("ChallengerRecord")
            .targetId("42")
            .actorMemberId(73L)
            .details(Map.of())
            .ipAddress("198.51.100.7")
            .build();

        // when
        auditLogCommandService.save(event);
        entityManager.flush();
        entityManager.clear();

        // then
        AuditLog stored = auditLogJpaRepository.findAll().getFirst();
        assertThat(stored.getDomain()).isEqualTo(Domain.CHALLENGER);
        assertThat(stored.getAction()).isEqualTo(AuditAction.CREATE);
        assertThat(stored.getTargetType()).isEqualTo("ChallengerRecord");
        assertThat(stored.getTargetId()).isEqualTo("42");
        assertThat(stored.getActorMemberId()).isEqualTo(73L);
        assertThat(stored.getIpAddress()).isEqualTo("198.51.100.7");
        assertThat(stored.getOutcome()).isEqualTo(AuditOutcome.SUCCESS);
        assertThat(stored.getSource()).isEqualTo(AuditSource.ANNOTATION);
        assertThat(stored.getRequestId()).isNull();
        assertThat(stored.getTraceId()).isNull();
        assertThat(stored.getCreatedAt()).isBetween(beforeSave, Instant.now());
    }

    @Test
    @DisplayName("신규 감사 필드와 기존 details 타입을 영속화 후 복원한다")
    void 신규_감사_필드와_기존_details_타입을_영속화_후_복원한다() throws Exception {
        // given
        AuditLogEvent event = AuditLogEvent.builder()
            .domain(Domain.AUTHENTICATION)
            .action(AuditAction.LOGIN)
            .targetType("Authentication")
            .targetId("member-73")
            .details(Map.of("reason", "invalid_credentials"))
            .outcome(AuditOutcome.FAILURE)
            .source(AuditSource.AUTHENTICATION_SERVICE)
            .requestId("request-123")
            .traceId("0123456789abcdef0123456789abcdef")
            .build();

        // when
        auditLogCommandService.save(event);
        entityManager.flush();
        entityManager.clear();

        // then
        AuditLog stored = auditLogJpaRepository.findAll().getFirst();
        assertThat(stored.getOutcome()).isEqualTo(AuditOutcome.FAILURE);
        assertThat(stored.getSource()).isEqualTo(AuditSource.AUTHENTICATION_SERVICE);
        assertThat(stored.getRequestId()).isEqualTo("request-123");
        assertThat(stored.getTraceId()).isEqualTo("0123456789abcdef0123456789abcdef");
        JsonNode details = objectMapper.readTree(stored.getDetails());
        assertThat(details.path("schemaVersion").asInt()).isEqualTo(1);
        assertThat(details.path("context").path("reason").asText())
            .isEqualTo("invalid_credentials");
        assertThat(details.has("reason")).isFalse();
    }

    @Test
    @DisplayName("실제 PostgreSQL read-back에서도 허용 details scalar의 hostile 값을 저장하지 않는다")
    void 실제_PostgreSQL_read_back에서도_허용_details_scalar의_hostile_값을_저장하지_않는다()
        throws Exception {
        // given
        String hostile = "Bearer raw-token password=raw-password body=raw-body "
            + "authorization=raw-authorization secret=raw-secret\r\nforged-entry";
        Map<String, Object> details = new LinkedHashMap<>();
        details.put("actor", Map.of("name", "홍길동", "nickname", hostile));
        details.put("target", Map.of("type", "Schedule", "name", "정기 세션", "title", hostile));
        details.put("context", Map.of("reason", hostile));
        details.put("before", Map.of("status", hostile));
        details.put("after", Map.of("status", hostile));
        AuditLogEvent event = AuditLogEvent.builder()
            .domain(Domain.SCHEDULE)
            .action(AuditAction.UPDATE)
            .targetType("Schedule")
            .targetId("10")
            .details(details)
            .build();

        // when
        auditLogCommandService.save(event);
        entityManager.flush();
        entityManager.clear();

        // then
        AuditLog stored = auditLogJpaRepository.findAll().getFirst();
        String storedDetails = stored.getDetails();
        JsonNode sanitized = objectMapper.readTree(storedDetails);
        assertThat(storedDetails)
            .doesNotContain(
                "Bearer",
                "raw-token",
                "raw-password",
                "raw-body",
                "raw-authorization",
                "raw-secret",
                "forged-entry",
                "\r",
                "\n"
            );
        assertThat(sanitized.path("actor").path("name").asText()).isEqualTo("홍길동");
        assertThat(sanitized.path("target").path("name").asText()).isEqualTo("정기 세션");
        assertThat(sanitized.path("actor").path("nickname").asText()).isEqualTo("[REDACTED]");
        assertThat(sanitized.path("target").path("title").asText()).isEqualTo("[REDACTED]");
        assertThat(sanitized.path("context").path("reason").asText()).isEqualTo("[REDACTED]");
        assertThat(sanitized.path("before").path("status").asText()).isEqualTo("[REDACTED]");
        assertThat(sanitized.path("after").path("status").asText()).isEqualTo("[REDACTED]");
    }

    @Test
    @DisplayName("빈 details는 기존 legacy null 표현으로 저장한다")
    void 빈_details는_기존_legacy_null_표현으로_저장한다() {
        // given
        AuditLogEvent event = AuditLogEvent.builder()
            .domain(Domain.MEMBER)
            .action(AuditAction.UPDATE)
            .targetType("MemberProfile")
            .targetId("7")
            .details(Map.of())
            .build();

        // when
        auditLogCommandService.save(event);
        entityManager.flush();
        entityManager.clear();

        // then
        AuditLog stored = auditLogJpaRepository.findAll().getFirst();
        assertThat(stored.getDetails()).isNull();
    }

    @TestConfiguration(proxyBeanMethods = false)
    static class ObjectMapperTestConfiguration {

        @Bean
        ObjectMapper objectMapper() {
            return new ObjectMapper();
        }
    }
}
