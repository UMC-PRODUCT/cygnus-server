package com.umc.product.form.application.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;

import com.umc.product.form.application.port.in.FormActorContext;
import com.umc.product.form.application.port.in.FormOwnerReferenceFactory;
import com.umc.product.form.application.port.out.FormOwnerPolicy;
import com.umc.product.form.application.port.out.LoadFormOwnershipPort;
import com.umc.product.form.application.port.out.SaveFormOwnershipPort;
import com.umc.product.form.domain.FormOperation;
import com.umc.product.form.domain.FormOwnerReference;
import com.umc.product.form.domain.FormOwnership;
import com.umc.product.form.domain.exception.FormDomainException;
import com.umc.product.form.domain.exception.FormErrorCode;
import com.umc.product.global.logging.OperationalMetrics;
import com.umc.product.registry.application.port.in.query.GetRegistryReadinessUseCase;
import com.umc.product.registry.domain.OwnershipEnforcementMode;
import com.umc.product.registry.domain.RegistryName;

@DisplayName("FormOwnershipAccessService")
class FormOwnershipAccessServiceTest {

    private static final FormOwnerReference EXPECTED = FormOwnerReference.of(
        10L, "project.application-form", "20", "default"
    );
    private static final FormActorContext ACTOR = FormActorContext.authenticated(30L);

    @Test
    @DisplayName("mutation은 ownership row를 잠근 뒤 expected owner와 policy를 순서대로 검증한다")
    void mutation은_ownership_row를_잠근_뒤_expected_owner와_policy를_순서대로_검증한다() {
        LoadFormOwnershipPort loadPort = mock(LoadFormOwnershipPort.class);
        SaveFormOwnershipPort savePort = mock(SaveFormOwnershipPort.class);
        FormOwnerPolicy policy = mock(FormOwnerPolicy.class);
        given(policy.namespace()).willReturn(EXPECTED.namespace());
        FormOwnerPolicyRegistry registry = new FormOwnerPolicyRegistry(List.of(policy));
        FormOwnershipAccessService sut = strictService(loadPort, savePort, registry);
        given(loadPort.findByFormIdForUpdate(EXPECTED.formId()))
            .willReturn(Optional.of(FormOwnership.from(EXPECTED)));
        given(policy.allows(EXPECTED, FormOperation.MANAGE_STRUCTURE, ACTOR)).willReturn(true);

        sut.requireMutation(EXPECTED.formId(), EXPECTED, ACTOR, FormOperation.MANAGE_STRUCTURE);

        InOrder order = inOrder(loadPort, policy);
        order.verify(loadPort).findByFormIdForUpdate(EXPECTED.formId());
        order.verify(policy).allows(EXPECTED, FormOperation.MANAGE_STRUCTURE, ACTOR);
    }

    @Test
    @DisplayName("query는 ownership row를 잠그지 않고 검증한다")
    void query는_ownership_row를_잠그지_않고_검증한다() {
        LoadFormOwnershipPort loadPort = mock(LoadFormOwnershipPort.class);
        SaveFormOwnershipPort savePort = mock(SaveFormOwnershipPort.class);
        FormOwnerPolicy policy = policy(EXPECTED.namespace(), true);
        FormOwnershipAccessService sut = strictService(
            loadPort,
            savePort,
            new FormOwnerPolicyRegistry(List.of(policy))
        );
        given(loadPort.findByFormId(EXPECTED.formId()))
            .willReturn(Optional.of(FormOwnership.from(EXPECTED)));

        sut.requireRead(EXPECTED.formId(), EXPECTED, ACTOR, FormOperation.READ);

        verify(loadPort).findByFormId(EXPECTED.formId());
        verify(loadPort, never()).findByFormIdForUpdate(EXPECTED.formId());
    }

    @Test
    @DisplayName("binding이 없으면 policy와 mutation 전에 거부한다")
    void binding이_없으면_policy와_mutation_전에_거부한다() {
        LoadFormOwnershipPort loadPort = mock(LoadFormOwnershipPort.class);
        SaveFormOwnershipPort savePort = mock(SaveFormOwnershipPort.class);
        FormOwnerPolicy policy = policy(EXPECTED.namespace(), true);
        FormOwnershipAccessService sut = strictService(
            loadPort,
            savePort,
            new FormOwnerPolicyRegistry(List.of(policy))
        );
        given(loadPort.findByFormIdForUpdate(EXPECTED.formId())).willReturn(Optional.empty());

        assertOwnershipDenied(() -> sut.requireMutation(
            EXPECTED.formId(), EXPECTED, ACTOR, FormOperation.DELETE
        ));

        verify(policy, never()).allows(EXPECTED, FormOperation.DELETE, ACTOR);
        verifyNoInteractions(savePort);
    }

    @Test
    @DisplayName("audit은 기존 actor와 policy가 모두 허용한 missing legacy binding만 허용한다")
    void audit은_인가를_통과한_missing_legacy_binding만_허용한다() {
        LoadFormOwnershipPort loadPort = mock(LoadFormOwnershipPort.class);
        SaveFormOwnershipPort savePort = mock(SaveFormOwnershipPort.class);
        FormOwnerPolicy policy = policy(EXPECTED.namespace(), true);
        GetRegistryReadinessUseCase readiness = mock(GetRegistryReadinessUseCase.class);
        OperationalMetrics metrics = mock(OperationalMetrics.class);
        FormOwnershipAccessService sut = new FormOwnershipAccessService(
            loadPort,
            savePort,
            new FormOwnerPolicyRegistry(List.of(policy)),
            readiness,
            metrics
        );
        given(readiness.ownershipMode(RegistryName.FORM_OWNERSHIP))
            .willReturn(OwnershipEnforcementMode.AUDIT);
        given(loadPort.findByFormIdForUpdate(EXPECTED.formId())).willReturn(Optional.empty());

        sut.requireMutation(EXPECTED.formId(), EXPECTED, ACTOR, FormOperation.DELETE);

        verify(policy).allows(EXPECTED, FormOperation.DELETE, ACTOR);
        verify(metrics).recordSecurityEvent("form", "ownership_missing_binding", "audit_allowed");
        verifyNoInteractions(savePort);
    }

    @Test
    @DisplayName("actual root와 expected owner의 namespace 또는 owner tuple이 다르면 policy 전에 거부한다")
    void actual_root와_expected_owner가_다르면_policy_전에_거부한다() {
        LoadFormOwnershipPort loadPort = mock(LoadFormOwnershipPort.class);
        SaveFormOwnershipPort savePort = mock(SaveFormOwnershipPort.class);
        FormOwnerPolicy policy = policy(EXPECTED.namespace(), true);
        FormOwnershipAccessService sut = strictService(
            loadPort,
            savePort,
            new FormOwnerPolicyRegistry(List.of(policy))
        );
        FormOwnerReference actual = FormOwnerReference.of(
            11L, "notice.vote", "20", "default"
        );
        given(loadPort.findByFormIdForUpdate(actual.formId()))
            .willReturn(Optional.of(FormOwnership.from(actual)));

        assertOwnershipDenied(() -> sut.requireMutation(
            actual.formId(), EXPECTED, ACTOR, FormOperation.MANAGE_STRUCTURE
        ));

        verify(policy, never()).allows(EXPECTED, FormOperation.MANAGE_STRUCTURE, ACTOR);
        verifyNoInteractions(savePort);
    }

    @Test
    @DisplayName("등록되지 않은 namespace evaluator는 fail closed한다")
    void 등록되지_않은_namespace_evaluator는_fail_closed한다() {
        LoadFormOwnershipPort loadPort = mock(LoadFormOwnershipPort.class);
        SaveFormOwnershipPort savePort = mock(SaveFormOwnershipPort.class);
        FormOwnershipAccessService sut = strictService(
            loadPort,
            savePort,
            new FormOwnerPolicyRegistry(List.of())
        );
        given(loadPort.findByFormId(EXPECTED.formId()))
            .willReturn(Optional.of(FormOwnership.from(EXPECTED)));

        assertOwnershipDenied(() -> sut.requireRead(
            EXPECTED.formId(), EXPECTED, ACTOR, FormOperation.READ
        ));
    }

    @Test
    @DisplayName("같은 namespace evaluator가 두 개면 registry 생성 자체를 거부한다")
    void 같은_namespace_evaluator가_두_개면_registry_생성_자체를_거부한다() {
        FormOwnerPolicy first = policy(EXPECTED.namespace(), true);
        FormOwnerPolicy second = policy(EXPECTED.namespace(), true);

        assertThatThrownBy(() -> new FormOwnerPolicyRegistry(List.of(first, second)))
            .isInstanceOf(IllegalStateException.class);
    }

    @Test
    @DisplayName("관리 operation은 인증 member가 없으면 policy가 허용해도 거부한다")
    void 관리_operation은_인증_member가_없으면_거부한다() {
        LoadFormOwnershipPort loadPort = mock(LoadFormOwnershipPort.class);
        SaveFormOwnershipPort savePort = mock(SaveFormOwnershipPort.class);
        FormOwnerPolicy policy = policy(EXPECTED.namespace(), true);
        FormOwnershipAccessService sut = strictService(
            loadPort,
            savePort,
            new FormOwnerPolicyRegistry(List.of(policy))
        );
        given(loadPort.findByFormId(EXPECTED.formId()))
            .willReturn(Optional.of(FormOwnership.from(EXPECTED)));

        assertOwnershipDenied(() -> sut.requireRead(
            EXPECTED.formId(), EXPECTED, FormActorContext.anonymous(), FormOperation.READ_RESPONSES
        ));
    }

    @Test
    @DisplayName("생성 owner factory가 null 또는 다른 form ID를 반환하면 binding을 저장하지 않는다")
    void 생성_owner_factory가_잘못된_reference를_반환하면_binding을_저장하지_않는다() {
        LoadFormOwnershipPort loadPort = mock(LoadFormOwnershipPort.class);
        SaveFormOwnershipPort savePort = mock(SaveFormOwnershipPort.class);
        FormOwnerPolicy policy = policy(EXPECTED.namespace(), true);
        FormOwnershipAccessService sut = strictService(
            loadPort,
            savePort,
            new FormOwnerPolicyRegistry(List.of(policy))
        );
        FormOwnerReferenceFactory nullFactory = formId -> null;
        FormOwnerReferenceFactory foreignFactory = formId -> FormOwnerReference.of(
            formId + 1, EXPECTED.namespace(), EXPECTED.ownerResourceKey(), EXPECTED.slot()
        );

        assertOwnershipDenied(() -> sut.registerNewForm(
            EXPECTED.formId(), nullFactory, ACTOR, FormOperation.MANAGE_STRUCTURE
        ));
        assertOwnershipDenied(() -> sut.registerNewForm(
            EXPECTED.formId(), foreignFactory, ACTOR, FormOperation.MANAGE_STRUCTURE
        ));

        verifyNoInteractions(savePort);
    }

    @Test
    @DisplayName("생성은 저장된 form ID로 policy를 통과한 뒤 ownership binding을 등록한다")
    void 생성은_policy_검증_후_ownership_binding을_등록한다() {
        LoadFormOwnershipPort loadPort = mock(LoadFormOwnershipPort.class);
        SaveFormOwnershipPort savePort = mock(SaveFormOwnershipPort.class);
        FormOwnerPolicy policy = policy(EXPECTED.namespace(), true);
        FormOwnershipAccessService sut = strictService(
            loadPort,
            savePort,
            new FormOwnerPolicyRegistry(List.of(policy))
        );
        FormOwnerReferenceFactory factory = formId -> FormOwnerReference.of(
            formId, EXPECTED.namespace(), EXPECTED.ownerResourceKey(), EXPECTED.slot()
        );
        given(savePort.save(EXPECTED)).willReturn(EXPECTED);

        sut.registerNewForm(
            EXPECTED.formId(), factory, ACTOR, FormOperation.MANAGE_STRUCTURE
        );

        InOrder order = inOrder(policy, savePort);
        order.verify(policy).allows(EXPECTED, FormOperation.MANAGE_STRUCTURE, ACTOR);
        order.verify(savePort).save(EXPECTED);
        verifyNoInteractions(loadPort);
    }

    private static FormOwnerPolicy policy(String namespace, boolean allowed) {
        FormOwnerPolicy policy = mock(FormOwnerPolicy.class);
        given(policy.namespace()).willReturn(namespace);
        given(policy.allows(
            org.mockito.ArgumentMatchers.any(),
            org.mockito.ArgumentMatchers.any(),
            org.mockito.ArgumentMatchers.any()
        )).willReturn(allowed);
        return policy;
    }

    private static FormOwnershipAccessService strictService(
        LoadFormOwnershipPort loadPort,
        SaveFormOwnershipPort savePort,
        FormOwnerPolicyRegistry policyRegistry
    ) {
        GetRegistryReadinessUseCase readiness = mock(GetRegistryReadinessUseCase.class);
        given(readiness.ownershipMode(RegistryName.FORM_OWNERSHIP))
            .willReturn(OwnershipEnforcementMode.BLOCKED);
        return new FormOwnershipAccessService(
            loadPort,
            savePort,
            policyRegistry,
            readiness,
            mock(OperationalMetrics.class)
        );
    }

    private static void assertOwnershipDenied(org.assertj.core.api.ThrowableAssert.ThrowingCallable callable) {
        assertThatThrownBy(callable)
            .isInstanceOf(FormDomainException.class)
            .hasFieldOrPropertyWithValue("baseCode", FormErrorCode.FORM_OWNERSHIP_FORBIDDEN);
    }
}
