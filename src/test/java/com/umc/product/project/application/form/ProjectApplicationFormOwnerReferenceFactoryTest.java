package com.umc.product.project.application.form;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.umc.product.form.domain.FormOwnerReference;

@DisplayName("Project 지원 폼 owner reference factory")
class ProjectApplicationFormOwnerReferenceFactoryTest {

    @Test
    @DisplayName("서버가 project.application-form/{projectId}/default 좌표를 생성한다")
    void 정확한_owner_좌표를_생성한다() {
        FormOwnerReference reference = ProjectApplicationFormOwnerReferenceFactory
            .forProject(42L)
            .create(100L);

        assertThat(reference).isEqualTo(FormOwnerReference.of(
            100L,
            "project.application-form",
            "42",
            "default"
        ));
    }
}
