package com.umc.product.chat.application.authorization;

import org.springframework.stereotype.Component;

import com.umc.product.authorization.application.service.policy.rollout.PolicyRolloutEvaluator;
import com.umc.product.authorization.domain.policy.rollout.PolicyRolloutEvaluation;

@Component
public class LegacyChatAuthorizationAdapter
    implements PolicyRolloutEvaluator<ChatAuthorizationContext, Boolean> {

    @Override
    public PolicyRolloutEvaluation<Boolean> evaluate(ChatAuthorizationContext context) {
        boolean allowed = switch (context.action()) {
            case ROOM_READ, ROOM_SUMMARY_READ, MESSAGE_CREATE, MESSAGE_READ,
                 REACTION_UPDATE, READ_UPDATE, READ_STATUS -> context.roomMember();
            case MESSAGE_UPDATE -> context.roomMember() && context.messageAuthor();
            case MESSAGE_DELETE -> context.roomMember()
                && (context.messageAuthor() || context.moderator());
        };
        return new PolicyRolloutEvaluation.Success<>(allowed);
    }
}
