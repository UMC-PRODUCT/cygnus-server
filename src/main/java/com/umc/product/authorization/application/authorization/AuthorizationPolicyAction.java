package com.umc.product.authorization.application.authorization;

import com.umc.product.authorization.domain.PermissionType;

public enum AuthorizationPolicyAction {
    CHALLENGER_ROLE_READ("challenger-role:read"),
    CHALLENGER_ROLE_CREATE("challenger-role:create"),
    CHALLENGER_ROLE_DELETE("challenger-role:delete");

    private final String id;

    AuthorizationPolicyAction(String id) {
        this.id = id;
    }

    public String id() {
        return id;
    }

    public static AuthorizationPolicyAction from(PermissionType permission) {
        return switch (permission) {
            case READ -> CHALLENGER_ROLE_READ;
            case WRITE -> CHALLENGER_ROLE_CREATE;
            case DELETE -> CHALLENGER_ROLE_DELETE;
            default -> throw new IllegalArgumentException(
                "지원하지 않는 ChallengerRole permission입니다: " + permission);
        };
    }
}
