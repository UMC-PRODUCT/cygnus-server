package com.umc.product.project.application.service.evaluator;

import java.util.List;
import java.util.Set;

import org.springframework.stereotype.Component;

import com.umc.product.authorization.application.port.out.SemanticResourcePermissionEvaluator;
import com.umc.product.authorization.domain.ResourcePermission;
import com.umc.product.authorization.domain.ResourceType;
import com.umc.product.authorization.domain.SubjectAttributes;
import com.umc.product.authorization.domain.exception.AuthorizationDomainException;
import com.umc.product.authorization.domain.exception.AuthorizationErrorCode;
import com.umc.product.authorization.domain.policy.PolicyEffect;
import com.umc.product.project.application.authorization.ProjectPolicyAction;
import com.umc.product.project.application.authorization.ProjectPolicyAuthorizationService;
import com.umc.product.project.application.authorization.ProjectPolicyResourceContext;
import com.umc.product.project.application.port.out.LoadProjectPort;
import com.umc.product.project.domain.Project;
import com.umc.product.project.domain.exception.ProjectDomainException;
import com.umc.product.project.domain.exception.ProjectErrorCode;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class ProjectPermissionEvaluator implements SemanticResourcePermissionEvaluator {

    private static final Set<ProjectPolicyAction> ACTIONS = Set.of(
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
        ProjectPolicyAction.PROJECT_READ,
        ProjectPolicyAction.PROJECT_MEMBER_LIST,
        ProjectPolicyAction.PROJECT_MEMBER_BATCH,
        ProjectPolicyAction.FORM_UPDATE
    );

    private final LoadProjectPort loadProjectPort;
    private final ProjectPolicyAuthorizationService authorizationService;
    private final SuperAdminProperties superAdminProperties;

    @Override
    public ResourceType supportedResourceType() {
        return ResourceType.PROJECT;
    }

    @Override
    public boolean evaluate(SubjectAttributes subject, ResourcePermission permission) {
        return switch (permission.permission()) {
            case READ -> permission.resourceId() == null || evaluateProject(
                subject, permission, List.of(ProjectPolicyAction.PROJECT_READ));
            case WRITE -> false;
            case EDIT -> evaluateProject(subject, permission, List.of(
                ProjectPolicyAction.PROJECT_UPDATE,
                ProjectPolicyAction.PROJECT_SUBMIT,
                ProjectPolicyAction.PROJECT_TRANSFER_OWNERSHIP,
                ProjectPolicyAction.PROJECT_MEMBER_ADD,
                ProjectPolicyAction.PROJECT_MEMBER_REMOVE,
                ProjectPolicyAction.PROJECT_MEMBER_STATUS_UPDATE,
                ProjectPolicyAction.FORM_UPDATE));
            case MANAGE -> evaluateProject(subject, permission, List.of(
                ProjectPolicyAction.PROJECT_PUBLISH,
                ProjectPolicyAction.PROJECT_QUOTA_UPDATE,
                ProjectPolicyAction.PROJECT_ABORT));
            case DELETE -> evaluateProject(subject, permission, List.of(ProjectPolicyAction.PROJECT_DELETE));
            default -> false;
        };
    }

    @Override
    public boolean evaluateSemantic(
        SubjectAttributes subject,
        ResourcePermission permission,
        String actionId
    ) {
        ProjectPolicyAction action = action(actionId);
        if (action == ProjectPolicyAction.PROJECT_MEMBER_BATCH) {
            return authorizationService.evaluate(
                subject, action, ProjectPolicyResourceContext.builder().build()).effect()
                == PolicyEffect.ALLOW;
        }
        return evaluateProject(subject, permission, List.of(action));
    }

    private boolean evaluateProject(
        SubjectAttributes subject,
        ResourcePermission permission,
        List<ProjectPolicyAction> actions
    ) {
        Project project = loadProjectPort.findById(permission.getResourceIdAsLong())
            .orElseThrow(() -> new ProjectDomainException(ProjectErrorCode.PROJECT_NOT_FOUND));
        return authorizationService.allowsAny(subject, actions, context(project));
    }

    private ProjectPolicyResourceContext context(Project project) {
        return ProjectPolicyResourceContext.builder()
            .project(project.getId(), project.getGisuId(), project.getChapterId(), project.getStatus())
            .creatorMemberId(project.getCreatorMemberId())
            .productOwnerMemberId(project.getProductOwnerMemberId())
            .superAdminAllowDraftRead(superAdminProperties.allowDraftRead())
            .build();
    }

    private ProjectPolicyAction action(String actionId) {
        try {
            ProjectPolicyAction action = ProjectPolicyAction.fromId(actionId);
            if (!ACTIONS.contains(action)) {
                throw new IllegalArgumentException();
            }
            return action;
        } catch (IllegalArgumentException exception) {
            throw new AuthorizationDomainException(AuthorizationErrorCode.POLICY_EVALUATION_FAILED);
        }
    }
}
