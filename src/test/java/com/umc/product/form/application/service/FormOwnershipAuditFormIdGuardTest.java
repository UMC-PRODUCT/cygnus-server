package com.umc.product.form.application.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.umc.product.form.application.port.in.FormActorContext;
import com.umc.product.form.application.port.out.FormOwnerPolicy;
import com.umc.product.form.application.port.out.LoadFormOwnershipPort;
import com.umc.product.form.application.port.out.SaveFormOwnershipPort;
import com.umc.product.form.domain.FormOperation;
import com.umc.product.form.domain.FormOwnerReference;
import com.umc.product.form.domain.exception.FormDomainException;
import com.umc.product.form.domain.exception.FormErrorCode;
import com.umc.product.global.logging.OperationalMetrics;
import com.umc.product.registry.application.port.in.query.GetRegistryReadinessUseCase;
import com.umc.product.registry.domain.OwnershipEnforcementMode;
import com.umc.product.registry.domain.RegistryName;

@DisplayName("Form ownership AUDIT Form ID guard")
class FormOwnershipAuditFormIdGuardTest {

    private static final Long RESOLVED_FORM_ID = 11L;
    private static final FormOwnerReference EXPECTED_OWNER = FormOwnerReference.of(
        10L, "project.application-form", "20", "default"
    );
    private static final FormActorContext ACTOR = FormActorContext.authenticated(30L);

    @Test
    @DisplayName("AUDIT mutation missing binding은 Form ID가 다르면 policy 전에 거부한다")
    void auditMutationMissingBinding은FormId가다르면거부한다() {
        // Given
        TestFixture fixture = fixture();
        given(fixture.loadPort.findByFormIdForUpdate(RESOLVED_FORM_ID)).willReturn(Optional.empty());

        // When / Then
        assertOwnershipDenied(() -> fixture.sut.requireMutation(
            RESOLVED_FORM_ID, EXPECTED_OWNER, ACTOR, FormOperation.DELETE
        ));
        verifyDeniedBeforePolicyAndPersistence(fixture, FormOperation.DELETE);
    }

    @Test
    @DisplayName("AUDIT read missing binding은 Form ID가 다르면 policy 전에 거부한다")
    void auditReadMissingBinding은FormId가다르면거부한다() {
        // Given
        TestFixture fixture = fixture();
        given(fixture.loadPort.findByFormId(RESOLVED_FORM_ID)).willReturn(Optional.empty());

        // When / Then
        assertOwnershipDenied(() -> fixture.sut.requireRead(
            RESOLVED_FORM_ID, EXPECTED_OWNER, ACTOR, FormOperation.READ
        ));
        verifyDeniedBeforePolicyAndPersistence(fixture, FormOperation.READ);
    }

    private static TestFixture fixture() {
        LoadFormOwnershipPort loadPort = mock(LoadFormOwnershipPort.class);
        SaveFormOwnershipPort savePort = mock(SaveFormOwnershipPort.class);
        FormOwnerPolicy policy = mock(FormOwnerPolicy.class);
        GetRegistryReadinessUseCase readiness = mock(GetRegistryReadinessUseCase.class);
        OperationalMetrics metrics = mock(OperationalMetrics.class);
        given(policy.namespace()).willReturn(EXPECTED_OWNER.namespace());
        given(policy.allows(any(), any(), any())).willReturn(true);
        given(readiness.ownershipMode(RegistryName.FORM_OWNERSHIP))
            .willReturn(OwnershipEnforcementMode.AUDIT);
        FormOwnershipAccessService sut = new FormOwnershipAccessService(
            loadPort,
            savePort,
            new FormOwnerPolicyRegistry(List.of(policy)),
            readiness,
            metrics
        );
        return new TestFixture(sut, loadPort, savePort, policy, metrics);
    }

    private static void verifyDeniedBeforePolicyAndPersistence(
        TestFixture fixture,
        FormOperation operation
    ) {
        verify(fixture.policy, never()).allows(EXPECTED_OWNER, operation, ACTOR);
        verify(fixture.metrics, never())
            .recordSecurityEvent("form", "ownership_missing_binding", "audit_allowed");
        verifyNoInteractions(fixture.loadPort, fixture.savePort);
    }

    private static void assertOwnershipDenied(
        org.assertj.core.api.ThrowableAssert.ThrowingCallable callable
    ) {
        assertThatThrownBy(callable)
            .isInstanceOf(FormDomainException.class)
            .hasFieldOrPropertyWithValue("baseCode", FormErrorCode.FORM_OWNERSHIP_FORBIDDEN);
    }

    private record TestFixture(
        FormOwnershipAccessService sut,
        LoadFormOwnershipPort loadPort,
        SaveFormOwnershipPort savePort,
        FormOwnerPolicy policy,
        OperationalMetrics metrics
    ) {
    }
}
