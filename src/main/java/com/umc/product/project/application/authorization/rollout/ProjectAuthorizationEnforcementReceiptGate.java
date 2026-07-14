package com.umc.product.project.application.authorization.rollout;

import java.time.Clock;
import java.time.Instant;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

import com.umc.product.project.application.authorization.ProjectPolicyAction;

public final class ProjectAuthorizationEnforcementReceiptGate {

    private final ProjectAuthorizationEnforcementReceiptDocument document;
    private final ProjectAuthorizationEnforcementTarget target;
    private final Clock clock;

    public ProjectAuthorizationEnforcementReceiptGate(
        ProjectAuthorizationEnforcementReceiptDocument document,
        ProjectAuthorizationEnforcementTarget target,
        Clock clock
    ) {
        this.document = Objects.requireNonNull(document);
        this.target = Objects.requireNonNull(target);
        this.clock = Objects.requireNonNull(clock);
    }

    public void validate(ProjectAuthorizationRolloutConfiguration configuration) {
        Objects.requireNonNull(configuration);
        validateReceipts();
        Set<ProjectPolicyAction> enforcedActions = enforcedActions(configuration);
        Set<ProjectPolicyAction> receiptedActions = receiptedActions();
        if (!receiptedActions.containsAll(enforcedActions)) {
            fail(ProjectAuthorizationRolloutFailureCode.ENFORCEMENT_ACTION_SET_MISMATCH);
        }
    }

    private void validateReceipts() {
        if (!"1.0".equals(document.schemaVersion())) {
            fail(ProjectAuthorizationRolloutFailureCode.RECEIPT_SCHEMA_UNSUPPORTED);
        }
        Set<ProjectAuthorizationEnforcementWave> waves = EnumSet.noneOf(ProjectAuthorizationEnforcementWave.class);
        Set<ProjectPolicyAction> actions = EnumSet.noneOf(ProjectPolicyAction.class);
        Instant now = clock.instant();
        for (ProjectAuthorizationEnforcementReceipt receipt : document.receipts()) {
            if (!waves.add(receipt.wave())) {
                fail(ProjectAuthorizationRolloutFailureCode.RECEIPT_DUPLICATE_WAVE);
            }
            if (!receipt.actions().equals(receipt.wave().actions())) {
                fail(ProjectAuthorizationRolloutFailureCode.RECEIPT_ACTION_SET_MISMATCH);
            }
            Set<ProjectPolicyAction> overlap = new HashSet<>(actions);
            overlap.retainAll(receipt.actions());
            if (!overlap.isEmpty()) {
                fail(ProjectAuthorizationRolloutFailureCode.RECEIPT_OVERLAPPING_WAVE);
            }
            actions.addAll(receipt.actions());
            validateMetadata(receipt);
            validateTime(receipt, now);
        }
    }

    private void validateMetadata(ProjectAuthorizationEnforcementReceipt receipt) {
        if (!target.policyVersion().equals(receipt.policyVersion())) {
            fail(ProjectAuthorizationRolloutFailureCode.RECEIPT_POLICY_VERSION_MISMATCH);
        }
        if (!target.policyFingerprint().equals(receipt.policyFingerprint())) {
            fail(ProjectAuthorizationRolloutFailureCode.RECEIPT_POLICY_FINGERPRINT_MISMATCH);
        }
        if (!target.artifactSha256().equals(receipt.artifactSha256())) {
            fail(ProjectAuthorizationRolloutFailureCode.RECEIPT_ARTIFACT_SHA_MISMATCH);
        }
    }

    private void validateTime(ProjectAuthorizationEnforcementReceipt receipt, Instant now) {
        if (receipt.approvedAt().isAfter(now)) {
            fail(ProjectAuthorizationRolloutFailureCode.RECEIPT_APPROVED_AT_IN_FUTURE);
        }
        if (!receipt.approvedAt().isBefore(receipt.expiresAt())) {
            fail(ProjectAuthorizationRolloutFailureCode.RECEIPT_TIME_WINDOW_INVALID);
        }
        if (!now.isBefore(receipt.expiresAt())) {
            fail(ProjectAuthorizationRolloutFailureCode.RECEIPT_EXPIRED);
        }
    }

    private Set<ProjectPolicyAction> enforcedActions(ProjectAuthorizationRolloutConfiguration configuration) {
        Set<ProjectPolicyAction> result = EnumSet.noneOf(ProjectPolicyAction.class);
        for (ProjectPolicyAction action : ProjectPolicyAction.values()) {
            ProjectAuthorizationRolloutMode mode = configuration.actionOverrides()
                .getOrDefault(action, configuration.defaultMode());
            if (mode == ProjectAuthorizationRolloutMode.ENFORCE) {
                result.add(action);
            }
        }
        return result;
    }

    private Set<ProjectPolicyAction> receiptedActions() {
        Set<ProjectPolicyAction> result = EnumSet.noneOf(ProjectPolicyAction.class);
        document.receipts().forEach(receipt -> result.addAll(receipt.actions()));
        return result;
    }

    private static void fail(ProjectAuthorizationRolloutFailureCode code) {
        throw new ProjectAuthorizationRolloutConfigurationException(code);
    }
}
