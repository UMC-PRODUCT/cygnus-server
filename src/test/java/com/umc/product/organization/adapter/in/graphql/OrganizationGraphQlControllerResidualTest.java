package com.umc.product.organization.adapter.in.graphql;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.umc.product.organization.adapter.in.graphql.dto.GisuChapterGraphQlResponse;
import com.umc.product.organization.adapter.in.graphql.dto.GisuGraphQlResponse;
import com.umc.product.organization.application.port.in.query.GetChapterUseCase;
import com.umc.product.organization.application.port.in.query.GetGisuOrganizationUseCase;
import com.umc.product.organization.application.port.in.query.GetGisuUseCase;
import com.umc.product.organization.application.port.in.query.GetSchoolUseCase;
import com.umc.product.organization.application.port.in.query.dto.chapter.ChapterInfo;
import com.umc.product.organization.application.port.in.query.dto.chapter.ChapterWithSchoolsInfo;

@DisplayName("Organization GraphQL batch merge 잔여 경로")
class OrganizationGraphQlControllerResidualTest {

    @Test
    @DisplayName("중복 batch source와 중복 지부 조회 모델은 최초 결과를 안정적으로 유지한다")
    void 중복_batch_source를_병합한다() {
        GetGisuOrganizationUseCase organizationUseCase = mock(GetGisuOrganizationUseCase.class);
        GetGisuUseCase gisuUseCase = mock(GetGisuUseCase.class);
        GetChapterUseCase chapterUseCase = mock(GetChapterUseCase.class);
        GetSchoolUseCase schoolUseCase = mock(GetSchoolUseCase.class);
        var controller = new OrganizationGraphQlController(
            organizationUseCase, gisuUseCase, chapterUseCase, schoolUseCase
        );
        var gisu = new GisuGraphQlResponse(1L, 9L, null, null, true);
        var chapter = new GisuChapterGraphQlResponse(1L, 10L, "서울");
        var first = new ChapterWithSchoolsInfo(
            10L, "서울", List.of(new ChapterWithSchoolsInfo.SchoolInfo(100L, "첫 학교"))
        );
        var duplicate = new ChapterWithSchoolsInfo(
            10L, "중복", List.of(new ChapterWithSchoolsInfo.SchoolInfo(200L, "둘째 학교"))
        );
        given(chapterUseCase.listByGisuIds(Set.of(1L)))
            .willReturn(Map.of(1L, List.of(new ChapterInfo(10L, "서울"))));
        given(schoolUseCase.getSchoolListByGisuIds(Set.of(1L))).willReturn(Map.of());
        given(chapterUseCase.getChaptersWithSchoolsByGisuIds(Set.of(1L)))
            .willReturn(Map.of(1L, List.of(first, duplicate)));

        assertThat(controller.chaptersByGisu(List.of(gisu, gisu))).hasSize(1);
        assertThat(controller.schoolsByGisu(List.of(gisu, gisu))).hasSize(1);
        assertThat(controller.schoolsByGisuChapter(List.of(chapter, chapter)))
            .hasSize(1)
            .satisfies(result -> assertThat(result.get(chapter).getFirst().schoolName()).isEqualTo("첫 학교"));
    }
}
