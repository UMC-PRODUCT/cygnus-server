package com.umc.product.analytics.application.authorization;

public enum AnalyticsPolicyAction {
    READ_DASHBOARD("analytics:read-dashboard"),
    READ_SCHOOL("analytics:read-school");

    private final String id;

    AnalyticsPolicyAction(String id) {
        this.id = id;
    }

    public String id() {
        return id;
    }
}
