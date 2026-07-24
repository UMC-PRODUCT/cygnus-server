package com.umc.product.chat.application.authorization;

import java.util.Arrays;
import java.util.List;
import java.util.Set;

import com.umc.product.authorization.domain.policy.ActionSchema;
import com.umc.product.authorization.domain.policy.AttributeSchema;
import com.umc.product.authorization.domain.policy.PolicyBundleKey;
import com.umc.product.authorization.domain.policy.PolicyDomainSchema;

public final class ChatPolicyDomainSchema {

    public static final String NAMESPACE = "chat";
    public static final String VERSION = "chat-1.0";
    public static final String POLICY_VERSION = "1.0.0";
    public static final PolicyBundleKey BUNDLE_KEY = new PolicyBundleKey(NAMESPACE, VERSION);

    private ChatPolicyDomainSchema() {
    }

    public static PolicyDomainSchema create() {
        Set<String> requiredAttributes = Set.of(
            ChatPolicyAttributes.ROOM_MEMBER.name(),
            ChatPolicyAttributes.MESSAGE_AUTHOR.name(),
            ChatPolicyAttributes.MODERATOR.name());
        return new PolicyDomainSchema(
            VERSION,
            Arrays.stream(ChatPolicyAction.values())
                .map(action -> new ActionSchema(
                    action.id(),
                    requiredAttributes,
                    Set.of(),
                    Set.of()))
                .toList(),
            List.of(
                new AttributeSchema(
                    ChatPolicyAttributes.ROOM_MEMBER.name(),
                    ChatPolicyAttributes.ROOM_MEMBER.type(),
                    Set.of()),
                new AttributeSchema(
                    ChatPolicyAttributes.MESSAGE_AUTHOR.name(),
                    ChatPolicyAttributes.MESSAGE_AUTHOR.type(),
                    Set.of()),
                new AttributeSchema(
                    ChatPolicyAttributes.MODERATOR.name(),
                    ChatPolicyAttributes.MODERATOR.type(),
                    Set.of())),
            List.of());
    }
}
