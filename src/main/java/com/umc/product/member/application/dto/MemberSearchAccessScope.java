package com.umc.product.member.application.dto;

import java.util.Objects;
import java.util.Set;

public final class MemberSearchAccessScope {

    private static final MemberSearchAccessScope DENIED = new MemberSearchAccessScope(true, false, Set.of(), Set.of());
    private static final MemberSearchAccessScope UNRESTRICTED =
        new MemberSearchAccessScope(false, true, Set.of(), Set.of());

    private final boolean denied;
    private final boolean unrestricted;
    private final Set<Long> allowedSchoolIds;
    private final Set<Long> allowedGisuIds;

    private MemberSearchAccessScope(
        boolean denied,
        boolean unrestricted,
        Set<Long> allowedSchoolIds,
        Set<Long> allowedGisuIds
    ) {
        this.denied = denied;
        this.unrestricted = unrestricted;
        this.allowedSchoolIds = Set.copyOf(allowedSchoolIds);
        this.allowedGisuIds = Set.copyOf(allowedGisuIds);
    }

    public static MemberSearchAccessScope denyAll() {
        return DENIED;
    }

    public static MemberSearchAccessScope allowAll() {
        return UNRESTRICTED;
    }

    public static MemberSearchAccessScope restrictedTo(Set<Long> allowedSchoolIds, Set<Long> allowedGisuIds) {
        Objects.requireNonNull(allowedSchoolIds, "allowedSchoolIds must not be null");
        Objects.requireNonNull(allowedGisuIds, "allowedGisuIds must not be null");
        validateIds(allowedSchoolIds, "allowedSchoolIds");
        validateIds(allowedGisuIds, "allowedGisuIds");

        if (allowedSchoolIds.isEmpty() && allowedGisuIds.isEmpty()) {
            throw new IllegalArgumentException("Restricted scope must allow at least one school or gisu");
        }

        return new MemberSearchAccessScope(false, false, allowedSchoolIds, allowedGisuIds);
    }

    public boolean denied() {
        return denied;
    }

    public boolean unrestricted() {
        return unrestricted;
    }

    public Set<Long> allowedSchoolIds() {
        return allowedSchoolIds;
    }

    public Set<Long> allowedGisuIds() {
        return allowedGisuIds;
    }

    private static void validateIds(Set<Long> ids, String fieldName) {
        if (ids.stream().anyMatch(id -> id == null || id <= 0)) {
            throw new IllegalArgumentException(fieldName + " must contain only positive IDs");
        }
    }
}
