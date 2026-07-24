package com.umc.product.notification.application.authorization;

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
import com.umc.product.authorization.domain.policy.rollout.PolicyRolloutEvaluation;
import com.umc.product.common.domain.enums.ChallengerRoleType;
import com.umc.product.common.domain.enums.OrganizationType;

class NotificationTargetPolicyTest {

    private static final Instant START = Instant.parse("2026-07-01T00:00:00Z");
    private static final Instant END = Instant.parse("2026-08-01T00:00:00Z");

    private TargetNotificationAuthorizationAdapter target;

    @BeforeEach
    void setUp() {
        CompiledPolicyRegistry registry = new CompiledPolicyRegistry(
            new PolicySemanticCompiler(),
            List.of(new NotificationPolicyBundleContributor()));
        target = new TargetNotificationAuthorizationAdapter(
            new RegisteredPolicyEvaluationService(registry, new PolicyEvaluationService()));
    }

    @Test
    @DisplayName("중앙 총괄 역할은 Gisu 시작을 포함하고 종료를 제외해 FCM 발송을 허용한다")
    void evaluatesActiveCentralCoreAtHalfOpenBoundary() {
        assertThat(target.evaluate(sendContext(START)))
            .isEqualTo(new PolicyRolloutEvaluation.Success<>(true));
        assertThat(target.evaluate(sendContext(END.minusNanos(1))))
            .isEqualTo(new PolicyRolloutEvaluation.Success<>(true));
        assertThat(target.evaluate(sendContext(END)))
            .isEqualTo(new PolicyRolloutEvaluation.Success<>(false));
    }

    @Test
    @DisplayName("token 소유자는 운영진 역할 없이도 자신의 installation을 삭제한다")
    void allowsTokenOwner() {
        assertThat(target.evaluate(new NotificationAuthorizationContext(
            NotificationPolicyAction.DELETE_TOKEN,
            Optional.empty(),
            Optional.empty(),
            true,
            END)))
            .isEqualTo(new PolicyRolloutEvaluation.Success<>(true));
    }

    @Test
    @DisplayName("token 비소유자이고 활성 중앙 총괄도 아니면 삭제를 거부한다")
    void deniesNonOwnerWithoutActiveCentralCore() {
        assertThat(target.evaluate(new NotificationAuthorizationContext(
            NotificationPolicyAction.DELETE_TOKEN,
            Optional.empty(),
            Optional.empty(),
            false,
            END)))
            .isEqualTo(new PolicyRolloutEvaluation.Success<>(false));
    }

    @Test
    @DisplayName("만료 중앙 총괄의 권한 제거만 expected difference로 분류한다")
    void classifiesExpiredCentralRoleOnly() {
        NotificationAuthorizationContext context = sendContext(END);

        assertThat(new NotificationExpectedDifference().matches(context, true, false)).isTrue();
        assertThat(new NotificationExpectedDifference().matches(context, false, false)).isFalse();
    }

    private NotificationAuthorizationContext sendContext(Instant evaluatedAt) {
        AuthorizationRoleTuple role = new AuthorizationRoleTuple(
            ChallengerRoleType.CENTRAL_PRESIDENT,
            OrganizationType.CENTRAL,
            null,
            null,
            1L,
            START,
            END);
        SubjectAttributes legacySubject = SubjectAttributes.builder()
            .memberId(1L)
            .roleAttributes(List.of(new RoleAttribute(
                role.roleType(),
                role.organizationType(),
                role.organizationId(),
                role.responsiblePart(),
                role.gisuId())))
            .build();
        AuthorizationSubjectSnapshot subject = AuthorizationSubjectSnapshot.member(
            1L,
            null,
            evaluatedAt,
            Set.of(),
            List.of(role),
            List.of(),
            Map.of());
        return new NotificationAuthorizationContext(
            NotificationPolicyAction.SEND_FCM,
            Optional.of(legacySubject),
            Optional.of(subject),
            false,
            evaluatedAt);
    }
}
