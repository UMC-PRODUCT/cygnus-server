package com.umc.product.curriculum.application.authorization;

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
public class CurriculumPolicyBundleContributor implements PolicyBundleContributor {

    private static final String MODULE_ID = "curriculum-resource";

    @Override
    public String namespace() {
        return CurriculumPolicyDomainSchema.NAMESPACE;
    }

    @Override
    public PolicyDomainSchema domainSchema() {
        return CurriculumPolicyDomainSchema.create();
    }

    @Override
    public PolicyResourceManifest resourceManifest() {
        return new PolicyResourceManifest(
            new PolicyClasspathResource("bundle.json", "policies/curriculum/bundle.json"),
            List.of(new PolicyClasspathResource(
                "curriculum-resource.policy.json",
                "policies/curriculum/curriculum-resource.policy.json")));
    }

    @Override
    public CompiledPolicyContractValidator compiledContractValidator() {
        return this::validate;
    }

    @Override
    public List<PolicySurfaceDescriptor> policySurfaces() {
        return List.of(
            rest("POST /api/v2/curriculums/original-workbooks",
                "OriginalWorkbookCommandV2Controller#createOriginalWorkbook",
                CurriculumPolicyAction.ORIGINAL_WORKBOOK_MANAGE),
            rest("POST /api/v2/curriculums/original-workbooks/draft",
                "OriginalWorkbookCommandV2Controller#createOriginalWorkbookAsDraft",
                CurriculumPolicyAction.ORIGINAL_WORKBOOK_MANAGE),
            rest("PATCH /api/v2/curriculums/original-workbooks/{originalWorkbookId}",
                "OriginalWorkbookCommandV2Controller#editOriginalWorkbook",
                CurriculumPolicyAction.ORIGINAL_WORKBOOK_MANAGE),
            rest("DELETE /api/v2/curriculums/original-workbooks/{originalWorkbookId}",
                "OriginalWorkbookCommandV2Controller#deleteOriginalWorkbook",
                CurriculumPolicyAction.ORIGINAL_WORKBOOK_MANAGE),
            rest("PATCH /api/v2/curriculums/original-workbooks/status",
                "OriginalWorkbookCommandV2Controller#changeOriginalWorkbookStatus",
                CurriculumPolicyAction.ORIGINAL_WORKBOOK_RELEASE),
            rest("POST /api/v2/curriculums/original-workbooks/missions",
                "OriginalWorkbookMissionCommandV2Controller#createOriginalWorkbookMission",
                CurriculumPolicyAction.ORIGINAL_WORKBOOK_MANAGE),
            rest("PATCH /api/v2/curriculums/original-workbooks/missions/{originalWorkbookMissionId}",
                "OriginalWorkbookMissionCommandV2Controller#editOriginalMission",
                CurriculumPolicyAction.ORIGINAL_WORKBOOK_MANAGE),
            rest("DELETE /api/v2/curriculums/original-workbooks/missions/{originalWorkbookMissionId}",
                "OriginalWorkbookMissionCommandV2Controller#deleteOriginalMission",
                CurriculumPolicyAction.ORIGINAL_WORKBOOK_MANAGE));
    }

    @Override
    public boolean commonRolloutEnabled() {
        return true;
    }

    private PolicySurfaceDescriptor rest(
        String route,
        String handler,
        CurriculumPolicyAction action
    ) {
        return new PolicySurfaceDescriptor(
            namespace(),
            "rest:" + route,
            "com.umc.product.curriculum.adapter.in.web.v2." + handler,
            PolicySurfaceType.REST,
            action.id(),
            MODULE_ID,
            PolicySurfaceGate.DIRECT);
    }

    private void validate(CompiledPolicyBundle bundle) {
        require("1.0".equals(bundle.schemaVersion()), "Curriculum policy schemaVersion이 일치하지 않습니다.");
        require(CurriculumPolicyDomainSchema.VERSION.equals(bundle.contextSchemaVersion()),
            "Curriculum policy contextSchemaVersion이 일치하지 않습니다.");
        require(namespace().equals(bundle.namespace()), "Curriculum policy namespace가 일치하지 않습니다.");
        require(CurriculumPolicyDomainSchema.POLICY_VERSION.equals(bundle.policyVersion()),
            "Curriculum policyVersion이 일치하지 않습니다.");
        require(bundle.modules().size() == 1 && MODULE_ID.equals(bundle.modules().getFirst().id()),
            "Curriculum policy module이 일치하지 않습니다.");
        Set<String> actions = Arrays.stream(CurriculumPolicyAction.values())
            .map(CurriculumPolicyAction::id)
            .collect(Collectors.toUnmodifiableSet());
        require(bundle.statementsByAction().keySet().equals(actions),
            "Curriculum policy action coverage가 일치하지 않습니다.");
    }

    private void require(boolean condition, String message) {
        if (!condition) {
            throw new IllegalStateException(message);
        }
    }
}
