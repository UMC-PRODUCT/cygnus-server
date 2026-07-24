package com.umc.product.term.application.authorization;

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
import com.umc.product.authorization.domain.AuthorizationSubjectSnapshot;
import com.umc.product.authorization.domain.SubjectAttributes;
import com.umc.product.authorization.domain.SystemRoleType;
import com.umc.product.authorization.domain.policy.rollout.PolicyRolloutEvaluation;

class TermTargetPolicyTest {

    private static final Instant EVALUATED_AT = Instant.parse("2026-07-24T00:00:00Z");

    private TargetTermAuthorizationAdapter target;

    @BeforeEach
    void setUp() {
        CompiledPolicyRegistry registry = new CompiledPolicyRegistry(
            new PolicySemanticCompiler(),
            List.of(new TermPolicyBundleContributor()));
        target = new TargetTermAuthorizationAdapter(
            new RegisteredPolicyEvaluationService(registry, new PolicyEvaluationService()));
    }

    @Test
    @DisplayName("SUPER_ADMIN은 약관 생성 target policy에서 허용한다")
    void allowsSuperAdmin() {
        var result = target.evaluate(context(Set.of(SystemRoleType.SUPER_ADMIN)));

        assertThat(result).isEqualTo(new PolicyRolloutEvaluation.Success<>(true));
    }

    @Test
    @DisplayName("일반 회원은 약관 생성 target policy에서 기본 거부한다")
    void deniesMemberWithoutSuperAdmin() {
        var result = target.evaluate(context(Set.of()));

        assertThat(result).isEqualTo(new PolicyRolloutEvaluation.Success<>(false));
    }

    @Test
    @DisplayName("Term bundle은 시작 시 정확한 version과 action index로 컴파일된다")
    void compilesTermBundleWithActionIndex() {
        CompiledPolicyRegistry registry = new CompiledPolicyRegistry(
            new PolicySemanticCompiler(),
            List.of(new TermPolicyBundleContributor()));
        var bundle = registry.require(TermPolicyDomainSchema.BUNDLE_KEY);

        assertThat(bundle.policyVersion()).isEqualTo("1.0.0");
        assertThat(bundle.statementsForAction(TermPolicyAction.CREATE.id()))
            .extracting(statement -> statement.id())
            .containsExactly("term.create.super-admin");
    }

    private TermAuthorizationContext context(Set<SystemRoleType> systemRoles) {
        SubjectAttributes subject = SubjectAttributes.builder()
            .memberId(1L)
            .schoolId(1L)
            .systemRoles(systemRoles)
            .gisuChallengerInfos(List.of())
            .roleAttributes(List.of())
            .build();
        return new TermAuthorizationContext(
            subject,
            Optional.of(AuthorizationSubjectSnapshot.member(
                1L,
                1L,
                EVALUATED_AT,
                systemRoles,
                List.of(),
                List.of(),
                Map.of())),
            EVALUATED_AT);
    }
}
