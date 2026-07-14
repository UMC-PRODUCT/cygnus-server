package com.umc.product.project.application.port.in.query.dto;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import org.springframework.data.domain.Pageable;

import com.umc.product.common.domain.enums.ChallengerPart;
import com.umc.product.project.application.access.ScopeClause;
import com.umc.product.project.domain.enums.PartQuotaStatus;
import com.umc.product.project.domain.enums.ProjectStatus;

import lombok.Builder;

/**
 * 프로젝트 목록 검색 Query (PROJECT-001).
 * <p>
 * 권한(챌린저 vs Admin)에 따라 노출되는 상태 범위가 다릅니다.
 * 직접 {@code builder()}/생성자로 만들지 말고 {@link #forChallenger} 또는 {@link #forAdmin}을 사용하세요.
 * Controller는 분기 로직을 가지지 않고, Request DTO의 {@code toQuery(...)}에서 팩토리를 선택합니다.
 * <p>
 * Service 계층에서 {@link com.umc.product.project.application.access.ProjectAccessScope} 적용 시
 * {@link #withStatuses}, {@link #withChapterFilter}, {@link #withOwnerFilter}
 * 헬퍼로 새 인스턴스를 만들어 어댑터에 전달합니다.
 */
@Builder
public record SearchProjectQuery(
    Long gisuId,
    String keyword,
    Long chapterId,
    List<Long> productOwnerSchoolIds,
    Long productOwnerMemberId,
    Long includedOwnerMemberId,
    List<ChallengerPart> parts,
    PartQuotaStatus partQuotaStatus,
    List<ProjectStatus> statuses,
    List<ProjectStatus> includedOwnerStatuses,
    List<ScopeClause> scopeClauses,
    Pageable pageable
) {
    public SearchProjectQuery {
        Objects.requireNonNull(gisuId, "gisuId must not be null");
        Objects.requireNonNull(pageable, "pageable must not be null");
        if (statuses == null || statuses.isEmpty()) {
            throw new IllegalArgumentException("statuses must contain at least one ProjectStatus");
        }
        if (includedOwnerMemberId == null && includedOwnerStatuses != null && !includedOwnerStatuses.isEmpty()) {
            throw new IllegalArgumentException(
                "includedOwnerMemberId must not be null when includedOwnerStatuses exists");
        }
        if (includedOwnerMemberId != null && (includedOwnerStatuses == null || includedOwnerStatuses.isEmpty())) {
            throw new IllegalArgumentException("includedOwnerStatuses must contain at least one ProjectStatus");
        }
        scopeClauses = scopeClauses == null
            ? legacyScopeClauses(
                gisuId,
                chapterId,
                productOwnerMemberId,
                statuses,
                includedOwnerMemberId,
                includedOwnerStatuses)
            : List.copyOf(scopeClauses);
    }

    /**
     * 일반 챌린저용 — {@code statuses}를 {@code [IN_PROGRESS]}로 강제합니다.
     * 챌린저 요청에서 넘어온 {@code statuses} 값은 무시됩니다.
     */
    public static SearchProjectQuery forChallenger(
        Long gisuId,
        String keyword,
        Long chapterId,
        List<Long> productOwnerSchoolIds,
        List<ChallengerPart> parts,
        PartQuotaStatus partQuotaStatus,
        Pageable pageable
    ) {
        return SearchProjectQuery.builder()
            .gisuId(gisuId)
            .keyword(keyword)
            .chapterId(chapterId)
            .productOwnerSchoolIds(productOwnerSchoolIds)
            .parts(parts)
            .partQuotaStatus(partQuotaStatus)
            .statuses(List.of(ProjectStatus.IN_PROGRESS))
            .pageable(pageable)
            .build();
    }

    /**
     * Admin용 — {@code statuses}를 자유 지정합니다.
     * null/빈 리스트면 {@code [IN_PROGRESS]}로 default 처리합니다.
     */
    public static SearchProjectQuery forAdmin(
        Long gisuId,
        String keyword,
        Long chapterId,
        List<Long> productOwnerSchoolIds,
        List<ChallengerPart> parts,
        PartQuotaStatus partQuotaStatus,
        List<ProjectStatus> statuses,
        Pageable pageable
    ) {
        List<ProjectStatus> effectiveStatuses = (statuses == null || statuses.isEmpty())
            ? List.of(ProjectStatus.IN_PROGRESS)
            : statuses;
        return SearchProjectQuery.builder()
            .gisuId(gisuId)
            .keyword(keyword)
            .chapterId(chapterId)
            .productOwnerSchoolIds(productOwnerSchoolIds)
            .parts(parts)
            .partQuotaStatus(partQuotaStatus)
            .statuses(effectiveStatuses)
            .pageable(pageable)
            .build();
    }

    /** scope 적용 — 상태 필터만 교체. */
    public SearchProjectQuery withStatuses(Set<ProjectStatus> newStatuses) {
        Set<ProjectStatus> effectiveStatuses = toSet(newStatuses);
        return copyBuilder()
            .statuses(new ArrayList<>(effectiveStatuses))
            .scopeClauses(scopeClauses.stream()
                .map(clause -> clause.withStatuses(effectiveStatuses))
                .toList())
            .build();
    }

    /** scope 적용 — 지부 한정 + 상태 교체. */
    public SearchProjectQuery withChapterFilter(Long chapterId, Set<ProjectStatus> newStatuses) {
        Set<ProjectStatus> effectiveStatuses = toSet(newStatuses);
        return copyBuilder()
            .chapterId(chapterId)
            .statuses(new ArrayList<>(effectiveStatuses))
            .scopeClauses(List.of(ScopeClause.gisu(Set.of(gisuId), effectiveStatuses)
                .andChapterIds(Set.of(chapterId))))
            .build();
    }

    /** scope 적용 — owner 한정 + 상태 교체. */
    public SearchProjectQuery withOwnerFilter(Long memberId, Set<ProjectStatus> newStatuses) {
        Set<ProjectStatus> effectiveStatuses = toSet(newStatuses);
        return copyBuilder()
            .productOwnerMemberId(memberId)
            .statuses(new ArrayList<>(effectiveStatuses))
            .scopeClauses(List.of(ScopeClause.gisu(Set.of(gisuId), effectiveStatuses)
                .andOwnerMemberIds(Set.of(memberId))))
            .build();
    }

    /** scope 적용 — 기존 scope 조건에 본인 PO 프로젝트를 OR 로 추가 포함. */
    public SearchProjectQuery withIncludedOwner(Long memberId, Set<ProjectStatus> ownerStatuses) {
        Objects.requireNonNull(memberId, "memberId must not be null");
        Set<ProjectStatus> effectiveOwnerStatuses = toRequiredSet(ownerStatuses);
        List<ScopeClause> clauses = new ArrayList<>(scopeClauses);
        clauses.add(ScopeClause.owner(Set.of(memberId), effectiveOwnerStatuses));
        return copyBuilder()
            .includedOwnerMemberId(memberId)
            .includedOwnerStatuses(new ArrayList<>(effectiveOwnerStatuses))
            .scopeClauses(clauses)
            .build();
    }

    public SearchProjectQuery withScopeClauses(List<ScopeClause> clauses) {
        return copyBuilder()
            .scopeClauses(constrainToRequestedChapter(clauses, chapterId))
            .build();
    }

    private static List<ScopeClause> constrainToRequestedChapter(List<ScopeClause> clauses, Long chapterId) {
        List<ScopeClause> immutableClauses = List.copyOf(clauses);
        if (chapterId == null) {
            return immutableClauses;
        }
        return immutableClauses.stream()
            .filter(clause -> clause.chapterIds().map(ids -> ids.contains(chapterId)).orElse(true))
            .map(clause -> clause.andChapterIds(Set.of(chapterId)))
            .toList();
    }

    private SearchProjectQueryBuilder copyBuilder() {
        return SearchProjectQuery.builder()
            .gisuId(gisuId)
            .keyword(keyword)
            .chapterId(chapterId)
            .productOwnerSchoolIds(productOwnerSchoolIds)
            .productOwnerMemberId(productOwnerMemberId)
            .includedOwnerMemberId(includedOwnerMemberId)
            .parts(parts)
            .partQuotaStatus(partQuotaStatus)
            .statuses(statuses)
            .includedOwnerStatuses(includedOwnerStatuses)
            .scopeClauses(scopeClauses)
            .pageable(pageable);
    }

    private static List<ProjectStatus> toList(Set<ProjectStatus> set) {
        return new ArrayList<>(toSet(set));
    }

    private static Set<ProjectStatus> toSet(Set<ProjectStatus> set) {
        return set == null || set.isEmpty()
            ? Set.of(ProjectStatus.IN_PROGRESS)
            : Set.copyOf(set);
    }

    private static Set<ProjectStatus> toRequiredSet(Set<ProjectStatus> set) {
        if (set == null || set.isEmpty()) {
            throw new IllegalArgumentException("ownerStatuses must contain at least one ProjectStatus");
        }
        return Set.copyOf(set);
    }

    private static List<ScopeClause> legacyScopeClauses(
        Long gisuId,
        Long chapterId,
        Long productOwnerMemberId,
        List<ProjectStatus> statuses,
        Long includedOwnerMemberId,
        List<ProjectStatus> includedOwnerStatuses
    ) {
        ScopeClause base = ScopeClause.gisu(Set.of(gisuId), Set.copyOf(statuses));
        if (chapterId != null) {
            base = base.andChapterIds(Set.of(chapterId));
        }
        if (productOwnerMemberId != null) {
            base = base.andOwnerMemberIds(Set.of(productOwnerMemberId));
        }

        List<ScopeClause> clauses = new ArrayList<>();
        clauses.add(base);
        if (includedOwnerMemberId != null) {
            clauses.add(ScopeClause.owner(Set.of(includedOwnerMemberId), Set.copyOf(includedOwnerStatuses)));
        }
        return List.copyOf(clauses);
    }
}
