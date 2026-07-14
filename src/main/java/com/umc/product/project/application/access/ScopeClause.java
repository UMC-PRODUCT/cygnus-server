package com.umc.product.project.application.access;

import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import com.umc.product.project.domain.enums.ProjectStatus;

public record ScopeClause(
    Set<ProjectStatus> statuses,
    Optional<Set<Long>> gisuIds,
    Optional<Set<Long>> chapterIds,
    Optional<Set<Long>> ownerMemberIds
) {
    public ScopeClause {
        statuses = requiredValues(statuses, "statuses");
        gisuIds = immutableOptional(gisuIds, "gisuIds");
        chapterIds = immutableOptional(chapterIds, "chapterIds");
        ownerMemberIds = immutableOptional(ownerMemberIds, "ownerMemberIds");
    }

    public static ScopeClause statuses(Set<ProjectStatus> statuses) {
        return new ScopeClause(statuses, Optional.empty(), Optional.empty(), Optional.empty());
    }

    public static ScopeClause publicProjects() {
        return statuses(Set.of(ProjectStatus.IN_PROGRESS, ProjectStatus.COMPLETED));
    }

    public static ScopeClause gisu(Set<Long> gisuIds, Set<ProjectStatus> statuses) {
        return statuses(statuses).andGisuIds(gisuIds);
    }

    public static ScopeClause chapter(Set<Long> chapterIds, Set<ProjectStatus> statuses) {
        return statuses(statuses).andChapterIds(chapterIds);
    }

    public static ScopeClause owner(Set<Long> ownerMemberIds, Set<ProjectStatus> statuses) {
        return statuses(statuses).andOwnerMemberIds(ownerMemberIds);
    }

    public ScopeClause andGisuIds(Set<Long> ids) {
        return new ScopeClause(statuses, Optional.of(requiredValues(ids, "gisuIds")), chapterIds, ownerMemberIds);
    }

    public ScopeClause andChapterIds(Set<Long> ids) {
        return new ScopeClause(statuses, gisuIds, Optional.of(requiredValues(ids, "chapterIds")), ownerMemberIds);
    }

    public ScopeClause andOwnerMemberIds(Set<Long> ids) {
        return new ScopeClause(statuses, gisuIds, chapterIds, Optional.of(requiredValues(ids, "ownerMemberIds")));
    }

    public ScopeClause withStatuses(Set<ProjectStatus> newStatuses) {
        return new ScopeClause(newStatuses, gisuIds, chapterIds, ownerMemberIds);
    }

    private static <T> Optional<Set<T>> immutableOptional(Optional<Set<T>> values, String name) {
        Objects.requireNonNull(values, name + " must not be null");
        return values.map(items -> requiredValues(items, name));
    }

    private static <T> Set<T> requiredValues(Set<T> values, String name) {
        Objects.requireNonNull(values, name + " must not be null");
        if (values.isEmpty()) {
            throw new IllegalArgumentException(name + " must not be empty");
        }
        if (values.stream().anyMatch(Objects::isNull)) {
            throw new IllegalArgumentException(name + " must not contain null");
        }
        return Set.copyOf(values);
    }
}
