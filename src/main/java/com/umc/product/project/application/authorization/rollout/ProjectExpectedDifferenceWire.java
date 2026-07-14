package com.umc.product.project.application.authorization.rollout;

import java.util.List;

import com.umc.product.authorization.domain.policy.PolicyEffect;
import com.umc.product.project.application.authorization.ProjectPolicyAction;

final class ProjectExpectedDifferenceWire {

    private ProjectExpectedDifferenceWire() {
    }

    record Document(List<Entry> entries) {
        Document {
            entries = List.copyOf(entries);
        }
    }

    record Entry(
        String id,
        ProjectExpectedDifferenceId testCaseId,
        ProjectPolicyAction action,
        ProjectAuthorizationInternalOrigin internalOrigin,
        ClassificationScope classificationScope,
        String predicateAttribute,
        Decision expectedLegacy,
        Decision expectedTarget,
        String reason,
        String owner
    ) {
    }

    record Decision(
        PolicyEffect effect,
        boolean capabilityAuthorized,
        List<Outcome> outcomes,
        List<Outcome> obligations
    ) {
        Decision {
            outcomes = List.copyOf(outcomes);
            obligations = List.copyOf(obligations);
        }
    }

    record Outcome(String key, Value value) {
    }

    sealed interface Value permits BooleanValue, EnumValue, LongSetAttributes, AttributeValue {
    }

    record BooleanValue(boolean value) implements Value {
    }

    record EnumValue(String value) implements Value {
    }

    record LongSetAttributes(List<String> attributes) implements Value {
        LongSetAttributes {
            attributes = List.copyOf(attributes);
        }
    }

    record AttributeValue(String name) implements Value {
    }

    enum ClassificationScope {
        RUNTIME,
        SYNTHETIC_ONLY
    }
}
