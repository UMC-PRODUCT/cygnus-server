package com.umc.product.organization.application.port.service.query;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.umc.product.organization.application.port.in.query.dto.school.SchoolChapterInfo;
import com.umc.product.organization.application.port.in.query.dto.school.SchoolChapterNameInfo;
import com.umc.product.organization.application.port.in.query.dto.school.SchoolDetailInfo;
import com.umc.product.organization.application.port.in.query.dto.school.SchoolGisuChapterInfo;
import com.umc.product.organization.application.port.out.query.LoadSchoolPort;
import com.umc.product.organization.domain.enums.SchoolLinkType;
import com.umc.product.storage.application.port.in.query.GetFileUseCase;

@ExtendWith(MockitoExtension.class)
@DisplayName("SchoolQueryService")
class SchoolQueryServiceTest {

    @Mock
    LoadSchoolPort loadSchoolPort;

    @Mock
    GetFileUseCase getFileUseCase;

    @InjectMocks
    SchoolQueryService schoolQueryService;

    @Test
    @DisplayName("getSchoolListByGisuIds는 기수별 학교 상세 목록을 그룹화한다")
    void getSchoolListByGisuIds는_기수별_학교_상세_목록을_그룹화한다() {
        LinkedHashSet<Long> gisuIds = new LinkedHashSet<>(List.of(1L, 2L));
        given(loadSchoolPort.findSchoolDetailsByGisuIds(gisuIds)).willReturn(List.of(
            school(1L, 10L, "A 지부", 100L, "A 대학교", "logo-a"),
            school(2L, 20L, "B 지부", 200L, "B 대학교", null)
        ));
        given(loadSchoolPort.findLinksBySchoolIds(List.of(100L, 200L))).willReturn(Map.of(
            100L, List.of(new SchoolDetailInfo.SchoolLinkItem(
                "인스타그램",
                SchoolLinkType.INSTAGRAM,
                "https://instagram.com/a"
            ))
        ));
        given(getFileUseCase.getFileLinks(List.of("logo-a"))).willReturn(Map.of(
            "logo-a",
            "https://cdn.example.com/logo-a.png"
        ));

        Map<Long, List<SchoolDetailInfo>> result = schoolQueryService.getSchoolListByGisuIds(gisuIds);

        assertThat(result.get(1L)).extracting(SchoolDetailInfo::schoolName).containsExactly("A 대학교");
        assertThat(result.get(1L).getFirst().logoImageUrl()).isEqualTo("https://cdn.example.com/logo-a.png");
        assertThat(result.get(1L).getFirst().links()).hasSize(1);
        assertThat(result.get(2L)).extracting(SchoolDetailInfo::schoolName).containsExactly("B 대학교");
        assertThat(result.get(2L).getFirst().links()).isEmpty();
        then(loadSchoolPort).should().findSchoolDetailsByGisuIds(gisuIds);
    }

    @Test
    @DisplayName("getSchoolListByGisuIds는 기수 목록이 비어 있으면 조회하지 않는다")
    void getSchoolListByGisuIds는_기수_목록이_비어_있으면_조회하지_않는다() {
        Map<Long, List<SchoolDetailInfo>> result = schoolQueryService.getSchoolListByGisuIds(new LinkedHashSet<>());

        assertThat(result).isEmpty();
        then(loadSchoolPort).shouldHaveNoInteractions();
        then(getFileUseCase).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("getSchoolChapterNamesByGisuId는 로고와 링크를 조회하지 않고 학교 및 지부 이름을 반환한다")
    void getSchoolChapterNamesByGisuId는_로고와_링크를_조회하지_않고_학교_및_지부_이름을_반환한다() {
        given(loadSchoolPort.findSchoolChapterNamesByGisuId(1L)).willReturn(List.of(
            new SchoolChapterNameInfo(10L, "A 지부", "A 대학교", 100L)
        ));

        List<SchoolChapterNameInfo> result = schoolQueryService.getSchoolChapterNamesByGisuId(1L);

        assertThat(result).containsExactly(new SchoolChapterNameInfo(10L, "A 지부", "A 대학교", 100L));
        then(loadSchoolPort).should().findSchoolChapterNamesByGisuId(1L);
        then(loadSchoolPort).shouldHaveNoMoreInteractions();
        then(getFileUseCase).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("listDetailsByIds는 학교 ID별 상세 정보를 조회하고 링크와 로고 URL을 조립한다")
    void listDetailsByIds는_학교_ID별_상세_정보를_조회하고_링크와_로고_URL을_조립한다() {
        LinkedHashSet<Long> schoolIds = new LinkedHashSet<>(List.of(100L, 200L));
        given(loadSchoolPort.findSchoolDetailsByIds(schoolIds)).willReturn(List.of(
            school(10L, "A 지부", 100L, "A 대학교", "logo-a"),
            school(20L, "B 지부", 200L, "B 대학교", null)
        ));
        given(loadSchoolPort.findLinksBySchoolIds(List.of(100L, 200L))).willReturn(Map.of(
            100L, List.of(new SchoolDetailInfo.SchoolLinkItem(
                "인스타그램",
                SchoolLinkType.INSTAGRAM,
                "https://instagram.com/a"
            ))
        ));
        given(getFileUseCase.getFileLinks(List.of("logo-a"))).willReturn(Map.of(
            "logo-a",
            "https://cdn.example.com/logo-a.png"
        ));

        List<SchoolDetailInfo> result = schoolQueryService.listDetailsByIds(schoolIds);

        assertThat(result).extracting(SchoolDetailInfo::schoolName).containsExactly("A 대학교", "B 대학교");
        assertThat(result.getFirst().logoImageUrl()).isEqualTo("https://cdn.example.com/logo-a.png");
        assertThat(result.getFirst().links()).hasSize(1);
        assertThat(result.getLast().links()).isEmpty();
        then(loadSchoolPort).should().findSchoolDetailsByIds(schoolIds);
    }

    @Test
    @DisplayName("listDetailsByIds는 학교 ID 목록이 비어 있으면 조회하지 않는다")
    void listDetailsByIds는_학교_ID_목록이_비어_있으면_조회하지_않는다() {
        List<SchoolDetailInfo> result = schoolQueryService.listDetailsByIds(Set.of());

        assertThat(result).isEmpty();
        then(loadSchoolPort).shouldHaveNoInteractions();
        then(getFileUseCase).shouldHaveNoInteractions();
    }

    private SchoolChapterInfo school(
        Long chapterId,
        String chapterName,
        Long schoolId,
        String schoolName,
        String logoImageId
    ) {
        return new SchoolChapterInfo(
            chapterId,
            chapterName,
            schoolName,
            null,
            schoolId,
            "비고",
            logoImageId,
            true,
            Instant.parse("2026-01-01T00:00:00Z"),
            Instant.parse("2026-01-02T00:00:00Z")
        );
    }

    private SchoolGisuChapterInfo school(
        Long gisuId,
        Long chapterId,
        String chapterName,
        Long schoolId,
        String schoolName,
        String logoImageId
    ) {
        return new SchoolGisuChapterInfo(
            gisuId,
            chapterId,
            chapterName,
            schoolName,
            null,
            schoolId,
            "비고",
            logoImageId,
            true,
            Instant.parse("2026-01-01T00:00:00Z"),
            Instant.parse("2026-01-02T00:00:00Z")
        );
    }
}
