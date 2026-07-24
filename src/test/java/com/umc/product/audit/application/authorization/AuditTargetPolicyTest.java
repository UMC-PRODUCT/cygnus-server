package com.umc.product.audit.application.authorization;

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
import com.umc.product.authorization.domain.RoleAttribute;
import com.umc.product.authorization.domain.SubjectAttributes;
import com.umc.product.authorization.domain.SystemRoleType;
import com.umc.product.authorization.domain.policy.rollout.PolicyRolloutEvaluation;
import com.umc.product.common.domain.enums.ChallengerRoleType;
import com.umc.product.common.domain.enums.OrganizationType;

class AuditTargetPolicyTest {

    private static final Instant START = Instant.parse("2026-07-01T00:00:00Z");
    private static final Instant END = Instant.parse("2026-08-01T00:00:00Z");

    private TargetAuditAuthorizationAdapter target;

    @BeforeEach
    void setUp() {
        CompiledPolicyRegistry registry = new CompiledPolicyRegistry(
            new PolicySemanticCompiler(),
            List.of(new AuditPolicyBundleContributor()));
        target = new TargetAuditAuthorizationAdapter(
            new RegisteredPolicyEvaluationService(registry, new PolicyEvaluationService()));
    }

    @Test
    @DisplayName("Gisu 시작은 포함하고 종료는 제외해 중앙 운영진 권한을 평가한다")
    void evaluatesHalfOpenGisuWindow() {
        assertThat(target.evaluate(context(START, Set.of(), List.of(centralRole()))))
            .isEqualTo(new PolicyRolloutEvaluation.Success<>(true));
        assertThat(target.evaluate(context(END.minusNanos(1), Set.of(), List.of(centralRole()))))
            .isEqualTo(new PolicyRolloutEvaluation.Success<>(true));
        assertThat(target.evaluate(context(END, Set.of(), List.of(centralRole()))))
            .isEqualTo(new PolicyRolloutEvaluation.Success<>(false));
    }

    @Test
    @DisplayName("SUPER_ADMIN은 Gisu 역할과 무관하게 감사 로그를 조회한다")
    void allowsSuperAdminGlobally() {
        assertThat(target.evaluate(context(END, Set.of(SystemRoleType.SUPER_ADMIN), List.of())))
            .isEqualTo(new PolicyRolloutEvaluation.Success<>(true));
    }

    @Test
    @DisplayName("만료 중앙 역할의 legacy allow와 target deny를 승인된 차이로만 분류한다")
    void classifiesExpiredCentralRoleDifference() {
        AuditAuthorizationContext context = context(END, Set.of(), List.of(centralRole()));

        assertThat(new AuditExpectedDifference().matches(context, true, false)).isTrue();
        assertThat(new AuditExpectedDifference().matches(context, false, false)).isFalse();
    }

    private AuditAuthorizationContext context(
        Instant evaluatedAt,
        Set<SystemRoleType> systemRoles,
        List<AuthorizationRoleTuple> roles
    ) {
        SubjectAttributes legacySubject = SubjectAttributes.builder()
            .memberId(1L)
            .schoolId(1L)
            .systemRoles(systemRoles)
            .roleAttributes(roles.stream()
                .map(role -> new RoleAttribute(
                    role.roleType(),
                    role.organizationType(),
                    role.organizationId(),
                    role.responsiblePart(),
                    role.gisuId()))
                .toList())
            .gisuChallengerInfos(List.of())
            .build();
        AuthorizationSubjectSnapshot subject = AuthorizationSubjectSnapshot.member(
            1L,
            1L,
            evaluatedAt,
            systemRoles,
            roles,
            List.of(),
            Map.of());
        return new AuditAuthorizationContext(
            legacySubject,
            Optional.of(subject),
            evaluatedAt);
    }

    private AuthorizationRoleTuple centralRole() {
        return new AuthorizationRoleTuple(
            ChallengerRoleType.CENTRAL_OPERATING_TEAM_MEMBER,
            OrganizationType.CENTRAL,
            null,
            null,
            1L,
            START,
            END);
    }
}
