package com.umc.product.certificate.application.authorization;

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
public class CertificatePolicyBundleContributor implements PolicyBundleContributor {

    private static final String MODULE_ID = "certificate-resource";

    @Override
    public String namespace() {
        return CertificatePolicyDomainSchema.NAMESPACE;
    }

    @Override
    public PolicyDomainSchema domainSchema() {
        return CertificatePolicyDomainSchema.create();
    }

    @Override
    public PolicyResourceManifest resourceManifest() {
        return new PolicyResourceManifest(
            new PolicyClasspathResource("bundle.json", "policies/certificate/bundle.json"),
            List.of(new PolicyClasspathResource(
                "certificate-resource.policy.json",
                "policies/certificate/certificate-resource.policy.json")));
    }

    @Override
    public CompiledPolicyContractValidator compiledContractValidator() {
        return this::validate;
    }

    @Override
    public List<PolicySurfaceDescriptor> policySurfaces() {
        String controller =
            "com.umc.product.certificate.adapter.in.web.AdminCertificateController#";
        return List.of(
            new PolicySurfaceDescriptor(
                namespace(),
                "rest:POST /api/v1/certificates/admin",
                controller + "issue",
                PolicySurfaceType.REST,
                CertificatePolicyAction.ISSUE_ADMIN.id(),
                MODULE_ID,
                PolicySurfaceGate.DIRECT),
            new PolicySurfaceDescriptor(
                namespace(),
                "rest:PATCH /api/v1/certificates/admin/{certificateId}/revoke",
                controller + "revoke",
                PolicySurfaceType.REST,
                CertificatePolicyAction.REVOKE.id(),
                MODULE_ID,
                PolicySurfaceGate.DIRECT));
    }

    @Override
    public boolean commonRolloutEnabled() {
        return true;
    }

    private void validate(CompiledPolicyBundle bundle) {
        require("1.0".equals(bundle.schemaVersion()),
            "Certificate policy schemaVersion이 일치하지 않습니다.");
        require(CertificatePolicyDomainSchema.VERSION.equals(bundle.contextSchemaVersion()),
            "Certificate policy contextSchemaVersion이 일치하지 않습니다.");
        require(namespace().equals(bundle.namespace()),
            "Certificate policy namespace가 일치하지 않습니다.");
        require(CertificatePolicyDomainSchema.POLICY_VERSION.equals(bundle.policyVersion()),
            "Certificate policyVersion이 일치하지 않습니다.");
        require(bundle.modules().size() == 1 && MODULE_ID.equals(bundle.modules().getFirst().id()),
            "Certificate policy module이 일치하지 않습니다.");
        require(bundle.statementsByAction().keySet().equals(Set.of(
            CertificatePolicyAction.ISSUE_ADMIN.id(),
            CertificatePolicyAction.REVOKE.id())),
            "Certificate policy action coverage가 일치하지 않습니다.");
        require(bundle.statementsForAction(CertificatePolicyAction.ISSUE_ADMIN.id()).size() == 2
            && bundle.statementsForAction(CertificatePolicyAction.REVOKE.id()).size() == 2,
            "Certificate policy statement coverage가 일치하지 않습니다.");
    }

    private void require(boolean condition, String message) {
        if (!condition) {
            throw new IllegalStateException(message);
        }
    }
}
