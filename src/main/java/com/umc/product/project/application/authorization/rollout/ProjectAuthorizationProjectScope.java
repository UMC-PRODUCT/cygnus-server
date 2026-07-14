package com.umc.product.project.application.authorization.rollout;

import java.util.Collections;
import java.util.Set;
import java.util.TreeSet;

public record ProjectAuthorizationProjectScope(
    boolean all,
    boolean publicOnly,
    Set<Long> gisuIds,
    Set<Long> chapterIds,
    Set<Long> ownerMemberIds,
    boolean includeOwnDrafts
) {
    public ProjectAuthorizationProjectScope {
        gisuIds = sorted(gisuIds);
        chapterIds = sorted(chapterIds);
        ownerMemberIds = sorted(ownerMemberIds);
    }

    public static ProjectAuthorizationProjectScope none() {
        return new ProjectAuthorizationProjectScope(false, false, Set.of(), Set.of(), Set.of(), false);
    }

    private static Set<Long> sorted(Set<Long> values) {
        return Collections.unmodifiableSet(new TreeSet<>(values));
    }
}
