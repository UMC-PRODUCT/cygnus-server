package com.umc.product.project.application.authorization.rollout;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import com.umc.product.authorization.domain.policy.PolicyValue;
import com.umc.product.project.application.authorization.ProjectPolicyOutcomes;
import com.umc.product.project.application.authorization.rollout.ProjectExpectedDifferenceWire.AttributeValue;
import com.umc.product.project.application.authorization.rollout.ProjectExpectedDifferenceWire.BooleanValue;
import com.umc.product.project.application.authorization.rollout.ProjectExpectedDifferenceWire.Decision;
import com.umc.product.project.application.authorization.rollout.ProjectExpectedDifferenceWire.EnumValue;
import com.umc.product.project.application.authorization.rollout.ProjectExpectedDifferenceWire.LongSetAttributes;
import com.umc.product.project.application.authorization.rollout.ProjectExpectedDifferenceWire.Outcome;
import com.umc.product.project.application.authorization.rollout.ProjectExpectedDifferenceWire.Value;

final class ProjectExpectedDecisionTemplate {

    private ProjectExpectedDecisionTemplate() {
    }

    static ProjectAuthorizationDecision resolve(
        Decision template,
        ProjectAuthorizationComparisonRequest request
    ) {
        return resolve(template, request, ProjectExpectedDifferenceFactProvider.runtime());
    }

    static ProjectAuthorizationDecision resolve(
        Decision template,
        ProjectAuthorizationComparisonRequest request,
        ProjectExpectedDifferenceFactProvider facts
    ) {
        ResolvedOutcomes outcomes = new ResolvedOutcomes();
        for (Outcome outcome : template.outcomes()) {
            outcomes.put(outcome.key(), resolveValue(outcome.value(), request, facts));
        }
        List<ProjectAuthorizationObligation> obligations = new ArrayList<>();
        for (Outcome obligation : template.obligations()) {
            obligations.add(new ProjectAuthorizationObligation(
                obligation.key(), resolveValue(obligation.value(), request, facts)));
        }
        return new ProjectAuthorizationDecision(
            template.effect(),
            new ProjectAuthorizationProjectScope(
                outcomes.booleanValue(ProjectPolicyOutcomes.PROJECT_ALL),
                outcomes.booleanValue(ProjectPolicyOutcomes.PROJECT_PUBLIC_ONLY),
                outcomes.longSet(ProjectPolicyOutcomes.PROJECT_GISU_IDS),
                outcomes.longSet(ProjectPolicyOutcomes.PROJECT_CHAPTER_IDS),
                outcomes.longSet(ProjectPolicyOutcomes.PROJECT_OWNER_MEMBER_IDS),
                outcomes.booleanValue(ProjectPolicyOutcomes.PROJECT_INCLUDE_OWN_DRAFTS)
            ),
            new ProjectAuthorizationApplicationScope(
                outcomes.booleanValue(ProjectPolicyOutcomes.APPLICATION_ALL),
                outcomes.longSet(ProjectPolicyOutcomes.APPLICATION_GISU_IDS),
                outcomes.longSet(ProjectPolicyOutcomes.APPLICATION_CHAPTER_IDS),
                outcomes.longSet(ProjectPolicyOutcomes.APPLICATION_PROJECT_IDS),
                outcomes.longSet(ProjectPolicyOutcomes.APPLICATION_OWNER_MEMBER_IDS),
                outcomes.booleanValue(ProjectPolicyOutcomes.APPLICATION_INCLUDE_ONGOING_ROUNDS)
            ),
            outcomes.formView(),
            outcomes.booleanValue(ProjectPolicyOutcomes.APPLICATION_FORCE_DECISION),
            template.capabilityAuthorized(),
            obligations
        );
    }

    static void validate(Decision template) {
        Set<String> keys = new LinkedHashSet<>();
        for (Outcome outcome : template.outcomes()) {
            if (!keys.add(outcome.key())) {
                throw new IllegalStateException("EXPECTED_DIFFERENCE_DUPLICATE_OUTCOME");
            }
            validateOutcome(outcome);
        }
        for (Outcome obligation : template.obligations()) {
            validateValueAttributes(obligation.value());
        }
    }

    private static void validateOutcome(Outcome outcome) {
        String key = outcome.key();
        Value value = outcome.value();
        validateValueAttributes(value);
        if (Set.of(
            ProjectPolicyOutcomes.PROJECT_ALL,
            ProjectPolicyOutcomes.PROJECT_PUBLIC_ONLY,
            ProjectPolicyOutcomes.PROJECT_INCLUDE_OWN_DRAFTS,
            ProjectPolicyOutcomes.APPLICATION_ALL,
            ProjectPolicyOutcomes.APPLICATION_INCLUDE_ONGOING_ROUNDS,
            ProjectPolicyOutcomes.APPLICATION_FORCE_DECISION
        ).contains(key)) {
            requireType(value, BooleanValue.class);
            return;
        }
        if (Set.of(
            ProjectPolicyOutcomes.PROJECT_GISU_IDS,
            ProjectPolicyOutcomes.PROJECT_CHAPTER_IDS,
            ProjectPolicyOutcomes.PROJECT_OWNER_MEMBER_IDS,
            ProjectPolicyOutcomes.APPLICATION_GISU_IDS,
            ProjectPolicyOutcomes.APPLICATION_CHAPTER_IDS,
            ProjectPolicyOutcomes.APPLICATION_PROJECT_IDS,
            ProjectPolicyOutcomes.APPLICATION_OWNER_MEMBER_IDS
        ).contains(key)) {
            if (!(value instanceof LongSetAttributes) && !(value instanceof AttributeValue)) {
                throw new IllegalStateException("EXPECTED_DIFFERENCE_OUTCOME_TYPE_MISMATCH");
            }
            return;
        }
        if (ProjectPolicyOutcomes.FORM_VIEW.equals(key)) {
            requireType(value, EnumValue.class);
            return;
        }
        throw new IllegalStateException("EXPECTED_DIFFERENCE_OUTCOME_UNREGISTERED");
    }

    private static void validateValueAttributes(Value value) {
        switch (value) {
            case LongSetAttributes attributes -> {
                if (attributes.attributes().isEmpty()
                    || attributes.attributes().stream()
                    .anyMatch(name -> !ProjectExpectedDifferenceFacts.supportsLongSet(name))) {
                    throw new IllegalStateException("EXPECTED_DIFFERENCE_ATTRIBUTE_UNSUPPORTED");
                }
            }
            case AttributeValue attribute -> {
                if (!ProjectExpectedDifferenceFacts.supportsLongSet(attribute.name())) {
                    throw new IllegalStateException("EXPECTED_DIFFERENCE_ATTRIBUTE_UNSUPPORTED");
                }
            }
            case BooleanValue ignored -> {
            }
            case EnumValue ignored -> {
            }
        }
    }

    private static PolicyValue resolveValue(
        Value value,
        ProjectAuthorizationComparisonRequest request,
        ProjectExpectedDifferenceFactProvider facts
    ) {
        return switch (value) {
            case BooleanValue booleanValue -> new PolicyValue.BooleanValue(booleanValue.value());
            case EnumValue enumValue -> new PolicyValue.EnumValue(enumValue.value());
            case LongSetAttributes attributes -> new PolicyValue.LongSetValue(
                attributes.attributes().stream()
                    .flatMap(name -> facts.longSet(name, request).stream())
                    .collect(java.util.stream.Collectors.toSet()));
            case AttributeValue attribute -> new PolicyValue.LongSetValue(
                facts.longSet(attribute.name(), request));
        };
    }

    private static void requireType(Value value, Class<? extends Value> type) {
        if (!type.isInstance(value)) {
            throw new IllegalStateException("EXPECTED_DIFFERENCE_OUTCOME_TYPE_MISMATCH");
        }
    }

    private static final class ResolvedOutcomes {
        private final java.util.Map<String, PolicyValue> values = new java.util.LinkedHashMap<>();

        void put(String key, PolicyValue value) {
            if (values.put(key, value) != null) {
                throw new IllegalStateException("EXPECTED_DIFFERENCE_DUPLICATE_OUTCOME");
            }
        }

        boolean booleanValue(String key) {
            PolicyValue value = values.get(key);
            return value == null ? false : ((PolicyValue.BooleanValue) value).value();
        }

        Set<Long> longSet(String key) {
            PolicyValue value = values.get(key);
            return value == null ? Set.of() : ((PolicyValue.LongSetValue) value).value();
        }

        ProjectAuthorizationFormView formView() {
            PolicyValue value = values.get(ProjectPolicyOutcomes.FORM_VIEW);
            return value == null
                ? ProjectAuthorizationFormView.NONE
                : ProjectAuthorizationFormView.valueOf(((PolicyValue.EnumValue) value).value());
        }
    }
}
