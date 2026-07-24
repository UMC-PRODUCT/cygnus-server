package com.umc.product.blog.application.authorization;

public enum BlogPolicyAction {
    CONTENT_CREATE("blog-content:create"),
    CONTENT_READ("blog-content:read"),
    CONTENT_UPDATE("blog-content:update"),
    CONTENT_DELETE("blog-content:delete"),
    SERIES_CREATE("blog-series:create"),
    SERIES_READ("blog-series:read"),
    SERIES_UPDATE("blog-series:update"),
    SERIES_DELETE("blog-series:delete"),
    COMMENT_UPDATE("blog-comment:update"),
    COMMENT_DELETE("blog-comment:delete"),
    ADMIN_VIEW("blog:admin-view");

    private final String id;

    BlogPolicyAction(String id) {
        this.id = id;
    }

    public String id() {
        return id;
    }
}
