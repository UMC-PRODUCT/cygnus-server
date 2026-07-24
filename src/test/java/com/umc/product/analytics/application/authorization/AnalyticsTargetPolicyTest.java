package com.umc.product.analytics.application.authorization;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.umc.product.authorization.application.service.policy.CompiledPolicyRegistry;
import com.umc.product.authorization.application.service.policy.PolicyEvaluationService;
import com.umc.product.authorization.application.service.policy.PolicySemanticCompiler;
import com.umc.product.authorization.application.service.policy.RegisteredPolicyEvaluationService;
import com.umc.product.authorization.domain.AuthorizationRoleTuple;
import com.umc.product.authorization.domain.AuthorizationSubjectSnapshot;
import com.umc.product.authorization.domain.SubjectAttributes;
import com.umc.product.authorization.domain.policy.rollout.PolicyRolloutEvaluation;
import com.umc.product.common.domain.enums.ChallengerRoleType;
import com.umc.product.common.domain.enums.OrganizationType;

class AnalyticsTargetPolicyTest {

    private static final Instant EVALUATED_AT = Instant.parse("2026-07-01T00:00:00Z");

    private TargetAnalyticsAuthorizationAdapter target;

    @BeforeEach
    void setUp() {
        CompiledPolicyRegistry registry = new CompiledPolicyRegistry(
            new PolicySemanticCompiler(),
            List.of(new AnalyticsPolicyBundleContributor()));
        target = new TargetAnalyticsAuthorizationAdapter(
            new RegisteredPolicyEvaluationService(registry, new PolicyEvaluationService()));
    }

    @Test
    @DisplayName("활성 중앙 운영진은 Analytics 대시보드를 조회한다")
    void allowsActiveCentralMember() {
        assertThat(target.evaluate(context(role(
            ChallengerRoleType.CENTRAL_OPERATING_TEAM_MEMBER,
            Instant.parse("2026-01-01T00:00:00Z"),
            Instant.parse("2027-01-01T00:00:00Z")))))
            .isEqualTo(new PolicyRolloutEvaluation.Success<>(true));
    }

    @Test
    @DisplayName("만료된 운영진은 Analytics 대시보드를 조회하지 못한다")
    void deniesExpiredStaff() {
        assertThat(target.evaluate(context(role(
            ChallengerRoleType.SCHOOL_PRESIDENT,
            Instant.parse("2025-01-01T00:00:00Z"),
            Instant.parse("2026-01-01T00:00:00Z")))))
            .isEqualTo(new PolicyRolloutEvaluation.Success<>(false));
    }

    private AnalyticsAuthorizationContext context(AuthorizationRoleTuple role) {
        var legacy = SubjectAttributes.builder().memberId(1L).build();
        var subject = AuthorizationSubjectSnapshot.member(
            1L,
            null,
            EVALUATED_AT,
            Set.of(),
            List.of(role),
            List.of(),
            Map.of());
        return new AnalyticsAuthorizationContext(
            AnalyticsPolicyAction.READ_DASHBOARD,
            legacy,
            Optional.of(subject),
            EVALUATED_AT);
    }

    private AuthorizationRoleTuple role(
        ChallengerRoleType roleType,
        Instant startAt,
        Instant endAt
    ) {
        OrganizationType organizationType = roleType.isAtLeastCentralMember()
            ? OrganizationType.CENTRAL
            : OrganizationType.SCHOOL;
        return new AuthorizationRoleTuple(
            roleType,
            organizationType,
            organizationType == OrganizationType.SCHOOL ? 10L : null,
            null,
            1L,
            startAt,
            endAt);
    }
}
