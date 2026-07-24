package com.umc.product.challenger.application.authorization;

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

class ChallengerTargetPolicyTest {

    private static final Instant EVALUATED_AT = Instant.parse("2026-07-01T00:00:00Z");
    private static final long TARGET_GISU_ID = 2L;
    private static final long TARGET_SCHOOL_ID = 30L;

    private TargetChallengerAuthorizationAdapter target;

    @BeforeEach
    void setUp() {
        CompiledPolicyRegistry registry = new CompiledPolicyRegistry(
            new PolicySemanticCompiler(),
            List.of(new ChallengerPolicyBundleContributor()));
        target = new TargetChallengerAuthorizationAdapter(
            new RegisteredPolicyEvaluationService(registry, new PolicyEvaluationService()));
    }

    @Test
    @DisplayName("활성 학교 회장단은 Challenger 생성과 Record 조회를 수행한다")
    void allowsActiveSchoolCore() {
        var subject = subject(List.of(activeRole(
            ChallengerRoleType.SCHOOL_PRESIDENT,
            OrganizationType.SCHOOL,
            TARGET_SCHOOL_ID,
            TARGET_GISU_ID)));

        assertThat(target.evaluate(context(ChallengerPolicyAction.CHALLENGER_CREATE, subject)))
            .isEqualTo(new PolicyRolloutEvaluation.Success<>(true));
        assertThat(target.evaluate(context(ChallengerPolicyAction.RECORD_READ, subject)))
            .isEqualTo(new PolicyRolloutEvaluation.Success<>(true));
    }

    @Test
    @DisplayName("Point 변경은 target Gisu 중앙 운영진 또는 같은 학교 회장단에게 허용한다")
    void allowsTargetScopedPointMutation() {
        var central = subject(List.of(activeRole(
            ChallengerRoleType.CENTRAL_OPERATING_TEAM_MEMBER,
            OrganizationType.CENTRAL,
            null,
            TARGET_GISU_ID)));
        var schoolCore = subject(List.of(activeRole(
            ChallengerRoleType.SCHOOL_VICE_PRESIDENT,
            OrganizationType.SCHOOL,
            TARGET_SCHOOL_ID,
            TARGET_GISU_ID)));

        assertThat(target.evaluate(context(ChallengerPolicyAction.POINT_CREATE, central)))
            .isEqualTo(new PolicyRolloutEvaluation.Success<>(true));
        assertThat(target.evaluate(context(ChallengerPolicyAction.POINT_UPDATE, schoolCore)))
            .isEqualTo(new PolicyRolloutEvaluation.Success<>(true));
    }

    @Test
    @DisplayName("다른 Gisu 또는 학교 역할은 target Challenger Point 권한을 만들지 않는다")
    void preservesRoleTuple() {
        var otherGisu = subject(List.of(activeRole(
            ChallengerRoleType.CENTRAL_OPERATING_TEAM_MEMBER,
            OrganizationType.CENTRAL,
            null,
            3L)));
        var otherSchool = subject(List.of(activeRole(
            ChallengerRoleType.SCHOOL_PRESIDENT,
            OrganizationType.SCHOOL,
            31L,
            TARGET_GISU_ID)));

        assertThat(target.evaluate(context(ChallengerPolicyAction.POINT_CREATE, otherGisu)))
            .isEqualTo(new PolicyRolloutEvaluation.Success<>(false));
        assertThat(target.evaluate(context(ChallengerPolicyAction.POINT_CREATE, otherSchool)))
            .isEqualTo(new PolicyRolloutEvaluation.Success<>(false));
    }

    @Test
    @DisplayName("만료된 운영진 역할은 Challenger 권한을 만들지 않는다")
    void deniesExpiredRoles() {
        var expired = new AuthorizationRoleTuple(
            ChallengerRoleType.CENTRAL_PRESIDENT,
            OrganizationType.CENTRAL,
            null,
            null,
            TARGET_GISU_ID,
            Instant.parse("2025-01-01T00:00:00Z"),
            Instant.parse("2026-01-01T00:00:00Z"));

        assertThat(target.evaluate(context(
            ChallengerPolicyAction.CHALLENGER_DELETE,
            subject(List.of(expired)))))
            .isEqualTo(new PolicyRolloutEvaluation.Success<>(false));
    }

    private ChallengerAuthorizationContext context(
        ChallengerPolicyAction action,
        AuthorizationSubjectSnapshot subject
    ) {
        return new ChallengerAuthorizationContext(
            action,
            SubjectAttributes.builder().memberId(1L).schoolId(TARGET_SCHOOL_ID).build(),
            Optional.of(subject),
            TARGET_GISU_ID,
            TARGET_SCHOOL_ID,
            EVALUATED_AT);
    }

    private AuthorizationSubjectSnapshot subject(List<AuthorizationRoleTuple> roles) {
        return AuthorizationSubjectSnapshot.member(
            1L,
            TARGET_SCHOOL_ID,
            EVALUATED_AT,
            Set.of(),
            roles,
            List.of(),
            Map.of());
    }

    private AuthorizationRoleTuple activeRole(
        ChallengerRoleType role,
        OrganizationType organization,
        Long organizationId,
        long gisuId
    ) {
        return new AuthorizationRoleTuple(
            role,
            organization,
            organizationId,
            null,
            gisuId,
            Instant.parse("2026-01-01T00:00:00Z"),
            Instant.parse("2027-01-01T00:00:00Z"));
    }
}
