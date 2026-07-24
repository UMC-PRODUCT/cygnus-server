package com.umc.product.authorization.application.authorization;

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
import com.umc.product.authorization.domain.SystemRoleType;
import com.umc.product.authorization.domain.policy.rollout.PolicyRolloutEvaluation;
import com.umc.product.common.domain.enums.ChallengerRoleType;
import com.umc.product.common.domain.enums.OrganizationType;

class AuthorizationTargetPolicyTest {

    private static final Instant EVALUATED_AT = Instant.parse("2026-07-01T00:00:00Z");

    private TargetAuthorizationPolicyAdapter target;

    @BeforeEach
    void setUp() {
        CompiledPolicyRegistry registry = new CompiledPolicyRegistry(
            new PolicySemanticCompiler(),
            List.of(new AuthorizationPolicyBundleContributor()));
        target = new TargetAuthorizationPolicyAdapter(
            new RegisteredPolicyEvaluationService(registry, new PolicyEvaluationService()));
    }

    @Test
    @DisplayName("인증 회원은 ChallengerRole 단건을 조회한다")
    void allowsAuthenticatedRead() {
        assertThat(target.evaluate(context(
            AuthorizationPolicyAction.CHALLENGER_ROLE_READ,
            memberSubject(Set.of(), List.of()))))
            .isEqualTo(new PolicyRolloutEvaluation.Success<>(true));
    }

    @Test
    @DisplayName("활성 중앙 CORE는 ChallengerRole을 생성하고 삭제한다")
    void allowsActiveCentralCore() {
        AuthorizationSubjectSnapshot subject = memberSubject(
            Set.of(),
            List.of(centralRole(
                Instant.parse("2026-01-01T00:00:00Z"),
                Instant.parse("2027-01-01T00:00:00Z"))));

        for (AuthorizationPolicyAction action : List.of(
            AuthorizationPolicyAction.CHALLENGER_ROLE_CREATE,
            AuthorizationPolicyAction.CHALLENGER_ROLE_DELETE)) {
            assertThat(target.evaluate(context(action, subject)))
                .isEqualTo(new PolicyRolloutEvaluation.Success<>(true));
        }
    }

    @Test
    @DisplayName("만료된 중앙 CORE는 ChallengerRole을 생성하거나 삭제하지 못한다")
    void deniesExpiredCentralCore() {
        AuthorizationSubjectSnapshot subject = memberSubject(
            Set.of(),
            List.of(centralRole(
                Instant.parse("2025-01-01T00:00:00Z"),
                Instant.parse("2026-01-01T00:00:00Z"))));

        assertThat(target.evaluate(context(
            AuthorizationPolicyAction.CHALLENGER_ROLE_CREATE,
            subject)))
            .isEqualTo(new PolicyRolloutEvaluation.Success<>(false));
    }

    @Test
    @DisplayName("SUPER_ADMIN은 Gisu 역할 없이 ChallengerRole을 관리한다")
    void allowsSuperAdmin() {
        AuthorizationSubjectSnapshot subject = memberSubject(
            Set.of(SystemRoleType.SUPER_ADMIN),
            List.of());

        assertThat(target.evaluate(context(
            AuthorizationPolicyAction.CHALLENGER_ROLE_DELETE,
            subject)))
            .isEqualTo(new PolicyRolloutEvaluation.Success<>(true));
    }

    private AuthorizationPolicyContext context(
        AuthorizationPolicyAction action,
        AuthorizationSubjectSnapshot subject
    ) {
        SubjectAttributes legacySubject = SubjectAttributes.builder()
            .memberId(1L)
            .systemRoles(subject.systemRoles())
            .build();
        return new AuthorizationPolicyContext(
            action,
            legacySubject,
            Optional.of(subject),
            EVALUATED_AT);
    }

    private AuthorizationSubjectSnapshot memberSubject(
        Set<SystemRoleType> roles,
        List<AuthorizationRoleTuple> challengerRoles
    ) {
        return AuthorizationSubjectSnapshot.member(
            1L,
            null,
            EVALUATED_AT,
            roles,
            challengerRoles,
            List.of(),
            Map.of());
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
