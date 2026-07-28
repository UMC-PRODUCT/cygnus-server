package com.umc.product.organization.application.port.in.command;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.umc.product.global.exception.BusinessException;
import com.umc.product.organization.application.port.in.command.dto.CreateChapterCommand;
import com.umc.product.organization.application.port.out.command.SaveSchoolPort;
import com.umc.product.organization.application.port.out.query.LoadChapterPort;
import com.umc.product.organization.application.port.out.query.LoadChapterSchoolPort;
import com.umc.product.organization.application.port.out.query.LoadSchoolPort;
import com.umc.product.organization.domain.Chapter;
import com.umc.product.organization.domain.Gisu;
import com.umc.product.organization.domain.School;
import com.umc.product.support.UseCaseTestSupport;
import com.umc.product.support.fixture.GisuFixture;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class ManageChapterUseCaseTest extends UseCaseTestSupport {

    @Autowired
    private ManageChapterUseCase manageChapterUseCase;

    @Autowired
    private GisuFixture gisuFixture;

    @Autowired
    private SaveSchoolPort saveSchoolPort;

    @Autowired
    private LoadChapterPort loadChapterPort;

    @Autowired
    private LoadSchoolPort loadSchoolPort;

    @Autowired
    private LoadChapterSchoolPort loadChapterSchoolPort;

    @Test
    void 지부를_생성한다() {
        // given
        Gisu gisu = gisuFixture.활성_기수(9L);
        CreateChapterCommand command = new CreateChapterCommand(gisu.getId(), "Scorpio", List.of());

        // when
        Long chapterId = manageChapterUseCase.create(command);

        // then
        Chapter chapter = loadChapterPort.findById(chapterId);
        assertThat(chapter.getName()).isEqualTo("Scorpio");
        assertThat(chapter.getGisu().getId()).isEqualTo(gisu.getId());
    }

    @Test
    void 학교와_함께_지부를_생성한다() {
        // given
        Gisu gisu = gisuFixture.활성_기수(9L);
        School school1 = saveSchoolPort.save(School.create("한성대", null, null));
        School school2 = saveSchoolPort.save(School.create("동국대", null, null));

        CreateChapterCommand command = new CreateChapterCommand(
            gisu.getId(),
            "Scorpio",
            List.of(school1.getId(), school2.getId())
        );

        // when
        Long chapterId = manageChapterUseCase.create(command);

        // then
        Chapter chapter = loadChapterPort.findById(chapterId);
        assertThat(chapter.getName()).isEqualTo("Scorpio");

        School savedSchool1 = loadSchoolPort.findSchoolDetailById(school1.getId());
        School savedSchool2 = loadSchoolPort.findSchoolDetailById(school2.getId());

        assertThat(savedSchool1.getChapterSchools()).hasSize(1);
        assertThat(savedSchool1.getChapterSchools().get(0).getChapter().getId()).isEqualTo(chapterId);

        assertThat(savedSchool2.getChapterSchools()).hasSize(1);
        assertThat(savedSchool2.getChapterSchools().get(0).getChapter().getId()).isEqualTo(chapterId);
    }

    @Test
    void 존재하지_않는_학교로_지부를_생성하면_예외가_발생한다() {
        // given
        Gisu gisu = gisuFixture.활성_기수(9L);
        CreateChapterCommand command = new CreateChapterCommand(
            gisu.getId(),
            "서울",
            List.of(999L)
        );

        // when & then
        assertThatThrownBy(() -> manageChapterUseCase.create(command))
            .isInstanceOf(BusinessException.class);
    }

    @Test
    void 존재하지_않는_기수로_지부를_생성하면_예외가_발생한다() {
        // given
        CreateChapterCommand command = new CreateChapterCommand(999L, "Scorpio", List.of());

        // when & then
        assertThatThrownBy(() -> manageChapterUseCase.create(command))
            .isInstanceOf(BusinessException.class);
    }

    @Test
    void 지부를_삭제한다() {
        // given
        Gisu gisu = gisuFixture.활성_기수(9L);
        Long chapterId = manageChapterUseCase.create(
            new CreateChapterCommand(gisu.getId(), "Scorpio", List.of())
        );

        // when
        manageChapterUseCase.delete(chapterId);

        // then
        assertThatThrownBy(() -> loadChapterPort.findById(chapterId))
            .isInstanceOf(BusinessException.class);
    }

    @Test
    void 지부_삭제_시_소속_학교는_유지된다() {
        // given
        Gisu gisu = gisuFixture.활성_기수(9L);
        School school1 = saveSchoolPort.save(School.create("한성대", null, null));
        School school2 = saveSchoolPort.save(School.create("동국대", null, null));

        Long chapterId = manageChapterUseCase.create(
            new CreateChapterCommand(gisu.getId(), "Scorpio", List.of(school1.getId(), school2.getId()))
        );

        // when
        manageChapterUseCase.delete(chapterId);

        // then
        assertThat(loadSchoolPort.existsById(school1.getId())).isTrue();
        assertThat(loadSchoolPort.existsById(school2.getId())).isTrue();
    }

    @Test
    void 지부_삭제_시_학교와_지부_연결이_제거된다() {
        // given
        Gisu gisu = gisuFixture.활성_기수(9L);
        School school = saveSchoolPort.save(School.create("한성대", null, null));

        Long chapterId = manageChapterUseCase.create(
            new CreateChapterCommand(gisu.getId(), "Scorpio", List.of(school.getId()))
        );

        // when
        manageChapterUseCase.delete(chapterId);

        // then
        assertThat(loadChapterSchoolPort.findByGisuId(gisu.getId())).isEmpty();
    }

    @Test
    void 존재하지_않는_지부를_삭제하면_예외가_발생한다() {
        // when & then
        assertThatThrownBy(() -> manageChapterUseCase.delete(999L))
            .isInstanceOf(BusinessException.class);
    }

    @Test
    void 같은_기수에_중복된_지부_이름으로_생성하면_예외가_발생한다() {
        // given
        Gisu gisu = gisuFixture.활성_기수(9L);
        manageChapterUseCase.create(new CreateChapterCommand(gisu.getId(), "Scorpio", List.of()));

        // when & then
        assertThatThrownBy(() -> manageChapterUseCase.create(
            new CreateChapterCommand(gisu.getId(), "Scorpio", List.of())
        )).isInstanceOf(BusinessException.class);
    }

    @Test
    void 같은_기수에_이미_다른_지부에_배정된_학교로_생성하면_예외가_발생한다() {
        // given
        Gisu gisu = gisuFixture.활성_기수(9L);
        School school = saveSchoolPort.save(School.create("한성대", null, null));

        manageChapterUseCase.create(
            new CreateChapterCommand(gisu.getId(), "Scorpio", List.of(school.getId()))
        );

        // when & then
        assertThatThrownBy(() -> manageChapterUseCase.create(
            new CreateChapterCommand(gisu.getId(), "Leo", List.of(school.getId()))
        )).isInstanceOf(BusinessException.class);
    }
}
