package com.umc.product.authorization.application.service.policy.rollout;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.umc.product.authorization.application.service.policy.CompiledPolicyRegistry;
import com.umc.product.authorization.application.service.policy.PolicySemanticCompiler;
import com.umc.product.authorization.domain.policy.rollout.PolicyRolloutMode;
import com.umc.product.term.application.authorization.TermPolicyBundleContributor;

class PolicyEnforcementReceiptGateTest {

    private PolicyEnforcementReceiptGate gate;

    @BeforeEach
    void setUp() {
        CompiledPolicyRegistry registry = new CompiledPolicyRegistry(
            new PolicySemanticCompiler(),
            List.of(new TermPolicyBundleContributor()));
        gate = new PolicyEnforcementReceiptGate(
            registry,
            new PolicyEnforcementReceiptLoader(),
            Clock.fixed(Instant.parse("2026-07-24T00:00:00Z"), ZoneOffset.UTC));
    }

    @Test
    @DisplayName("빈 receipt에서도 SHADOW startup은 허용한다")
    void allowsShadowWithoutReceipt() {
        assertThatCode(() -> gate.validate(
            new PolicyRolloutConfiguration(PolicyRolloutMode.SHADOW, Map.of())))
            .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("ENFORCE는 일치하는 승인 receipt가 없으면 startup을 거부한다")
    void rejectsEnforceWithoutReceipt() {
        assertThatThrownBy(() -> gate.validate(
            new PolicyRolloutConfiguration(PolicyRolloutMode.ENFORCE, Map.of())))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("유효한 policy receipt");
    }
}
