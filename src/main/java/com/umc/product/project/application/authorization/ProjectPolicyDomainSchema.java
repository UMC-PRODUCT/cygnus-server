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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import com.umc.product.authorization.domain.policy.ActionSchema;
import com.umc.product.authorization.domain.policy.AttributeSchema;
import com.umc.product.authorization.domain.policy.OutcomeMergeStrategy;
import com.umc.product.authorization.domain.policy.OutcomeSchema;
import com.umc.product.authorization.domain.policy.PolicyAttributeKey;
import com.umc.product.authorization.domain.policy.PolicyDomainSchema;
import com.umc.product.authorization.domain.policy.PolicyValueType;
import com.umc.product.project.domain.enums.ProjectApplicationStatus;
import com.umc.product.project.domain.enums.ProjectStatus;

public final class ProjectPolicyDomainSchema {

    public static final String VERSION = "project-1.0";

    private static final Set<String> MEMBER = names(SUBJECT_KIND, SUBJECT_MEMBER_ID);
    private static final Set<String> PROJECT_COORDINATES = names(PROJECT_ID, PROJECT_GISU_ID, PROJECT_CHAPTER_ID);
    private static final Set<String> PROJECT_TARGET = names(PROJECT_GISU_ID, PROJECT_CHAPTER_ID);
    private static final Set<String> PROJECT_ADMIN = names(ACTIVE_SUPER_ADMIN, ACTIVE_CENTRAL_CORE,
        ACTIVE_CHAPTER_PRESIDENT);

    private ProjectPolicyDomainSchema() {}

    public static PolicyDomainSchema create() {
        List<ActionSchema> actions = Arrays.stream(ProjectPolicyAction.values())
            .map(ProjectPolicyDomainSchema::actionSchema)
            .toList();
        return new PolicyDomainSchema(VERSION, actions, attributes(), outcomes());
    }

    private static ActionSchema actionSchema(ProjectPolicyAction action) {
        return switch (action) {
            case PROJECT_CREATE -> action(action, required(MEMBER, PROJECT_TARGET,
                names(ACTIVE_PLAN_CHALLENGER, ACTIVE_SUPER_ADMIN, ACTIVE_CENTRAL_CORE,
                    ACTIVE_CHAPTER_PRESIDENT, ACTIVE_SCHOOL_CORE, IS_CREATOR)));
            case PROJECT_READ, PROJECT_MEMBER_LIST -> action(action, required(MEMBER, PROJECT_COORDINATES,
                names(PROJECT_STATUS, SUPER_ADMIN_ALLOW_DRAFT_READ, IS_PRODUCT_OWNER), PROJECT_ADMIN));
            case PROJECT_MEMBER_BATCH, CAPABILITY_LIST -> action(action, MEMBER);
            case PROJECT_UPDATE, PROJECT_TRANSFER_OWNERSHIP, PROJECT_MEMBER_ADD, PROJECT_MEMBER_REMOVE,
                    PROJECT_MEMBER_STATUS_UPDATE, FORM_UPDATE -> action(action, required(MEMBER, PROJECT_COORDINATES,
                names(PROJECT_STATUS, IS_CREATOR, IS_PRODUCT_OWNER), PROJECT_ADMIN));
            case PROJECT_SUBMIT -> action(action, required(MEMBER, PROJECT_COORDINATES,
                names(PROJECT_STATUS, IS_CREATOR)));
            case PROJECT_PUBLISH, PROJECT_QUOTA_UPDATE, PROJECT_ABORT -> action(action,
                required(MEMBER, PROJECT_COORDINATES, names(PROJECT_STATUS), PROJECT_ADMIN));
            case PROJECT_DELETE -> action(action, required(MEMBER, PROJECT_COORDINATES,
                names(PROJECT_STATUS, IS_PRODUCT_OWNER), PROJECT_ADMIN));
            case PROJECT_LIST_PUBLIC -> action(action, required(MEMBER, names(PROJECT_GISU_ID,
                    ACTIVE_SUPER_ADMIN, ACTIVE_CENTRAL_CORE, HAS_ACTIVE_CHAPTER_PRESIDENT,
                    MANAGED_GISU_IDS, MANAGED_CHAPTER_IDS)),
                ProjectPolicyOutcomes.PROJECT_ALL, ProjectPolicyOutcomes.PROJECT_PUBLIC_ONLY,
                ProjectPolicyOutcomes.PROJECT_GISU_IDS, ProjectPolicyOutcomes.PROJECT_CHAPTER_IDS);
            case PROJECT_LIST_MANAGED -> action(action, required(MEMBER, names(PROJECT_GISU_ID,
                    ACTIVE_SUPER_ADMIN, ACTIVE_CENTRAL_CORE, HAS_ACTIVE_CHAPTER_PRESIDENT,
                    HAS_ACTIVE_SCHOOL_CORE, REQUESTER_HAS_OWNED_PROJECT, MANAGED_GISU_IDS,
                    MANAGED_CHAPTER_IDS, MANAGED_SCHOOL_CORE_CHAPTER_IDS, REQUESTER_MEMBER_IDS)),
                ProjectPolicyOutcomes.PROJECT_ALL, ProjectPolicyOutcomes.PROJECT_GISU_IDS,
                ProjectPolicyOutcomes.PROJECT_CHAPTER_IDS, ProjectPolicyOutcomes.PROJECT_OWNER_MEMBER_IDS,
                ProjectPolicyOutcomes.PROJECT_INCLUDE_OWN_DRAFTS);
            case PROJECT_LIST_OWN_DRAFTS -> action(action,
                required(MEMBER, names(PROJECT_GISU_ID, REQUESTER_MEMBER_IDS)),
                ProjectPolicyOutcomes.PROJECT_OWNER_MEMBER_IDS, ProjectPolicyOutcomes.PROJECT_INCLUDE_OWN_DRAFTS);
            case APPLICATION_CREATE -> action(action, required(MEMBER, PROJECT_COORDINATES,
                names(CHALLENGER_IN_RESOURCE_GISU)));
            case APPLICATION_READ -> action(action, required(MEMBER, PROJECT_COORDINATES,
                names(APPLICATION_ID, APPLICATION_STATUS, SUPER_ADMIN_ALLOW_DRAFT_READ, IS_APPLICANT,
                    IS_PRODUCT_OWNER, IS_ACTIVE_PLAN_MEMBER), PROJECT_ADMIN));
            case APPLICATION_UPDATE, APPLICATION_SUBMIT, APPLICATION_CANCEL -> action(action,
                required(MEMBER, PROJECT_COORDINATES, names(APPLICATION_ID, APPLICATION_STATUS, IS_APPLICANT)));
            case APPLICATION_DECIDE -> action(action, required(MEMBER, PROJECT_COORDINATES,
                    names(APPLICATION_ID, APPLICATION_STATUS, IS_PRODUCT_OWNER, ACTIVE_SUPER_ADMIN)),
                ProjectPolicyOutcomes.APPLICATION_FORCE_DECISION);
            case APPLICATION_LIST_SELF -> action(action, required(MEMBER, names(REQUESTER_MEMBER_IDS)),
                ProjectPolicyOutcomes.APPLICATION_OWNER_MEMBER_IDS);
            case APPLICATION_LIST_PROJECT, APPLICATION_LIST_PROJECT_BATCH -> action(action,
                required(MEMBER, PROJECT_COORDINATES, names(RESOURCE_PROJECT_IDS, IS_PRODUCT_OWNER,
                    IS_ACTIVE_PLAN_MEMBER), PROJECT_ADMIN),
                ProjectPolicyOutcomes.APPLICATION_PROJECT_IDS,
                ProjectPolicyOutcomes.APPLICATION_INCLUDE_ONGOING_ROUNDS);
            case APPLICATION_LIST_MANAGEMENT -> action(action, required(MEMBER, names(PROJECT_GISU_ID,
                    ACTIVE_SUPER_ADMIN, ACTIVE_CENTRAL_CORE, HAS_ACTIVE_CHAPTER_PRESIDENT,
                    MANAGED_GISU_IDS, MANAGED_CHAPTER_IDS)),
                ProjectPolicyOutcomes.APPLICATION_ALL, ProjectPolicyOutcomes.APPLICATION_GISU_IDS,
                ProjectPolicyOutcomes.APPLICATION_CHAPTER_IDS);
            case FORM_READ -> action(action, required(MEMBER, PROJECT_COORDINATES,
                    names(PROJECT_STATUS, SUPER_ADMIN_ALLOW_DRAFT_READ,
                        IS_PRODUCT_OWNER, ACTIVE_SUPER_ADMIN, ACTIVE_CENTRAL_CORE,
                        ACTIVE_CHAPTER_PRESIDENT, CHALLENGER_IN_RESOURCE_GISU)),
                ProjectPolicyOutcomes.FORM_VIEW);
            case STATISTICS_PROJECT -> action(action, required(MEMBER, PROJECT_COORDINATES,
                names(IS_PRODUCT_OWNER, IS_ACTIVE_PLAN_MEMBER, ACTIVE_SUPER_ADMIN, ACTIVE_CENTRAL_CORE,
                    ACTIVE_CHAPTER_PRESIDENT, ACTIVE_SCHOOL_CORE)));
            case STATISTICS_CHAPTER -> action(action, required(MEMBER,
                names(PROJECT_GISU_ID, PROJECT_CHAPTER_ID, ACTIVE_SUPER_ADMIN, ACTIVE_CENTRAL_CORE,
                    ACTIVE_CHAPTER_PRESIDENT, ACTIVE_SCHOOL_CORE)));
            case STATISTICS_PUBLIC_MATCHING -> action(action,
                required(MEMBER, names(PROJECT_GISU_ID, PROJECT_CHAPTER_ID)));
            case MATCHING_LIST -> action(action, MEMBER);
            case MATCHING_CREATE -> action(action, required(MEMBER,
                names(MATCHING_ROUND_GISU_ID, MATCHING_ROUND_CHAPTER_ID), PROJECT_ADMIN));
            case MATCHING_UPDATE, MATCHING_DELETE, MATCHING_HUMAN_AUTO_DECIDE -> action(action,
                required(MEMBER, names(MATCHING_ROUND_ID, MATCHING_ROUND_GISU_ID,
                    MATCHING_ROUND_CHAPTER_ID), PROJECT_ADMIN));
            case MATCHING_SYSTEM_AUTO_DECIDE -> action(action,
                names(SUBJECT_KIND, SUBJECT_SYSTEM_ID, MATCHING_ROUND_ID,
                    MATCHING_ROUND_GISU_ID, MATCHING_ROUND_CHAPTER_ID));
        };
    }

    private static List<AttributeSchema> attributes() {
        List<AttributeSchema> attributes = new ArrayList<>();
        attributes.add(attribute(SUBJECT_KIND, Set.of("MEMBER", "SYSTEM")));
        attributes.add(attribute(SUBJECT_MEMBER_ID));
        attributes.add(attribute(SUBJECT_SYSTEM_ID));
        attributes.add(attribute(SUPER_ADMIN_ALLOW_DRAFT_READ));
        attributes.add(attribute(PROJECT_ID));
        attributes.add(attribute(PROJECT_GISU_ID));
        attributes.add(attribute(PROJECT_CHAPTER_ID));
        attributes.add(attribute(PROJECT_STATUS, enumNames(ProjectStatus.values())));
        attributes.add(attribute(APPLICATION_ID));
        attributes.add(attribute(APPLICATION_STATUS, enumNames(ProjectApplicationStatus.values())));
        attributes.add(attribute(MATCHING_ROUND_ID));
        attributes.add(attribute(MATCHING_ROUND_GISU_ID));
        attributes.add(attribute(MATCHING_ROUND_CHAPTER_ID));
        List.of(ACTIVE_SUPER_ADMIN, ACTIVE_CENTRAL_CORE, ACTIVE_CHAPTER_PRESIDENT, ACTIVE_SCHOOL_CORE,
                ACTIVE_PLAN_CHALLENGER, HAS_ACTIVE_CHAPTER_PRESIDENT, HAS_ACTIVE_SCHOOL_CORE,
                IS_CREATOR, IS_PRODUCT_OWNER, IS_APPLICANT, IS_ACTIVE_PLAN_MEMBER,
                CHALLENGER_IN_RESOURCE_GISU, REQUESTER_HAS_OWNED_PROJECT)
            .forEach(key -> attributes.add(attribute(key)));
        List.of(MANAGED_GISU_IDS, MANAGED_CHAPTER_IDS, MANAGED_SCHOOL_CORE_CHAPTER_IDS,
                REQUESTER_MEMBER_IDS, RESOURCE_PROJECT_IDS)
            .forEach(key -> attributes.add(attribute(key)));
        return List.copyOf(attributes);
    }

    private static List<OutcomeSchema> outcomes() {
        return List.of(
            booleanOutcome(ProjectPolicyOutcomes.PROJECT_ALL),
            booleanOutcome(ProjectPolicyOutcomes.PROJECT_PUBLIC_ONLY),
            setOutcome(ProjectPolicyOutcomes.PROJECT_GISU_IDS),
            setOutcome(ProjectPolicyOutcomes.PROJECT_CHAPTER_IDS),
            setOutcome(ProjectPolicyOutcomes.PROJECT_OWNER_MEMBER_IDS),
            booleanOutcome(ProjectPolicyOutcomes.PROJECT_INCLUDE_OWN_DRAFTS),
            booleanOutcome(ProjectPolicyOutcomes.APPLICATION_ALL),
            setOutcome(ProjectPolicyOutcomes.APPLICATION_GISU_IDS),
            setOutcome(ProjectPolicyOutcomes.APPLICATION_CHAPTER_IDS),
            setOutcome(ProjectPolicyOutcomes.APPLICATION_PROJECT_IDS),
            setOutcome(ProjectPolicyOutcomes.APPLICATION_OWNER_MEMBER_IDS),
            booleanOutcome(ProjectPolicyOutcomes.APPLICATION_INCLUDE_ONGOING_ROUNDS),
            new OutcomeSchema(ProjectPolicyOutcomes.FORM_VIEW, PolicyValueType.ENUM,
                OutcomeMergeStrategy.DOMINANCE, List.of("FULL", "APPLICANT", "NONE")),
            booleanOutcome(ProjectPolicyOutcomes.APPLICATION_FORCE_DECISION)
        );
    }

    private static ActionSchema action(ProjectPolicyAction action, Set<String> required, String... outcomes) {
        return new ActionSchema(action.id(), required, Set.of(), Set.of(outcomes));
    }

    private static AttributeSchema attribute(PolicyAttributeKey<?> key) {
        return attribute(key, Set.of());
    }

    private static AttributeSchema attribute(PolicyAttributeKey<?> key, Set<String> symbols) {
        return new AttributeSchema(key.name(), key.type(), symbols);
    }

    private static OutcomeSchema booleanOutcome(String key) {
        return new OutcomeSchema(key, PolicyValueType.BOOLEAN, OutcomeMergeStrategy.BOOLEAN_OR, List.of());
    }

    private static OutcomeSchema setOutcome(String key) {
        return new OutcomeSchema(key, PolicyValueType.LONG_SET, OutcomeMergeStrategy.SET_UNION, List.of());
    }

    private static Set<String> names(PolicyAttributeKey<?>... keys) {
        LinkedHashSet<String> names = new LinkedHashSet<>();
        Arrays.stream(keys).map(PolicyAttributeKey::name).forEach(names::add);
        return Set.copyOf(names);
    }

    @SafeVarargs
    private static Set<String> required(Set<String>... groups) {
        LinkedHashSet<String> names = new LinkedHashSet<>();
        Arrays.stream(groups).forEach(names::addAll);
        return Set.copyOf(names);
    }

    private static Set<String> enumNames(Enum<?>[] values) {
        return Arrays.stream(values).map(Enum::name).collect(java.util.stream.Collectors.toUnmodifiableSet());
    }
}
