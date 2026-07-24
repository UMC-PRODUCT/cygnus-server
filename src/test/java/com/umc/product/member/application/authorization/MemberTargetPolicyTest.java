package com.umc.product.member.application.authorization;

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

class MemberTargetPolicyTest {

    private static final Instant EVALUATED_AT = Instant.parse("2026-07-01T00:00:00Z");

    private TargetMemberAuthorizationAdapter target;

    @BeforeEach
    void setUp() {
        CompiledPolicyRegistry registry = new CompiledPolicyRegistry(
            new PolicySemanticCompiler(),
            List.of(new MemberPolicyBundleContributor()));
        target = new TargetMemberAuthorizationAdapter(
            new RegisteredPolicyEvaluationService(registry, new PolicyEvaluationService()));
    }

    @Test
    @DisplayName("Challenger 이력이 있는 회원은 Member 단건을 조회한다")
    void allowsChallengerRead() {
        SubjectAttributes legacy = SubjectAttributes.builder()
            .memberId(1L)
            .gisuChallengerInfos(List.of(
                new SubjectAttributes.GisuChallengerInfo(1L, 10L, null, 100L)))
            .build();

        assertThat(target.evaluate(context(MemberPolicyAction.READ, legacy, List.of())))
            .isEqualTo(new PolicyRolloutEvaluation.Success<>(true));
    }

    @Test
    @DisplayName("활성 중앙 CORE는 Member를 강제 삭제한다")
    void allowsActiveCentralCoreDelete() {
        var active = centralRole(
            Instant.parse("2026-01-01T00:00:00Z"),
            Instant.parse("2027-01-01T00:00:00Z"));

        assertThat(target.evaluate(context(
            MemberPolicyAction.DELETE,
            SubjectAttributes.builder().memberId(1L).build(),
            List.of(active))))
            .isEqualTo(new PolicyRolloutEvaluation.Success<>(true));
    }

    @Test
    @DisplayName("만료된 중앙 CORE는 Member를 강제 삭제하지 못한다")
    void deniesExpiredCentralCore() {
        var expired = centralRole(
            Instant.parse("2025-01-01T00:00:00Z"),
            Instant.parse("2026-01-01T00:00:00Z"));

        assertThat(target.evaluate(context(
            MemberPolicyAction.DELETE,
            SubjectAttributes.builder().memberId(1L).build(),
            List.of(expired))))
            .isEqualTo(new PolicyRolloutEvaluation.Success<>(false));
    }

    private MemberAuthorizationContext context(
        MemberPolicyAction action,
        SubjectAttributes legacy,
        List<AuthorizationRoleTuple> roles
    ) {
        var subject = AuthorizationSubjectSnapshot.member(
            1L,
            null,
            EVALUATED_AT,
            Set.of(),
            roles,
            List.of(),
            Map.of());
        return new MemberAuthorizationContext(
            action,
            legacy,
            Optional.of(subject),
            EVALUATED_AT);
    }

    private AuthorizationRoleTuple centralRole(Instant startAt, Instant endAt) {
        return new AuthorizationRoleTuple(
            ChallengerRoleType.CENTRAL_PRESIDENT,
            OrganizationType.CENTRAL,
            null,
            null,
            1L,
            startAt,
            endAt);
    }
}
