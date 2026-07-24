package com.umc.product.notice.application.authorization;

public enum NoticePolicyAction {
    READ("notice:read"),
    CREATE("notice:create"),
    UPDATE("notice:update"),
    DELETE("notice:delete"),
    READ_RECIPIENTS("notice:read-recipients"),
    CHECK_RECIPIENTS("notice:check-recipients");

    private final String id;

    NoticePolicyAction(String id) {
        this.id = id;
    }

    public String id() {
        return id;
    }
}
