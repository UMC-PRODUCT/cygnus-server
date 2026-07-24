package com.umc.product.maintenance.application.authorization;

public enum MaintenancePolicyAction {
    BYPASS("maintenance:bypass");

    private final String id;

    MaintenancePolicyAction(String id) {
        this.id = id;
    }

    public String id() {
        return id;
    }
}
