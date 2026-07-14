package com.umc.product.project.application.authorization;

public enum ProjectPolicyModule {
    PROJECT_RESOURCE("project-resource"),
    PROJECT_SCOPE("project-scope"),
    APPLICATION_RESOURCE("application-resource"),
    APPLICATION_SCOPE("application-scope"),
    FORM("form"),
    STATISTICS("statistics"),
    MATCHING_ROUND("matching-round");

    private final String id;

    ProjectPolicyModule(String id) {
        this.id = id;
    }

    public String id() {
        return id;
    }
}
