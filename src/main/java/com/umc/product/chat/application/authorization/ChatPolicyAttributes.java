package com.umc.product.chat.application.authorization;

import com.umc.product.authorization.domain.policy.PolicyAttributeKey;
import com.umc.product.authorization.domain.policy.PolicyValue;
import com.umc.product.authorization.domain.policy.PolicyValueType;

public final class ChatPolicyAttributes {

    public static final PolicyAttributeKey<PolicyValue.BooleanValue> ROOM_MEMBER =
        new PolicyAttributeKey<>("relation.isRoomMember", PolicyValueType.BOOLEAN);
    public static final PolicyAttributeKey<PolicyValue.BooleanValue> MESSAGE_AUTHOR =
        new PolicyAttributeKey<>("relation.isMessageAuthor", PolicyValueType.BOOLEAN);
    public static final PolicyAttributeKey<PolicyValue.BooleanValue> MODERATOR =
        new PolicyAttributeKey<>("relation.isModerator", PolicyValueType.BOOLEAN);

    private ChatPolicyAttributes() {
    }
}
