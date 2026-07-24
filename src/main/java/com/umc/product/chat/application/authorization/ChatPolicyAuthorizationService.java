package com.umc.product.chat.application.authorization;

import java.time.Clock;
import java.util.EnumMap;
import java.util.Map;

import org.springframework.stereotype.Service;

import com.umc.product.authorization.application.service.policy.CompiledPolicyRegistry;
import com.umc.product.authorization.application.service.policy.rollout.BooleanPolicyRolloutExecutor;
import com.umc.product.authorization.application.service.policy.rollout.PolicyExpectedDifference;
import com.umc.product.authorization.application.service.policy.rollout.PolicyRolloutModeResolver;
import com.umc.product.authorization.application.service.policy.rollout.PolicyRolloutObserver;

@Service
public class ChatPolicyAuthorizationService {

    private final Map<ChatPolicyAction, BooleanPolicyRolloutExecutor<ChatAuthorizationContext>> rollouts;
    private final Clock clock;

    public ChatPolicyAuthorizationService(
        LegacyChatAuthorizationAdapter legacyEvaluator,
        TargetChatAuthorizationAdapter targetEvaluator,
        PolicyRolloutModeResolver modeResolver,
        PolicyRolloutObserver observer,
        CompiledPolicyRegistry registry,
        Clock clock
    ) {
        this.clock = clock;
        EnumMap<ChatPolicyAction, BooleanPolicyRolloutExecutor<ChatAuthorizationContext>> configured =
            new EnumMap<>(ChatPolicyAction.class);
        for (ChatPolicyAction action : ChatPolicyAction.values()) {
            configured.put(action, new BooleanPolicyRolloutExecutor<>(
                legacyEvaluator,
                targetEvaluator,
                PolicyExpectedDifference.none(),
                modeResolver,
                observer,
                registry,
                ChatPolicyDomainSchema.BUNDLE_KEY,
                action.id()));
        }
        this.rollouts = Map.copyOf(configured);
    }

    public boolean evaluate(
        ChatPolicyAction action,
        boolean roomMember,
        boolean messageAuthor,
        boolean moderator
    ) {
        var evaluatedAt = clock.instant();
        var context = new ChatAuthorizationContext(
            action,
            roomMember,
            messageAuthor,
            moderator,
            evaluatedAt);
        return rollouts.get(action).evaluate(context, evaluatedAt);
    }
}
