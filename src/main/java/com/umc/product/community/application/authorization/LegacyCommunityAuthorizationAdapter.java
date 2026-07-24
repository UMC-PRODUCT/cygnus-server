package com.umc.product.community.application.authorization;

import org.springframework.stereotype.Component;

import com.umc.product.authorization.application.service.policy.rollout.PolicyRolloutEvaluator;
import com.umc.product.authorization.domain.policy.rollout.PolicyRolloutEvaluation;

@Component
public class LegacyCommunityAuthorizationAdapter
    implements PolicyRolloutEvaluator<CommunityAuthorizationContext, Boolean> {

    @Override
    public PolicyRolloutEvaluation<Boolean> evaluate(CommunityAuthorizationContext context) {
        boolean isAuthor = context.authorMemberId() != null
            && context.authorMemberId().equals(context.legacySubject().memberId());
        boolean allowed = switch (context.action()) {
            case POST_READ, COMMENT_READ -> true;
            case POST_WRITE, COMMENT_WRITE -> context.authorHasChallengerHistory();
            case POST_UPDATE, COMMENT_UPDATE -> isAuthor;
            case POST_DELETE, COMMENT_DELETE ->
                context.legacySubject().toAuthoritySnapshot().isCentralCoreInAnyGisu()
                    || isAuthor;
        };
        return new PolicyRolloutEvaluation.Success<>(allowed);
    }
}
