package com.umc.product.project.application.authorization.rollout;

import java.util.Collections;
import java.util.Set;
import java.util.TreeSet;

public record ProjectAuthorizationApplicationScope(
    boolean all,
    Set<Long> gisuIds,
    Set<Long> chapterIds,
    Set<Long> projectIds,
    Set<Long> ownerMemberIds,
    boolean includeOngoingRounds
) {
    public ProjectAuthorizationApplicationScope {
        gisuIds = sorted(gisuIds);
        chapterIds = sorted(chapterIds);
        projectIds = sorted(projectIds);
        ownerMemberIds = sorted(ownerMemberIds);
    }

    public static ProjectAuthorizationApplicationScope none() {
        return new ProjectAuthorizationApplicationScope(false, Set.of(), Set.of(), Set.of(), Set.of(), false);
    }

    private static Set<Long> sorted(Set<Long> values) {
        return Collections.unmodifiableSet(new TreeSet<>(values));
    }
}
