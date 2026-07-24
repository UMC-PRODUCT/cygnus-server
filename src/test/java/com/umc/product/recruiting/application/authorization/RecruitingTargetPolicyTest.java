package com.umc.product.recruiting.application.authorization;

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
import com.umc.product.authorization.domain.policy.rollout.PolicyRolloutEvaluation;
import com.umc.product.common.domain.enums.ChallengerRoleType;
import com.umc.product.common.domain.enums.OrganizationType;

class RecruitingTargetPolicyTest {

    private static final Instant EVALUATED_AT = Instant.parse("2026-07-01T00:00:00Z");
    private static final long TARGET_GISU_ID = 2L;
    private static final long TARGET_SCHOOL_ID = 30L;

    private TargetRecruitingAuthorizationAdapter target;

    @BeforeEach
    void setUp() {
        CompiledPolicyRegistry registry = new CompiledPolicyRegistry(
            new PolicySemanticCompiler(),
            List.of(new RecruitingPolicyBundleContributor()));
        target = new TargetRecruitingAuthorizationAdapter(
            new RegisteredPolicyEvaluationService(registry, new PolicyEvaluationService()));
    }

    @Test
    @DisplayName("활성 학교 회장단은 같은 Gisu와 학교의 시즌 생성 및 합불 결정을 수행한다")
    void allowsActiveSchoolCoreForTarget() {
        AuthorizationSubjectSnapshot subject = subject(List.of(activeRole(
            ChallengerRoleType.SCHOOL_PRESIDENT,
            OrganizationType.SCHOOL,
            TARGET_SCHOOL_ID,
            TARGET_GISU_ID)));

        assertThat(evaluate(RecruitingPolicyAction.SEASON_CREATE, subject, true))
            .isEqualTo(new PolicyRolloutEvaluation.Success<>(true));
        assertThat(evaluate(RecruitingPolicyAction.APPLICATION_DECIDE, subject, true))
            .isEqualTo(new PolicyRolloutEvaluation.Success<>(true));
    }

    @Test
    @DisplayName("활성 중앙 회장단은 같은 Gisu의 등록·집계·내보내기를 수행한다")
    void allowsActiveCentralCoreForTarget() {
        AuthorizationSubjectSnapshot subject = subject(List.of(activeRole(
            ChallengerRoleType.CENTRAL_PRESIDENT,
            OrganizationType.CENTRAL,
            null,
            TARGET_GISU_ID)));

        assertThat(evaluate(RecruitingPolicyAction.REGISTRATION_MANAGE, subject, true))
            .isEqualTo(new PolicyRolloutEvaluation.Success<>(true));
        assertThat(evaluate(RecruitingPolicyAction.SUMMARY_READ, subject, true))
            .isEqualTo(new PolicyRolloutEvaluation.Success<>(true));
        assertThat(evaluate(RecruitingPolicyAction.CSV_EXPORT, subject, true))
            .isEqualTo(new PolicyRolloutEvaluation.Success<>(true));
    }

    @Test
    @DisplayName("다른 Gisu 또는 학교 역할은 target Recruiting 권한을 만들지 않는다")
    void preservesRoleTuple() {
        AuthorizationSubjectSnapshot otherGisu = subject(List.of(activeRole(
            ChallengerRoleType.CENTRAL_PRESIDENT,
            OrganizationType.CENTRAL,
            null,
            3L)));
        AuthorizationSubjectSnapshot otherSchool = subject(List.of(activeRole(
            ChallengerRoleType.SCHOOL_VICE_PRESIDENT,
            OrganizationType.SCHOOL,
            31L,
            TARGET_GISU_ID)));

        assertThat(evaluate(RecruitingPolicyAction.REGISTRATION_MANAGE, otherGisu, true))
            .isEqualTo(new PolicyRolloutEvaluation.Success<>(false));
        assertThat(evaluate(RecruitingPolicyAction.APPLICATION_DECIDE, otherSchool, true))
            .isEqualTo(new PolicyRolloutEvaluation.Success<>(false));
    }

    @Test
    @DisplayName("resource 없는 capability는 활성 역할만 사용한다")
    void evaluatesUnspecifiedResourceCapability() {
        AuthorizationSubjectSnapshot schoolCore = subject(List.of(activeRole(
            ChallengerRoleType.SCHOOL_PRESIDENT,
            OrganizationType.SCHOOL,
            TARGET_SCHOOL_ID,
            TARGET_GISU_ID)));

        assertThat(evaluate(RecruitingPolicyAction.OPERATE_SCHOOL, schoolCore, false))
            .isEqualTo(new PolicyRolloutEvaluation.Success<>(true));
        assertThat(evaluate(RecruitingPolicyAction.MANAGE_ALL, schoolCore, false))
            .isEqualTo(new PolicyRolloutEvaluation.Success<>(false));
    }

    @Test
    @DisplayName("만료된 운영진은 Recruiting 권한을 갖지 않는다")
    void deniesExpiredStaff() {
        AuthorizationSubjectSnapshot subject = subject(List.of(new AuthorizationRoleTuple(
            ChallengerRoleType.CENTRAL_PRESIDENT,
            OrganizationType.CENTRAL,
            null,
            null,
            TARGET_GISU_ID,
            Instant.parse("2025-01-01T00:00:00Z"),
            Instant.parse("2026-01-01T00:00:00Z"))));

        assertThat(evaluate(RecruitingPolicyAction.MANAGE_ALL, subject, true))
            .isEqualTo(new PolicyRolloutEvaluation.Success<>(false));
    }

    private PolicyRolloutEvaluation<Boolean> evaluate(
        RecruitingPolicyAction action,
        AuthorizationSubjectSnapshot subject,
        boolean resourceSpecified
    ) {
        return target.evaluate(new RecruitingAuthorizationContext(
            action,
            1L,
            Optional.empty(),
            Optional.of(subject),
            resourceSpecified,
            resourceSpecified ? TARGET_GISU_ID : null,
            resourceSpecified ? TARGET_SCHOOL_ID : null,
            EVALUATED_AT));
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
        ChallengerRoleType roleType,
        OrganizationType organizationType,
        Long organizationId,
        long gisuId
    ) {
        return new AuthorizationRoleTuple(
            roleType,
            organizationType,
            organizationId,
            null,
            gisuId,
            Instant.parse("2026-01-01T00:00:00Z"),
            Instant.parse("2027-01-01T00:00:00Z"));
    }
}
