package com.umc.product.challenger.application.authorization;

public enum ChallengerPolicyAction {
    CHALLENGER_CREATE("challenger:create"),
    CHALLENGER_UPDATE("challenger:update"),
    CHALLENGER_DELETE("challenger:delete"),
    POINT_CREATE("challenger-point:create"),
    POINT_UPDATE("challenger-point:update"),
    POINT_DELETE("challenger-point:delete"),
    RECORD_READ("challenger-record:read"),
    RECORD_CREATE("challenger-record:create"),
    RECORD_DELETE("challenger-record:delete");

    private final String id;

    ChallengerPolicyAction(String id) {
        this.id = id;
    }

    public String id() {
        return id;
    }
}
