package com.umc.product.project.application.authorization;

import static com.umc.product.authorization.domain.PermissionType.APPROVE;
import static com.umc.product.authorization.domain.PermissionType.DELETE;
import static com.umc.product.authorization.domain.PermissionType.EDIT;
import static com.umc.product.authorization.domain.PermissionType.MANAGE;
import static com.umc.product.authorization.domain.PermissionType.READ;
import static com.umc.product.authorization.domain.PermissionType.WRITE;
import static com.umc.product.authorization.domain.ResourceType.PROJECT;
import static com.umc.product.authorization.domain.ResourceType.PROJECT_APPLICATION;
import static com.umc.product.project.application.authorization.ProjectPolicyAction.APPLICATION_CANCEL;
import static com.umc.product.project.application.authorization.ProjectPolicyAction.APPLICATION_CREATE;
import static com.umc.product.project.application.authorization.ProjectPolicyAction.APPLICATION_DECIDE;
import static com.umc.product.project.application.authorization.ProjectPolicyAction.APPLICATION_READ;
import static com.umc.product.project.application.authorization.ProjectPolicyAction.APPLICATION_SUBMIT;
import static com.umc.product.project.application.authorization.ProjectPolicyAction.APPLICATION_UPDATE;
import static com.umc.product.project.application.authorization.ProjectPolicyAction.FORM_UPDATE;
import static com.umc.product.project.application.authorization.ProjectPolicyAction.PROJECT_ABORT;
import static com.umc.product.project.application.authorization.ProjectPolicyAction.PROJECT_DELETE;
import static com.umc.product.project.application.authorization.ProjectPolicyAction.PROJECT_MEMBER_ADD;
import static com.umc.product.project.application.authorization.ProjectPolicyAction.PROJECT_MEMBER_BATCH;
import static com.umc.product.project.application.authorization.ProjectPolicyAction.PROJECT_MEMBER_LIST;
import static com.umc.product.project.application.authorization.ProjectPolicyAction.PROJECT_MEMBER_REMOVE;
import static com.umc.product.project.application.authorization.ProjectPolicyAction.PROJECT_MEMBER_STATUS_UPDATE;
import static com.umc.product.project.application.authorization.ProjectPolicyAction.PROJECT_PUBLISH;
import static com.umc.product.project.application.authorization.ProjectPolicyAction.PROJECT_QUOTA_UPDATE;
import static com.umc.product.project.application.authorization.ProjectPolicyAction.PROJECT_READ;
import static com.umc.product.project.application.authorization.ProjectPolicyAction.PROJECT_SUBMIT;
import static com.umc.product.project.application.authorization.ProjectPolicyAction.PROJECT_TRANSFER_OWNERSHIP;
import static com.umc.product.project.application.authorization.ProjectPolicyAction.PROJECT_UPDATE;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import com.umc.product.authorization.domain.PermissionType;
import com.umc.product.authorization.domain.ResourceType;

final class ProjectSemanticActionBindings {

    private static final List<Binding> VALUES = List.of(
        binding(PROJECT_UPDATE, PROJECT, EDIT),
        binding(PROJECT_SUBMIT, PROJECT, EDIT),
        binding(PROJECT_TRANSFER_OWNERSHIP, PROJECT, EDIT),
        binding(PROJECT_MEMBER_ADD, PROJECT, EDIT),
        binding(PROJECT_PUBLISH, PROJECT, MANAGE),
        binding(PROJECT_QUOTA_UPDATE, PROJECT, MANAGE),
        binding(PROJECT_DELETE, PROJECT, DELETE),
        binding(PROJECT_ABORT, PROJECT, MANAGE),
        binding(PROJECT_MEMBER_REMOVE, PROJECT, EDIT),
        binding(PROJECT_MEMBER_STATUS_UPDATE, PROJECT, EDIT),
        binding(PROJECT_READ, PROJECT, READ),
        binding(PROJECT_MEMBER_LIST, PROJECT, READ),
        binding(PROJECT_MEMBER_BATCH, PROJECT, READ),
        binding(FORM_UPDATE, PROJECT, EDIT),
        binding(APPLICATION_CREATE, PROJECT_APPLICATION, WRITE),
        binding(APPLICATION_UPDATE, PROJECT_APPLICATION, EDIT),
        binding(APPLICATION_SUBMIT, PROJECT_APPLICATION, EDIT),
        binding(APPLICATION_CANCEL, PROJECT_APPLICATION, DELETE),
        binding(APPLICATION_DECIDE, PROJECT_APPLICATION, APPROVE),
        binding(APPLICATION_READ, PROJECT_APPLICATION, READ)
    );

    private ProjectSemanticActionBindings() {
    }

    static List<Binding> values() {
        return VALUES;
    }

    static Set<ProjectPolicyAction> actions() {
        return VALUES.stream().map(Binding::action).collect(Collectors.toUnmodifiableSet());
    }

    private static Binding binding(
        ProjectPolicyAction action,
        ResourceType resourceType,
        PermissionType permissionType
    ) {
        return new Binding(action, resourceType, permissionType);
    }

    record Binding(
        ProjectPolicyAction action,
        ResourceType resourceType,
        PermissionType permissionType
    ) {
    }
}
