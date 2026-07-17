package com.umc.product.project.application.form;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verifyNoInteractions;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.umc.product.form.application.port.in.FormActorContext;
import com.umc.product.form.application.port.out.LoadFormOwnershipPort;
import com.umc.product.form.application.port.out.SaveFormOwnershipPort;
import com.umc.product.form.application.service.FormOwnerPolicyRegistry;
import com.umc.product.form.application.service.FormOwnershipAccessService;
import com.umc.product.form.domain.FormOperation;
import com.umc.product.form.domain.FormOwnerReference;
import com.umc.product.form.domain.FormOwnership;
import com.umc.product.form.domain.exception.FormDomainException;
import com.umc.product.project.application.port.in.query.GetProjectPermissionsUseCase;

@ExtendWith(MockitoExtension.class)
@DisplayName("Project 지원 폼 ownership contract")
class ProjectApplicationFormOwnershipContractTest {

    @Mock
    LoadFormOwnershipPort loadFormOwnershipPort;
    @Mock
    SaveFormOwnershipPort saveFormOwnershipPort;
    @Mock
    GetProjectPermissionsUseCase getProjectPermissionsUseCase;

    FormOwnershipAccessService sut;

    @BeforeEach
    void setUp() {
        ProjectApplicationFormOwnerPolicy policy = new ProjectApplicationFormOwnerPolicy(
            getProjectPermissionsUseCase
        );
        sut = new FormOwnershipAccessService(
            loadFormOwnershipPort,
            saveFormOwnershipPort,
            new FormOwnerPolicyRegistry(List.of(policy))
        );
    }

    @Test
    @DisplayName("다른 Project owner key로 같은 Form에 접근하면 policy 전에 거부한다")
    void 다른_Project_owner를_거부한다() {
        FormOwnerReference actual = ProjectApplicationFormOwnerReferenceFactory.forProject(42L).create(100L);
        FormOwnerReference wrongProject = ProjectApplicationFormOwnerReferenceFactory.forProject(43L).create(100L);
        given(loadFormOwnershipPort.findByFormId(100L))
            .willReturn(Optional.of(FormOwnership.from(actual)));

        assertThatThrownBy(() -> sut.requireRead(
            100L,
            wrongProject,
            FormActorContext.authenticated(7L),
            FormOperation.READ
        )).isInstanceOf(FormDomainException.class);

        verifyNoInteractions(getProjectPermissionsUseCase);
    }

    @Test
    @DisplayName("child에서 resolve한 Form ID가 expected owner Form ID와 다르면 거부한다")
    void 다른_Form으로_resolve된_child를_거부한다() {
        FormOwnerReference actual = ProjectApplicationFormOwnerReferenceFactory.forProject(42L).create(100L);
        FormOwnerReference otherForm = ProjectApplicationFormOwnerReferenceFactory.forProject(42L).create(200L);
        given(loadFormOwnershipPort.findByFormId(100L))
            .willReturn(Optional.of(FormOwnership.from(actual)));

        assertThatThrownBy(() -> sut.requireRead(
            100L,
            otherForm,
            FormActorContext.authenticated(7L),
            FormOperation.READ
        )).isInstanceOf(FormDomainException.class);

        verifyNoInteractions(getProjectPermissionsUseCase);
    }
}
