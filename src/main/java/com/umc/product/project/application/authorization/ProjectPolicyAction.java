package com.umc.product.project.application.authorization;

import java.util.Arrays;
import java.util.Set;

public enum ProjectPolicyAction {
    PROJECT_CREATE("project:create"),
    PROJECT_UPDATE("project:update-info"),
    PROJECT_SUBMIT("project:submit-review"),
    PROJECT_TRANSFER_OWNERSHIP("project:transfer-ownership"),
    PROJECT_MEMBER_ADD("project-member:add"),
    PROJECT_PUBLISH("project:publish"),
    PROJECT_QUOTA_UPDATE("project:update-part-quota"),
    PROJECT_DELETE("project:delete"),
    PROJECT_ABORT("project:abort"),
    PROJECT_MEMBER_REMOVE("project-member:remove"),
    PROJECT_MEMBER_STATUS_UPDATE("project-member:change-status"),
    PROJECT_LIST_PUBLIC("project:list-public"),
    PROJECT_READ("project:read"),
    PROJECT_MEMBER_LIST("project-member:list"),
    PROJECT_MEMBER_BATCH("project-member:batch"),
    PROJECT_LIST_MANAGED("project:list-managed"),
    PROJECT_LIST_OWN_DRAFTS("project:list-own-drafts"),
    APPLICATION_CREATE("project-application:create"),
    APPLICATION_UPDATE("project-application:update"),
    APPLICATION_SUBMIT("project-application:submit"),
    APPLICATION_DECIDE("project-application:decide"),
    APPLICATION_CANCEL("project-application:cancel"),
    APPLICATION_LIST_SELF("project-application:list-self"),
    APPLICATION_LIST_PROJECT_BATCH("project-application:list-project-batch"),
    APPLICATION_LIST_PROJECT("project-application:list-project"),
    APPLICATION_LIST_MANAGEMENT("project-application:list-management"),
    APPLICATION_READ("project-application:read"),
    FORM_UPDATE("project-form:update"),
    FORM_READ("project-form:read"),
    MATCHING_LIST("project-matching-round:list"),
    MATCHING_CREATE("project-matching-round:create"),
    MATCHING_UPDATE("project-matching-round:update"),
    MATCHING_DELETE("project-matching-round:delete"),
    MATCHING_HUMAN_AUTO_DECIDE("project-matching-round:human-auto-decide"),
    MATCHING_SYSTEM_AUTO_DECIDE("project-matching-round:system-auto-decide"),
    STATISTICS_PROJECT("project-statistics:read-project"),
    STATISTICS_CHAPTER("project-statistics:read-chapter"),
    STATISTICS_PUBLIC_MATCHING("project-statistics:read-public-matching"),
    CAPABILITY_LIST("project:capability-list");

    private static final Set<ProjectPolicyAction> NON_SURFACE_ACTIONS = Set.of(APPLICATION_LIST_MANAGEMENT);

    private final String id;

    ProjectPolicyAction(String id) {
        this.id = id;
    }

    public String id() {
        return id;
    }

    public static ProjectPolicyAction fromId(String id) {
        return Arrays.stream(values())
            .filter(action -> action.id.equals(id))
            .findFirst()
            .orElseThrow(() -> new IllegalArgumentException("등록되지 않은 Project 정책 action입니다: " + id));
    }

    public static Set<ProjectPolicyAction> nonSurfaceActions() {
        return NON_SURFACE_ACTIONS;
    }
}
