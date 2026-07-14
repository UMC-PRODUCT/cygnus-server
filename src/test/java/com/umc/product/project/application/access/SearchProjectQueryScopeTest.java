package com.umc.product.project.application.access;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageRequest;

import com.umc.product.project.application.port.in.query.dto.SearchProjectQuery;
import com.umc.product.project.domain.enums.ProjectStatus;

class SearchProjectQueryScopeTest {

    private static final long REQUESTED_CHAPTER_ID = 8L;
    private static final Set<ProjectStatus> STATUSES = Set.of(ProjectStatus.IN_PROGRESS);

    @Test
    void 지부_제약이_없는_권한_clause를_요청_지부로_좁힌다() {
        ScopeClause unrestricted = ScopeClause.gisu(Set.of(1L), STATUSES);

        SearchProjectQuery result = query().withScopeClauses(List.of(unrestricted));

        assertThat(result.scopeClauses()).containsExactly(
            unrestricted.andChapterIds(Set.of(REQUESTED_CHAPTER_ID))
        );
    }

    @Test
    void 허용_지부와_요청_지부의_교집합이_없으면_clause를_제거한다() {
        ScopeClause otherChapter = ScopeClause.gisu(Set.of(1L), STATUSES)
            .andChapterIds(Set.of(7L));

        SearchProjectQuery result = query().withScopeClauses(List.of(otherChapter));

        assertThat(result.scopeClauses()).isEmpty();
    }

    @Test
    void 복수_허용_지부는_요청한_한_지부로_좁힌다() {
        ScopeClause multipleChapters = ScopeClause.gisu(Set.of(1L), STATUSES)
            .andChapterIds(Set.of(7L, REQUESTED_CHAPTER_ID));

        SearchProjectQuery result = query().withScopeClauses(List.of(multipleChapters));

        assertThat(result.scopeClauses()).containsExactly(
            ScopeClause.gisu(Set.of(1L), STATUSES).andChapterIds(Set.of(REQUESTED_CHAPTER_ID))
        );
    }

    private SearchProjectQuery query() {
        return SearchProjectQuery.forChallenger(
            1L,
            null,
            REQUESTED_CHAPTER_ID,
            null,
            null,
            null,
            PageRequest.of(0, 20)
        );
    }
}
