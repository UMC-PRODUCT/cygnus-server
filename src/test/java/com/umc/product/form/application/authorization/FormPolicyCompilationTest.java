package com.umc.product.form.application.authorization;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.umc.product.authorization.application.port.in.policy.RegisteredPolicyEvaluationRequest;
import com.umc.product.authorization.application.service.policy.CompiledPolicyRegistry;
import com.umc.product.authorization.application.service.policy.PolicyEvaluationService;
import com.umc.product.authorization.application.service.policy.PolicySemanticCompiler;
import com.umc.product.authorization.application.service.policy.RegisteredPolicyEvaluationService;
import com.umc.product.authorization.domain.policy.PolicyAttributeSet;
import com.umc.product.authorization.domain.policy.PolicyDecision;
import com.umc.product.authorization.domain.policy.PolicyEffect;
import com.umc.product.authorization.domain.policy.PolicyValue;

class FormPolicyCompilationTest {

    private RegisteredPolicyEvaluationService evaluator;

    @BeforeEach
    void setUp() {
        CompiledPolicyRegistry registry = new CompiledPolicyRegistry(
            new PolicySemanticCompiler(),
            List.of(new FormPolicyBundleContributor()));
        evaluator = new RegisteredPolicyEvaluationService(
            registry,
            new PolicyEvaluationService());
    }

    @Test
    @DisplayName("기명 respondent는 자신의 FormResponse를 수정한다")
    void allowsRespondent() {
        PolicyDecision decision = (PolicyDecision) evaluator.evaluate(request(
            FormPolicyAction.RESPONSE_UPDATE,
            "MEMBER",
            false,
            false,
            true,
            false));

        assertThat(decision.effect()).isEqualTo(PolicyEffect.ALLOW);
    }

    @Test
    @DisplayName("검증된 capability는 bound FormResponse만 수정한다")
    void allowsBoundCapability() {
        PolicyDecision allowed = (PolicyDecision) evaluator.evaluate(request(
            FormPolicyAction.RESPONSE_UPDATE,
            "CAPABILITY",
            false,
            false,
            false,
            true));
        PolicyDecision denied = (PolicyDecision) evaluator.evaluate(request(
            FormPolicyAction.RESPONSE_UPDATE,
            "CAPABILITY",
            false,
            false,
            false,
            false));

        assertThat(allowed.effect()).isEqualTo(PolicyEffect.ALLOW);
        assertThat(denied.effect()).isEqualTo(PolicyEffect.DENY);
    }

    private RegisteredPolicyEvaluationRequest request(
        FormPolicyAction action,
        String subjectKind,
        boolean usageOwner,
        boolean consumer,
        boolean respondent,
        boolean capability
    ) {
        return new RegisteredPolicyEvaluationRequest(
            FormPolicyDomainSchema.BUNDLE_KEY,
            action.id(),
            PolicyAttributeSet.builder()
                .put(FormPolicyAttributes.SUBJECT_KIND, new PolicyValue.EnumValue(subjectKind))
                .put(FormPolicyAttributes.USAGE_OWNER_AUTHORIZED, bool(usageOwner))
                .put(FormPolicyAttributes.CONSUMER_AUTHORIZED, bool(consumer))
                .put(FormPolicyAttributes.IS_RESPONDENT, bool(respondent))
                .put(FormPolicyAttributes.CAPABILITY_BOUND_TO_RESPONSE, bool(capability))
                .build(),
            Instant.parse("2026-07-01T00:00:00Z"));
    }

    private PolicyValue.BooleanValue bool(boolean value) {
        return new PolicyValue.BooleanValue(value);
    }
}
