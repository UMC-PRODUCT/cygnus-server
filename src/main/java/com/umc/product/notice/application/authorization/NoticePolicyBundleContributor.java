package com.umc.product.notice.application.authorization;

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
public class NoticePolicyBundleContributor implements PolicyBundleContributor {

    private static final String MODULE_ID = "notice-resource";

    @Override
    public String namespace() {
        return NoticePolicyDomainSchema.NAMESPACE;
    }

    @Override
    public PolicyDomainSchema domainSchema() {
        return NoticePolicyDomainSchema.create();
    }

    @Override
    public PolicyResourceManifest resourceManifest() {
        return new PolicyResourceManifest(
            new PolicyClasspathResource("bundle.json", "policies/notice/bundle.json"),
            List.of(new PolicyClasspathResource(
                "notice-resource.policy.json",
                "policies/notice/notice-resource.policy.json")));
    }

    @Override
    public CompiledPolicyContractValidator compiledContractValidator() {
        return this::validate;
    }

    @Override
    public List<PolicySurfaceDescriptor> policySurfaces() {
        return List.of(
            rest("POST /api/v1/notices", "NoticeCommandController#createNotice",
                NoticePolicyAction.CREATE),
            rest("GET /api/v1/notices/{noticeId}", "NoticeQueryController#getNotice",
                NoticePolicyAction.READ),
            rest("POST /api/v1/notices/{noticeId}/read", "NoticeCommandController#recordNoticeRead",
                NoticePolicyAction.READ),
            rest("PATCH /api/v1/notices/{noticeId}", "NoticeCommandController#updateNotice",
                NoticePolicyAction.UPDATE),
            rest("DELETE /api/v1/notices/{noticeId}", "NoticeCommandController#deleteNotice",
                NoticePolicyAction.DELETE),
            rest("GET /api/v1/notices/{noticeId}/read-statics",
                "NoticeQueryController#getNoticeReadStatics",
                NoticePolicyAction.CHECK_RECIPIENTS),
            rest("GET /api/v1/notices/{noticeId}/read-status",
                "NoticeQueryController#getNoticeReadStatus",
                NoticePolicyAction.CHECK_RECIPIENTS));
    }

    @Override
    public boolean commonRolloutEnabled() {
        return true;
    }

    private PolicySurfaceDescriptor rest(
        String route,
        String handler,
        NoticePolicyAction action
    ) {
        return new PolicySurfaceDescriptor(
            namespace(),
            "rest:" + route,
            "com.umc.product.notice.adapter.in.web." + handler,
            PolicySurfaceType.REST,
            action.id(),
            MODULE_ID,
            PolicySurfaceGate.DIRECT);
    }

    private void validate(CompiledPolicyBundle bundle) {
        require("1.0".equals(bundle.schemaVersion()), "Notice policy schemaVersion이 일치하지 않습니다.");
        require(NoticePolicyDomainSchema.VERSION.equals(bundle.contextSchemaVersion()),
            "Notice policy contextSchemaVersion이 일치하지 않습니다.");
        require(namespace().equals(bundle.namespace()), "Notice policy namespace가 일치하지 않습니다.");
        require(NoticePolicyDomainSchema.POLICY_VERSION.equals(bundle.policyVersion()),
            "Notice policyVersion이 일치하지 않습니다.");
        require(bundle.modules().size() == 1 && MODULE_ID.equals(bundle.modules().getFirst().id()),
            "Notice policy module이 일치하지 않습니다.");
        Set<String> actions = Arrays.stream(NoticePolicyAction.values())
            .map(NoticePolicyAction::id)
            .collect(Collectors.toUnmodifiableSet());
        require(bundle.statementsByAction().keySet().equals(actions),
            "Notice policy action coverage가 일치하지 않습니다.");
    }

    private void require(boolean condition, String message) {
        if (!condition) {
            throw new IllegalStateException(message);
        }
    }
}
