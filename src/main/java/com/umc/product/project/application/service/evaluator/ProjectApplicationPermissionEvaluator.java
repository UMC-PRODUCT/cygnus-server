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
import com.umc.product.project.application.port.out.LoadProjectApplicationPort;
import com.umc.product.project.application.port.out.LoadProjectMemberPort;
import com.umc.product.project.application.port.out.LoadProjectPort;
import com.umc.product.project.domain.Project;
import com.umc.product.project.domain.ProjectApplication;
import com.umc.product.project.domain.exception.ProjectDomainException;
import com.umc.product.project.domain.exception.ProjectErrorCode;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class ProjectApplicationPermissionEvaluator implements SemanticResourcePermissionEvaluator {

    private static final Set<ProjectPolicyAction> ACTIONS = Set.of(
        ProjectPolicyAction.APPLICATION_CREATE,
        ProjectPolicyAction.APPLICATION_READ,
        ProjectPolicyAction.APPLICATION_UPDATE,
        ProjectPolicyAction.APPLICATION_SUBMIT,
        ProjectPolicyAction.APPLICATION_CANCEL,
        ProjectPolicyAction.APPLICATION_DECIDE
    );

    private final LoadProjectPort loadProjectPort;
    private final LoadProjectApplicationPort loadProjectApplicationPort;
    private final LoadProjectMemberPort loadProjectMemberPort;
    private final ProjectPolicyAuthorizationService authorizationService;
    private final SuperAdminProperties superAdminProperties;

    @Override
    public ResourceType supportedResourceType() {
        return ResourceType.PROJECT_APPLICATION;
    }

    @Override
    public boolean evaluate(SubjectAttributes subject, ResourcePermission permission) {
        return switch (permission.permission()) {
            case WRITE -> evaluateProject(subject, permission, ProjectPolicyAction.APPLICATION_CREATE);
            case READ -> permission.resourceId() != null
                && evaluateApplication(subject, permission, List.of(ProjectPolicyAction.APPLICATION_READ));
            case EDIT -> evaluateApplication(subject, permission, List.of(
                ProjectPolicyAction.APPLICATION_UPDATE,
                ProjectPolicyAction.APPLICATION_SUBMIT));
            case DELETE -> evaluateApplication(
                subject, permission, List.of(ProjectPolicyAction.APPLICATION_CANCEL));
            case APPROVE -> evaluateApplication(
                subject, permission, List.of(ProjectPolicyAction.APPLICATION_DECIDE));
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
        if (action == ProjectPolicyAction.APPLICATION_CREATE) {
            return evaluateProject(subject, permission, action);
        }
        return evaluateApplication(subject, permission, List.of(action));
    }

    private boolean evaluateProject(
        SubjectAttributes subject,
        ResourcePermission permission,
        ProjectPolicyAction action
    ) {
        Project project = loadProjectPort.findById(permission.getResourceIdAsLong())
            .orElseThrow(() -> new ProjectDomainException(ProjectErrorCode.PROJECT_NOT_FOUND));
        return authorizationService.evaluate(subject, action, projectContext(project)).effect() == PolicyEffect.ALLOW;
    }

    private boolean evaluateApplication(
        SubjectAttributes subject,
        ResourcePermission permission,
        List<ProjectPolicyAction> actions
    ) {
        ProjectApplication application = loadProjectApplicationPort.findById(permission.getResourceIdAsLong())
            .orElseThrow(() -> new ProjectDomainException(ProjectErrorCode.PROJECT_APPLICATION_NOT_FOUND));
        Project project = application.getApplicationForm().getProject();
        boolean activePlanMember = actions.contains(ProjectPolicyAction.APPLICATION_READ)
            && loadProjectMemberPort.isActivePlanMember(project.getId(), subject.memberId());
        ProjectPolicyResourceContext resource = applicationContext(project, application, activePlanMember);
        return authorizationService.allowsAny(subject, actions, resource);
    }

    private ProjectPolicyResourceContext projectContext(Project project) {
        return ProjectPolicyResourceContext.builder()
            .project(project.getId(), project.getGisuId(), project.getChapterId(), project.getStatus())
            .creatorMemberId(project.getCreatorMemberId())
            .productOwnerMemberId(project.getProductOwnerMemberId())
            .superAdminAllowDraftRead(superAdminProperties.allowDraftRead())
            .build();
    }

    private ProjectPolicyResourceContext applicationContext(
        Project project,
        ProjectApplication application,
        boolean activePlanMember
    ) {
        return ProjectPolicyResourceContext.builder()
            .project(project.getId(), project.getGisuId(), project.getChapterId(), project.getStatus())
            .creatorMemberId(project.getCreatorMemberId())
            .productOwnerMemberId(project.getProductOwnerMemberId())
            .application(application.getId(), application.getStatus(), application.getApplicantMemberId())
            .activePlanMember(activePlanMember)
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
