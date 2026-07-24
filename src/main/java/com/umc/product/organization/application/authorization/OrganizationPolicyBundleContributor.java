package com.umc.product.organization.application.authorization;

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
public class OrganizationPolicyBundleContributor implements PolicyBundleContributor {

    private static final String MODULE_ID = "organization-resource";

    @Override
    public String namespace() {
        return OrganizationPolicyDomainSchema.NAMESPACE;
    }

    @Override
    public PolicyDomainSchema domainSchema() {
        return OrganizationPolicyDomainSchema.create();
    }

    @Override
    public PolicyResourceManifest resourceManifest() {
        return new PolicyResourceManifest(
            new PolicyClasspathResource("bundle.json", "policies/organization/bundle.json"),
            List.of(new PolicyClasspathResource(
                "organization-resource.policy.json",
                "policies/organization/organization-resource.policy.json")));
    }

    @Override
    public CompiledPolicyContractValidator compiledContractValidator() {
        return this::validate;
    }

    @Override
    public List<PolicySurfaceDescriptor> policySurfaces() {
        return List.of(
            rest("POST /api/v1/gisu", "GisuCommandController#createGisu",
                OrganizationPolicyAction.GISU_CREATE),
            rest("POST /api/v1/gisu/{gisuId}/active", "GisuCommandController#updateActiveGisu",
                OrganizationPolicyAction.GISU_UPDATE),
            rest("DELETE /api/v1/gisu/{gisuId}", "GisuCommandController#deleteGisu",
                OrganizationPolicyAction.GISU_DELETE),
            rest("POST /api/v1/chapters", "ChapterCommandController#createChapter",
                OrganizationPolicyAction.CHAPTER_CREATE),
            rest("POST /api/v1/chapters/bulk", "ChapterCommandController#createChapterBulk",
                OrganizationPolicyAction.CHAPTER_CREATE),
            rest("DELETE /api/v1/chapters/{chapterId}", "ChapterCommandController#deleteChapter",
                OrganizationPolicyAction.CHAPTER_DELETE),
            rest("POST /api/v1/schools", "SchoolCommandController#createSchool",
                OrganizationPolicyAction.SCHOOL_CREATE),
            rest("PATCH /api/v1/schools/{schoolId}", "SchoolCommandController#updateSchool",
                OrganizationPolicyAction.SCHOOL_UPDATE),
            rest("PATCH /api/v1/schools/{schoolId}/assign", "SchoolCommandController#assignToChapter",
                OrganizationPolicyAction.SCHOOL_UPDATE),
            rest("PATCH /api/v1/schools/{schoolId}/unassign",
                "SchoolCommandController#unassignFromChapter",
                OrganizationPolicyAction.SCHOOL_UPDATE),
            rest("DELETE /api/v1/schools", "SchoolCommandController#deleteSchools",
                OrganizationPolicyAction.SCHOOL_DELETE),
            rest("GET /api/v1/study-groups/{studyGroupId}",
                "StudyGroupQueryController#getStudyGroupInfo",
                OrganizationPolicyAction.STUDY_GROUP_READ),
            rest("POST /api/v1/study-groups", "StudyGroupCommandController#create",
                OrganizationPolicyAction.STUDY_GROUP_CREATE),
            rest("PATCH /api/v1/study-groups/{studyGroupId}",
                "StudyGroupCommandController#update",
                OrganizationPolicyAction.STUDY_GROUP_UPDATE),
            rest("PATCH /api/v1/study-groups/{studyGroupId}/members/{memberId}",
                "StudyGroupCommandController#addMember",
                OrganizationPolicyAction.STUDY_GROUP_UPDATE),
            rest("PATCH /api/v1/study-groups/{studyGroupId}/mentors/{mentorId}",
                "StudyGroupCommandController#addMentor",
                OrganizationPolicyAction.STUDY_GROUP_UPDATE),
            rest("DELETE /api/v1/study-groups/{studyGroupId}/members/{memberId}",
                "StudyGroupCommandController#deleteMember",
                OrganizationPolicyAction.STUDY_GROUP_UPDATE),
            rest("DELETE /api/v1/study-groups/{studyGroupId}/mentors/{mentorId}",
                "StudyGroupCommandController#deleteMentor",
                OrganizationPolicyAction.STUDY_GROUP_UPDATE),
            rest("DELETE /api/v1/study-groups/{studyGroupId}",
                "StudyGroupCommandController#delete",
                OrganizationPolicyAction.STUDY_GROUP_DELETE));
    }

    @Override
    public boolean commonRolloutEnabled() {
        return true;
    }

    private PolicySurfaceDescriptor rest(
        String route,
        String handler,
        OrganizationPolicyAction action
    ) {
        return new PolicySurfaceDescriptor(
            namespace(),
            "rest:" + route,
            "com.umc.product.organization.adapter.in.web." + handler,
            PolicySurfaceType.REST,
            action.id(),
            MODULE_ID,
            PolicySurfaceGate.DIRECT);
    }

    private void validate(CompiledPolicyBundle bundle) {
        require("1.0".equals(bundle.schemaVersion()), "Organization policy schemaVersion이 일치하지 않습니다.");
        require(OrganizationPolicyDomainSchema.VERSION.equals(bundle.contextSchemaVersion()),
            "Organization policy contextSchemaVersion이 일치하지 않습니다.");
        require(namespace().equals(bundle.namespace()), "Organization policy namespace가 일치하지 않습니다.");
        require(OrganizationPolicyDomainSchema.POLICY_VERSION.equals(bundle.policyVersion()),
            "Organization policyVersion이 일치하지 않습니다.");
        require(bundle.modules().size() == 1 && MODULE_ID.equals(bundle.modules().getFirst().id()),
            "Organization policy module이 일치하지 않습니다.");
        Set<String> actions = Arrays.stream(OrganizationPolicyAction.values())
            .map(OrganizationPolicyAction::id)
            .collect(Collectors.toUnmodifiableSet());
        require(bundle.statementsByAction().keySet().equals(actions),
            "Organization policy action coverage가 일치하지 않습니다.");
    }

    private void require(boolean condition, String message) {
        if (!condition) {
            throw new IllegalStateException(message);
        }
    }
}
