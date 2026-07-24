package com.umc.product.community.application.authorization;

public enum CommunityPolicyAction {
    POST_READ("community-post:read"),
    POST_WRITE("community-post:write"),
    POST_UPDATE("community-post:update"),
    POST_DELETE("community-post:delete"),
    COMMENT_READ("community-comment:read"),
    COMMENT_WRITE("community-comment:write"),
    COMMENT_UPDATE("community-comment:update"),
    COMMENT_DELETE("community-comment:delete");

    private final String id;

    CommunityPolicyAction(String id) {
        this.id = id;
    }

    public String id() {
        return id;
    }
}
