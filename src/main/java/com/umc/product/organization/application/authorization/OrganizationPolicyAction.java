package com.umc.product.organization.application.authorization;

public enum OrganizationPolicyAction {
    GISU_CREATE("gisu:create"),
    GISU_UPDATE("gisu:update"),
    GISU_DELETE("gisu:delete"),
    CHAPTER_CREATE("chapter:create"),
    CHAPTER_DELETE("chapter:delete"),
    SCHOOL_CREATE("school:create"),
    SCHOOL_UPDATE("school:update"),
    SCHOOL_DELETE("school:delete"),
    STUDY_GROUP_READ("study-group:read"),
    STUDY_GROUP_CREATE("study-group:create"),
    STUDY_GROUP_UPDATE("study-group:update"),
    STUDY_GROUP_DELETE("study-group:delete");

    private final String id;

    OrganizationPolicyAction(String id) {
        this.id = id;
    }

    public String id() {
        return id;
    }
}
