package com.umc.product.project.application.authorization.rollout;

import java.util.Collections;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;

import com.umc.product.project.application.authorization.ProjectPolicyResourceContext;

public record ProjectAuthorizationResourceSnapshot(
    ProjectPolicyResourceContext policyContext,
    Optional<Long> targetMemberSchoolId,
    Set<Long> targetChapterSchoolIds,
    boolean applicationRoundScopeMismatch
) {
    public ProjectAuthorizationResourceSnapshot {
        Objects.requireNonNull(policyContext);
        targetMemberSchoolId = Objects.requireNonNull(targetMemberSchoolId);
        targetChapterSchoolIds = Collections.unmodifiableSet(new TreeSet<>(targetChapterSchoolIds));
    }

    public static ProjectAuthorizationResourceSnapshot of(ProjectPolicyResourceContext policyContext) {
        return new ProjectAuthorizationResourceSnapshot(policyContext, Optional.empty(), Set.of(), false);
    }

    public static ProjectAuthorizationResourceSnapshot withTargetMemberSchool(
        ProjectPolicyResourceContext policyContext,
        long targetMemberSchoolId
    ) {
        return new ProjectAuthorizationResourceSnapshot(
            policyContext, Optional.of(targetMemberSchoolId), Set.of(), false);
    }

    public static ProjectAuthorizationResourceSnapshot withTargetChapterSchools(
        ProjectPolicyResourceContext policyContext,
        Set<Long> targetChapterSchoolIds
    ) {
        return new ProjectAuthorizationResourceSnapshot(
            policyContext, Optional.empty(), targetChapterSchoolIds, false);
    }

    public static ProjectAuthorizationResourceSnapshot withApplicationRoundScopeMismatch(
        ProjectPolicyResourceContext policyContext
    ) {
        return new ProjectAuthorizationResourceSnapshot(policyContext, Optional.empty(), Set.of(), true);
    }

    public ProjectAuthorizationResourceSnapshot(
        ProjectPolicyResourceContext policyContext,
        Optional<Long> targetMemberSchoolId,
        Set<Long> targetChapterSchoolIds
    ) {
        this(policyContext, targetMemberSchoolId, targetChapterSchoolIds, false);
    }
}
