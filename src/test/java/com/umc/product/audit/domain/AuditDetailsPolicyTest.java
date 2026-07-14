package com.umc.product.audit.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

@DisplayName("감사 details 민감정보 정책")
class AuditDetailsPolicyTest {

    private static final Set<String> FORBIDDEN_KEYS = Set.of(
        "email", "password", "token", "authorization", "providerid", "oauthsubject",
        "code", "verificationcode", "authcode", "rawbody", "content", "body"
    );

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    @DisplayName("schemaVersion 1과 actor target context before after 표준 shape을 JSON으로 제공한다")
    void schemaVersion_1과_표준_shape을_JSON으로_제공한다() throws Exception {
        // given
        AuditDetails details = AuditDetails.of(
            AuditDetails.Actor.from(Map.of(
                "type", "USER", "memberId", 1L, "name", "홍길동",
                "nickname", "길동", "schoolName", "한국대학교"
            )),
            AuditDetails.Target.from(Map.of(
                "type", "Schedule", "id", "10", "name", "정기 세션", "status", "OPEN"
            )),
            AuditDetails.Context.from(Map.of("requestId", "request-123")),
            AuditDetails.State.from(Map.of("status", "DRAFT")),
            AuditDetails.State.from(Map.of("status", "OPEN"))
        );

        // when
        JsonNode json = objectMapper.readTree(objectMapper.writeValueAsString(details.toMap()));

        // then
        assertThat(fieldNames(json)).containsExactly(
            "schemaVersion", "actor", "target", "context", "before", "after"
        );
        assertThat(json.path("schemaVersion").asInt()).isEqualTo(1);
        assertThat(json.path("actor").path("memberId").asLong()).isEqualTo(1L);
        assertThat(json.path("target").path("name").asText()).isEqualTo("정기 세션");
        assertThat(json.path("context").path("requestId").asText()).isEqualTo("request-123");
        assertThat(json.path("before").path("status").asText()).isEqualTo("DRAFT");
        assertThat(json.path("after").path("status").asText()).isEqualTo("OPEN");
    }

    @Test
    @DisplayName("section allowlist 밖의 필드는 구성 결과에서 제외한다")
    void section_allowlist_밖의_필드는_제외한다() throws Exception {
        // given
        AuditDetails details = AuditDetails.of(
            AuditDetails.Actor.from(Map.of(
                "member_id", 7L,
                "name", "운영자",
                "department", "server"
            )),
            AuditDetails.Target.from(Map.of(
                "type", "Schedule",
                "status", "OPEN",
                "permission", "DELETE"
            )),
            AuditDetails.Context.from(Map.of(
                "request_id", "request-7",
                "schoolName", "허용되지 않는 위치"
            )),
            AuditDetails.State.empty(),
            AuditDetails.State.empty()
        );

        // when
        JsonNode json = objectMapper.readTree(objectMapper.writeValueAsString(details.toMap()));

        // then
        assertThat(fieldNames(json.path("actor"))).containsExactly("memberId", "name");
        assertThat(fieldNames(json.path("target"))).containsExactly("type", "status");
        assertThat(fieldNames(json.path("context"))).containsExactly("requestId");
    }

    @Test
    @DisplayName("schemaVersion 없는 중첩 Map도 표준 shape과 금지 키 정책을 적용한다")
    void schemaVersion_없는_중첩_Map도_표준_shape과_금지_키_정책을_적용한다() throws Exception {
        // given
        Map<String, Object> nested = new LinkedHashMap<>();
        nested.put("type", "Schedule");
        nested.put("status", "OPEN");
        nested.put("statusCode", 200);
        nested.put("E_Mail", "member@example.com");
        nested.put("Pass-Word", "secret");
        nested.put("TO.KEN", "token-value");
        nested.put("AUTHORIZATION", "Bearer secret");
        nested.put("provider_id", "provider-secret");
        nested.put("oauth-subject", "oauth-secret");
        nested.put("C O D E", "123456");
        nested.put("verification_code", "654321");
        nested.put("auth-code", "111111");
        nested.put("raw_body", Map.of("body", "request-body"));
        nested.put("CONTENT", "post-content");
        nested.put("BoDy", "comment-body");
        Map<String, Object> input = Map.of("target", nested);

        // when
        Map<String, Object> sanitized = AuditDetailsPolicy.sanitizeForStorage(input);
        JsonNode json = objectMapper.readTree(objectMapper.writeValueAsString(sanitized));

        // then
        assertThat(json.path("schemaVersion").asInt()).isEqualTo(1);
        assertThat(json.path("target").path("type").asText()).isEqualTo("Schedule");
        assertThat(json.path("target").path("status").asText()).isEqualTo("OPEN");
        assertThat(json.path("target").has("statusCode")).isFalse();
        assertThat(json.size()).isEqualTo(6);
        assertThat(normalizedFieldNames(json)).doesNotContainAnyElementsOf(FORBIDDEN_KEYS);
    }

    @Test
    @DisplayName("일반 POJO getter가 생성하는 금지 키도 실제 JSON에 남기지 않는다")
    void 일반_POJO_getter가_생성하는_금지_키를_제거한다() throws Exception {
        // given
        Map<String, Object> input = Map.of("reason", new SecretValue());

        // when
        Map<String, Object> sanitized = AuditDetailsPolicy.sanitizeForStorage(input);
        JsonNode json = objectMapper.readTree(objectMapper.writeValueAsString(sanitized));

        // then
        assertThat(json.findValue("password")).isNull();
        assertThat(json.toString()).doesNotContain("secret");
    }

    @Test
    @DisplayName("null과 blank는 제외하고 긴 표시값은 정책 길이로 제한한다")
    void null_blank_긴_표시값_경계를_적용한다() throws Exception {
        // given
        Map<String, Object> actor = new LinkedHashMap<>();
        actor.put("name", "가".repeat(300));
        actor.put("nickname", "   ");
        actor.put("schoolName", null);
        AuditDetails details = AuditDetails.of(
            AuditDetails.Actor.from(actor),
            AuditDetails.Target.empty(),
            AuditDetails.Context.empty(),
            AuditDetails.State.empty(),
            AuditDetails.State.empty()
        );

        // when
        JsonNode json = objectMapper.readTree(objectMapper.writeValueAsString(details.toMap()));

        // then
        assertThat(json.path("actor").path("name").asText()).hasSize(255);
        assertThat(json.path("actor").has("nickname")).isFalse();
        assertThat(json.path("actor").has("schoolName")).isFalse();
    }

    @Test
    @DisplayName("구조처럼 보이는 자유 텍스트도 민감값 고정 redaction을 적용한다")
    void 구조처럼_보이는_자유_텍스트도_민감값_고정_redaction을_적용한다() throws Exception {
        // given
        String displayName = "{\"password\":\"secret\",\"token\":\"value\"}";
        AuditDetails details = AuditDetails.of(
            AuditDetails.Actor.empty(),
            AuditDetails.Target.from(Map.of("name", displayName, "type", "Schedule")),
            AuditDetails.Context.empty(),
            AuditDetails.State.empty(),
            AuditDetails.State.empty()
        );

        // when
        JsonNode json = objectMapper.readTree(objectMapper.writeValueAsString(details.toMap()));

        // then
        assertThat(json.path("target").path("name").asText()).isEqualTo("[REDACTED]");
        assertThat(normalizedFieldNames(json)).doesNotContainAnyElementsOf(FORBIDDEN_KEYS);
    }

    @Test
    @DisplayName("허용 section scalar의 secret body와 로그 개행 payload를 저장하지 않는다")
    void 허용_section_scalar의_민감값과_로그_개행_payload를_저장하지_않는다() throws Exception {
        // given
        String hostile = "Bearer raw-token password=raw-password body=raw-body "
            + "authorization=raw-authorization secret=raw-secret\r\nforged-entry";
        Map<String, Object> input = Map.of(
            "actor", Map.of("name", "홍길동", "nickname", hostile),
            "target", Map.of("type", "Schedule", "name", "정기 세션", "title", hostile),
            "context", Map.of("reason", hostile),
            "before", Map.of("status", hostile),
            "after", Map.of("status", hostile)
        );

        // when
        Map<String, Object> sanitized = AuditDetailsPolicy.sanitizeForStorage(input);
        String json = objectMapper.writeValueAsString(sanitized);

        // then
        assertThat(json)
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
            )
            .contains("홍길동", "정기 세션", "[REDACTED]");
        assertThat(sectionValue(sanitized, "actor", "nickname")).isEqualTo("[REDACTED]");
        assertThat(sectionValue(sanitized, "target", "title")).isEqualTo("[REDACTED]");
        assertThat(sectionValue(sanitized, "context", "reason")).isEqualTo("[REDACTED]");
        assertThat(sectionValue(sanitized, "before", "status")).isEqualTo("[REDACTED]");
        assertThat(sectionValue(sanitized, "after", "status")).isEqualTo("[REDACTED]");
    }

    private static List<String> fieldNames(JsonNode node) {
        List<String> names = new ArrayList<>();
        node.fieldNames().forEachRemaining(names::add);
        return names;
    }

    private static Set<String> normalizedFieldNames(JsonNode node) {
        Set<String> names = new java.util.HashSet<>();
        collectNormalizedFieldNames(node, names);
        return names;
    }

    private static Object sectionValue(Map<String, Object> details, String section, String key) {
        return ((Map<?, ?>) details.get(section)).get(key);
    }

    private static void collectNormalizedFieldNames(JsonNode node, Set<String> names) {
        if (node.isObject()) {
            node.fieldNames().forEachRemaining(fieldName -> {
                names.add(normalize(fieldName));
                collectNormalizedFieldNames(node.get(fieldName), names);
            });
        } else if (node.isArray()) {
            node.forEach(child -> collectNormalizedFieldNames(child, names));
        }
    }

    private static String normalize(String key) {
        return key.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]", "");
    }

    private static final class SecretValue {

        public String getPassword() {
            return "secret";
        }

        public String getStatus() {
            return "OPEN";
        }
    }
}
