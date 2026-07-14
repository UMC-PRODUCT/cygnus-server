package com.umc.product.audit.adapter.in.event;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.yaml.snakeyaml.Yaml;

@DisplayName("감사 로그 monitoring rule")
class AuditLogMetricsMonitoringRuleTest {

    private static final Path RULE_FILE = Path.of(
        "infra/monitoring/grafana/config/prometheus/rules/default-alerts.yml"
    );

    @Test
    @DisplayName("감사 로그 저장 실패 alert rule은 파싱 가능한 구조와 낮은 카디널리티를 유지한다")
    void audit_log_failure_alert_has_valid_low_cardinality_contract() throws IOException {
        Map<?, ?> rule = auditFailureRule();

        assertThat(rule.get("alert")).isEqualTo("AuditLogSaveFailures");
        assertThat(rule.get("for")).isEqualTo("0m");
        assertThat(rule.get("expr").toString())
            .isEqualTo("sum by (application, environment) "
                + "(increase(operational_audit_log_failure_total[5m])) > 0")
            .doesNotContain("reason", "action", "target", "request", "trace");
    }

    private Map<?, ?> auditFailureRule() throws IOException {
        try (InputStream input = Files.newInputStream(RULE_FILE)) {
            Object document = new Yaml().load(input);
            Map<?, ?> root = assertMap(document);
            Map<?, ?> auditGroup = list(root.get("groups")).stream()
                .map(this::assertMap)
                .filter(group -> "umc-product.audit".equals(group.get("name")))
                .findFirst()
                .orElseThrow();
            return list(auditGroup.get("rules")).stream()
                .map(this::assertMap)
                .filter(rule -> "AuditLogSaveFailures".equals(rule.get("alert")))
                .findFirst()
                .orElseThrow();
        }
    }

    private Map<?, ?> assertMap(Object value) {
        assertThat(value).isInstanceOf(Map.class);
        return (Map<?, ?>) value;
    }

    private List<?> list(Object value) {
        assertThat(value).isInstanceOf(List.class);
        return (List<?>) value;
    }
}
