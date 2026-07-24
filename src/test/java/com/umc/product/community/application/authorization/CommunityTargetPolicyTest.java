package com.umc.product.community.application.authorization;

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

class CommunityTargetPolicyTest {

    private static final Instant EVALUATED_AT = Instant.parse("2026-07-01T00:00:00Z");
    private static final long MEMBER_ID = 1L;
    private static final long AUTHOR_MEMBER_ID = 2L;

    private TargetCommunityAuthorizationAdapter target;

    @BeforeEach
    void setUp() {
        CompiledPolicyRegistry registry = new CompiledPolicyRegistry(
            new PolicySemanticCompiler(),
            List.of(new CommunityPolicyBundleContributor()));
        target = new TargetCommunityAuthorizationAdapter(
            new RegisteredPolicyEvaluationService(registry, new PolicyEvaluationService()));
    }

    @Test
    @DisplayName("작성자는 자신의 게시글을 수정하고 삭제한다")
    void allowsAuthorMutation() {
        CommunityAuthorizationContext update = context(
            CommunityPolicyAction.POST_UPDATE,
            subject(List.of()),
            MEMBER_ID);
        CommunityAuthorizationContext delete = context(
            CommunityPolicyAction.POST_DELETE,
            subject(List.of()),
            MEMBER_ID);

        assertThat(target.evaluate(update))
            .isEqualTo(new PolicyRolloutEvaluation.Success<>(true));
        assertThat(target.evaluate(delete))
            .isEqualTo(new PolicyRolloutEvaluation.Success<>(true));
    }

    @Test
    @DisplayName("활성 중앙 회장단은 다른 작성자의 게시글을 삭제한다")
    void allowsActiveCentralCore() {
        AuthorizationRoleTuple active = role(
            Instant.parse("2026-01-01T00:00:00Z"),
            Instant.parse("2027-01-01T00:00:00Z"));

        assertThat(target.evaluate(context(
            CommunityPolicyAction.POST_DELETE,
            subject(List.of(active)),
            AUTHOR_MEMBER_ID)))
            .isEqualTo(new PolicyRolloutEvaluation.Success<>(true));
    }

    @Test
    @DisplayName("만료된 중앙 회장단은 다른 작성자의 콘텐츠를 삭제하지 못한다")
    void deniesExpiredCentralCore() {
        AuthorizationRoleTuple expired = role(
            Instant.parse("2025-01-01T00:00:00Z"),
            Instant.parse("2026-01-01T00:00:00Z"));

        assertThat(target.evaluate(context(
            CommunityPolicyAction.COMMENT_DELETE,
            subject(List.of(expired)),
            AUTHOR_MEMBER_ID)))
            .isEqualTo(new PolicyRolloutEvaluation.Success<>(false));
    }

    private CommunityAuthorizationContext context(
        CommunityPolicyAction action,
        AuthorizationSubjectSnapshot subject,
        Long authorMemberId
    ) {
        return new CommunityAuthorizationContext(
            action,
            SubjectAttributes.builder().memberId(MEMBER_ID).build(),
            Optional.of(subject),
            true,
            authorMemberId,
            EVALUATED_AT);
    }

    private AuthorizationSubjectSnapshot subject(List<AuthorizationRoleTuple> roles) {
        return AuthorizationSubjectSnapshot.member(
            MEMBER_ID,
            null,
            EVALUATED_AT,
            Set.of(),
            roles,
            List.of(),
            Map.of());
    }

    private AuthorizationRoleTuple role(Instant startsAt, Instant endsAt) {
        return new AuthorizationRoleTuple(
            ChallengerRoleType.CENTRAL_PRESIDENT,
            OrganizationType.CENTRAL,
            null,
            null,
            1L,
            startsAt,
            endsAt);
    }
}
