package com.umc.product.audit.application.service.command;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.umc.product.audit.application.port.out.SaveAuditLogPort;
import com.umc.product.audit.domain.AuditAction;
import com.umc.product.audit.domain.AuditDetails;
import com.umc.product.audit.domain.AuditLog;
import com.umc.product.audit.domain.AuditLogEvent;
import com.umc.product.global.exception.constant.Domain;
import com.umc.product.global.logging.OperationalMetrics;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;

@DisplayName("AuditLogCommandService details 직렬화")
class AuditLogCommandServiceDetailsTest {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final CapturingSaveAuditLogPort saveAuditLogPort = new CapturingSaveAuditLogPort();
    private final SimpleMeterRegistry meterRegistry = new SimpleMeterRegistry();
    private final AuditLogCommandService service = new AuditLogCommandService(
        saveAuditLogPort,
        objectMapper,
        new AuditLogRecordingMonitor(new OperationalMetrics(meterRegistry))
    );

    @Test
    @DisplayName("versioned details를 실제 JSON 문자열로 저장한다")
    void versioned_details를_실제_JSON_문자열로_저장한다() throws Exception {
        // given
        AuditDetails details = AuditDetails.of(
            AuditDetails.Actor.from(Map.of("memberId", 7L, "name", "운영자")),
            AuditDetails.Target.from(Map.of("type", "Schedule", "id", "10", "status", "OPEN")),
            AuditDetails.Context.from(Map.of("requestId", "request-7")),
            AuditDetails.State.empty(),
            AuditDetails.State.empty()
        );
        AuditLogEvent event = eventWithDetails(details.toMap());

        // when
        service.save(event);

        // then
        JsonNode json = objectMapper.readTree(saveAuditLogPort.saved().getDetails());
        assertThat(json.path("schemaVersion").asInt()).isEqualTo(1);
        assertThat(json.path("actor").path("memberId").asLong()).isEqualTo(7L);
        assertThat(json.path("target").path("status").asText()).isEqualTo("OPEN");
        assertThat(json.path("context").path("requestId").asText()).isEqualTo("request-7");
    }

    @Test
    @DisplayName("legacy null details는 null 문자열 계약을 유지한다")
    void legacy_null_details는_null_문자열_계약을_유지한다() {
        // given
        AuditLogEvent event = eventWithDetails(null);

        // when
        service.save(event);

        // then
        assertThat(saveAuditLogPort.saved().getDetails()).isNull();
    }

    @Test
    @DisplayName("legacy empty details는 null 문자열 계약을 유지한다")
    void legacy_empty_details는_null_문자열_계약을_유지한다() {
        // given
        AuditLogEvent event = eventWithDetails(Map.of());

        // when
        service.save(event);

        // then
        assertThat(saveAuditLogPort.saved().getDetails()).isNull();
    }

    @Test
    @DisplayName("schemaVersion 없는 shape은 version 1 context로 정규화한다")
    void schemaVersion_없는_shape은_version_1_context로_정규화한다() throws Exception {
        // given
        Map<String, Object> nested = new LinkedHashMap<>();
        nested.put("status", "OPEN");
        nested.put("Pass_Word", "secret");
        nested.put("verification-code", "123456");
        AuditLogEvent event = eventWithDetails(Map.of(
            "reason", "invalid_credentials",
            "history", List.of(nested)
        ));

        // when
        service.save(event);

        // then
        JsonNode json = objectMapper.readTree(saveAuditLogPort.saved().getDetails());
        assertThat(json.path("schemaVersion").asInt()).isEqualTo(1);
        assertThat(json.path("context").path("reason").asText()).isEqualTo("invalid_credentials");
        assertThat(json.has("reason")).isFalse();
        assertThat(json.has("history")).isFalse();
        assertThat(json.toString()).doesNotContain("Pass_Word", "verification-code", "secret", "123456");
    }

    @Test
    @DisplayName("schemaVersion 없는 신규 Map도 version 1 표준 root로 저장한다")
    void schemaVersion_없는_신규_Map도_version_1_표준_root로_저장한다() throws Exception {
        // given
        AuditLogEvent event = eventWithDetails(Map.of(
            "actor", Map.of("name", "운영자"),
            "rogue", "kept"
        ));

        // when
        service.save(event);

        // then
        JsonNode json = objectMapper.readTree(saveAuditLogPort.saved().getDetails());
        assertThat(json.path("schemaVersion").asInt()).isEqualTo(1);
        assertThat(json.path("actor").path("name").asText()).isEqualTo("운영자");
        assertThat(json.has("rogue")).isFalse();
        assertThat(json.size()).isEqualTo(6);
        assertThat(json.has("target")).isTrue();
        assertThat(json.has("context")).isTrue();
        assertThat(json.has("before")).isTrue();
        assertThat(json.has("after")).isTrue();
    }

    @Test
    @DisplayName("중첩 POJO record list array를 structured tree로 정규화해 금지 키를 제거한다")
    void 중첩_직렬화_객체_전체에서_금지_키를_제거한다() throws Exception {
        // given
        String jsonLookingText = "{\"password\":\"free-text\"}";
        Map<String, Object> context = new LinkedHashMap<>();
        context.put("reason", jsonLookingText);
        context.put("history", List.of(new TargetPayload()));
        AuditLogEvent event = eventWithDetails(Map.of(
            "actor", new ActorPayload("운영자", "record-secret"),
            "target", new TargetPayload(),
            "context", context,
            "before", Map.of("status", new TargetPayload[]{new TargetPayload()})
        ));

        // when
        service.save(event);

        // then
        JsonNode json = objectMapper.readTree(saveAuditLogPort.saved().getDetails());
        assertThat(json.path("actor").path("name").asText()).isEqualTo("운영자");
        assertThat(json.path("target").path("type").asText()).isEqualTo("Schedule");
        assertThat(json.path("target").path("status").asText()).isEqualTo("OPEN");
        assertThat(json.path("context").path("reason").asText()).isEqualTo("[REDACTED]");
        assertThat(json.path("context").has("history")).isFalse();
        assertThat(json.path("before").has("status")).isFalse();
        assertThat(json.findValue("password")).isNull();
        assertThat(json.findValue("token")).isNull();
    }

    @Test
    @DisplayName("실제 ObjectMapper 직렬화 실패가 감사 저장과 원 요청을 깨지 않는다")
    void 실제_ObjectMapper_직렬화_실패를_격리한다() {
        // given
        AuditLogEvent event = eventWithDetails(Map.of("reason", new BrokenJsonValue()));

        // when & then
        assertThatCode(() -> service.save(event)).doesNotThrowAnyException();
        assertThat(saveAuditLogPort.saved().getDetails()).isNull();
        assertThat(meterRegistry.get("operational.audit.log.failure.total")
            .tags(
                "domain", "AUTHENTICATION",
                "action", "LOGIN",
                "reason", "details_serialization"
            )
            .counter()
            .count()).isEqualTo(1D);
    }

    private static AuditLogEvent eventWithDetails(Map<String, Object> details) {
        return AuditLogEvent.builder()
            .domain(Domain.AUTHENTICATION)
            .action(AuditAction.LOGIN)
            .targetType("Authentication")
            .targetId("member-7")
            .details(details)
            .build();
    }

    private static final class CapturingSaveAuditLogPort implements SaveAuditLogPort {

        private AuditLog saved;

        @Override
        public AuditLog save(AuditLog auditLog) {
            saved = auditLog;
            return auditLog;
        }

        AuditLog saved() {
            return saved;
        }
    }

    private static final class BrokenJsonValue {

        public String getValue() {
            throw new IllegalStateException("직렬화 실패 probe");
        }
    }

    private record ActorPayload(String name, String password) {
    }

    private static final class TargetPayload {

        public String getType() {
            return "Schedule";
        }

        public String getStatus() {
            return "OPEN";
        }

        public String getToken() {
            return "pojo-secret";
        }
    }
}
