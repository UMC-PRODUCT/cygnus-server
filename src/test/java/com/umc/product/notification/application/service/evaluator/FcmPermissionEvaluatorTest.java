package com.umc.product.notification.application.service.evaluator;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.umc.product.authorization.application.service.policy.CompiledPolicyRegistry;
import com.umc.product.authorization.application.service.policy.PolicyEvaluationService;
import com.umc.product.authorization.application.service.policy.PolicySemanticCompiler;
import com.umc.product.authorization.application.service.policy.RegisteredPolicyEvaluationService;
import com.umc.product.authorization.application.service.policy.rollout.PolicyRolloutObserver;
import com.umc.product.authorization.domain.PermissionType;
import com.umc.product.authorization.domain.ResourcePermission;
import com.umc.product.authorization.domain.ResourceType;
import com.umc.product.authorization.domain.RoleAttribute;
import com.umc.product.authorization.domain.SubjectAttributes;
import com.umc.product.authorization.domain.policy.rollout.PolicyRolloutMode;
import com.umc.product.common.domain.enums.ChallengerRoleType;
import com.umc.product.notification.application.authorization.LegacyNotificationAuthorizationAdapter;
import com.umc.product.notification.application.authorization.NotificationPolicyBundleContributor;
import com.umc.product.notification.application.authorization.TargetNotificationAuthorizationAdapter;

@DisplayName("FCM 권한 평가")
class FcmPermissionEvaluatorTest {

    private final FcmPermissionEvaluator evaluator = evaluator();

    @Test
    @DisplayName("중앙운영사무국 총괄단은 FCM 발송 요청 권한을 가진다")
    void central_core_can_write() {
        assertThat(evaluator.evaluate(
            subject(ChallengerRoleType.CENTRAL_PRESIDENT),
            ResourcePermission.ofType(ResourceType.FCM, PermissionType.WRITE)
        )).isTrue();
    }

    @Test
    @DisplayName("중앙운영사무국 일반 운영국원은 FCM 발송 요청 권한이 없다")
    void central_member_cannot_write() {
        assertThat(evaluator.evaluate(
            subject(ChallengerRoleType.CENTRAL_OPERATING_TEAM_MEMBER),
            ResourcePermission.ofType(ResourceType.FCM, PermissionType.WRITE)
        )).isFalse();
    }

    private SubjectAttributes subject(ChallengerRoleType roleType) {
        return SubjectAttributes.builder()
            .memberId(1L)
            .roleAttributes(List.of(new RoleAttribute(roleType, null, null, null, null)))
            .build();
    }

    private FcmPermissionEvaluator evaluator() {
        CompiledPolicyRegistry registry = new CompiledPolicyRegistry(
            new PolicySemanticCompiler(),
            List.of(new NotificationPolicyBundleContributor()));
        var target = new TargetNotificationAuthorizationAdapter(
            new RegisteredPolicyEvaluationService(registry, new PolicyEvaluationService()));
        return new FcmPermissionEvaluator(
            new LegacyNotificationAuthorizationAdapter(),
            target,
            ignored -> PolicyRolloutMode.SHADOW,
            PolicyRolloutObserver.noOp(),
            registry,
            Clock.fixed(Instant.parse("2026-07-01T00:00:00Z"), ZoneOffset.UTC));
    }
}
