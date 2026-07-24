package com.umc.product.authorization.application.service.policy.rollout;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class PolicyEnforcementReceiptJsonParserTest {

    private final PolicyEnforcementReceiptJsonParser parser =
        new PolicyEnforcementReceiptJsonParser();

    @Test
    @DisplayName("빈 공용 enforcement receipt를 strict parse한다")
    void parsesEmptyReceipt() {
        var document = parser.parse("""
            {
              "schemaVersion": "1.0",
              "namespace": "term",
              "receipts": []
            }
            """.getBytes(UTF_8));

        assertThat(document.namespace()).isEqualTo("term");
        assertThat(document.receipts()).isEmpty();
    }

    @Test
    @DisplayName("unknown field와 duplicate key와 null을 거부한다")
    void rejectsStrictJsonViolations() {
        assertInvalid("""
            {"schemaVersion":"1.0","namespace":"term","receipts":[],"unknown":true}
            """);
        assertInvalid("""
            {"schemaVersion":"1.0","namespace":"term","namespace":"term","receipts":[]}
            """);
        assertInvalid("""
            {"schemaVersion":"1.0","namespace":null,"receipts":[]}
            """);
    }

    @Test
    @DisplayName("wildcard action을 거부한다")
    void rejectsWildcardAction() {
        assertInvalid("""
            {
              "schemaVersion":"1.0",
              "namespace":"term",
              "receipts":[{
                "policyVersion":"1.0.0",
                "policyFingerprint":"fingerprint",
                "artifactSha256":"sha",
                "actions":["term:*"],
                "approver":"owner",
                "approvedAt":"2026-07-23T00:00:00Z",
                "expiresAt":"2026-07-25T00:00:00Z"
              }]
            }
            """);
    }

    private void assertInvalid(String json) {
        assertThatThrownBy(() -> parser.parse(json.getBytes(UTF_8)))
            .isInstanceOf(IllegalStateException.class);
    }
}
