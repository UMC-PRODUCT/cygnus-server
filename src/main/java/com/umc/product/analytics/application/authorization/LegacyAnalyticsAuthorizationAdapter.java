package com.umc.product.analytics.application.authorization;

import org.springframework.stereotype.Component;

import com.umc.product.authorization.application.service.policy.rollout.PolicyRolloutEvaluator;
import com.umc.product.authorization.domain.policy.rollout.PolicyRolloutEvaluation;
import com.umc.product.common.domain.enums.ChallengerRoleType;

@Component
public class LegacyAnalyticsAuthorizationAdapter
    implements PolicyRolloutEvaluator<AnalyticsAuthorizationContext, Boolean> {

    @Override
    public PolicyRolloutEvaluation<Boolean> evaluate(AnalyticsAuthorizationContext context) {
        if (context.legacySubject().toAuthoritySnapshot().isSuperAdmin()) {
            return new PolicyRolloutEvaluation.Success<>(true);
        }
        boolean allowed = context.legacySubject().roleAttributes().stream()
            .map(role -> role.roleType())
            .anyMatch(roleType -> roleType.isAtLeastCentralMember()
                || roleType == ChallengerRoleType.CHAPTER_PRESIDENT
                || roleType.isAtLeastSchoolAdmin());
        return new PolicyRolloutEvaluation.Success<>(allowed);
    }
}
