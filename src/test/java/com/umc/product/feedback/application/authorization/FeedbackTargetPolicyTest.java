package com.umc.product.feedback.application.authorization;

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
import com.umc.product.authorization.domain.AuthorizationChallengerTuple;
import com.umc.product.authorization.domain.AuthorizationRoleTuple;
import com.umc.product.authorization.domain.AuthorizationSubjectSnapshot;
import com.umc.product.authorization.domain.policy.rollout.PolicyRolloutEvaluation;
import com.umc.product.common.domain.enums.ChallengerPart;
import com.umc.product.common.domain.enums.ChallengerRoleType;
import com.umc.product.common.domain.enums.OrganizationType;
import com.umc.product.feedback.domain.enums.UserFeedbackTargetType;

class FeedbackTargetPolicyTest {

    private static final Instant EVALUATED_AT = Instant.parse("2026-07-01T00:00:00Z");
    private static final long TARGET_GISU_ID = 10L;

    private TargetFeedbackAuthorizationAdapter target;

    @BeforeEach
    void setUp() {
        CompiledPolicyRegistry registry = new CompiledPolicyRegistry(
            new PolicySemanticCompiler(),
            List.of(new FeedbackPolicyBundleContributor()));
        target = new TargetFeedbackAuthorizationAdapter(
            new RegisteredPolicyEvaluationService(registry, new PolicyEvaluationService()));
    }

    @Test
    @DisplayName("활성 target Gisu 중앙 운영진은 ADMIN target type을 받는다")
    void resolvesAdmin() {
        AuthorizationSubjectSnapshot subject = subject(
            List.of(role(
                Instant.parse("2026-01-01T00:00:00Z"),
                Instant.parse("2027-01-01T00:00:00Z"))),
            List.of());

        assertThat(target.evaluate(context(
            FeedbackPolicyAction.TEMPLATE_RESOLVE,
            subject,
            null)))
            .isEqualTo(new PolicyRolloutEvaluation.Success<>(
                FeedbackAuthorizationDecision.allow(UserFeedbackTargetType.ADMIN)));
    }

    @Test
    @DisplayName("이전 Gisu 이력이 있는 활성 챌린저는 EXPERIENCED target type을 받는다")
    void resolvesExperiencedChallenger() {
        AuthorizationSubjectSnapshot subject = subject(
            List.of(),
            List.of(challenger(10L, ChallengerPart.DESIGN), challenger(9L, ChallengerPart.PLAN)));

        assertThat(target.evaluate(context(
            FeedbackPolicyAction.TEMPLATE_RESOLVE,
            subject,
            null)))
            .isEqualTo(new PolicyRolloutEvaluation.Success<>(
                FeedbackAuthorizationDecision.allow(
                    UserFeedbackTargetType.EXPERIENCED_CHALLENGER)));
    }

    @Test
    @DisplayName("template target type과 계산된 target type이 같아야 제출한다")
    void allowsMatchingSubmissionTarget() {
        AuthorizationSubjectSnapshot subject = subject(
            List.of(),
            List.of(challenger(10L, ChallengerPart.DESIGN)));

        assertThat(target.evaluate(context(
            FeedbackPolicyAction.RESPONSE_SUBMIT,
            subject,
            UserFeedbackTargetType.NEW_CHALLENGER)))
            .isEqualTo(new PolicyRolloutEvaluation.Success<>(
                FeedbackAuthorizationDecision.allowWithoutOutcome()));
        assertThat(target.evaluate(context(
            FeedbackPolicyAction.RESPONSE_SUBMIT,
            subject,
            UserFeedbackTargetType.ADMIN)))
            .isEqualTo(new PolicyRolloutEvaluation.Success<>(
                FeedbackAuthorizationDecision.deny()));
    }

    @Test
    @DisplayName("만료된 중앙 운영진은 ADMIN target type을 받지 않는다")
    void deniesExpiredCentralMember() {
        AuthorizationSubjectSnapshot subject = subject(
            List.of(role(
                Instant.parse("2025-01-01T00:00:00Z"),
                Instant.parse("2026-01-01T00:00:00Z"))),
            List.of());

        assertThat(target.evaluate(context(
            FeedbackPolicyAction.TEMPLATE_RESOLVE,
            subject,
            null)))
            .isEqualTo(new PolicyRolloutEvaluation.Success<>(
                FeedbackAuthorizationDecision.deny()));
    }

    private FeedbackAuthorizationContext context(
        FeedbackPolicyAction action,
        AuthorizationSubjectSnapshot subject,
        UserFeedbackTargetType resourceTargetType
    ) {
        return new FeedbackAuthorizationContext(
            action,
            1L,
            TARGET_GISU_ID,
            10L,
            Optional.ofNullable(resourceTargetType),
            false,
            List.of(),
            subject,
            EVALUATED_AT);
    }

    private AuthorizationSubjectSnapshot subject(
        List<AuthorizationRoleTuple> roles,
        List<AuthorizationChallengerTuple> challengers
    ) {
        return AuthorizationSubjectSnapshot.member(
            1L,
            null,
            EVALUATED_AT,
            Set.of(),
            roles,
            challengers,
            Map.of());
    }

    private AuthorizationRoleTuple role(Instant startsAt, Instant endsAt) {
        return new AuthorizationRoleTuple(
            ChallengerRoleType.CENTRAL_OPERATING_TEAM_MEMBER,
            OrganizationType.CENTRAL,
            null,
            null,
            TARGET_GISU_ID,
            startsAt,
            endsAt);
    }

    private AuthorizationChallengerTuple challenger(
        long gisuId,
        ChallengerPart part
    ) {
        return new AuthorizationChallengerTuple(
            gisuId,
            gisuId,
            1L,
            part,
            Instant.parse("2026-01-01T00:00:00Z"),
            Instant.parse("2027-01-01T00:00:00Z"));
    }
}
