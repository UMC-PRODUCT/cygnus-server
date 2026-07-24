package com.umc.product.member.application.authorization;

import org.springframework.stereotype.Component;

import com.umc.product.authorization.application.service.policy.rollout.PolicyRolloutEvaluator;
import com.umc.product.authorization.domain.policy.rollout.PolicyRolloutEvaluation;

@Component
public class LegacyMemberAuthorizationAdapter
    implements PolicyRolloutEvaluator<MemberAuthorizationContext, Boolean> {

    @Override
    public PolicyRolloutEvaluation<Boolean> evaluate(MemberAuthorizationContext context) {
        boolean allowed = switch (context.action()) {
            case READ -> !context.legacySubject().gisuChallengerInfos().isEmpty();
            case DELETE -> context.legacySubject().toAuthoritySnapshot().isCentralCoreInAnyGisu();
        };
        return new PolicyRolloutEvaluation.Success<>(allowed);
    }
}
