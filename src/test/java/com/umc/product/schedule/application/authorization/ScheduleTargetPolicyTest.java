package com.umc.product.schedule.application.authorization;

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

class ScheduleTargetPolicyTest {

    private static final Instant EVALUATED_AT = Instant.parse("2026-07-01T00:00:00Z");

    private TargetScheduleAuthorizationAdapter target;

    @BeforeEach
    void setUp() {
        CompiledPolicyRegistry registry = new CompiledPolicyRegistry(
            new PolicySemanticCompiler(),
            List.of(new SchedulePolicyBundleContributor()));
        target = new TargetScheduleAuthorizationAdapter(
            new RegisteredPolicyEvaluationService(registry, new PolicyEvaluationService()));
    }

    @Test
    @DisplayName("일정 작성자는 자신의 일정을 수정한다")
    void allowsAuthorUpdate() {
        assertThat(target.evaluate(context(
            SchedulePolicyAction.SCHEDULE_UPDATE,
            List.of(),
            null,
            true,
            false)))
            .isEqualTo(new PolicyRolloutEvaluation.Success<>(true));
    }

    @Test
    @DisplayName("활성 target Gisu 운영진은 출석을 승인한다")
    void allowsActiveTargetGisuStaff() {
        assertThat(target.evaluate(context(
            SchedulePolicyAction.ATTENDANCE_APPROVE,
            List.of(role(
                Instant.parse("2026-01-01T00:00:00Z"),
                Instant.parse("2027-01-01T00:00:00Z"))),
            1L,
            false,
            false)))
            .isEqualTo(new PolicyRolloutEvaluation.Success<>(true));
    }

    @Test
    @DisplayName("만료된 target Gisu 운영진은 출석을 승인하지 못한다")
    void deniesExpiredTargetGisuStaff() {
        assertThat(target.evaluate(context(
            SchedulePolicyAction.ATTENDANCE_APPROVE,
            List.of(role(
                Instant.parse("2025-01-01T00:00:00Z"),
                Instant.parse("2026-01-01T00:00:00Z"))),
            1L,
            false,
            false)))
            .isEqualTo(new PolicyRolloutEvaluation.Success<>(false));
    }

    private ScheduleAuthorizationContext context(
        SchedulePolicyAction action,
        List<AuthorizationRoleTuple> roles,
        Long targetGisuId,
        boolean author,
        boolean participant
    ) {
        var legacy = SubjectAttributes.builder().memberId(1L).build();
        var subject = AuthorizationSubjectSnapshot.member(
            1L,
            null,
            EVALUATED_AT,
            Set.of(),
            roles,
            List.of(),
            Map.of());
        return new ScheduleAuthorizationContext(
            action,
            legacy,
            Optional.of(subject),
            true,
            targetGisuId,
            author,
            participant,
            EVALUATED_AT);
    }

    private AuthorizationRoleTuple role(Instant startAt, Instant endAt) {
        return new AuthorizationRoleTuple(
            ChallengerRoleType.CENTRAL_OPERATING_TEAM_MEMBER,
            OrganizationType.CENTRAL,
            null,
            null,
            1L,
            startAt,
            endAt);
    }
}
