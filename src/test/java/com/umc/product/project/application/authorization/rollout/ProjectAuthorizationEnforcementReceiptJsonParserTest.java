package com.umc.product.project.application.authorization.rollout;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.nio.charset.StandardCharsets;
import java.util.stream.Collectors;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import com.umc.product.project.application.authorization.ProjectPolicyAction;

class ProjectAuthorizationEnforcementReceiptJsonParserTest {

    private final ProjectAuthorizationEnforcementReceiptJsonParser parser =
        new ProjectAuthorizationEnforcementReceiptJsonParser();

    @Test
    @DisplayName("유효한 receipt 문서를 typed receipt로 해석한다")
    void 유효한_receipt_문서를_typed_receipt로_해석한다() {
        ProjectAuthorizationEnforcementReceiptDocument document = parser.parse(document(receipt(
            ProjectAuthorizationEnforcementWave.READ_CAPABILITY,
            "operator@umc.example",
            "2026-07-14T00:00:00Z",
            "2026-07-15T00:00:00Z"
        )).getBytes(StandardCharsets.UTF_8));

        assertThat(document.schemaVersion()).isEqualTo("1.0");
        assertThat(document.receipts()).singleElement().satisfies(value -> {
            assertThat(value.wave()).isEqualTo(ProjectAuthorizationEnforcementWave.READ_CAPABILITY);
            assertThat(value.actions())
                .containsExactlyInAnyOrderElementsOf(ProjectAuthorizationEnforcementWave.READ_CAPABILITY.actions());
        });
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("invalidJsonDocuments")
    @DisplayName("strict JSON 계약을 위반한 receipt를 stable code로 거부한다")
    void strict_json_계약을_위반한_receipt를_거부한다(
        String name,
        String json,
        ProjectAuthorizationRolloutFailureCode expectedCode
    ) {
        assertThatThrownBy(() -> parser.parse(json.getBytes(StandardCharsets.UTF_8)))
            .isInstanceOfSatisfying(
                ProjectAuthorizationRolloutConfigurationException.class,
                exception -> assertThat(exception.code()).isEqualTo(expectedCode)
            );
    }

    @Test
    @DisplayName("크기 제한을 초과한 receipt 문서를 거부한다")
    void 크기_제한을_초과한_receipt_문서를_거부한다() {
        byte[] bytes = new byte[ProjectAuthorizationEnforcementReceiptJsonParser.MAX_BYTES + 1];

        assertThatThrownBy(() -> parser.parse(bytes))
            .isInstanceOfSatisfying(
                ProjectAuthorizationRolloutConfigurationException.class,
                exception -> assertThat(exception.code())
                    .isEqualTo(ProjectAuthorizationRolloutFailureCode.RECEIPT_DOCUMENT_TOO_LARGE)
            );
    }

    @Test
    @DisplayName("receipt action은 wave 전체 exact set이어야 한다")
    void receipt_action은_wave_전체_exact_set이어야_한다() {
        String partial = receipt(ProjectAuthorizationEnforcementWave.READ_CAPABILITY,
            "operator@umc.example", "2026-07-14T00:00:00Z", "2026-07-15T00:00:00Z")
            .replace(",\"project:capability-list\"", "");

        assertCode(document(partial), ProjectAuthorizationRolloutFailureCode.RECEIPT_ACTION_SET_MISMATCH);
    }

    @Test
    @DisplayName("중복 wave receipt를 거부한다")
    void 중복_wave_receipt를_거부한다() {
        String value = receipt(ProjectAuthorizationEnforcementWave.READ_CAPABILITY,
            "operator@umc.example", "2026-07-14T00:00:00Z", "2026-07-15T00:00:00Z");

        assertCode(document(value + "," + value),
            ProjectAuthorizationRolloutFailureCode.RECEIPT_DUPLICATE_WAVE);
    }

    private static java.util.stream.Stream<Arguments> invalidJsonDocuments() {
        String valid = document(receipt(ProjectAuthorizationEnforcementWave.READ_CAPABILITY,
            "operator@umc.example", "2026-07-14T00:00:00Z", "2026-07-15T00:00:00Z"));
        return java.util.stream.Stream.of(
            Arguments.of("unknown field", valid.replace("\"receipts\"", "\"unknown\":true,\"receipts\""),
                ProjectAuthorizationRolloutFailureCode.RECEIPT_UNKNOWN_FIELD),
            Arguments.of("duplicate key", valid.replace("\"schemaVersion\":\"1.0\"",
                "\"schemaVersion\":\"1.0\",\"schemaVersion\":\"1.0\""),
                ProjectAuthorizationRolloutFailureCode.RECEIPT_DUPLICATE_KEY),
            Arguments.of("null", valid.replace("\"approver\":\"operator@umc.example\"", "\"approver\":null"),
                ProjectAuthorizationRolloutFailureCode.RECEIPT_NULL_NOT_ALLOWED),
            Arguments.of("coercion", valid.replace("\"schemaVersion\":\"1.0\"", "\"schemaVersion\":1"),
                ProjectAuthorizationRolloutFailureCode.RECEIPT_INVALID_FIELD_TYPE),
            Arguments.of("float", valid.replace("\"schemaVersion\":\"1.0\"", "\"schemaVersion\":1.0"),
                ProjectAuthorizationRolloutFailureCode.RECEIPT_FLOAT_NOT_ALLOWED),
            Arguments.of("comment", "{/*comment*/" + valid.substring(1),
                ProjectAuthorizationRolloutFailureCode.RECEIPT_MALFORMED_JSON),
            Arguments.of("trailing comma", valid.substring(0, valid.length() - 1) + ",}",
                ProjectAuthorizationRolloutFailureCode.RECEIPT_MALFORMED_JSON),
            Arguments.of("single quote", valid.replace('"', '\''),
                ProjectAuthorizationRolloutFailureCode.RECEIPT_MALFORMED_JSON),
            Arguments.of("trailing content", valid + "{}",
                ProjectAuthorizationRolloutFailureCode.RECEIPT_TRAILING_CONTENT),
            Arguments.of("unsupported schema", valid.replace("\"schemaVersion\":\"1.0\"",
                "\"schemaVersion\":\"2.0\""),
                ProjectAuthorizationRolloutFailureCode.RECEIPT_SCHEMA_UNSUPPORTED),
            Arguments.of("unsupported wave", valid.replace("READ_CAPABILITY", "UNKNOWN_WAVE"),
                ProjectAuthorizationRolloutFailureCode.RECEIPT_WAVE_UNSUPPORTED),
            Arguments.of("wildcard", valid.replace("project:read", "project:*"),
                ProjectAuthorizationRolloutFailureCode.RECEIPT_WILDCARD_ACTION),
            Arguments.of("unknown action", valid.replace("project:read", "project:unknown"),
                ProjectAuthorizationRolloutFailureCode.RECEIPT_ACTION_UNSUPPORTED),
            Arguments.of("duplicate action", valid.replace(
                "\"project:read\"", "\"project:read\",\"project:read\""),
                ProjectAuthorizationRolloutFailureCode.RECEIPT_DUPLICATE_ACTION),
            Arguments.of("invalid instant", valid.replace(
                "2026-07-14T00:00:00Z", "not-an-instant"),
                ProjectAuthorizationRolloutFailureCode.RECEIPT_INSTANT_INVALID),
            Arguments.of("blank approver", valid.replace("operator@umc.example", " "),
                ProjectAuthorizationRolloutFailureCode.RECEIPT_APPROVER_BLANK)
        );
    }

    private void assertCode(String json, ProjectAuthorizationRolloutFailureCode code) {
        assertThatThrownBy(() -> parser.parse(json.getBytes(StandardCharsets.UTF_8)))
            .isInstanceOfSatisfying(
                ProjectAuthorizationRolloutConfigurationException.class,
                exception -> assertThat(exception.code()).isEqualTo(code)
            );
    }

    static String document(String receipts) {
        return "{\"schemaVersion\":\"1.0\",\"receipts\":[" + receipts + "]}";
    }

    static String receipt(
        ProjectAuthorizationEnforcementWave wave,
        String approver,
        String approvedAt,
        String expiresAt
    ) {
        String actions = wave.actions().stream()
            .map(ProjectPolicyAction::id)
            .sorted()
            .map(value -> "\"" + value + "\"")
            .collect(Collectors.joining(","));
        return "{" +
            "\"policyVersion\":\"1.0.0\"," +
            "\"policyFingerprint\":\"ce61cd46827cf7b277d607d7626dfb2817b1f17893a324a3b949ec6582ecf2bb\"," +
            "\"artifactSha256\":\"2b76e2f5a4d6d8abab40940f75acc70e99e5ffd778a275ebc466886e8bb168d8\"," +
            "\"wave\":\"" + wave.name() + "\"," +
            "\"actions\":[" + actions + "]," +
            "\"approver\":\"" + approver + "\"," +
            "\"approvedAt\":\"" + approvedAt + "\"," +
            "\"expiresAt\":\"" + expiresAt + "\"}";
    }
}
