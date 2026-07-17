package com.umc.product.project.application.form;

import java.util.List;

import org.springframework.stereotype.Component;

import com.umc.product.form.application.port.in.FormActorContext;
import com.umc.product.form.application.port.out.FormOwnerPolicy;
import com.umc.product.form.domain.FormOperation;
import com.umc.product.form.domain.FormOwnerReference;
import com.umc.product.project.application.port.in.query.GetProjectPermissionsUseCase;
import com.umc.product.project.application.port.in.query.dto.ProjectPermissionInfo;

import lombok.RequiredArgsConstructor;

/** Project public permission 경계를 통해 지원 폼 접근을 판정하는 consumer policy다. */
@Component
@RequiredArgsConstructor
public class ProjectApplicationFormOwnerPolicy implements FormOwnerPolicy {

    private final GetProjectPermissionsUseCase getProjectPermissionsUseCase;

    @Override
    public String namespace() {
        return ProjectApplicationFormOwnerReferenceFactory.NAMESPACE;
    }

    @Override
    public boolean allows(
        FormOwnerReference ownerReference,
        FormOperation operation,
        FormActorContext actorContext
    ) {
        Long projectId = parseCanonicalProjectId(ownerReference);
        if (projectId == null || operation == null || actorContext == null) {
            return false;
        }
        Long memberId = actorContext.authenticatedMemberId().orElse(null);
        if (memberId == null) {
            return false;
        }

        return getProjectPermissionsUseCase.listByProjectIds(memberId, List.of(projectId)).stream()
            .filter(permission -> projectId.equals(permission.projectId()) && permission.exists())
            .findFirst()
            .map(permission -> allows(permission, operation))
            .orElse(false);
    }

    private Long parseCanonicalProjectId(FormOwnerReference ownerReference) {
        if (ownerReference == null
            || !namespace().equals(ownerReference.namespace())
            || !ProjectApplicationFormOwnerReferenceFactory.DEFAULT_SLOT.equals(ownerReference.slot())) {
            return null;
        }
        try {
            Long projectId = Long.valueOf(ownerReference.ownerResourceKey());
            if (projectId <= 0 || !projectId.toString().equals(ownerReference.ownerResourceKey())) {
                return null;
            }
            return projectId;
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    private boolean allows(ProjectPermissionInfo permission, FormOperation operation) {
        return switch (operation) {
            case MANAGE_STRUCTURE -> permission.applicationForm().canCreate().allowed()
                || permission.applicationForm().canEdit().allowed();
            case PUBLISH -> permission.status().canPublish().allowed();
            case DELETE -> permission.canDelete().allowed();
            case READ -> permission.applicationForm().canRead().allowed();
            case RESPOND -> permission.application().canCreate().allowed();
            case READ_RESPONSES -> permission.application().canReadList().allowed();
        };
    }
}
