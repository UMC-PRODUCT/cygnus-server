package com.umc.product.organization.application.port.service;

import static com.umc.product.support.fixture.OrganizationUnitFixture.기수;
import static com.umc.product.support.fixture.OrganizationUnitFixture.스터디_그룹;
import static com.umc.product.support.fixture.OrganizationUnitFixture.지부;
import static com.umc.product.support.fixture.OrganizationUnitFixture.지부_학교;
import static com.umc.product.support.fixture.OrganizationUnitFixture.학교;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.umc.product.common.domain.enums.ChallengerPart;
import com.umc.product.organization.application.port.in.command.dto.AddStudyMemberCommand;
import com.umc.product.organization.application.port.in.command.dto.AddStudyMentorCommand;
import com.umc.product.organization.application.port.in.command.dto.CreateStudyGroupCommand;
import com.umc.product.organization.application.port.in.command.dto.DeleteStudyMemberCommand;
import com.umc.product.organization.application.port.in.command.dto.DeleteStudyMentorCommand;
import com.umc.product.organization.application.port.in.command.dto.UpdateStudyGroupCommand;
import com.umc.product.organization.application.port.in.query.dto.school.SchoolChapterInfo;
import com.umc.product.organization.application.port.in.query.dto.school.SchoolDetailInfo;
import com.umc.product.organization.application.port.in.query.dto.school.SchoolGisuChapterInfo;
import com.umc.product.organization.application.port.in.query.dto.school.SchoolNameInfo;
import com.umc.product.organization.application.port.out.command.SaveStudyGroupPort;
import com.umc.product.organization.application.port.out.query.LoadChapterPort;
import com.umc.product.organization.application.port.out.query.LoadChapterSchoolPort;
import com.umc.product.organization.application.port.out.query.LoadGisuPort;
import com.umc.product.organization.application.port.out.query.LoadSchoolPort;
import com.umc.product.organization.application.port.out.query.LoadStudyGroupPort;
import com.umc.product.organization.application.port.service.command.StudyGroupCommandService;
import com.umc.product.organization.application.port.service.query.ChapterQueryService;
import com.umc.product.organization.application.port.service.query.SchoolQueryService;
import com.umc.product.organization.domain.SchoolLink;
import com.umc.product.organization.domain.enums.SchoolLinkType;
import com.umc.product.organization.exception.OrganizationDomainException;
import com.umc.product.storage.application.port.in.query.GetFileUseCase;
import com.umc.product.storage.application.port.in.query.dto.FileInfo;

@DisplayName("Organization 핵심 서비스 잔여 경로")
class OrganizationCoreServiceResidualTest {

    @Nested
    @ExtendWith(MockitoExtension.class)
    @DisplayName("지부 조회")
    class ChapterQuery {

        @Mock
        LoadChapterPort loadChapterPort;

        @Mock
        LoadChapterSchoolPort loadChapterSchoolPort;

        @InjectMocks
        ChapterQueryService service;

        @Test
        @DisplayName("전체·기수별·학교별 지부를 조회하고 없는 기수-학교 조합은 거부한다")
        void 지부를_여러_표면으로_조회한다() {
            var gisu = 기수(1L, 9L, true);
            var chapter = 지부(2L, gisu, "서울");
            var school = 학교(3L, "테스트대학교");
            var assignment = 지부_학교(4L, chapter, school);
            given(loadChapterPort.findAll()).willReturn(List.of(chapter));
            given(loadChapterPort.findByGisuId(1L)).willReturn(List.of(chapter));
            given(loadChapterSchoolPort.findBySchoolId(3L)).willReturn(List.of(assignment));
            given(loadChapterSchoolPort.findBySchoolId(999L)).willReturn(List.of());
            given(loadChapterSchoolPort.findBySchoolIds(List.of(3L))).willReturn(List.of(assignment, assignment));

            assertThat(service.getAllChapters()).singleElement().satisfies(info -> assertThat(info.id()).isEqualTo(2L));
            assertThat(service.listByGisuId(1L)).singleElement().satisfies(info -> assertThat(info.name()).isEqualTo("서울"));
            assertThat(service.byGisuAndSchool(1L, 3L).id()).isEqualTo(2L);
            assertThatThrownBy(() -> service.byGisuAndSchool(1L, 999L))
                .isInstanceOf(OrganizationDomainException.class);
            assertThat(service.getChaptersBySchool(3L)).hasSize(1);
            assertThat(service.getChaptersBySchoolIds(List.of(3L))).hasSize(1);
            assertThat(service.getChaptersBySchoolIds(null)).isEmpty();
            assertThat(service.getChaptersBySchoolIds(List.of())).isEmpty();
        }

        @Test
        @DisplayName("기수-학교 지부 조회는 다른 기수 소속을 건너뛴다")
        void 다른_기수의_학교_소속을_건너뛴다() {
            var targetGisu = 기수(1L, 9L, true);
            var otherGisu = 기수(10L, 10L, false);
            var school = 학교(3L, "테스트대학교");
            var target = 지부_학교(4L, 지부(2L, targetGisu, "서울"), school);
            var other = 지부_학교(5L, 지부(11L, otherGisu, "경기"), school);
            given(loadChapterSchoolPort.findBySchoolId(3L)).willReturn(List.of(other, target));

            assertThat(service.byGisuAndSchool(1L, 3L).id()).isEqualTo(2L);
        }

        @Test
        @DisplayName("기수별 지부·학교 조회는 빈 입력과 학교 없는 지부 및 이중 맵을 처리한다")
        void 기수별_지부와_학교를_조립한다() {
            var gisu = 기수(1L, 9L, true);
            var chapter = 지부(2L, gisu, "서울");
            var emptyChapter = 지부(5L, gisu, "학교없음");
            var school = 학교(3L, "테스트대학교");
            var assignment = 지부_학교(4L, chapter, school);
            given(loadChapterPort.findByGisuId(1L)).willReturn(List.of(chapter, emptyChapter));
            given(loadChapterSchoolPort.findByGisuId(1L)).willReturn(List.of(assignment));
            given(loadChapterPort.findById(2L)).willReturn(chapter);
            given(loadChapterSchoolPort.findByGisuIdsAndSchoolIds(Set.of(1L), Set.of(3L)))
                .willReturn(List.of(assignment));

            var result = service.getChaptersWithSchoolsByGisuId(1L);

            assertThat(result).hasSize(2);
            assertThat(result).filteredOn(info -> info.chapterId().equals(5L)).singleElement()
                .satisfies(info -> assertThat(info.schools()).isEmpty());
            assertThat(service.listByGisuIds(Set.of())).isEmpty();
            assertThat(service.getChaptersWithSchoolsByGisuIds(Set.of())).isEmpty();
            assertThat(service.getChapterById(2L).name()).isEqualTo("서울");
            assertThat(service.getChapterMapByGisuIdsAndSchoolIds(Set.of(), Set.of(3L))).isEmpty();
            assertThat(service.getChapterMapByGisuIdsAndSchoolIds(Set.of(1L), Set.of())).isEmpty();
            assertThat(service.getChapterMapByGisuIdsAndSchoolIds(Set.of(1L), Set.of(3L)))
                .containsKey(1L)
                .satisfies(map -> assertThat(map.get(1L).get(3L).id()).isEqualTo(2L));
        }
    }

    @Nested
    @ExtendWith(MockitoExtension.class)
    @DisplayName("학교 조회")
    class SchoolQuery {

        @Mock
        LoadSchoolPort loadSchoolPort;

        @Mock
        GetFileUseCase getFileUseCase;

        @InjectMocks
        SchoolQueryService service;

        @Test
        @DisplayName("학교 이름·상세·링크·미배정 목록을 조회한다")
        void 학교의_개별_조회_표면을_조립한다() {
            var school = 학교(3L, "테스트대학교");
            school.updateLinks(List.of(SchoolLink.create(
                school, "인스타", SchoolLinkType.INSTAGRAM, "https://instagram.test"
            )));
            var chapterInfo = schoolInfo(3L, "logo");
            var link = new SchoolDetailInfo.SchoolLinkItem(
                "인스타", SchoolLinkType.INSTAGRAM, "https://instagram.test"
            );
            given(loadSchoolPort.findAllNames()).willReturn(List.of(new SchoolNameInfo(3L, "테스트대학교")));
            given(loadSchoolPort.findSchoolDetailByIdWithActiveChapter(3L)).willReturn(chapterInfo);
            given(getFileUseCase.getById("logo")).willReturn(new FileInfo(
                "logo", "logo.png", null, "image/png", 1L, "https://cdn/logo", true, 1L, null
            ));
            given(loadSchoolPort.findLinksBySchoolId(3L)).willReturn(List.of(link));
            given(loadSchoolPort.findById(3L)).willReturn(school);
            given(loadSchoolPort.findUnassignedByGisuId(1L)).willReturn(List.of(school));

            assertThat(service.getAllSchoolNames()).hasSize(1);
            assertThat(service.getSchoolDetail(3L).logoImageUrl()).isEqualTo("https://cdn/logo");
            assertThat(service.getSchoolLink(3L).links()).hasSize(1);
            assertThat(service.getUnassignedSchools(1L)).singleElement()
                .satisfies(info -> assertThat(info.schoolName()).isEqualTo("테스트대학교"));
        }

        @Test
        @DisplayName("학교 목록 조회는 빈 결과와 로고 없는 학교를 처리한다")
        void 학교_목록의_빈값과_로고_없음을_처리한다() {
            given(loadSchoolPort.findSchoolDetailsByIds(Set.of(3L))).willReturn(List.of());
            given(loadSchoolPort.findSchoolDetailsByGisuId(1L)).willReturn(List.of());
            given(loadSchoolPort.findSchoolDetailsByGisuId(2L)).willReturn(List.of(schoolInfo(3L, null)));
            given(loadSchoolPort.findLinksBySchoolIds(List.of(3L))).willReturn(Map.of());
            given(loadSchoolPort.findSchoolDetailsByGisuIds(Set.of(1L))).willReturn(List.of());

            assertThat(service.listDetailsByIds(Set.of(3L))).isEmpty();
            assertThat(service.getSchoolListByGisuId(1L)).isEmpty();
            assertThat(service.getSchoolListByGisuId(2L)).singleElement()
                .satisfies(info -> assertThat(info.logoImageUrl()).isNull());
            assertThat(service.getSchoolListByGisuIds(Set.of(1L))).isEmpty();
        }

        @Test
        @DisplayName("다중 기수 학교 목록은 중복 로고를 한 번만 조회하고 기수별로 그룹화한다")
        void 다중_기수_학교를_그룹화한다() {
            var ids = new LinkedHashSet<>(List.of(1L, 2L));
            given(loadSchoolPort.findSchoolDetailsByGisuIds(ids)).willReturn(List.of(
                gisuSchoolInfo(1L, 3L, "logo"),
                gisuSchoolInfo(2L, 4L, "logo")
            ));
            given(loadSchoolPort.findLinksBySchoolIds(List.of(3L, 4L))).willReturn(Map.of());
            given(getFileUseCase.getFileLinks(List.of("logo"))).willReturn(Map.of("logo", "url"));

            var result = service.getSchoolListByGisuIds(ids);

            assertThat(result).containsOnlyKeys(1L, 2L);
            assertThat(result.get(1L).getFirst().logoImageUrl()).isEqualTo("url");
        }

        private SchoolChapterInfo schoolInfo(Long schoolId, String logoImageId) {
            return new SchoolChapterInfo(
                2L, "서울", "테스트대학교", schoolId, "비고", logoImageId, true,
                Instant.EPOCH, Instant.EPOCH
            );
        }

        private SchoolGisuChapterInfo gisuSchoolInfo(Long gisuId, Long schoolId, String logoImageId) {
            return new SchoolGisuChapterInfo(
                gisuId, 2L, "서울", "테스트대학교", schoolId, "비고", logoImageId, true,
                Instant.EPOCH, Instant.EPOCH
            );
        }
    }

    @Nested
    @ExtendWith(MockitoExtension.class)
    @DisplayName("스터디 그룹 명령")
    class StudyGroupCommand {

        @Mock
        LoadStudyGroupPort loadStudyGroupPort;

        @Mock
        LoadGisuPort loadGisuPort;

        @Mock
        SaveStudyGroupPort saveStudyGroupPort;

        @InjectMocks
        StudyGroupCommandService service;

        @Test
        @DisplayName("스터디 그룹의 생성·수정·멤버·멘토·삭제 수명주기를 수행한다")
        void 스터디_그룹_수명주기를_수행한다() {
            var gisu = 기수(1L, 9L, true);
            var group = 스터디_그룹(2L, 1L, Set.of(20L), Set.of(10L));
            given(loadGisuPort.getById(1L)).willReturn(gisu);
            given(loadStudyGroupPort.findConflictedMemberIds(1L, ChallengerPart.SPRINGBOOT, Set.of(10L), null))
                .willReturn(Set.of());
            given(loadStudyGroupPort.findConflictedMemberIds(1L, ChallengerPart.SPRINGBOOT, Set.of(30L), 2L))
                .willReturn(Set.of());
            given(loadStudyGroupPort.getEntityById(2L)).willReturn(group);

            service.create(new CreateStudyGroupCommand(
                "스프링 스터디", 1L, ChallengerPart.SPRINGBOOT, Set.of(20L), Set.of(10L)
            ));
            service.update(new UpdateStudyGroupCommand(2L, "변경된 이름", ChallengerPart.WEB));
            group.updatePart(ChallengerPart.SPRINGBOOT);
            service.addMember(AddStudyMemberCommand.of(2L, 30L));
            service.addMentor(AddStudyMentorCommand.of(2L, 40L));
            service.deleteMember(DeleteStudyMemberCommand.of(2L, 10L));
            service.deleteMentor(DeleteStudyMentorCommand.of(2L, 20L));
            service.delete(2L);

            assertThat(group.getName()).isEqualTo("변경된 이름");
            assertThat(group.getMembers()).extracting(member -> member.getMemberId()).containsExactly(30L);
            assertThat(group.getMentors()).extracting(mentor -> mentor.getMemberId()).containsExactly(40L);
            then(saveStudyGroupPort).should().delete(group);
        }

        @Test
        @DisplayName("멤버가 없으면 충돌 조회를 생략하고 동일 기수·파트 중복 멤버는 거부한다")
        void 멤버_충돌의_경계값을_처리한다() {
            var gisu = 기수(1L, 9L, true);
            given(loadGisuPort.getById(1L)).willReturn(gisu);
            given(loadStudyGroupPort.findConflictedMemberIds(
                1L, ChallengerPart.SPRINGBOOT, Set.of(10L), null
            )).willReturn(Set.of(10L));

            assertThatThrownBy(() -> service.create(new CreateStudyGroupCommand(
                "빈 스터디", 1L, ChallengerPart.SPRINGBOOT, null, null
            ))).isInstanceOf(OrganizationDomainException.class);

            assertThatThrownBy(() -> service.create(new CreateStudyGroupCommand(
                "중복 스터디", 1L, ChallengerPart.SPRINGBOOT, Set.of(), Set.of(10L)
            ))).isInstanceOf(OrganizationDomainException.class)
                .hasMessageContaining("10");
            then(saveStudyGroupPort).shouldHaveNoInteractions();
        }
    }
}
