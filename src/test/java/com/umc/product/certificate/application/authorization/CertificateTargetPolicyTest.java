package com.umc.product.certificate.application.authorization;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.List;
import java.util.Map;
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
import com.umc.product.authorization.domain.SystemRoleType;
import com.umc.product.authorization.domain.policy.rollout.PolicyRolloutEvaluation;
import com.umc.product.common.domain.enums.ChallengerRoleType;
import com.umc.product.common.domain.enums.OrganizationType;

class CertificateTargetPolicyTest {

    private static final Instant START = Instant.parse("2026-07-01T00:00:00Z");
    private static final Instant END = Instant.parse("2026-08-01T00:00:00Z");

    private TargetCertificateAuthorizationAdapter target;

    @BeforeEach
    void setUp() {
        CompiledPolicyRegistry registry = new CompiledPolicyRegistry(
            new PolicySemanticCompiler(),
            List.of(new CertificatePolicyBundleContributor()));
        target = new TargetCertificateAuthorizationAdapter(
            new RegisteredPolicyEvaluationService(registry, new PolicyEvaluationService()));
    }

    @Test
    @DisplayName("target Gisu의 중앙 총괄은 시작을 포함하고 종료를 제외해 발급한다")
    void evaluatesHalfOpenGisuWindow() {
        assertThat(target.evaluate(context(
            CertificatePolicyAction.ISSUE_ADMIN,
            1L,
            START,
            Set.of(),
            List.of(centralCore(1L)))))
            .isEqualTo(new PolicyRolloutEvaluation.Success<>(true));
        assertThat(target.evaluate(context(
            CertificatePolicyAction.ISSUE_ADMIN,
            1L,
            END,
            Set.of(),
            List.of(centralCore(1L)))))
            .isEqualTo(new PolicyRolloutEvaluation.Success<>(false));
    }

    @Test
    @DisplayName("다른 Gisu의 활성 중앙 총괄은 인증서를 관리하지 못한다")
    void deniesCentralCoreFromAnotherGisu() {
        assertThat(target.evaluate(context(
            CertificatePolicyAction.REVOKE,
            2L,
            START,
            Set.of(),
            List.of(centralCore(1L)))))
            .isEqualTo(new PolicyRolloutEvaluation.Success<>(false));
    }

    @Test
    @DisplayName("SUPER_ADMIN은 Gisu와 무관하게 운영진 발급과 폐기를 수행한다")
    void allowsSuperAdminGlobally() {
        for (CertificatePolicyAction action : CertificatePolicyAction.values()) {
            assertThat(target.evaluate(context(
                action,
                2L,
                END,
                Set.of(SystemRoleType.SUPER_ADMIN),
                List.of())))
                .isEqualTo(new PolicyRolloutEvaluation.Success<>(true));
        }
    }

    @Test
    @DisplayName("만료된 target Gisu 중앙 총괄 제거만 expected difference로 분류한다")
    void classifiesExpiredCentralCoreDifference() {
        CertificateAuthorizationContext context = context(
            CertificatePolicyAction.REVOKE,
            1L,
            END,
            Set.of(),
            List.of(centralCore(1L)));

        assertThat(new CertificateExpectedDifference().matches(context, true, false)).isTrue();
        assertThat(new CertificateExpectedDifference().matches(context, false, false)).isFalse();
    }

    private CertificateAuthorizationContext context(
        CertificatePolicyAction action,
        long targetGisuId,
        Instant evaluatedAt,
        Set<SystemRoleType> systemRoles,
        List<AuthorizationRoleTuple> roles
    ) {
        return new CertificateAuthorizationContext(
            action,
            targetGisuId,
            AuthorizationSubjectSnapshot.member(
                1L,
                null,
                evaluatedAt,
                systemRoles,
                roles,
                List.of(),
                Map.of()));
    }

    private AuthorizationRoleTuple centralCore(long gisuId) {
        return new AuthorizationRoleTuple(
            ChallengerRoleType.CENTRAL_PRESIDENT,
            OrganizationType.CENTRAL,
            null,
            null,
            gisuId,
            START,
            END);
    }
}
