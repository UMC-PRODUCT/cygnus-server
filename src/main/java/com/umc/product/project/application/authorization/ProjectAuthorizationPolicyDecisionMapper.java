package com.umc.product.project.application.authorization;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import com.umc.product.authorization.domain.policy.CompiledPolicyBundle;
import com.umc.product.authorization.domain.policy.PolicyDecision;
import com.umc.product.authorization.domain.policy.PolicyEffect;
import com.umc.product.authorization.domain.policy.PolicyResolvedOutcome;
import com.umc.product.authorization.domain.policy.PolicyValue;
import com.umc.product.project.application.authorization.rollout.ProjectAuthorizationComparisonRequest;
import com.umc.product.project.application.authorization.rollout.ProjectAuthorizationDecision;
import com.umc.product.project.application.authorization.rollout.ProjectAuthorizationEvaluationPoint;
import com.umc.product.project.application.authorization.rollout.ProjectAuthorizationFormView;
import com.umc.product.project.application.authorization.rollout.ProjectAuthorizationInternalOrigin;

final class ProjectAuthorizationPolicyDecisionMapper {

    PolicyDecision map(
        ProjectAuthorizationComparisonRequest request,
        ProjectAuthorizationDecision decision,
        ProjectCompiledPolicyBundle projectBundle
    ) {
        CompiledPolicyBundle bundle = projectBundle.value();
        return new PolicyDecision(
            effect(request.evaluationPoint(), decision),
            List.of(),
            List.of(),
            outcomes(decision),
            request.evaluatedAt(),
            bundle.schemaVersion(),
            bundle.contextSchemaVersion(),
            bundle.policyVersion(),
            bundle.policyFingerprint()
        );
    }

    private PolicyEffect effect(
        ProjectAuthorizationEvaluationPoint evaluationPoint,
        ProjectAuthorizationDecision decision
    ) {
        if (evaluationPoint instanceof ProjectAuthorizationEvaluationPoint.Internal internal
            && internal.origin() == ProjectAuthorizationInternalOrigin.CAPABILITY_QUERY) {
            return decision.capabilityAuthorized() ? PolicyEffect.ALLOW : PolicyEffect.DENY;
        }
        return decision.effect();
    }

    private List<PolicyResolvedOutcome> outcomes(ProjectAuthorizationDecision decision) {
        List<PolicyResolvedOutcome> outcomes = new ArrayList<>();
        addBoolean(outcomes, ProjectPolicyOutcomes.PROJECT_ALL, decision.projectScope().all());
        addBoolean(outcomes, ProjectPolicyOutcomes.PROJECT_PUBLIC_ONLY, decision.projectScope().publicOnly());
        addLongSet(outcomes, ProjectPolicyOutcomes.PROJECT_GISU_IDS, decision.projectScope().gisuIds());
        addLongSet(outcomes, ProjectPolicyOutcomes.PROJECT_CHAPTER_IDS, decision.projectScope().chapterIds());
        addLongSet(outcomes, ProjectPolicyOutcomes.PROJECT_OWNER_MEMBER_IDS,
            decision.projectScope().ownerMemberIds());
        addBoolean(outcomes, ProjectPolicyOutcomes.PROJECT_INCLUDE_OWN_DRAFTS,
            decision.projectScope().includeOwnDrafts());
        addBoolean(outcomes, ProjectPolicyOutcomes.APPLICATION_ALL, decision.applicationScope().all());
        addLongSet(outcomes, ProjectPolicyOutcomes.APPLICATION_GISU_IDS, decision.applicationScope().gisuIds());
        addLongSet(outcomes, ProjectPolicyOutcomes.APPLICATION_CHAPTER_IDS,
            decision.applicationScope().chapterIds());
        addLongSet(outcomes, ProjectPolicyOutcomes.APPLICATION_PROJECT_IDS,
            decision.applicationScope().projectIds());
        addLongSet(outcomes, ProjectPolicyOutcomes.APPLICATION_OWNER_MEMBER_IDS,
            decision.applicationScope().ownerMemberIds());
        addBoolean(outcomes, ProjectPolicyOutcomes.APPLICATION_INCLUDE_ONGOING_ROUNDS,
            decision.applicationScope().includeOngoingRounds());
        if (decision.formView() != ProjectAuthorizationFormView.NONE) {
            outcomes.add(new PolicyResolvedOutcome(
                ProjectPolicyOutcomes.FORM_VIEW,
                new PolicyValue.EnumValue(decision.formView().name())
            ));
        }
        addBoolean(outcomes, ProjectPolicyOutcomes.APPLICATION_FORCE_DECISION, decision.forceDecision());
        decision.obligations().forEach(obligation -> outcomes.add(
            new PolicyResolvedOutcome(obligation.key(), obligation.value())));
        return outcomes;
    }

    private void addBoolean(List<PolicyResolvedOutcome> outcomes, String key, boolean value) {
        if (value) {
            outcomes.add(new PolicyResolvedOutcome(key, new PolicyValue.BooleanValue(true)));
        }
    }

    private void addLongSet(List<PolicyResolvedOutcome> outcomes, String key, Set<Long> values) {
        if (!values.isEmpty()) {
            outcomes.add(new PolicyResolvedOutcome(key, new PolicyValue.LongSetValue(values)));
        }
    }
}
