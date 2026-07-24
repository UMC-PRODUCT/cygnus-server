package com.umc.product.chat.application.authorization;

import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import com.umc.product.authorization.application.port.out.policy.CompiledPolicyContractValidator;
import com.umc.product.authorization.application.port.out.policy.PolicyBundleContributor;
import com.umc.product.authorization.application.port.out.policy.PolicyClasspathResource;
import com.umc.product.authorization.application.port.out.policy.PolicyResourceManifest;
import com.umc.product.authorization.domain.policy.CompiledPolicyBundle;
import com.umc.product.authorization.domain.policy.PolicyDomainSchema;
import com.umc.product.authorization.domain.policy.PolicySurfaceDescriptor;
import com.umc.product.authorization.domain.policy.PolicySurfaceGate;
import com.umc.product.authorization.domain.policy.PolicySurfaceType;

@Component
public class ChatPolicyBundleContributor implements PolicyBundleContributor {

    private static final String MODULE_ID = "chat-resource";

    @Override
    public String namespace() {
        return ChatPolicyDomainSchema.NAMESPACE;
    }

    @Override
    public PolicyDomainSchema domainSchema() {
        return ChatPolicyDomainSchema.create();
    }

    @Override
    public PolicyResourceManifest resourceManifest() {
        return new PolicyResourceManifest(
            new PolicyClasspathResource("bundle.json", "policies/chat/bundle.json"),
            List.of(new PolicyClasspathResource(
                "chat-resource.policy.json",
                "policies/chat/chat-resource.policy.json")));
    }

    @Override
    public CompiledPolicyContractValidator compiledContractValidator() {
        return this::validate;
    }

    @Override
    public List<PolicySurfaceDescriptor> policySurfaces() {
        return Arrays.stream(ChatPolicyAction.values())
            .map(action -> new PolicySurfaceDescriptor(
                namespace(),
                "internal:" + action.id(),
                "com.umc.product.chat.application.policy.ChatRoomAccessPolicy#"
                    + action.name().toLowerCase(java.util.Locale.ROOT),
                PolicySurfaceType.INTERNAL_BATCH,
                action.id(),
                MODULE_ID,
                PolicySurfaceGate.DIRECT))
            .toList();
    }

    @Override
    public boolean commonRolloutEnabled() {
        return true;
    }

    private void validate(CompiledPolicyBundle bundle) {
        require("1.0".equals(bundle.schemaVersion()), "Chat policy schemaVersion이 일치하지 않습니다.");
        require(ChatPolicyDomainSchema.VERSION.equals(bundle.contextSchemaVersion()),
            "Chat policy contextSchemaVersion이 일치하지 않습니다.");
        require(namespace().equals(bundle.namespace()), "Chat policy namespace가 일치하지 않습니다.");
        require(ChatPolicyDomainSchema.POLICY_VERSION.equals(bundle.policyVersion()),
            "Chat policyVersion이 일치하지 않습니다.");
        require(bundle.modules().size() == 1 && MODULE_ID.equals(bundle.modules().getFirst().id()),
            "Chat policy module이 일치하지 않습니다.");
        Set<String> actions = Arrays.stream(ChatPolicyAction.values())
            .map(ChatPolicyAction::id)
            .collect(Collectors.toUnmodifiableSet());
        require(bundle.statementsByAction().keySet().equals(actions),
            "Chat policy action coverage가 일치하지 않습니다.");
    }

    private void require(boolean condition, String message) {
        if (!condition) {
            throw new IllegalStateException(message);
        }
    }
}
