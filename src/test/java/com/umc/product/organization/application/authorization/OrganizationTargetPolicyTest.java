package com.umc.product.organization.application.authorization;

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

class OrganizationTargetPolicyTest {

    private static final Instant EVALUATED_AT = Instant.parse("2026-07-01T00:00:00Z");
    private static final long SCHOOL_ID = 30L;

    private TargetOrganizationAuthorizationAdapter target;

    @BeforeEach
    void setUp() {
        CompiledPolicyRegistry registry = new CompiledPolicyRegistry(
            new PolicySemanticCompiler(),
            List.of(new OrganizationPolicyBundleContributor()));
        target = new TargetOrganizationAuthorizationAdapter(
            new RegisteredPolicyEvaluationService(registry, new PolicyEvaluationService()));
    }

    @Test
    @DisplayName("활성 중앙 CORE는 Gisu·Chapter·School 구조를 관리한다")
    void allowsActiveCentralCore() {
        var subject = subject(List.of(role(
            ChallengerRoleType.CENTRAL_PRESIDENT,
            OrganizationType.CENTRAL,
            null,
            Instant.parse("2026-01-01T00:00:00Z"),
            Instant.parse("2027-01-01T00:00:00Z"))));

        for (OrganizationPolicyAction action : List.of(
            OrganizationPolicyAction.GISU_CREATE,
            OrganizationPolicyAction.GISU_UPDATE,
            OrganizationPolicyAction.GISU_DELETE,
            OrganizationPolicyAction.CHAPTER_CREATE,
            OrganizationPolicyAction.CHAPTER_DELETE,
            OrganizationPolicyAction.SCHOOL_CREATE,
            OrganizationPolicyAction.SCHOOL_UPDATE,
            OrganizationPolicyAction.SCHOOL_DELETE)) {
            assertThat(target.evaluate(context(action, subject)))
                .isEqualTo(new PolicyRolloutEvaluation.Success<>(true));
        }
    }

    @Test
    @DisplayName("활성 학교 운영진은 단건 조회하고 회장단만 StudyGroup을 변경한다")
    void separatesSchoolAdminAndCore() {
        var partLeader = subject(List.of(role(
            ChallengerRoleType.SCHOOL_PART_LEADER,
            OrganizationType.SCHOOL,
            SCHOOL_ID,
            Instant.parse("2026-01-01T00:00:00Z"),
            Instant.parse("2027-01-01T00:00:00Z"))));
        var president = subject(List.of(role(
            ChallengerRoleType.SCHOOL_PRESIDENT,
            OrganizationType.SCHOOL,
            SCHOOL_ID,
            Instant.parse("2026-01-01T00:00:00Z"),
            Instant.parse("2027-01-01T00:00:00Z"))));

        assertThat(target.evaluate(context(OrganizationPolicyAction.STUDY_GROUP_READ, partLeader)))
            .isEqualTo(new PolicyRolloutEvaluation.Success<>(true));
        assertThat(target.evaluate(context(OrganizationPolicyAction.STUDY_GROUP_UPDATE, partLeader)))
            .isEqualTo(new PolicyRolloutEvaluation.Success<>(false));
        assertThat(target.evaluate(context(OrganizationPolicyAction.STUDY_GROUP_UPDATE, president)))
            .isEqualTo(new PolicyRolloutEvaluation.Success<>(true));
    }

    @Test
    @DisplayName("다른 학교 운영진은 subject 학교의 StudyGroup 권한을 얻지 못한다")
    void preservesOrganizationTuple() {
        var subject = subject(List.of(role(
            ChallengerRoleType.SCHOOL_PRESIDENT,
            OrganizationType.SCHOOL,
            31L,
            Instant.parse("2026-01-01T00:00:00Z"),
            Instant.parse("2027-01-01T00:00:00Z"))));

        assertThat(target.evaluate(context(OrganizationPolicyAction.STUDY_GROUP_READ, subject)))
            .isEqualTo(new PolicyRolloutEvaluation.Success<>(false));
    }

    @Test
    @DisplayName("만료된 운영진 역할은 Organization 권한을 만들지 않는다")
    void deniesExpiredRoles() {
        var subject = subject(List.of(role(
            ChallengerRoleType.CENTRAL_PRESIDENT,
            OrganizationType.CENTRAL,
            null,
            Instant.parse("2025-01-01T00:00:00Z"),
            Instant.parse("2026-01-01T00:00:00Z"))));

        assertThat(target.evaluate(context(OrganizationPolicyAction.GISU_CREATE, subject)))
            .isEqualTo(new PolicyRolloutEvaluation.Success<>(false));
    }

    private OrganizationAuthorizationContext context(
        OrganizationPolicyAction action,
        AuthorizationSubjectSnapshot subject
    ) {
        SubjectAttributes legacy = SubjectAttributes.builder()
            .memberId(1L)
            .schoolId(SCHOOL_ID)
            .build();
        return new OrganizationAuthorizationContext(
            action,
            legacy,
            Optional.of(subject),
            EVALUATED_AT);
    }

    private AuthorizationSubjectSnapshot subject(List<AuthorizationRoleTuple> roles) {
        return AuthorizationSubjectSnapshot.member(
            1L,
            SCHOOL_ID,
            EVALUATED_AT,
            Set.of(),
            roles,
            List.of(),
            Map.of());
    }

    private AuthorizationRoleTuple role(
        ChallengerRoleType roleType,
        OrganizationType organizationType,
        Long organizationId,
        Instant startAt,
        Instant endAt
    ) {
        return new AuthorizationRoleTuple(
            roleType,
            organizationType,
            organizationId,
            null,
            1L,
            startAt,
            endAt);
    }
}
