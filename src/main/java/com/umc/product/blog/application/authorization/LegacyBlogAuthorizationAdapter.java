package com.umc.product.blog.application.authorization;

import org.springframework.stereotype.Component;

import com.umc.product.authorization.application.service.policy.rollout.PolicyRolloutEvaluator;
import com.umc.product.authorization.domain.policy.rollout.PolicyRolloutEvaluation;

@Component
public class LegacyBlogAuthorizationAdapter
    implements PolicyRolloutEvaluator<BlogAuthorizationContext, Boolean> {

    @Override
    public PolicyRolloutEvaluation<Boolean> evaluate(BlogAuthorizationContext context) {
        boolean allowed = switch (context.action()) {
            case CONTENT_CREATE, SERIES_CREATE, ADMIN_VIEW -> context.legacySuperAdmin();
            case CONTENT_READ, CONTENT_DELETE, SERIES_READ, SERIES_DELETE, COMMENT_DELETE ->
                context.author() || context.legacySuperAdmin();
            case CONTENT_UPDATE, SERIES_UPDATE, COMMENT_UPDATE -> context.author();
        };
        return new PolicyRolloutEvaluation.Success<>(allowed);
    }
}
