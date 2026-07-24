package com.umc.product.notification.application.authorization;

import java.util.List;
import java.util.Set;

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
public class NotificationPolicyBundleContributor implements PolicyBundleContributor {

    private static final String MODULE_ID = "notification-resource";

    @Override
    public String namespace() {
        return NotificationPolicyDomainSchema.NAMESPACE;
    }

    @Override
    public PolicyDomainSchema domainSchema() {
        return NotificationPolicyDomainSchema.create();
    }

    @Override
    public PolicyResourceManifest resourceManifest() {
        return new PolicyResourceManifest(
            new PolicyClasspathResource("bundle.json", "policies/notification/bundle.json"),
            List.of(new PolicyClasspathResource(
                "notification-resource.policy.json",
                "policies/notification/notification-resource.policy.json")));
    }

    @Override
    public CompiledPolicyContractValidator compiledContractValidator() {
        return this::validate;
    }

    @Override
    public List<PolicySurfaceDescriptor> policySurfaces() {
        return List.of(
            new PolicySurfaceDescriptor(
                namespace(),
                "rest:POST /api/v1/notifications/admin/fcm/messages",
                "com.umc.product.notification.adapter.in.web.FcmAdminController#send",
                PolicySurfaceType.REST,
                NotificationPolicyAction.SEND_FCM.id(),
                MODULE_ID,
                PolicySurfaceGate.ACTOR),
            new PolicySurfaceDescriptor(
                namespace(),
                "rest:DELETE /api/v1/notifications/fcm/installations/{installationId}",
                "com.umc.product.notification.adapter.in.web.FcmController#unregisterFcmInstallation",
                PolicySurfaceType.REST,
                NotificationPolicyAction.DELETE_TOKEN.id(),
                MODULE_ID,
                PolicySurfaceGate.DIRECT),
            new PolicySurfaceDescriptor(
                namespace(),
                "capability:ResourceType.FCM/DELETE",
                "com.umc.product.notification.application.service.evaluator.FcmPermissionEvaluator#evaluate",
                PolicySurfaceType.INTERNAL_BATCH,
                NotificationPolicyAction.DELETE_TOKEN.id(),
                MODULE_ID,
                PolicySurfaceGate.TRANSITIVE));
    }

    @Override
    public boolean commonRolloutEnabled() {
        return true;
    }

    private void validate(CompiledPolicyBundle bundle) {
        require("1.0".equals(bundle.schemaVersion()),
            "Notification policy schemaVersion이 일치하지 않습니다.");
        require(NotificationPolicyDomainSchema.VERSION.equals(bundle.contextSchemaVersion()),
            "Notification policy contextSchemaVersion이 일치하지 않습니다.");
        require(namespace().equals(bundle.namespace()),
            "Notification policy namespace가 일치하지 않습니다.");
        require(NotificationPolicyDomainSchema.POLICY_VERSION.equals(bundle.policyVersion()),
            "Notification policyVersion이 일치하지 않습니다.");
        require(bundle.modules().size() == 1 && MODULE_ID.equals(bundle.modules().getFirst().id()),
            "Notification policy module이 일치하지 않습니다.");
        require(bundle.statementsByAction().keySet().equals(Set.of(
            NotificationPolicyAction.SEND_FCM.id(),
            NotificationPolicyAction.DELETE_TOKEN.id())),
            "Notification policy action coverage가 일치하지 않습니다.");
        require(bundle.modules().getFirst().statements().stream()
            .allMatch(statement -> statement.outcomes().isEmpty()),
            "Notification policy는 outcome을 방출할 수 없습니다.");
    }

    private void require(boolean condition, String message) {
        if (!condition) {
            throw new IllegalStateException(message);
        }
    }
}
