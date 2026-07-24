package com.umc.product.authorization.application.service.policy.rollout;

import java.time.Clock;
import java.time.Instant;
import java.util.HashSet;
import java.util.Set;

import org.springframework.stereotype.Component;

import com.umc.product.authorization.application.service.policy.CompiledPolicyRegistry;
import com.umc.product.authorization.domain.policy.CompiledPolicyBundle;
import com.umc.product.authorization.domain.policy.rollout.PolicyEnforcementReceipt;
import com.umc.product.authorization.domain.policy.rollout.PolicyRolloutKey;
import com.umc.product.authorization.domain.policy.rollout.PolicyRolloutMode;

@Component
public class PolicyEnforcementReceiptGate {

    private final CompiledPolicyRegistry registry;
    private final PolicyEnforcementReceiptLoader loader;
    private final Clock clock;

    public PolicyEnforcementReceiptGate(
        CompiledPolicyRegistry registry,
        PolicyEnforcementReceiptLoader loader,
        Clock clock
    ) {
        this.registry = registry;
        this.loader = loader;
        this.clock = clock;
    }

    public void validate(PolicyRolloutConfiguration configuration) {
        for (String namespace : registry.commonRolloutNamespaces()) {
            CompiledPolicyBundle bundle = registry.require(namespace);
            LoadedPolicyEnforcementReceipts loaded = loader.load(namespace);
            if (!namespace.equals(loaded.document().namespace())) {
                throw new IllegalStateException("Policy receipt namespace가 일치하지 않습니다: " + namespace);
            }
            Set<String> receipted = validateReceipts(bundle, loaded);
            Set<String> enforced = enforcedActions(namespace, bundle, configuration);
            if (!receipted.containsAll(enforced)) {
                throw new IllegalStateException(
                    "ENFORCE action에 유효한 policy receipt가 없습니다: " + difference(enforced, receipted));
            }
        }
    }

    private Set<String> validateReceipts(
        CompiledPolicyBundle bundle,
        LoadedPolicyEnforcementReceipts loaded
    ) {
        Set<String> receipted = new HashSet<>();
        Instant now = clock.instant();
        for (PolicyEnforcementReceipt receipt : loaded.document().receipts()) {
            if (!bundle.policyVersion().equals(receipt.policyVersion())) {
                throw new IllegalStateException("Policy receipt policyVersion이 일치하지 않습니다.");
            }
            if (!bundle.policyFingerprint().equals(receipt.policyFingerprint())) {
                throw new IllegalStateException("Policy receipt fingerprint가 일치하지 않습니다.");
            }
            if (!loaded.artifactSha256().equals(receipt.artifactSha256())) {
                throw new IllegalStateException("Policy receipt artifact SHA-256이 일치하지 않습니다.");
            }
            if (receipt.approvedAt().isAfter(now)
                || !receipt.approvedAt().isBefore(receipt.expiresAt())
                || !now.isBefore(receipt.expiresAt())) {
                throw new IllegalStateException("Policy receipt 승인 기간이 유효하지 않습니다.");
            }
            for (String action : receipt.actions()) {
                if (bundle.domainSchema().action(action).isEmpty()) {
                    throw new IllegalStateException("Policy receipt action이 schema에 없습니다: " + action);
                }
                if (!receipted.add(action)) {
                    throw new IllegalStateException("Policy receipt action이 중복되었습니다: " + action);
                }
            }
        }
        return Set.copyOf(receipted);
    }

    private Set<String> enforcedActions(
        String namespace,
        CompiledPolicyBundle bundle,
        PolicyRolloutConfiguration configuration
    ) {
        Set<String> enforced = new HashSet<>();
        for (var action : bundle.domainSchema().actions()) {
            PolicyRolloutMode mode = configuration.overrides().getOrDefault(
                new PolicyRolloutKey(namespace, action.actionId()),
                configuration.defaultMode());
            if (mode == PolicyRolloutMode.ENFORCE) {
                enforced.add(action.actionId());
            }
        }
        return Set.copyOf(enforced);
    }

    private Set<String> difference(Set<String> left, Set<String> right) {
        Set<String> result = new java.util.TreeSet<>(left);
        result.removeAll(right);
        return result;
    }
}
