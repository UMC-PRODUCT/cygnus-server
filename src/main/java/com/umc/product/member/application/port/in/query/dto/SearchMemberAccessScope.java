package com.umc.product.member.application.port.in.query.dto;

import java.util.Set;
import java.util.stream.Collectors;

public record SearchMemberAccessScope(
    boolean unrestricted,
    boolean noAccess,
    Set<Long> allowedGisuIds,
    Set<Long> allowedSchoolIds
) {

    public SearchMemberAccessScope {
        allowedGisuIds = allowedGisuIds == null ? Set.of() : Set.copyOf(allowedGisuIds);
        allowedSchoolIds = allowedSchoolIds == null ? Set.of() : Set.copyOf(allowedSchoolIds);
    }

    public static SearchMemberAccessScope all() {
        return new SearchMemberAccessScope(true, false, Set.of(), Set.of());
    }

    public static SearchMemberAccessScope none() {
        return new SearchMemberAccessScope(false, true, Set.of(), Set.of());
    }

    public static SearchMemberAccessScope ofGisuIds(Set<Long> gisuIds) {
        Set<Long> sanitized = sanitize(gisuIds);
        if (sanitized.isEmpty()) {
            return none();
        }
        return new SearchMemberAccessScope(false, false, sanitized, Set.of());
    }

    public static SearchMemberAccessScope ofSchoolIds(Set<Long> schoolIds) {
        Set<Long> sanitized = sanitize(schoolIds);
        if (sanitized.isEmpty()) {
            return none();
        }
        return new SearchMemberAccessScope(false, false, Set.of(), sanitized);
    }

    private static Set<Long> sanitize(Set<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return Set.of();
        }

        return ids.stream()
            .filter(id -> id != null)
            .collect(Collectors.toUnmodifiableSet());
    }
}
