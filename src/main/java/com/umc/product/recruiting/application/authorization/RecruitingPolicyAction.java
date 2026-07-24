package com.umc.product.recruiting.application.authorization;

public enum RecruitingPolicyAction {
    OPERATE_SCHOOL("recruiting:operate-school"),
    MANAGE_ALL("recruiting:manage-all"),
    SEASON_CREATE("recruiting-season:create"),
    APPLICATION_DECIDE("recruiting-application:decide"),
    REGISTRATION_MANAGE("recruiting-registration:manage"),
    SUMMARY_READ("recruiting-summary:read"),
    CSV_EXPORT("recruiting:export");

    private final String id;

    RecruitingPolicyAction(String id) {
        this.id = id;
    }

    public String id() {
        return id;
    }
}
