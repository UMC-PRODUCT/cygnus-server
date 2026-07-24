package com.umc.product.chat.application.authorization;

import java.time.Instant;
import java.util.Objects;

public record ChatAuthorizationContext(
    ChatPolicyAction action,
    boolean roomMember,
    boolean messageAuthor,
    boolean moderator,
    Instant evaluatedAt
) {

    public ChatAuthorizationContext {
        Objects.requireNonNull(action);
        Objects.requireNonNull(evaluatedAt);
    }
}
