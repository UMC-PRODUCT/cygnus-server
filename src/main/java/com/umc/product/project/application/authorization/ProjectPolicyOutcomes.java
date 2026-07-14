package com.umc.product.project.application.authorization;

public final class ProjectPolicyOutcomes {

    public static final String PROJECT_ALL = "project.scope.all";
    public static final String PROJECT_PUBLIC_ONLY = "project.scope.publicOnly";
    public static final String PROJECT_GISU_IDS = "project.scope.gisuIds";
    public static final String PROJECT_CHAPTER_IDS = "project.scope.chapterIds";
    public static final String PROJECT_OWNER_MEMBER_IDS = "project.scope.ownerMemberIds";
    public static final String PROJECT_INCLUDE_OWN_DRAFTS = "project.scope.includeOwnDrafts";

    public static final String APPLICATION_ALL = "application.scope.all";
    public static final String APPLICATION_GISU_IDS = "application.scope.gisuIds";
    public static final String APPLICATION_CHAPTER_IDS = "application.scope.chapterIds";
    public static final String APPLICATION_PROJECT_IDS = "application.scope.projectIds";
    public static final String APPLICATION_OWNER_MEMBER_IDS = "application.scope.ownerMemberIds";
    public static final String APPLICATION_INCLUDE_ONGOING_ROUNDS = "application.includeOngoingRounds";

    public static final String FORM_VIEW = "form.view";
    public static final String APPLICATION_FORCE_DECISION = "application.forceDecision";

    private ProjectPolicyOutcomes() {}
}
