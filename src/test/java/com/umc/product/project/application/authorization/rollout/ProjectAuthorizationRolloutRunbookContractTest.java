package com.umc.product.project.application.authorization.rollout;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.umc.product.project.application.authorization.ProjectPolicyAction;

class ProjectAuthorizationRolloutRunbookContractTest {

    private static final Path RUNBOOK = Path.of(
        "docs/onboarding/project/project-authorization-rollout-runbook.md");
    private static final String CONTRACT_SCHEMA = "project-rollout-runbook-1.0";

    @Test
    @DisplayName("runbook machine contract는 exact39 wave와 운영 임계값을 고정한다")
    void runbook_machine_contract는_exact39_wave와_운영_임계값을_고정한다() throws Exception {
        assertThat(Files.exists(RUNBOOK)).isTrue();
        JsonNode contract = contract(Files.readString(RUNBOOK));

        assertThat(contract.path("defaultMode").textValue()).isEqualTo("SHADOW");
        assertThat(contract.path("receiptResource").textValue())
            .isEqualTo("policies/project/rollout/enforcement-receipts.json");
        assertThat(contract.path("metrics").path("decision").textValue())
            .isEqualTo("project.authorization.decision.total");
        assertThat(contract.path("metrics").path("evaluation").textValue())
            .isEqualTo("project.authorization.evaluation.seconds");
        assertThat(contract.path("metrics").path("prometheusDecision").textValue())
            .isEqualTo("project_authorization_decision_total");
        assertThat(contract.path("metrics").path("prometheusEvaluationBucket").textValue())
            .isEqualTo("project_authorization_evaluation_seconds_bucket");
        assertThat(textSet(contract.path("metrics").path("tags")))
            .containsExactlyInAnyOrder(
                "action",
                "mode",
                "classification",
                "schemaVersion",
                "contextSchemaVersion",
                "policyVersion",
                "policyFingerprint",
                "evaluationPoint"
            );
        assertThat(contract.path("promotion").path("observationHours").longValue()).isEqualTo(24);
        assertThat(contract.path("promotion").path("unexpectedDifferenceMax").longValue()).isZero();
        assertThat(contract.path("promotion").path("targetFailureMax").longValue()).isZero();
        assertThat(contract.path("promotion").path("unauthorizedExpansionMax").longValue()).isZero();
        assertThat(contract.path("promotion").path("p95IncreasePercentMax").longValue()).isEqualTo(10);
        assertThat(contract.path("promotion").path("legacyRetentionDaysAfterFullEnforce").longValue())
            .isEqualTo(7);
        assertThat(contract.path("promotion").path("automaticEnforceTransition").booleanValue()).isFalse();
        assertThat(contract.path("currentShippedState").path("receiptCount").longValue()).isZero();
        assertThat(contract.path("currentShippedState").path("enforcedActionCount").longValue()).isZero();
        assertThat(contract.path("currentShippedState").path("observationSatisfied").booleanValue()).isFalse();
        assertThat(contract.path("currentShippedState").path("legacyRemovalEligible").booleanValue()).isFalse();
        assertThat(textSet(contract.path("receiptContract").path("fields")))
            .containsExactlyInAnyOrder(
                "policyVersion",
                "policyFingerprint",
                "artifactSha256",
                "wave",
                "actions",
                "approver",
                "approvedAt",
                "expiresAt"
            );
        assertThat(contract.path("receiptContract").path("wildcardAllowed").booleanValue()).isFalse();
        assertThat(contract.path("receiptContract").path("externalPathAllowed").booleanValue()).isFalse();
        assertThat(contract.path("receiptContract").path("expiryRequired").booleanValue()).isTrue();
        assertThat(contract.path("receiptContract").path("staleReceiptRejected").booleanValue()).isTrue();
        assertThat(contract.path("rollback").path("startupCompileFailureAction").textValue())
            .isEqualTo("PREVIOUS_IMAGE_ROLLBACK");
        assertThat(contract.path("rollback").path("behaviorRegressionAction").textValue())
            .isEqualTo("CONFIG_ROLLBACK_TO_SHADOW_OR_LEGACY");
        assertThat(contract.path("rollback").path("owner").textValue())
            .isEqualTo("Project backend on-call and authorization policy owner");
        assertThat(contract.path("rollback").path("configCommand").textValue())
            .startsWith("kubectl -n umc-product set env deployment/umc-product-server")
            .contains("default-mode\":\"SHADOW", "action-overrides\":[]");
        assertThat(contract.path("rollback").path("previousImageCommand").textValue())
            .isEqualTo("kubectl -n umc-product set image deployment/umc-product-server app=<previous-image-reference>");
        assertThat(contract.path("queries").path("expectedDifference").textValue())
            .contains("project_authorization_decision_total", "EXPECTED_DIFFERENCE", "24h");
        assertThat(contract.path("queries").path("unexpectedDifference").textValue())
            .contains("project_authorization_decision_total", "UNEXPECTED_DIFFERENCE", "24h");
        assertThat(contract.path("queries").path("targetFailure").textValue())
            .contains("project_authorization_decision_total", "TARGET_FAILURE", "24h");
        assertThat(contract.path("queries").path("p95").textValue())
            .contains("project_authorization_evaluation_seconds_bucket", "0.95");
        assertThat(contract.path("queries").path("unauthorizedExpansionLog").textValue())
            .contains("targetPrivilegeExpansion", "YES", "EXPECTED_DIFFERENCE");

        Map<ProjectAuthorizationEnforcementWave, Set<ProjectPolicyAction>> actual = waves(contract);
        assertThat(actual.keySet()).containsExactlyInAnyOrder(ProjectAuthorizationEnforcementWave.values());
        for (ProjectAuthorizationEnforcementWave wave : ProjectAuthorizationEnforcementWave.values()) {
            assertThat(actual.get(wave)).containsExactlyInAnyOrderElementsOf(wave.actions());
        }
        Set<ProjectPolicyAction> union = EnumSet.noneOf(ProjectPolicyAction.class);
        actual.values().forEach(union::addAll);
        assertThat(union).containsExactlyInAnyOrder(ProjectPolicyAction.values());
    }

    private JsonNode contract(String markdown) throws Exception {
        String marker = "\"schemaVersion\": \"" + CONTRACT_SCHEMA + "\"";
        int markerIndex = markdown.indexOf(marker);
        assertThat(markerIndex).isGreaterThanOrEqualTo(0);
        int fenceStart = markdown.lastIndexOf("```json", markerIndex);
        int jsonStart = markdown.indexOf('\n', fenceStart) + 1;
        int fenceEnd = markdown.indexOf("```", markerIndex);
        JsonNode root = JsonMapper.builder().build().readTree(markdown.substring(jsonStart, fenceEnd));
        assertThat(root.path("schemaVersion").textValue()).isEqualTo(CONTRACT_SCHEMA);
        return root;
    }

    private Map<ProjectAuthorizationEnforcementWave, Set<ProjectPolicyAction>> waves(JsonNode contract) {
        Map<ProjectAuthorizationEnforcementWave, Set<ProjectPolicyAction>> result =
            new EnumMap<>(ProjectAuthorizationEnforcementWave.class);
        for (JsonNode waveNode : contract.path("waves")) {
            ProjectAuthorizationEnforcementWave wave =
                ProjectAuthorizationEnforcementWave.valueOf(waveNode.path("wave").textValue());
            Set<ProjectPolicyAction> actions = EnumSet.noneOf(ProjectPolicyAction.class);
            Iterator<JsonNode> values = waveNode.path("actions").elements();
            values.forEachRemaining(value -> actions.add(ProjectPolicyAction.fromId(value.textValue())));
            assertThat(result.put(wave, actions)).isNull();
        }
        return result;
    }

    private Set<String> textSet(JsonNode array) {
        Set<String> result = new java.util.HashSet<>();
        array.elements().forEachRemaining(value -> result.add(value.textValue()));
        return result;
    }
}
