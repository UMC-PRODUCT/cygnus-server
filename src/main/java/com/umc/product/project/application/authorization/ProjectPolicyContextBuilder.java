package com.umc.product.project.application.authorization;

import static com.umc.product.project.application.authorization.ProjectPolicyAttributes.ACTIVE_CENTRAL_CORE;
import static com.umc.product.project.application.authorization.ProjectPolicyAttributes.ACTIVE_CHAPTER_PRESIDENT;
import static com.umc.product.project.application.authorization.ProjectPolicyAttributes.ACTIVE_PLAN_CHALLENGER;
import static com.umc.product.project.application.authorization.ProjectPolicyAttributes.ACTIVE_SCHOOL_CORE;
import static com.umc.product.project.application.authorization.ProjectPolicyAttributes.ACTIVE_SUPER_ADMIN;
import static com.umc.product.project.application.authorization.ProjectPolicyAttributes.APPLICATION_ID;
import static com.umc.product.project.application.authorization.ProjectPolicyAttributes.APPLICATION_STATUS;
import static com.umc.product.project.application.authorization.ProjectPolicyAttributes.CHALLENGER_IN_RESOURCE_GISU;
import static com.umc.product.project.application.authorization.ProjectPolicyAttributes.HAS_ACTIVE_CHAPTER_PRESIDENT;
import static com.umc.product.project.application.authorization.ProjectPolicyAttributes.HAS_ACTIVE_SCHOOL_CORE;
import static com.umc.product.project.application.authorization.ProjectPolicyAttributes.IS_ACTIVE_PLAN_MEMBER;
import static com.umc.product.project.application.authorization.ProjectPolicyAttributes.IS_APPLICANT;
import static com.umc.product.project.application.authorization.ProjectPolicyAttributes.IS_CREATOR;
import static com.umc.product.project.application.authorization.ProjectPolicyAttributes.IS_PRODUCT_OWNER;
import static com.umc.product.project.application.authorization.ProjectPolicyAttributes.MANAGED_CHAPTER_IDS;
import static com.umc.product.project.application.authorization.ProjectPolicyAttributes.MANAGED_GISU_IDS;
import static com.umc.product.project.application.authorization.ProjectPolicyAttributes.MANAGED_SCHOOL_CORE_CHAPTER_IDS;
import static com.umc.product.project.application.authorization.ProjectPolicyAttributes.MATCHING_ROUND_CHAPTER_ID;
import static com.umc.product.project.application.authorization.ProjectPolicyAttributes.MATCHING_ROUND_GISU_ID;
import static com.umc.product.project.application.authorization.ProjectPolicyAttributes.MATCHING_ROUND_ID;
import static com.umc.product.project.application.authorization.ProjectPolicyAttributes.PROJECT_CHAPTER_ID;
import static com.umc.product.project.application.authorization.ProjectPolicyAttributes.PROJECT_GISU_ID;
import static com.umc.product.project.application.authorization.ProjectPolicyAttributes.PROJECT_ID;
import static com.umc.product.project.application.authorization.ProjectPolicyAttributes.PROJECT_STATUS;
import static com.umc.product.project.application.authorization.ProjectPolicyAttributes.REQUESTER_HAS_OWNED_PROJECT;
import static com.umc.product.project.application.authorization.ProjectPolicyAttributes.REQUESTER_MEMBER_IDS;
import static com.umc.product.project.application.authorization.ProjectPolicyAttributes.RESOURCE_PROJECT_IDS;
import static com.umc.product.project.application.authorization.ProjectPolicyAttributes.SUBJECT_KIND;
import static com.umc.product.project.application.authorization.ProjectPolicyAttributes.SUBJECT_MEMBER_ID;
import static com.umc.product.project.application.authorization.ProjectPolicyAttributes.SUBJECT_SYSTEM_ID;
import static com.umc.product.project.application.authorization.ProjectPolicyAttributes.SUPER_ADMIN_ALLOW_DRAFT_READ;

import java.util.Optional;
import java.util.Set;

import com.umc.product.authorization.domain.policy.ActionSchema;
import com.umc.product.authorization.domain.policy.PolicyAttributeKey;
import com.umc.product.authorization.domain.policy.PolicyAttributeSet;
import com.umc.product.authorization.domain.policy.PolicyDomainSchema;
import com.umc.product.authorization.domain.policy.PolicyValue;
import com.umc.product.project.application.authorization.rollout.ProjectAuthorizationResourceSnapshot;

public class ProjectPolicyContextBuilder {

    private final PolicyDomainSchema domainSchema;
    private final ProjectPolicyRelationResolver relationResolver;

    public ProjectPolicyContextBuilder() {
        this(ProjectPolicyDomainSchema.create(), new ProjectPolicyRelationResolver());
    }

    ProjectPolicyContextBuilder(
        PolicyDomainSchema domainSchema,
        ProjectPolicyRelationResolver relationResolver
    ) {
        this.domainSchema = domainSchema;
        this.relationResolver = relationResolver;
    }

    public PolicyAttributeSet build(
        ProjectPolicyAction action,
        ProjectPolicySubjectSnapshot subject,
        ProjectPolicyResourceContext resource
    ) {
        return build(action, subject, ProjectAuthorizationResourceSnapshot.of(resource));
    }

    public PolicyAttributeSet build(
        ProjectPolicyAction action,
        ProjectPolicySubjectSnapshot subject,
        ProjectAuthorizationResourceSnapshot resourceSnapshot
    ) {
        ProjectPolicyResourceContext resource = resourceSnapshot.policyContext();
        ActionSchema actionSchema = domainSchema.action(action.id())
            .orElseThrow(() -> new IllegalArgumentException("Project policy action schema가 없습니다: " + action.id()));
        Set<String> required = actionSchema.requiredAttributes();
        ProjectPolicyRelationFacts facts = relationResolver.resolve(
            subject, resource, resourceSnapshot.targetMemberSchoolId());
        PolicyAttributeSet.Builder builder = PolicyAttributeSet.builder();

        put(required, builder, SUBJECT_KIND, new PolicyValue.EnumValue(subject.principal().kind().name()));
        if (subject.principal() instanceof ProjectPolicyPrincipal.Member member) {
            put(required, builder, SUBJECT_MEMBER_ID, new PolicyValue.LongValue(member.memberId()));
        }
        if (subject.principal() instanceof ProjectPolicyPrincipal.SystemPrincipal system) {
            put(required, builder, SUBJECT_SYSTEM_ID, new PolicyValue.StringValue(system.systemId()));
        }
        put(required, builder, SUPER_ADMIN_ALLOW_DRAFT_READ,
            new PolicyValue.BooleanValue(resource.superAdminAllowDraftRead()));

        putLong(required, builder, PROJECT_ID, resource.projectId());
        putLong(required, builder, PROJECT_GISU_ID, resource.gisuId());
        putLong(required, builder, PROJECT_CHAPTER_ID, resource.chapterId());
        if (required.contains(PROJECT_STATUS.name())) {
            builder.put(PROJECT_STATUS, new PolicyValue.EnumValue(resource.projectStatus()
                .orElseThrow(() -> missing(PROJECT_STATUS.name())).name()));
        }
        putLong(required, builder, APPLICATION_ID, resource.applicationId());
        if (required.contains(APPLICATION_STATUS.name())) {
            builder.put(APPLICATION_STATUS, new PolicyValue.EnumValue(resource.applicationStatus()
                .orElseThrow(() -> missing(APPLICATION_STATUS.name())).name()));
        }
        putLong(required, builder, MATCHING_ROUND_ID, resource.matchingRoundId());
        putLong(required, builder, MATCHING_ROUND_GISU_ID, resource.gisuId());
        putLong(required, builder, MATCHING_ROUND_CHAPTER_ID, resource.chapterId());

        putBoolean(required, builder, ACTIVE_SUPER_ADMIN, facts.activeSuperAdmin());
        putBoolean(required, builder, ACTIVE_CENTRAL_CORE, facts.activeCentralCoreInResourceGisu());
        putBoolean(required, builder, ACTIVE_CHAPTER_PRESIDENT, facts.activeChapterPresidentForResource());
        putBoolean(required, builder, ACTIVE_SCHOOL_CORE, facts.activeSchoolCoreForResource());
        putBoolean(required, builder, ACTIVE_PLAN_CHALLENGER, facts.activePlanChallengerInResourceGisu());
        putBoolean(required, builder, HAS_ACTIVE_CHAPTER_PRESIDENT,
            facts.hasActiveChapterPresidentInResourceGisu());
        putBoolean(required, builder, HAS_ACTIVE_SCHOOL_CORE, facts.hasActiveSchoolCoreInResourceGisu());
        putBoolean(required, builder, IS_CREATOR, facts.creator());
        putBoolean(required, builder, IS_PRODUCT_OWNER, facts.productOwner());
        putBoolean(required, builder, IS_APPLICANT, facts.applicant());
        putBoolean(required, builder, IS_ACTIVE_PLAN_MEMBER, facts.activePlanMember());
        putBoolean(required, builder, CHALLENGER_IN_RESOURCE_GISU, facts.challengerInResourceGisu());
        putBoolean(required, builder, REQUESTER_HAS_OWNED_PROJECT,
            facts.requesterHasOwnedProjectInResourceGisu());

        putLongSet(required, builder, MANAGED_GISU_IDS, facts.managedGisuIds());
        putLongSet(required, builder, MANAGED_CHAPTER_IDS, facts.managedChapterIdsInResourceGisu());
        putLongSet(required, builder, MANAGED_SCHOOL_CORE_CHAPTER_IDS,
            facts.managedSchoolCoreChapterIdsInResourceGisu());
        putLongSet(required, builder, REQUESTER_MEMBER_IDS, facts.requesterMemberIds());
        putLongSet(required, builder, RESOURCE_PROJECT_IDS, facts.resourceProjectIds());

        PolicyAttributeSet result = builder.build();
        if (!result.entries().stream().map(PolicyAttributeSet.PolicyAttribute::name).collect(
                java.util.stream.Collectors.toUnmodifiableSet()).equals(required)) {
            throw new IllegalStateException("Project policy context required attribute 구성이 일치하지 않습니다.");
        }
        return result;
    }

    private void putLong(
        Set<String> required,
        PolicyAttributeSet.Builder builder,
        PolicyAttributeKey<PolicyValue.LongValue> key,
        Optional<Long> value
    ) {
        if (required.contains(key.name())) {
            builder.put(key, new PolicyValue.LongValue(value.orElseThrow(() -> missing(key.name()))));
        }
    }

    private void putBoolean(
        Set<String> required,
        PolicyAttributeSet.Builder builder,
        PolicyAttributeKey<PolicyValue.BooleanValue> key,
        boolean value
    ) {
        put(required, builder, key, new PolicyValue.BooleanValue(value));
    }

    private void putLongSet(
        Set<String> required,
        PolicyAttributeSet.Builder builder,
        PolicyAttributeKey<PolicyValue.LongSetValue> key,
        Set<Long> value
    ) {
        put(required, builder, key, new PolicyValue.LongSetValue(value));
    }

    private <V extends PolicyValue> void put(
        Set<String> required,
        PolicyAttributeSet.Builder builder,
        PolicyAttributeKey<V> key,
        V value
    ) {
        if (required.contains(key.name())) {
            builder.put(key, value);
        }
    }

    private IllegalArgumentException missing(String attribute) {
        return new IllegalArgumentException("Project policy resource fact가 없습니다: " + attribute);
    }
}
