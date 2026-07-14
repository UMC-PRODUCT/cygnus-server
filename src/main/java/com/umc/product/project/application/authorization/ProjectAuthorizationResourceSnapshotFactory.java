package com.umc.product.project.application.authorization;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import com.umc.product.common.domain.enums.ChallengerRoleType;
import com.umc.product.organization.application.port.in.query.GetChapterUseCase;
import com.umc.product.organization.application.port.in.query.dto.chapter.ChapterInfo;
import com.umc.product.project.application.authorization.rollout.ProjectAuthorizationResourceSnapshot;

@Component
public class ProjectAuthorizationResourceSnapshotFactory {

    private final GetChapterUseCase getChapterUseCase;

    public ProjectAuthorizationResourceSnapshotFactory(GetChapterUseCase getChapterUseCase) {
        this.getChapterUseCase = getChapterUseCase;
    }

    public ProjectAuthorizationResourceSnapshot create(
        ProjectPolicySubjectSnapshot subject,
        ProjectPolicyResourceContext policyContext,
        Optional<Long> targetMemberSchoolId
    ) {
        return createAll(
            subject,
            Map.of(Boolean.TRUE, policyContext),
            Map.of(Boolean.TRUE, targetMemberSchoolId)
        ).get(Boolean.TRUE);
    }

    public <K> Map<K, ProjectAuthorizationResourceSnapshot> createAll(
        ProjectPolicySubjectSnapshot subject,
        Map<K, ProjectPolicyResourceContext> policyContexts,
        Map<K, Optional<Long>> targetMemberSchoolIds
    ) {
        Objects.requireNonNull(subject);
        Objects.requireNonNull(policyContexts);
        Objects.requireNonNull(targetMemberSchoolIds);
        if (!policyContexts.keySet().equals(targetMemberSchoolIds.keySet())) {
            throw new IllegalArgumentException("policyContexts와 targetMemberSchoolIds의 키는 같아야 합니다.");
        }
        policyContexts.values().forEach(Objects::requireNonNull);
        targetMemberSchoolIds.values().forEach(Objects::requireNonNull);

        Set<Long> subjectSchoolIds = subject.roles().stream()
            .filter(role -> role.roleType() == ChallengerRoleType.SCHOOL_PRESIDENT
                || role.roleType() == ChallengerRoleType.SCHOOL_VICE_PRESIDENT)
            .map(ProjectPolicyRoleTuple::organizationId)
            .filter(Objects::nonNull)
            .collect(Collectors.toCollection(LinkedHashSet::new));
        Set<Long> resourceGisuIds = policyContexts.values().stream()
            .filter(context -> context.gisuId().isPresent() && context.chapterId().isPresent())
            .map(context -> context.gisuId().orElseThrow())
            .collect(Collectors.toCollection(LinkedHashSet::new));
        Map<Long, Map<Long, ChapterInfo>> chapterByGisuAndSchool =
            subjectSchoolIds.isEmpty() || resourceGisuIds.isEmpty()
                ? Map.of()
                : getChapterUseCase.getChapterMapByGisuIdsAndSchoolIds(
                    Set.copyOf(resourceGisuIds), Set.copyOf(subjectSchoolIds));

        Map<K, ProjectAuthorizationResourceSnapshot> snapshots = new LinkedHashMap<>();
        policyContexts.forEach((key, policyContext) -> snapshots.put(
            key,
            new ProjectAuthorizationResourceSnapshot(
                policyContext,
                targetMemberSchoolIds.get(key),
                targetChapterSchoolIds(policyContext, subjectSchoolIds, chapterByGisuAndSchool)
            )
        ));
        return Map.copyOf(snapshots);
    }

    private Set<Long> targetChapterSchoolIds(
        ProjectPolicyResourceContext policyContext,
        Set<Long> subjectSchoolIds,
        Map<Long, Map<Long, ChapterInfo>> chapterByGisuAndSchool
    ) {
        if (policyContext.gisuId().isEmpty() || policyContext.chapterId().isEmpty()) {
            return Set.of();
        }
        Map<Long, ChapterInfo> chapterBySchool = chapterByGisuAndSchool.getOrDefault(
            policyContext.gisuId().orElseThrow(), Map.of());
        long targetChapterId = policyContext.chapterId().orElseThrow();
        return chapterBySchool.entrySet().stream()
            .filter(entry -> subjectSchoolIds.contains(entry.getKey()))
            .filter(entry -> Objects.equals(entry.getValue().id(), targetChapterId))
            .map(Map.Entry::getKey)
            .collect(Collectors.toUnmodifiableSet());
    }
}
