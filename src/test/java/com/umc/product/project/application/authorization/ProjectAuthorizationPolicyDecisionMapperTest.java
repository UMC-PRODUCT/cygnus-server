package com.umc.product.project.application.authorization;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.umc.product.authorization.domain.policy.CompiledPolicyBundle;
import com.umc.product.authorization.domain.policy.PolicyDecision;
import com.umc.product.authorization.domain.policy.PolicyEffect;
import com.umc.product.authorization.domain.policy.PolicyResolvedOutcome;
import com.umc.product.authorization.domain.policy.PolicyValue;
import com.umc.product.project.application.authorization.rollout.ProjectAuthorizationApplicationScope;
import com.umc.product.project.application.authorization.rollout.ProjectAuthorizationComparisonRequest;
import com.umc.product.project.application.authorization.rollout.ProjectAuthorizationDecision;
import com.umc.product.project.application.authorization.rollout.ProjectAuthorizationEvaluationPoint;
import com.umc.product.project.application.authorization.rollout.ProjectAuthorizationFormView;
import com.umc.product.project.application.authorization.rollout.ProjectAuthorizationInternalOrigin;
import com.umc.product.project.application.authorization.rollout.ProjectAuthorizationObligation;
import com.umc.product.project.application.authorization.rollout.ProjectAuthorizationProjectScope;

class ProjectAuthorizationPolicyDecisionMapperTest {

    private static final Instant EVALUATED_AT = Instant.parse("2026-07-13T00:00:00Z");

    private final ProjectAuthorizationPolicyDecisionMapper mapper =
        new ProjectAuthorizationPolicyDecisionMapper();

    @Test
    @DisplayName("모든 canonical scope, form, force, obligation과 정책 metadata를 PolicyDecision으로 매핑한다")
    void mapsAllCanonicalOutcomesAndMetadata() {
        // Given
        ProjectAuthorizationDecision decision = new ProjectAuthorizationDecision(
            PolicyEffect.ALLOW,
            new ProjectAuthorizationProjectScope(
                true, true, Set.of(1L), Set.of(2L), Set.of(3L), true),
            new ProjectAuthorizationApplicationScope(
                true, Set.of(4L), Set.of(5L), Set.of(6L), Set.of(7L), true),
            ProjectAuthorizationFormView.FULL,
            true,
            true,
            List.of(new ProjectAuthorizationObligation(
                "audit.reason", new PolicyValue.StringValue("approved")))
        );

        // When
        PolicyDecision result = mapper.map(request(ProjectAuthorizationInternalOrigin.PARENT_TRANSITIVE),
            decision, bundle());

        // Then
        assertThat(result.effect()).isEqualTo(PolicyEffect.ALLOW);
        assertThat(result.outcomes()).containsExactlyInAnyOrder(
            outcome(ProjectPolicyOutcomes.PROJECT_ALL, bool()),
            outcome(ProjectPolicyOutcomes.PROJECT_PUBLIC_ONLY, bool()),
            outcome(ProjectPolicyOutcomes.PROJECT_GISU_IDS, longs(1L)),
            outcome(ProjectPolicyOutcomes.PROJECT_CHAPTER_IDS, longs(2L)),
            outcome(ProjectPolicyOutcomes.PROJECT_OWNER_MEMBER_IDS, longs(3L)),
            outcome(ProjectPolicyOutcomes.PROJECT_INCLUDE_OWN_DRAFTS, bool()),
            outcome(ProjectPolicyOutcomes.APPLICATION_ALL, bool()),
            outcome(ProjectPolicyOutcomes.APPLICATION_GISU_IDS, longs(4L)),
            outcome(ProjectPolicyOutcomes.APPLICATION_CHAPTER_IDS, longs(5L)),
            outcome(ProjectPolicyOutcomes.APPLICATION_PROJECT_IDS, longs(6L)),
            outcome(ProjectPolicyOutcomes.APPLICATION_OWNER_MEMBER_IDS, longs(7L)),
            outcome(ProjectPolicyOutcomes.APPLICATION_INCLUDE_ONGOING_ROUNDS, bool()),
            outcome(ProjectPolicyOutcomes.FORM_VIEW, new PolicyValue.EnumValue("FULL")),
            outcome(ProjectPolicyOutcomes.APPLICATION_FORCE_DECISION, bool()),
            outcome("audit.reason", new PolicyValue.StringValue("approved"))
        );
        assertThat(result.evaluatedAt()).isEqualTo(EVALUATED_AT);
        assertThat(result.schemaVersion()).isEqualTo("schema-1");
        assertThat(result.contextSchemaVersion()).isEqualTo("project-context-1");
        assertThat(result.policyVersion()).isEqualTo("policy-1");
        assertThat(result.policyFingerprint()).isEqualTo("fingerprint-1");
        assertThat(result.matchedAllowStatementIds()).isEmpty();
        assertThat(result.matchedDenyStatementIds()).isEmpty();
    }

    @Test
    @DisplayName("false, empty scope, NONE form은 outcome에서 생략한다")
    void omitsDefaultOutcomes() {
        // When
        PolicyDecision result = mapper.map(request(ProjectAuthorizationInternalOrigin.PARENT_TRANSITIVE),
            ProjectAuthorizationDecision.denied(), bundle());

        // Then
        assertThat(result.effect()).isEqualTo(PolicyEffect.DENY);
        assertThat(result.outcomes()).isEmpty();
    }

    @Test
    @DisplayName("CAPABILITY_QUERY는 decision effect 대신 capabilityAuthorized로 effect를 결정한다")
    void overridesEffectForCapabilityQuery() {
        // Given
        ProjectAuthorizationDecision capabilityAllowed = decision(PolicyEffect.DENY, true);
        ProjectAuthorizationDecision capabilityDenied = decision(PolicyEffect.ALLOW, false);

        // When
        PolicyDecision allowed = mapper.map(request(ProjectAuthorizationInternalOrigin.CAPABILITY_QUERY),
            capabilityAllowed, bundle());
        PolicyDecision denied = mapper.map(request(ProjectAuthorizationInternalOrigin.CAPABILITY_QUERY),
            capabilityDenied, bundle());

        // Then
        assertThat(allowed.effect()).isEqualTo(PolicyEffect.ALLOW);
        assertThat(denied.effect()).isEqualTo(PolicyEffect.DENY);
    }

    private ProjectAuthorizationDecision decision(PolicyEffect effect, boolean capabilityAuthorized) {
        return new ProjectAuthorizationDecision(
            effect,
            ProjectAuthorizationProjectScope.none(),
            ProjectAuthorizationApplicationScope.none(),
            ProjectAuthorizationFormView.NONE,
            false,
            capabilityAuthorized,
            List.of()
        );
    }

    private ProjectAuthorizationComparisonRequest request(ProjectAuthorizationInternalOrigin origin) {
        return new ProjectAuthorizationComparisonRequest(
            new ProjectPolicySubjectSnapshot(
                new ProjectPolicyPrincipal.Member(1L), EVALUATED_AT, List.of(), List.of(), Map.of()),
            ProjectPolicyAction.CAPABILITY_LIST,
            ProjectPolicyResourceContext.builder().build(),
            ProjectAuthorizationEvaluationPoint.internal(origin)
        );
    }

    private ProjectCompiledPolicyBundle bundle() {
        CompiledPolicyBundle bundle = mock(CompiledPolicyBundle.class);
        given(bundle.schemaVersion()).willReturn("schema-1");
        given(bundle.contextSchemaVersion()).willReturn("project-context-1");
        given(bundle.policyVersion()).willReturn("policy-1");
        given(bundle.policyFingerprint()).willReturn("fingerprint-1");
        return new ProjectCompiledPolicyBundle(bundle);
    }

    private PolicyResolvedOutcome outcome(String key, PolicyValue value) {
        return new PolicyResolvedOutcome(key, value);
    }

    private PolicyValue.BooleanValue bool() {
        return new PolicyValue.BooleanValue(true);
    }

    private PolicyValue.LongSetValue longs(Long... values) {
        return new PolicyValue.LongSetValue(Set.of(values));
    }
}
