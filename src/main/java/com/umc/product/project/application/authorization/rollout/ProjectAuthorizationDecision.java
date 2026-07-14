package com.umc.product.project.application.authorization.rollout;

import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import com.umc.product.authorization.domain.policy.PolicyDecision;
import com.umc.product.authorization.domain.policy.PolicyEffect;
import com.umc.product.authorization.domain.policy.PolicyResolvedOutcome;
import com.umc.product.authorization.domain.policy.PolicyValue;
import com.umc.product.project.application.authorization.ProjectPolicyAction;
import com.umc.product.project.application.authorization.ProjectPolicyOutcomes;

public record ProjectAuthorizationDecision(
    PolicyEffect effect,
    ProjectAuthorizationProjectScope projectScope,
    ProjectAuthorizationApplicationScope applicationScope,
    ProjectAuthorizationFormView formView,
    boolean forceDecision,
    boolean capabilityAuthorized,
    List<ProjectAuthorizationObligation> obligations
) implements ProjectAuthorizationEvaluationResult {
    private static final Set<String> CANONICAL_OUTCOME_KEYS = Set.of(
        ProjectPolicyOutcomes.PROJECT_ALL,
        ProjectPolicyOutcomes.PROJECT_PUBLIC_ONLY,
        ProjectPolicyOutcomes.PROJECT_GISU_IDS,
        ProjectPolicyOutcomes.PROJECT_CHAPTER_IDS,
        ProjectPolicyOutcomes.PROJECT_OWNER_MEMBER_IDS,
        ProjectPolicyOutcomes.PROJECT_INCLUDE_OWN_DRAFTS,
        ProjectPolicyOutcomes.APPLICATION_ALL,
        ProjectPolicyOutcomes.APPLICATION_GISU_IDS,
        ProjectPolicyOutcomes.APPLICATION_CHAPTER_IDS,
        ProjectPolicyOutcomes.APPLICATION_PROJECT_IDS,
        ProjectPolicyOutcomes.APPLICATION_OWNER_MEMBER_IDS,
        ProjectPolicyOutcomes.APPLICATION_INCLUDE_ONGOING_ROUNDS,
        ProjectPolicyOutcomes.FORM_VIEW,
        ProjectPolicyOutcomes.APPLICATION_FORCE_DECISION
    );

    public ProjectAuthorizationDecision {
        Objects.requireNonNull(effect);
        Objects.requireNonNull(projectScope);
        Objects.requireNonNull(applicationScope);
        Objects.requireNonNull(formView);
        Objects.requireNonNull(obligations);
        obligations = obligations.stream()
            .map(Objects::requireNonNull)
            .sorted(Comparator.comparing(ProjectAuthorizationObligation::key))
            .toList();
        for (int index = 1; index < obligations.size(); index++) {
            if (obligations.get(index - 1).key().equals(obligations.get(index).key())) {
                throw new IllegalArgumentException("중복된 authorization obligation입니다.");
            }
        }
    }

    public static ProjectAuthorizationDecision fromPolicyDecision(
        ProjectPolicyAction action,
        PolicyDecision decision
    ) {
        Objects.requireNonNull(action);
        Objects.requireNonNull(decision);
        return new ProjectAuthorizationDecision(
            decision.effect(),
            new ProjectAuthorizationProjectScope(
                booleanOutcome(decision, ProjectPolicyOutcomes.PROJECT_ALL),
                booleanOutcome(decision, ProjectPolicyOutcomes.PROJECT_PUBLIC_ONLY),
                longSetOutcome(decision, ProjectPolicyOutcomes.PROJECT_GISU_IDS),
                longSetOutcome(decision, ProjectPolicyOutcomes.PROJECT_CHAPTER_IDS),
                longSetOutcome(decision, ProjectPolicyOutcomes.PROJECT_OWNER_MEMBER_IDS),
                booleanOutcome(decision, ProjectPolicyOutcomes.PROJECT_INCLUDE_OWN_DRAFTS)
            ),
            new ProjectAuthorizationApplicationScope(
                booleanOutcome(decision, ProjectPolicyOutcomes.APPLICATION_ALL),
                longSetOutcome(decision, ProjectPolicyOutcomes.APPLICATION_GISU_IDS),
                longSetOutcome(decision, ProjectPolicyOutcomes.APPLICATION_CHAPTER_IDS),
                longSetOutcome(decision, ProjectPolicyOutcomes.APPLICATION_PROJECT_IDS),
                longSetOutcome(decision, ProjectPolicyOutcomes.APPLICATION_OWNER_MEMBER_IDS),
                booleanOutcome(decision, ProjectPolicyOutcomes.APPLICATION_INCLUDE_ONGOING_ROUNDS)
            ),
            formView(decision),
            booleanOutcome(decision, ProjectPolicyOutcomes.APPLICATION_FORCE_DECISION),
            decision.effect() == PolicyEffect.ALLOW,
            obligations(decision)
        );
    }

    public static ProjectAuthorizationDecision allowed() {
        return withEffect(PolicyEffect.ALLOW);
    }

    public static ProjectAuthorizationDecision denied() {
        return withEffect(PolicyEffect.DENY);
    }

    private static ProjectAuthorizationDecision withEffect(PolicyEffect effect) {
        return new ProjectAuthorizationDecision(
            effect,
            ProjectAuthorizationProjectScope.none(),
            ProjectAuthorizationApplicationScope.none(),
            ProjectAuthorizationFormView.NONE,
            false,
            effect == PolicyEffect.ALLOW,
            List.of()
        );
    }

    @Override
    public boolean allows() {
        return effect == PolicyEffect.ALLOW;
    }

    private static boolean booleanOutcome(PolicyDecision decision, String key) {
        return decision.outcome(key)
            .map(value -> requiredType(value, PolicyValue.BooleanValue.class, key).value())
            .orElse(false);
    }

    private static Set<Long> longSetOutcome(PolicyDecision decision, String key) {
        return decision.outcome(key)
            .map(value -> requiredType(value, PolicyValue.LongSetValue.class, key).value())
            .orElseGet(Set::of);
    }

    private static ProjectAuthorizationFormView formView(PolicyDecision decision) {
        return decision.outcome(ProjectPolicyOutcomes.FORM_VIEW)
            .map(value -> requiredType(
                value, PolicyValue.EnumValue.class, ProjectPolicyOutcomes.FORM_VIEW).value())
            .map(ProjectAuthorizationFormView::valueOf)
            .orElse(ProjectAuthorizationFormView.NONE);
    }

    private static List<ProjectAuthorizationObligation> obligations(PolicyDecision decision) {
        return decision.outcomes().stream()
            .filter(outcome -> !CANONICAL_OUTCOME_KEYS.contains(outcome.key()))
            .map(ProjectAuthorizationDecision::obligation)
            .toList();
    }

    private static ProjectAuthorizationObligation obligation(PolicyResolvedOutcome outcome) {
        return new ProjectAuthorizationObligation(outcome.key(), outcome.value());
    }

    private static <T extends PolicyValue> T requiredType(
        PolicyValue value,
        Class<T> type,
        String key
    ) {
        if (!type.isInstance(value)) {
            throw new IllegalArgumentException("authorization outcome type이 일치하지 않습니다: " + key);
        }
        return type.cast(value);
    }
}
