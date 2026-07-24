package com.umc.product.notice.application.authorization;

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
import com.umc.product.notice.domain.NoticeTargetInfo;
import com.umc.product.notice.domain.enums.NoticeTab;

class NoticeTargetPolicyTest {

    private static final Instant EVALUATED_AT = Instant.parse("2026-07-01T00:00:00Z");
    private static final NoticeTargetInfo TARGET = new NoticeTargetInfo(
        1L,
        null,
        null,
        List.of(),
        NoticeTab.CHALLENGER);

    private TargetNoticeAuthorizationAdapter target;

    @BeforeEach
    void setUp() {
        CompiledPolicyRegistry registry = new CompiledPolicyRegistry(
            new PolicySemanticCompiler(),
            List.of(new NoticePolicyBundleContributor()));
        target = new TargetNoticeAuthorizationAdapter(
            new RegisteredPolicyEvaluationService(registry, new PolicyEvaluationService()),
            new NoticePolicyRelationResolver());
    }

    @Test
    @DisplayName("활성 중앙 운영진은 같은 Gisu 공지를 조회한다")
    void allowsActiveCentralStaff() {
        assertThat(target.evaluate(context(role(
            Instant.parse("2026-01-01T00:00:00Z"),
            Instant.parse("2027-01-01T00:00:00Z")))))
            .isEqualTo(new PolicyRolloutEvaluation.Success<>(true));
    }

    @Test
    @DisplayName("만료된 운영진은 공지 권한을 갖지 않는다")
    void deniesExpiredStaff() {
        assertThat(target.evaluate(context(role(
            Instant.parse("2025-01-01T00:00:00Z"),
            Instant.parse("2026-01-01T00:00:00Z")))))
            .isEqualTo(new PolicyRolloutEvaluation.Success<>(false));
    }

    private NoticeAuthorizationContext context(AuthorizationRoleTuple role) {
        AuthorizationSubjectSnapshot subject = AuthorizationSubjectSnapshot.member(
            1L,
            null,
            EVALUATED_AT,
            Set.of(),
            List.of(role),
            List.of(),
            Map.of());
        return new NoticeAuthorizationContext(
            NoticePolicyAction.READ,
            1L,
            Optional.empty(),
            Optional.of(subject),
            TARGET,
            null,
            false,
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
