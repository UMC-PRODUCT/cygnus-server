package com.umc.product.project.application.authorization;

import java.util.Set;

public record ProjectPolicyRelationFacts(
    boolean activeSuperAdmin,
    boolean activeCentralCoreInResourceGisu,
    boolean activeChapterPresidentForResource,
    boolean activeSchoolCoreForResource,
    boolean activePlanChallengerInResourceGisu,
    boolean hasActiveChapterPresidentInResourceGisu,
    boolean hasActiveSchoolCoreInResourceGisu,
    boolean creator,
    boolean productOwner,
    boolean applicant,
    boolean activePlanMember,
    boolean challengerInResourceGisu,
    boolean requesterHasOwnedProjectInResourceGisu,
    Set<Long> managedGisuIds,
    Set<Long> managedChapterIdsInResourceGisu,
    Set<Long> managedSchoolCoreChapterIdsInResourceGisu,
    Set<Long> requesterMemberIds,
    Set<Long> resourceProjectIds
) {
    public ProjectPolicyRelationFacts {
        managedGisuIds = Set.copyOf(managedGisuIds);
        managedChapterIdsInResourceGisu = Set.copyOf(managedChapterIdsInResourceGisu);
        managedSchoolCoreChapterIdsInResourceGisu = Set.copyOf(managedSchoolCoreChapterIdsInResourceGisu);
        requesterMemberIds = Set.copyOf(requesterMemberIds);
        resourceProjectIds = Set.copyOf(resourceProjectIds);
    }
}
