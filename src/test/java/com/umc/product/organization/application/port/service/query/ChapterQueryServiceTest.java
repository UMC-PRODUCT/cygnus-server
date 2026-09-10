package com.umc.product.organization.application.port.service.query;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
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
import com.umc.product.organization.application.port.in.query.dto.chapter.ChapterWithSchoolsInfo;
import com.umc.product.organization.application.port.out.query.LoadChapterPort;
import com.umc.product.organization.application.port.out.query.LoadChapterSchoolPort;
import com.umc.product.organization.domain.Chapter;
import com.umc.product.organization.domain.ChapterSchool;
import com.umc.product.organization.domain.Gisu;
import com.umc.product.organization.domain.School;
import com.umc.product.organization.exception.OrganizationDomainException;
import com.umc.product.organization.exception.OrganizationErrorCode;

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
    @DisplayName("학교가 여러 기수에 참여했어도 요청한 기수의 지부를 조회한다")
    void 요청한_기수의_학교_지부를_조회한다() {
        // given
        School school = school(100L, "A 대학교");
        Chapter previousChapter = chapter(10L, gisu(1L, 10L), "이전 지부");
        Chapter currentChapter = chapter(20L, gisu(2L, 11L), "현재 지부");
        given(loadChapterSchoolPort.findBySchoolId(100L)).willReturn(List.of(
            ChapterSchool.create(previousChapter, school),
            ChapterSchool.create(currentChapter, school)
        ));

        // when & then
        assertThat(chapterQueryService.findByGisuAndSchool(2L, 100L))
            .contains(new ChapterInfo(20L, "현재 지부"));
        assertThat(chapterQueryService.byGisuAndSchool(2L, 100L))
            .isEqualTo(new ChapterInfo(20L, "현재 지부"));
    }

    @Test
    @DisplayName("해당 기수 지부가 없으면 선택 조회는 빈 값을 반환하고 필수 조회는 거부한다")
    void 지부가_없는_학교의_선택_조회와_필수_조회를_구분한다() {
        // given
        School school = school(100L, "A 대학교");
        Chapter previousChapter = chapter(10L, gisu(1L, 10L), "이전 지부");
        given(loadChapterSchoolPort.findBySchoolId(100L))
            .willReturn(List.of(ChapterSchool.create(previousChapter, school)));

        // when & then
        assertThat(chapterQueryService.findByGisuAndSchool(2L, 100L)).isEmpty();
        assertThatThrownBy(() -> chapterQueryService.byGisuAndSchool(2L, 100L))
            .isInstanceOfSatisfying(OrganizationDomainException.class,
                exception -> assertThat(exception.getBaseCode()).isEqualTo(OrganizationErrorCode.CHAPTER_NOT_FOUND));
    }

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
        School school = School.create(name, null, null);
        ReflectionTestUtils.setField(school, "id", id);
        return school;
    }
}
