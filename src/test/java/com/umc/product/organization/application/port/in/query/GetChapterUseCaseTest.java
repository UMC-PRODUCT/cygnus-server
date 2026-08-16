package com.umc.product.organization.application.port.in.query;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.umc.product.organization.application.port.in.query.dto.chapter.ChapterInfo;
import com.umc.product.organization.application.port.in.query.dto.chapter.ChapterWithSchoolsInfo;
import com.umc.product.organization.application.port.out.command.SaveChapterPort;
import com.umc.product.organization.application.port.out.command.SaveChapterSchoolPort;
import com.umc.product.organization.application.port.out.command.SaveSchoolPort;
import com.umc.product.organization.domain.Chapter;
import com.umc.product.organization.domain.ChapterSchool;
import com.umc.product.organization.domain.Gisu;
import com.umc.product.organization.domain.School;
import com.umc.product.support.UseCaseTestSupport;
import com.umc.product.support.fixture.GisuFixture;

@Disabled
class GetChapterUseCaseTest extends UseCaseTestSupport {

    @Autowired
    private GetChapterUseCase getChapterUseCase;

    @Autowired
    private GisuFixture gisuFixture;

    @Autowired
    private SaveChapterPort saveChapterPort;

    @Autowired
    private SaveSchoolPort saveSchoolPort;

    @Autowired
    private SaveChapterSchoolPort saveChapterSchoolPort;

    @Test
    void 전체_지부_목록을_조회한다() {
        // given
        Gisu gisu = gisuFixture.활성_기수(8L);

        saveChapterPort.save(Chapter.create(gisu, "서울"));
        saveChapterPort.save(Chapter.create(gisu, "경기"));
        saveChapterPort.save(Chapter.create(gisu, "인천"));

        // when
        List<ChapterInfo> result = getChapterUseCase.getAllChapters();

        // then
        assertThat(result).hasSize(3).extracting(ChapterInfo::name)
            .containsExactlyInAnyOrder("서울", "경기", "인천");
    }

    @Test
    void 기수별_지부와_소속_학교_목록을_조회한다() {
        // given
        Gisu gisu9 = gisuFixture.활성_기수(9L);
        Gisu gisu10 = gisuFixture.활성_기수(10L);

        Chapter scorpioChapter = saveChapterPort.save(Chapter.create(gisu9, "Scorpio"));
        Chapter leoChapter = saveChapterPort.save(Chapter.create(gisu9, "Leo"));
        saveChapterPort.save(Chapter.create(gisu10, "Ain"));

        School school1 = saveSchoolPort.save(School.create("한성대", null, null));
        School school2 = saveSchoolPort.save(School.create("동국대", null, null));
        School school3 = saveSchoolPort.save(School.create("중앙대", null, null));

        saveChapterSchoolPort.save(ChapterSchool.create(scorpioChapter, school1));
        saveChapterSchoolPort.save(ChapterSchool.create(scorpioChapter, school2));
        saveChapterSchoolPort.save(ChapterSchool.create(leoChapter, school3));

        // when
        List<ChapterWithSchoolsInfo> result = getChapterUseCase.getChaptersWithSchoolsByGisuId(gisu9.getId());

        // then
        assertThat(result).hasSize(2);

        ChapterWithSchoolsInfo scorpioResult = result.stream()
            .filter(c -> c.chapterName().equals("Scorpio"))
            .findFirst().orElseThrow();
        assertThat(scorpioResult.schools()).hasSize(2)
            .extracting(ChapterWithSchoolsInfo.SchoolInfo::schoolName)
            .containsExactlyInAnyOrder("한성대", "동국대");

        ChapterWithSchoolsInfo leoResult = result.stream()
            .filter(c -> c.chapterName().equals("Leo"))
            .findFirst().orElseThrow();
        assertThat(leoResult.schools()).hasSize(1)
            .extracting(ChapterWithSchoolsInfo.SchoolInfo::schoolName)
            .containsExactly("중앙대");
    }

    @Test
    void 학교가_없는_지부도_조회된다() {
        // given
        Gisu gisu = gisuFixture.활성_기수(9L);
        saveChapterPort.save(Chapter.create(gisu, "Scorpio"));
        saveChapterPort.save(Chapter.create(gisu, "Leo"));

        // when
        List<ChapterWithSchoolsInfo> result = getChapterUseCase.getChaptersWithSchoolsByGisuId(gisu.getId());

        // then
        assertThat(result).hasSize(2);
        assertThat(result).allMatch(c -> c.schools().isEmpty());
    }
}
