package com.umc.product.organization.application.port.service.query;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

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
import org.springframework.test.util.ReflectionTestUtils;

import com.umc.product.organization.application.port.in.query.dto.chapter.ChapterInfo;
import com.umc.product.organization.application.port.in.query.dto.chapter.ChapterScopeInfo;
import com.umc.product.organization.application.port.in.query.dto.chapter.ChapterWithSchoolsInfo;
import com.umc.product.organization.application.port.out.query.LoadChapterPort;
import com.umc.product.organization.application.port.out.query.LoadChapterSchoolPort;
import com.umc.product.organization.domain.Chapter;
import com.umc.product.organization.domain.ChapterSchool;
import com.umc.product.organization.domain.Gisu;
import com.umc.product.organization.domain.School;

@ExtendWith(MockitoExtension.class)
@DisplayName("ChapterQueryService")
class ChapterQueryServiceTest {

    @Mock
    LoadChapterPort loadChapterPort;

    @Mock
    LoadChapterSchoolPort loadChapterSchoolPort;

    @InjectMocks
    ChapterQueryService chapterQueryService;

    @Test
    @DisplayName("listByGisuIds는 기수별 지부 목록을 그룹화한다")
    void listByGisuIds는_기수별_지부_목록을_그룹화한다() {
        Gisu gisu9 = gisu(1L, 9L);
        Gisu gisu10 = gisu(2L, 10L);
        Chapter scorpio = chapter(10L, gisu9, "Scorpio");
        Chapter ain = chapter(20L, gisu10, "Ain");
        given(loadChapterPort.findByGisuIds(new LinkedHashSet<>(List.of(1L, 2L))))
            .willReturn(List.of(scorpio, ain));

        Map<Long, List<ChapterInfo>> result = chapterQueryService.listByGisuIds(
            new LinkedHashSet<>(List.of(1L, 2L))
        );

        assertThat(result.get(1L)).extracting(ChapterInfo::name).containsExactly("Scorpio");
        assertThat(result.get(2L)).extracting(ChapterInfo::name).containsExactly("Ain");
    }

    @Test
    @DisplayName("belongsToGisu는 지부와 기수의 영속 관계를 반환한다")
    void belongsToGisu는_지부와_기수의_영속_관계를_반환한다() {
        given(loadChapterPort.existsByIdAndGisuId(10L, 1L)).willReturn(true);

        boolean result = chapterQueryService.belongsToGisu(10L, 1L);

        assertThat(result).isTrue();
        then(loadChapterPort).should().existsByIdAndGisuId(10L, 1L);
    }

    @Test
    @DisplayName("getChapterScopeById는 프로젝트가 없는 지부도 영속된 기수 좌표를 반환한다")
    void getChapterScopeById는_지부의_기수_좌표를_반환한다() {
        Gisu gisu = gisu(1L, 9L);
        Chapter chapter = chapter(10L, gisu, "Scorpio");
        given(loadChapterPort.findById(10L)).willReturn(chapter);

        ChapterScopeInfo result = chapterQueryService.getChapterScopeById(10L);

        assertThat(result).isEqualTo(new ChapterScopeInfo(10L, 1L));
        then(loadChapterPort).should().findById(10L);
    }

    @Test
    @DisplayName("getChaptersWithSchoolsByGisuIds는 기수별 지부와 학교 목록을 그룹화한다")
    void getChaptersWithSchoolsByGisuIds는_기수별_지부와_학교_목록을_그룹화한다() {
        Gisu gisu9 = gisu(1L, 9L);
        Gisu gisu10 = gisu(2L, 10L);
        Chapter scorpio = chapter(10L, gisu9, "Scorpio");
        Chapter ain = chapter(20L, gisu10, "Ain");
        School schoolA = school(100L, "A 대학교");
        School schoolB = school(200L, "B 대학교");
        LinkedHashSet<Long> gisuIds = new LinkedHashSet<>(List.of(1L, 2L));

        given(loadChapterPort.findByGisuIds(gisuIds)).willReturn(List.of(scorpio, ain));
        given(loadChapterSchoolPort.findByGisuIds(gisuIds)).willReturn(List.of(
            ChapterSchool.create(scorpio, schoolA),
            ChapterSchool.create(ain, schoolB)
        ));

        Map<Long, List<ChapterWithSchoolsInfo>> result = chapterQueryService.getChaptersWithSchoolsByGisuIds(gisuIds);

        assertThat(result.get(1L).getFirst().schools())
            .extracting(ChapterWithSchoolsInfo.SchoolInfo::schoolName)
            .containsExactly("A 대학교");
        assertThat(result.get(2L).getFirst().schools())
            .extracting(ChapterWithSchoolsInfo.SchoolInfo::schoolName)
            .containsExactly("B 대학교");
        then(loadChapterPort).should().findByGisuIds(gisuIds);
        then(loadChapterSchoolPort).should().findByGisuIds(gisuIds);
    }

    private Gisu gisu(Long id, Long generation) {
        Gisu gisu = Gisu.create(
            generation,
            Instant.parse("2026-03-01T00:00:00Z"),
            Instant.parse("2026-08-31T23:59:59Z"),
            true
        );
        ReflectionTestUtils.setField(gisu, "id", id);
        return gisu;
    }

    private Chapter chapter(Long id, Gisu gisu, String name) {
        Chapter chapter = Chapter.create(gisu, name);
        ReflectionTestUtils.setField(chapter, "id", id);
        return chapter;
    }

    private School school(Long id, String name) {
        School school = School.create(name, null);
        ReflectionTestUtils.setField(school, "id", id);
        return school;
    }
}
