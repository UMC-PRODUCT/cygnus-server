package com.umc.product.member.application.authorization;

public enum MemberPolicyAction {
    READ("member:read"),
    DELETE("member:delete");

    private final String id;

    MemberPolicyAction(String id) {
        this.id = id;
    }

    public String id() {
        return id;
    }
}
