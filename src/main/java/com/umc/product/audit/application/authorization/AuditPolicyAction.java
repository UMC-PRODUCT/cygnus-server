package com.umc.product.audit.application.authorization;

public enum AuditPolicyAction {
    LIST("audit-log:list");

    private final String id;

    AuditPolicyAction(String id) {
        this.id = id;
    }

    public String id() {
        return id;
    }
}
