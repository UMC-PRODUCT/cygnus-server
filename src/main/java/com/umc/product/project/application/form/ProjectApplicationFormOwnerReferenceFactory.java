package com.umc.product.project.application.form;

import com.umc.product.form.application.port.in.FormOwnerReferenceFactory;
import com.umc.product.form.domain.FormOwnerReference;

/** Project 지원 폼의 server-owned ownership 좌표를 생성한다. */
public final class ProjectApplicationFormOwnerReferenceFactory implements FormOwnerReferenceFactory {

    public static final String NAMESPACE = "project.application-form";
    public static final String DEFAULT_SLOT = "default";

    private final Long projectId;

    private ProjectApplicationFormOwnerReferenceFactory(Long projectId) {
        if (projectId == null || projectId <= 0) {
            throw new IllegalArgumentException("project id는 양수여야 합니다.");
        }
        this.projectId = projectId;
    }

    public static ProjectApplicationFormOwnerReferenceFactory forProject(Long projectId) {
        return new ProjectApplicationFormOwnerReferenceFactory(projectId);
    }

    @Override
    public FormOwnerReference create(Long savedFormId) {
        return FormOwnerReference.of(
            savedFormId,
            NAMESPACE,
            projectId.toString(),
            DEFAULT_SLOT
        );
    }
}
