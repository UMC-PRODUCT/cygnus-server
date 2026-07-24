package com.umc.product.feedback.application.authorization;

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
public class FeedbackPolicyBundleContributor implements PolicyBundleContributor {

    private static final String MODULE_ID = "feedback-resource";

    @Override
    public String namespace() {
        return FeedbackPolicyDomainSchema.NAMESPACE;
    }

    @Override
    public PolicyDomainSchema domainSchema() {
        return FeedbackPolicyDomainSchema.create();
    }

    @Override
    public PolicyResourceManifest resourceManifest() {
        return new PolicyResourceManifest(
            new PolicyClasspathResource("bundle.json", "policies/feedback/bundle.json"),
            List.of(new PolicyClasspathResource(
                "feedback-resource.policy.json",
                "policies/feedback/feedback-resource.policy.json")));
    }

    @Override
    public CompiledPolicyContractValidator compiledContractValidator() {
        return this::validate;
    }

    @Override
    public List<PolicySurfaceDescriptor> policySurfaces() {
        return List.of(
            rest(
                "GET /api/v1/user-feedbacks/templates",
                "UserFeedbackController#getTemplate",
                FeedbackPolicyAction.TEMPLATE_RESOLVE),
            rest(
                "POST /api/v1/user-feedbacks/responses",
                "UserFeedbackController#submit",
                FeedbackPolicyAction.RESPONSE_SUBMIT));
    }

    @Override
    public boolean commonRolloutEnabled() {
        return true;
    }

    private PolicySurfaceDescriptor rest(
        String route,
        String handler,
        FeedbackPolicyAction action
    ) {
        return new PolicySurfaceDescriptor(
            namespace(),
            "rest:" + route,
            "com.umc.product.feedback.adapter.in.web." + handler,
            PolicySurfaceType.REST,
            action.id(),
            MODULE_ID,
            PolicySurfaceGate.DIRECT);
    }

    private void validate(CompiledPolicyBundle bundle) {
        require("1.0".equals(bundle.schemaVersion()),
            "Feedback policy schemaVersion이 일치하지 않습니다.");
        require(FeedbackPolicyDomainSchema.VERSION.equals(bundle.contextSchemaVersion()),
            "Feedback policy contextSchemaVersion이 일치하지 않습니다.");
        require(namespace().equals(bundle.namespace()),
            "Feedback policy namespace가 일치하지 않습니다.");
        require(FeedbackPolicyDomainSchema.POLICY_VERSION.equals(bundle.policyVersion()),
            "Feedback policyVersion이 일치하지 않습니다.");
        require(bundle.modules().size() == 1 && MODULE_ID.equals(bundle.modules().getFirst().id()),
            "Feedback policy module이 일치하지 않습니다.");
        Set<String> actions = Arrays.stream(FeedbackPolicyAction.values())
            .map(FeedbackPolicyAction::id)
            .collect(Collectors.toUnmodifiableSet());
        require(bundle.statementsByAction().keySet().equals(actions),
            "Feedback policy action coverage가 일치하지 않습니다.");
    }

    private void require(boolean condition, String message) {
        if (!condition) {
            throw new IllegalStateException(message);
        }
    }
}
