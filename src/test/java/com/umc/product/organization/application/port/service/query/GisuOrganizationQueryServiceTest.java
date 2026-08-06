package com.umc.product.organization.application.port.service.query;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.umc.product.organization.application.port.in.query.GetChapterUseCase;
import com.umc.product.organization.application.port.in.query.GetGisuUseCase;
import com.umc.product.organization.application.port.in.query.GetSchoolUseCase;
import com.umc.product.organization.application.port.in.query.dto.chapter.ChapterInfo;
import com.umc.product.organization.application.port.in.query.dto.chapter.ChapterWithSchoolsInfo;
import com.umc.product.organization.application.port.in.query.dto.gisu.GisuInfo;
import com.umc.product.organization.application.port.in.query.dto.gisu.GisuOrganizationInfo;
import com.umc.product.organization.application.port.in.query.dto.gisu.GisuOrganizationQuery;
import com.umc.product.organization.application.port.in.query.dto.school.SchoolDetailInfo;
import com.umc.product.organization.domain.enums.SchoolLinkType;

@ExtendWith(MockitoExtension.class)
@DisplayName("GisuOrganizationQueryService")
class GisuOrganizationQueryServiceTest {

    @Mock
    GetGisuUseCase getGisuUseCase;

    @Mock
    GetChapterUseCase getChapterUseCase;

    @Mock
    GetSchoolUseCase getSchoolUseCase;

    @InjectMocks
    GisuOrganizationQueryService service;

    @Test
    @DisplayName("id 조회는 기수만 반환하고 include가 false면 지부와 학교를 조회하지 않는다")
    void id_조회는_기수만_반환하고_include가_false면_지부와_학교를_조회하지_않는다() {
        GisuInfo gisu9 = gisu(1L, 9L, false);
        GisuInfo gisu10 = gisu(2L, 10L, true);
        given(getGisuUseCase.batchGetByIds(List.of(1L, 2L))).willReturn(List.of(gisu9, gisu10));

        List<GisuOrganizationInfo> result = service.get(GisuOrganizationQuery.byIds(List.of(1L, 2L), false, false));

        assertThat(result).extracting(GisuOrganizationInfo::gisuId).containsExactly(1L, 2L);
        assertThat(result.getFirst().chapters()).isEmpty();
        assertThat(result.getFirst().schools()).isEmpty();
        then(getChapterUseCase).shouldHaveNoInteractions();
        then(getSchoolUseCase).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("generation 조회는 지부와 학교를 모두 포함해서 조립한다")
    void generation_조회는_지부와_학교를_모두_포함해서_조립한다() {
        given(getGisuUseCase.batchGetByGenerations(List.of(10L))).willReturn(List.of(gisu(2L, 10L, true)));
        given(getChapterUseCase.getChaptersWithSchoolsByGisuIds(new LinkedHashSet<>(List.of(2L))))
            .willReturn(Map.of(2L, List.of(chapterWithSchool(11L, "서울지부", 101L, "중앙대학교"))));
        given(getSchoolUseCase.getSchoolListByGisuIds(new LinkedHashSet<>(List.of(2L))))
            .willReturn(Map.of(2L, List.of(school(11L, "서울지부", 101L, "중앙대학교"))));

        List<GisuOrganizationInfo> result = service.get(
            GisuOrganizationQuery.byGenerations(List.of(10L), true, true)
        );

        GisuOrganizationInfo info = result.getFirst();
        assertThat(info.gisuId()).isEqualTo(2L);
        assertThat(info.chapters()).hasSize(1);
        assertThat(info.chapters().getFirst().schools()).extracting(GisuOrganizationInfo.ChapterSchoolInfo::schoolId)
            .containsExactly(101L);
        assertThat(info.schools()).hasSize(1);
        assertThat(info.schools().getFirst().links()).hasSize(1);
        then(getChapterUseCase).should().getChaptersWithSchoolsByGisuIds(new LinkedHashSet<>(List.of(2L)));
        then(getSchoolUseCase).should().getSchoolListByGisuIds(new LinkedHashSet<>(List.of(2L)));
        then(getChapterUseCase).should(never()).getChaptersWithSchoolsByGisuId(anyLong());
        then(getSchoolUseCase).should(never()).getSchoolListByGisuId(anyLong());
    }

    @Test
    @DisplayName("지부만 포함하면 지부 내 학교 목록은 비워서 반환한다")
    void 지부만_포함하면_지부_내_학교_목록은_비워서_반환한다() {
        given(getGisuUseCase.batchGetByIds(List.of(2L))).willReturn(List.of(gisu(2L, 10L, true)));
        given(getChapterUseCase.listByGisuIds(new LinkedHashSet<>(List.of(2L))))
            .willReturn(Map.of(2L, List.of(new ChapterInfo(11L, "서울지부"))));

        List<GisuOrganizationInfo> result = service.get(GisuOrganizationQuery.byIds(List.of(2L), true, false));

        assertThat(result.getFirst().chapters()).hasSize(1);
        assertThat(result.getFirst().chapters().getFirst().schools()).isEmpty();
        assertThat(result.getFirst().schools()).isEmpty();
        then(getChapterUseCase).should().listByGisuIds(new LinkedHashSet<>(List.of(2L)));
        then(getChapterUseCase).should(never()).listByGisuId(anyLong());
        then(getChapterUseCase).should(never()).getChaptersWithSchoolsByGisuId(anyLong());
        then(getSchoolUseCase).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("여러 기수 조회는 지부와 학교를 기수 목록 단위로 한 번에 조회한다")
    void 여러_기수_조회는_지부와_학교를_기수_목록_단위로_한_번에_조회한다() {
        given(getGisuUseCase.batchGetByIds(List.of(1L, 2L))).willReturn(List.of(
            gisu(1L, 9L, false),
            gisu(2L, 10L, true)
        ));
        given(getChapterUseCase.getChaptersWithSchoolsByGisuIds(new LinkedHashSet<>(List.of(1L, 2L))))
            .willReturn(Map.of(
                1L, List.of(chapterWithSchool(10L, "A 지부", 100L, "A 대학교")),
                2L, List.of(chapterWithSchool(20L, "B 지부", 200L, "B 대학교"))
            ));
        given(getSchoolUseCase.getSchoolListByGisuIds(new LinkedHashSet<>(List.of(1L, 2L))))
            .willReturn(Map.of(
                1L, List.of(school(10L, "A 지부", 100L, "A 대학교")),
                2L, List.of(school(20L, "B 지부", 200L, "B 대학교"))
            ));

        List<GisuOrganizationInfo> result = service.get(GisuOrganizationQuery.byIds(List.of(1L, 2L), true, true));

        assertThat(result).extracting(GisuOrganizationInfo::gisuId).containsExactly(1L, 2L);
        assertThat(result.get(0).chapters()).extracting(GisuOrganizationInfo.ChapterOrganizationInfo::chapterId)
            .containsExactly(10L);
        assertThat(result.get(1).schools()).extracting(GisuOrganizationInfo.SchoolOrganizationInfo::schoolId)
            .containsExactly(200L);
        then(getChapterUseCase).should().getChaptersWithSchoolsByGisuIds(new LinkedHashSet<>(List.of(1L, 2L)));
        then(getSchoolUseCase).should().getSchoolListByGisuIds(new LinkedHashSet<>(List.of(1L, 2L)));
        then(getChapterUseCase).should(never()).getChaptersWithSchoolsByGisuId(anyLong());
        then(getSchoolUseCase).should(never()).getSchoolListByGisuId(anyLong());
    }

    @Test
    @DisplayName("active 조회는 활성 기수 하나를 반환한다")
    void active_조회는_활성_기수_하나를_반환한다() {
        given(getGisuUseCase.getActiveGisu()).willReturn(gisu(2L, 10L, true));

        List<GisuOrganizationInfo> result = service.get(GisuOrganizationQuery.active(false, false));

        assertThat(result).extracting(GisuOrganizationInfo::gisuId).containsExactly(2L);
    }

    private GisuInfo gisu(Long gisuId, Long generation, boolean active) {
        return new GisuInfo(
            gisuId,
            generation,
            Instant.parse("2026-03-01T00:00:00Z"),
            Instant.parse("2026-08-31T23:59:59Z"),
            active
        );
    }

    private ChapterWithSchoolsInfo chapterWithSchool(Long chapterId, String chapterName, Long schoolId,
                                                     String schoolName) {
        return new ChapterWithSchoolsInfo(
            chapterId,
            chapterName,
            List.of(new ChapterWithSchoolsInfo.SchoolInfo(schoolId, schoolName))
        );
    }

    private SchoolDetailInfo school(Long chapterId, String chapterName, Long schoolId, String schoolName) {
        return new SchoolDetailInfo(
            chapterId,
            chapterName,
            schoolName,
            null,
            schoolId,
            "비고",
            "https://cdn.example.com/logo.png",
            List.of(new SchoolDetailInfo.SchoolLinkItem("인스타그램", SchoolLinkType.INSTAGRAM, "https://instagram.com/umc")),
            true,
            Instant.parse("2026-01-01T00:00:00Z"),
            Instant.parse("2026-01-02T00:00:00Z")
        );
    }
}
