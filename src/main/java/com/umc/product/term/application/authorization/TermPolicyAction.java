package com.umc.product.term.application.authorization;

public enum TermPolicyAction {
    CREATE("term:create");

    private final String id;

    TermPolicyAction(String id) {
        this.id = id;
    }

    public String id() {
        return id;
    }
}
