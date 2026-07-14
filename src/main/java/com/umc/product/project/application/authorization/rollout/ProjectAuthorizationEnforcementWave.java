package com.umc.product.project.application.authorization.rollout;

import java.util.EnumSet;
import java.util.Set;

import com.umc.product.project.application.authorization.ProjectPolicyAction;

public enum ProjectAuthorizationEnforcementWave {
    READ_CAPABILITY(EnumSet.of(
        ProjectPolicyAction.PROJECT_READ,
        ProjectPolicyAction.PROJECT_MEMBER_LIST,
        ProjectPolicyAction.PROJECT_MEMBER_BATCH,
        ProjectPolicyAction.APPLICATION_READ,
        ProjectPolicyAction.FORM_READ,
        ProjectPolicyAction.CAPABILITY_LIST
    )),
    LIST_GRAPHQL(EnumSet.of(
        ProjectPolicyAction.PROJECT_LIST_PUBLIC,
        ProjectPolicyAction.PROJECT_LIST_MANAGED,
        ProjectPolicyAction.PROJECT_LIST_OWN_DRAFTS,
        ProjectPolicyAction.APPLICATION_LIST_SELF,
        ProjectPolicyAction.APPLICATION_LIST_PROJECT_BATCH,
        ProjectPolicyAction.APPLICATION_LIST_PROJECT,
        ProjectPolicyAction.APPLICATION_LIST_MANAGEMENT
    )),
    COMMAND_STATISTICS(EnumSet.of(
        ProjectPolicyAction.PROJECT_CREATE,
        ProjectPolicyAction.PROJECT_UPDATE,
        ProjectPolicyAction.PROJECT_SUBMIT,
        ProjectPolicyAction.PROJECT_TRANSFER_OWNERSHIP,
        ProjectPolicyAction.PROJECT_MEMBER_ADD,
        ProjectPolicyAction.PROJECT_PUBLISH,
        ProjectPolicyAction.PROJECT_QUOTA_UPDATE,
        ProjectPolicyAction.PROJECT_DELETE,
        ProjectPolicyAction.PROJECT_ABORT,
        ProjectPolicyAction.PROJECT_MEMBER_REMOVE,
        ProjectPolicyAction.PROJECT_MEMBER_STATUS_UPDATE,
        ProjectPolicyAction.APPLICATION_CREATE,
        ProjectPolicyAction.APPLICATION_UPDATE,
        ProjectPolicyAction.APPLICATION_SUBMIT,
        ProjectPolicyAction.APPLICATION_DECIDE,
        ProjectPolicyAction.APPLICATION_CANCEL,
        ProjectPolicyAction.FORM_UPDATE,
        ProjectPolicyAction.STATISTICS_PROJECT,
        ProjectPolicyAction.STATISTICS_CHAPTER,
        ProjectPolicyAction.STATISTICS_PUBLIC_MATCHING
    )),
    MATCHING_SCHEDULER(EnumSet.of(
        ProjectPolicyAction.MATCHING_LIST,
        ProjectPolicyAction.MATCHING_CREATE,
        ProjectPolicyAction.MATCHING_UPDATE,
        ProjectPolicyAction.MATCHING_DELETE,
        ProjectPolicyAction.MATCHING_HUMAN_AUTO_DECIDE,
        ProjectPolicyAction.MATCHING_SYSTEM_AUTO_DECIDE
    ));

    private final Set<ProjectPolicyAction> actions;

    ProjectAuthorizationEnforcementWave(Set<ProjectPolicyAction> actions) {
        this.actions = Set.copyOf(actions);
    }

    public Set<ProjectPolicyAction> actions() {
        return actions;
    }
}
