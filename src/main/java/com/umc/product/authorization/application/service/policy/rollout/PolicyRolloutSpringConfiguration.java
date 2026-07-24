package com.umc.product.authorization.application.service.policy.rollout;

import java.util.Map;
import java.util.TreeMap;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.umc.product.authorization.application.service.policy.CompiledPolicyRegistry;
import com.umc.product.authorization.domain.policy.CompiledPolicyBundle;
import com.umc.product.authorization.domain.policy.rollout.PolicyRolloutKey;
import com.umc.product.authorization.domain.policy.rollout.PolicyRolloutMode;

@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(PolicyRolloutProperties.class)
public class PolicyRolloutSpringConfiguration {

    @Bean
    public PolicyRolloutConfiguration policyRolloutConfiguration(
        PolicyRolloutProperties properties,
        CompiledPolicyRegistry registry,
        PolicyEnforcementReceiptGate enforcementReceiptGate
    ) {
        Map<PolicyRolloutKey, PolicyRolloutMode> overrides = new TreeMap<>();
        for (PolicyRolloutProperties.ActionOverride override : properties.overrides()) {
            validateOverride(override, registry);
            PolicyRolloutKey key = new PolicyRolloutKey(override.namespace(), override.action());
            if (overrides.putIfAbsent(key, override.mode()) != null) {
                throw new IllegalArgumentException("중복된 policy rollout override입니다: " + key);
            }
        }
        PolicyRolloutConfiguration configuration =
            new PolicyRolloutConfiguration(properties.defaultMode(), overrides);
        enforcementReceiptGate.validate(configuration);
        return configuration;
    }

    @Bean
    public ConfiguredPolicyRolloutModeResolver policyRolloutModeResolver(
        PolicyRolloutConfiguration configuration
    ) {
        return new ConfiguredPolicyRolloutModeResolver(configuration);
    }

    private void validateOverride(
        PolicyRolloutProperties.ActionOverride override,
        CompiledPolicyRegistry registry
    ) {
        if (override.namespace() == null || override.namespace().isBlank()) {
            throw new IllegalArgumentException("Policy rollout namespace는 비어 있을 수 없습니다.");
        }
        if (override.action() == null || override.action().isBlank()) {
            throw new IllegalArgumentException("Policy rollout action은 비어 있을 수 없습니다.");
        }
        if (override.mode() == null) {
            throw new IllegalArgumentException("Policy rollout mode는 null일 수 없습니다.");
        }
        if (!registry.commonRolloutNamespaces().contains(override.namespace())) {
            throw new IllegalArgumentException(
                "공용 rollout에 등록되지 않은 namespace입니다: " + override.namespace());
        }
        CompiledPolicyBundle bundle = registry.require(override.namespace());
        if (bundle.domainSchema().action(override.action()).isEmpty()) {
            throw new IllegalArgumentException(
                "Policy rollout action이 domain schema에 등록되지 않았습니다: " + override.action());
        }
    }
}
