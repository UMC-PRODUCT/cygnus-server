package com.umc.product.project.application.authorization;

import com.umc.product.authorization.domain.policy.PolicyAttributeKey;
import com.umc.product.authorization.domain.policy.PolicyValue;
import com.umc.product.authorization.domain.policy.PolicyValueType;

public final class ProjectPolicyAttributes {

    public static final PolicyAttributeKey<PolicyValue.EnumValue> SUBJECT_KIND =
        key("subject.kind", PolicyValueType.ENUM);
    public static final PolicyAttributeKey<PolicyValue.LongValue> SUBJECT_MEMBER_ID =
        key("subject.memberId", PolicyValueType.LONG);
    public static final PolicyAttributeKey<PolicyValue.StringValue> SUBJECT_SYSTEM_ID =
        key("subject.systemId", PolicyValueType.STRING);
    public static final PolicyAttributeKey<PolicyValue.BooleanValue> SUPER_ADMIN_ALLOW_DRAFT_READ =
        key("environment.superAdminAllowDraftRead", PolicyValueType.BOOLEAN);

    public static final PolicyAttributeKey<PolicyValue.LongValue> PROJECT_ID =
        key("resource.project.id", PolicyValueType.LONG);
    public static final PolicyAttributeKey<PolicyValue.LongValue> PROJECT_GISU_ID =
        key("resource.project.gisuId", PolicyValueType.LONG);
    public static final PolicyAttributeKey<PolicyValue.LongValue> PROJECT_CHAPTER_ID =
        key("resource.project.chapterId", PolicyValueType.LONG);
    public static final PolicyAttributeKey<PolicyValue.EnumValue> PROJECT_STATUS =
        key("resource.project.status", PolicyValueType.ENUM);
    public static final PolicyAttributeKey<PolicyValue.LongValue> APPLICATION_ID =
        key("resource.application.id", PolicyValueType.LONG);
    public static final PolicyAttributeKey<PolicyValue.EnumValue> APPLICATION_STATUS =
        key("resource.application.status", PolicyValueType.ENUM);
    public static final PolicyAttributeKey<PolicyValue.LongValue> MATCHING_ROUND_ID =
        key("resource.matchingRound.id", PolicyValueType.LONG);
    public static final PolicyAttributeKey<PolicyValue.LongValue> MATCHING_ROUND_GISU_ID =
        key("resource.matchingRound.gisuId", PolicyValueType.LONG);
    public static final PolicyAttributeKey<PolicyValue.LongValue> MATCHING_ROUND_CHAPTER_ID =
        key("resource.matchingRound.chapterId", PolicyValueType.LONG);

    public static final PolicyAttributeKey<PolicyValue.BooleanValue> ACTIVE_SUPER_ADMIN =
        booleanKey("relation.activeSuperAdmin");
    public static final PolicyAttributeKey<PolicyValue.BooleanValue> ACTIVE_CENTRAL_CORE =
        booleanKey("relation.activeCentralCoreInResourceGisu");
    public static final PolicyAttributeKey<PolicyValue.BooleanValue> ACTIVE_CHAPTER_PRESIDENT =
        booleanKey("relation.activeChapterPresidentForResource");
    public static final PolicyAttributeKey<PolicyValue.BooleanValue> ACTIVE_SCHOOL_CORE =
        booleanKey("relation.activeSchoolCoreForResource");
    public static final PolicyAttributeKey<PolicyValue.BooleanValue> ACTIVE_PLAN_CHALLENGER =
        booleanKey("relation.activePlanChallengerInResourceGisu");
    public static final PolicyAttributeKey<PolicyValue.BooleanValue> HAS_ACTIVE_CHAPTER_PRESIDENT =
        booleanKey("relation.hasActiveChapterPresidentInResourceGisu");
    public static final PolicyAttributeKey<PolicyValue.BooleanValue> HAS_ACTIVE_SCHOOL_CORE =
        booleanKey("relation.hasActiveSchoolCoreInResourceGisu");
    public static final PolicyAttributeKey<PolicyValue.BooleanValue> IS_CREATOR =
        booleanKey("relation.isCreator");
    public static final PolicyAttributeKey<PolicyValue.BooleanValue> IS_PRODUCT_OWNER =
        booleanKey("relation.isProductOwner");
    public static final PolicyAttributeKey<PolicyValue.BooleanValue> IS_APPLICANT =
        booleanKey("relation.isApplicant");
    public static final PolicyAttributeKey<PolicyValue.BooleanValue> IS_ACTIVE_PLAN_MEMBER =
        booleanKey("relation.isActivePlanMember");
    public static final PolicyAttributeKey<PolicyValue.BooleanValue> CHALLENGER_IN_RESOURCE_GISU =
        booleanKey("relation.challengerInResourceGisu");
    public static final PolicyAttributeKey<PolicyValue.BooleanValue> REQUESTER_HAS_OWNED_PROJECT =
        booleanKey("relation.requesterHasOwnedProjectInResourceGisu");

    public static final PolicyAttributeKey<PolicyValue.LongSetValue> MANAGED_GISU_IDS =
        longSetKey("relation.managedGisuIds");
    public static final PolicyAttributeKey<PolicyValue.LongSetValue> MANAGED_CHAPTER_IDS =
        longSetKey("relation.managedChapterIdsInResourceGisu");
    public static final PolicyAttributeKey<PolicyValue.LongSetValue> MANAGED_SCHOOL_CORE_CHAPTER_IDS =
        longSetKey("relation.managedSchoolCoreChapterIdsInResourceGisu");
    public static final PolicyAttributeKey<PolicyValue.LongSetValue> REQUESTER_MEMBER_IDS =
        longSetKey("relation.requesterMemberIds");
    public static final PolicyAttributeKey<PolicyValue.LongSetValue> RESOURCE_PROJECT_IDS =
        longSetKey("relation.resourceProjectIds");

    private ProjectPolicyAttributes() {}

    private static PolicyAttributeKey<PolicyValue.BooleanValue> booleanKey(String name) {
        return key(name, PolicyValueType.BOOLEAN);
    }

    private static PolicyAttributeKey<PolicyValue.LongSetValue> longSetKey(String name) {
        return key(name, PolicyValueType.LONG_SET);
    }

    private static <V extends PolicyValue> PolicyAttributeKey<V> key(String name, PolicyValueType type) {
        return new PolicyAttributeKey<>(name, type);
    }
}
