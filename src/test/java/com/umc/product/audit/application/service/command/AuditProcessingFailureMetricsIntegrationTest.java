package com.umc.product.audit.application.service.command;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import com.umc.product.audit.application.port.in.annotation.Audited;
import com.umc.product.audit.application.port.in.command.SaveAuditLogUseCase;
import com.umc.product.audit.domain.AuditAction;
import com.umc.product.audit.domain.AuditLogEvent;
import com.umc.product.global.exception.constant.Domain;
import com.umc.product.global.security.annotation.Public;
import com.umc.product.support.IntegrationTestSupport;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;

@Import(AuditProcessingFailureMetricsIntegrationTest.FailureMetricController.class)
@DisplayName("감사 처리 실패 metric 통합 계약")
class AuditProcessingFailureMetricsIntegrationTest extends IntegrationTestSupport {

    private static final String FAILURE_METRIC = "operational.audit.log.failure.total";

    @Autowired
    SaveAuditLogUseCase saveAuditLogUseCase;

    @Autowired
    MeterRegistry meterRegistry;

    @Test
    @DisplayName("details 직렬화 실패는 고정 reason metric을 증가시키고 저장은 계속한다")
    void detailsSerializationFailureIncrementsLowCardinalityMetric() {
        double before = failureCount("AUTHENTICATION", "LOGIN", "details_serialization");
        AuditLogEvent event = AuditLogEvent.builder()
            .domain(Domain.AUTHENTICATION)
            .action(AuditAction.LOGIN)
            .targetType("Authentication")
            .targetId("serialization-failure")
            .details(Map.of("context", Map.of("reason", new BrokenJsonValue())))
            .build();

        saveAuditLogUseCase.save(event);

        assertThat(failureCount("AUTHENTICATION", "LOGIN", "details_serialization"))
            .isEqualTo(before + 1D);
    }

    @Test
    @DisplayName("SpEL 평가 실패는 원 응답을 유지하고 고정 reason metric을 증가시킨다")
    void spelFailureIncrementsLowCardinalityMetricWithoutBreakingResponse() throws Exception {
        double before = failureCount("AUDIT_LOG", "CREATE", "spel_evaluation");

        mockMvc.perform(get("/test/audit/failure-metric/spel"))
            .andExpect(status().isOk());

        assertThat(failureCount("AUDIT_LOG", "CREATE", "spel_evaluation"))
            .isEqualTo(before + 1D);
    }

    private double failureCount(String domain, String action, String reason) {
        Counter counter = meterRegistry.find(FAILURE_METRIC)
            .tags("domain", domain, "action", action, "reason", reason)
            .counter();
        return counter == null ? 0D : counter.count();
    }

    static final class BrokenJsonValue {

        public String getValue() {
            throw new IllegalStateException("serialization probe");
        }
    }

    @RestController
    static class FailureMetricController {

        @Public
        @Audited(
            domain = Domain.AUDIT_LOG,
            action = AuditAction.CREATE,
            targetType = "MetricProbe",
            targetId = "#result.missing",
            description = "'감사 metric probe를 기록했습니다.'"
        )
        @GetMapping("/test/audit/failure-metric/spel")
        String triggerSpelFailure() {
            return "ok";
        }
    }
}
