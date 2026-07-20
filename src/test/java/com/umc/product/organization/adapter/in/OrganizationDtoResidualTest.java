package com.umc.product.organization.adapter.in;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.umc.product.common.domain.enums.ChallengerPart;
import com.umc.product.global.response.PageResponse;
import com.umc.product.organization.adapter.in.graphql.dto.SchoolDetailGraphQlResponse;
import com.umc.product.organization.adapter.in.web.dto.request.AssignSchoolRequest;
import com.umc.product.organization.adapter.in.web.dto.request.CreateStudyGroupRequest;
import com.umc.product.organization.adapter.in.web.dto.request.CreateStudyGroupScheduleRequest;
import com.umc.product.organization.adapter.in.web.dto.request.SchoolListRequest;
import com.umc.product.organization.adapter.in.web.dto.request.UnassignSchoolRequest;
import com.umc.product.organization.adapter.in.web.dto.request.UpdateGisuRequest;
import com.umc.product.organization.adapter.in.web.dto.request.UpdateStudyGroupRequest;
import com.umc.product.organization.adapter.in.web.dto.response.chapter.ChapterWithSchoolsResponse;
import com.umc.product.organization.adapter.in.web.dto.response.gisu.GisuListResponse;
import com.umc.product.organization.adapter.in.web.dto.response.school.SchoolListItemResponse;
import com.umc.product.organization.adapter.in.web.dto.response.school.SchoolPageResponse;
import com.umc.product.organization.adapter.in.web.dto.response.school.SchoolSummaryResponse;
import com.umc.product.organization.adapter.in.web.dto.response.school.UnassignedSchoolListResponse;
import com.umc.product.organization.adapter.in.web.dto.response.studygroup.StudyGroupNameResponse;
import com.umc.product.organization.adapter.in.web.dto.response.studygroup.StudyGroupResponse;
import com.umc.product.organization.adapter.in.web.dto.response.umcproduct.UmcProductChapterMembershipResponse;
import com.umc.product.organization.adapter.in.web.dto.response.umcproduct.UmcProductLeadershipResponse;
import com.umc.product.organization.adapter.in.web.dto.response.umcproduct.UmcProductMemberActivityPeriodResponse;
import com.umc.product.organization.adapter.in.web.dto.response.umcproduct.UmcProductSquadParticipationResponse;
import com.umc.product.organization.application.port.in.command.dto.ReplaceStudyGroupMemberAndMentorCommand;
import com.umc.product.organization.application.port.in.query.dto.chapter.ChapterWithSchoolsInfo;
import com.umc.product.organization.application.port.in.query.dto.gisu.GisuInfo;
import com.umc.product.organization.application.port.in.query.dto.gisu.GisuOrganizationInfo;
import com.umc.product.organization.application.port.in.query.dto.school.SchoolAccessContext;
import com.umc.product.organization.application.port.in.query.dto.school.SchoolDeleteSearchCondition;
import com.umc.product.organization.application.port.in.query.dto.school.SchoolDetailInfo;
import com.umc.product.organization.application.port.in.query.dto.school.SchoolListItemInfo;
import com.umc.product.organization.application.port.in.query.dto.school.SchoolNameInfo;
import com.umc.product.organization.application.port.in.query.dto.school.SchoolSearchCondition;
import com.umc.product.organization.application.port.in.query.dto.school.SchoolSummary;
import com.umc.product.organization.application.port.in.query.dto.school.UnassignedSchoolInfo;
import com.umc.product.organization.application.port.in.query.dto.school.UpdateSchoolInfo;
import com.umc.product.organization.application.port.in.query.dto.studygroup.StudyGroupInfo;
import com.umc.product.organization.application.port.in.query.dto.studygroup.StudyGroupMemberInfo;
import com.umc.product.organization.application.port.in.query.dto.studygroup.StudyGroupNameInfo;
import com.umc.product.organization.application.port.in.query.dto.studygroup.StudyGroupWithMemberAndMentorInfo;
import com.umc.product.organization.application.port.in.query.dto.umcproduct.UmcProductChapterInfo;
import com.umc.product.organization.application.port.in.query.dto.umcproduct.UmcProductChapterMembershipInfo;
import com.umc.product.organization.application.port.in.query.dto.umcproduct.UmcProductLeadershipInfo;
import com.umc.product.organization.application.port.in.query.dto.umcproduct.UmcProductMemberActivityPeriodInfo;
import com.umc.product.organization.application.port.in.query.dto.umcproduct.UmcProductSquadInfo;
import com.umc.product.organization.application.port.in.query.dto.umcproduct.UmcProductSquadParticipationInfo;
import com.umc.product.organization.domain.School;
import com.umc.product.organization.domain.enums.SchoolLinkType;
import com.umc.product.organization.domain.enums.UmcProductLeadershipRole;
import com.umc.product.organization.domain.enums.UmcProductPosition;
import com.umc.product.organization.domain.enums.UmcProductSquadRole;
import com.umc.product.organization.exception.OrganizationDomainException;

@DisplayName("Organization 요청·응답 DTO 잔여 계약")
class OrganizationDtoResidualTest {

    private static final Instant NOW = Instant.parse("2026-07-19T00:00:00Z");
    private static final LocalDate TODAY = LocalDate.of(2026, 7, 19);

    @Test
    @DisplayName("스터디·기수·학교 요청은 경로 식별자와 본문을 명령·검색 조건으로 보존한다")
    void 요청을_명령과_검색_조건으로_변환한다() {
        var create = new CreateStudyGroupRequest(
            "스터디", 1L, ChallengerPart.SPRINGBOOT, Set.of(2L), Set.of(3L)
        ).toCommand();
        var update = new UpdateStudyGroupRequest("변경").toCommand(4L);
        var schedule = new CreateStudyGroupScheduleRequest(4L, 5L, 6L).toCommand();
        var updateGisu = new UpdateGisuRequest(NOW, NOW.plusSeconds(1)).toCommand(1L);
        var assign = new AssignSchoolRequest(2L).toCommand(3L);
        var unassign = new UnassignSchoolRequest(1L).toCommand(3L);
        var condition = new SchoolListRequest("학교", 2L).toCondition();

        assertThat(create.name()).isEqualTo("스터디");
        assertThat(update.groupId()).isEqualTo(4L);
        assertThat(schedule.toEntity().getWeeklyCurriculumId()).isEqualTo(6L);
        assertThat(updateGisu.gisuId()).isEqualTo(1L);
        assertThat(assign.schoolId()).isEqualTo(3L);
        assertThat(unassign.gisuId()).isEqualTo(1L);
        assertThat(condition).isEqualTo(new SchoolSearchCondition("학교", 2L));
    }

    @Test
    @DisplayName("스터디 멤버 교체 명령은 입력을 불변 복사하고 필수 멤버를 검증한다")
    void 멤버_교체_명령의_불변성과_필수값을_검증한다() {
        var command = ReplaceStudyGroupMemberAndMentorCommand.of(1L, Set.of(2L), Set.of(3L));

        assertThat(command.studyMemberIds()).containsExactly(2L);
        assertThat(command.studyMentorIds()).containsExactly(3L);
        assertThatThrownBy(() -> ReplaceStudyGroupMemberAndMentorCommand.of(null, Set.of(2L), Set.of()))
            .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> ReplaceStudyGroupMemberAndMentorCommand.of(1L, null, Set.of()))
            .isInstanceOf(OrganizationDomainException.class);
        assertThatThrownBy(() -> ReplaceStudyGroupMemberAndMentorCommand.of(1L, Set.of(), Set.of()))
            .isInstanceOf(OrganizationDomainException.class);
    }

    @Test
    @DisplayName("스터디 조회 응답은 멘토와 멤버 프로필을 손실 없이 변환한다")
    void 스터디_응답을_변환한다() {
        var member = StudyGroupMemberInfo.create(
            1L, 2L, "회원", 3L, "학교", "profile", "https://profile"
        );
        var info = StudyGroupWithMemberAndMentorInfo.create(
            1L, "스터디", 9L, ChallengerPart.SPRINGBOOT, NOW,
            List.of(member), List.of(member)
        );

        var response = StudyGroupResponse.from(info);
        var names = StudyGroupNameResponse.from(List.of(new StudyGroupNameInfo(1L, "스터디")));

        assertThat(response.mentors()).singleElement().satisfies(converted -> {
            assertThat(converted.memberId()).isEqualTo(2L);
            assertThat(converted.memberName()).isEqualTo("회원");
            assertThat(converted.schoolId()).isEqualTo(3L);
            assertThat(converted.schoolName()).isEqualTo("학교");
            assertThat(converted.profileImageUrl()).isEqualTo("https://profile");
        });
        assertThat(response.members()).hasSize(1);
        assertThat(names.studyGroups()).singleElement().satisfies(name -> assertThat(name.groupId()).isEqualTo(1L));
        assertThat(new StudyGroupMemberInfo(1L, 2L, "회원", 3L, "학교", "profile").profileImageUrl()).isNull();
        assertThat(new StudyGroupInfo(
            1L, "스터디", 9L, ChallengerPart.SPRINGBOOT, NOW, List.of(), List.of()
        ).groupId())
            .isEqualTo(1L);
    }

    @Test
    @DisplayName("지부·학교 목록 응답은 중첩 학교와 페이지 메타데이터를 변환한다")
    void 지부와_학교_응답을_변환한다() {
        var chapterInfo = new ChapterWithSchoolsInfo(
            1L, "서울", List.of(new ChapterWithSchoolsInfo.SchoolInfo(2L, "학교"))
        );
        var chapterResponse = ChapterWithSchoolsResponse.from(List.of(chapterInfo));
        var schoolInfo = new SchoolListItemInfo(2L, "학교", 1L, "서울", NOW, true, "비고", "logo");
        var schoolItem = SchoolListItemResponse.of(schoolInfo);
        var page = SchoolPageResponse.from(new PageResponse<>(
            List.of(schoolItem), 0, 20, 1L, 1, false, false
        ));
        var unassigned = UnassignedSchoolListResponse.from(List.of(new UnassignedSchoolInfo(2L, "학교")));
        var summary = SchoolSummaryResponse.from(new SchoolSummary(2L, "학교"));

        assertThat(chapterResponse.chapters().getFirst().schools().getFirst().schoolName()).isEqualTo("학교");
        assertThat(page.content()).containsExactly(schoolItem);
        assertThat(page.page()).isZero();
        assertThat(page.size()).isEqualTo(20);
        assertThat(page.totalElements()).isEqualTo(1L);
        assertThat(page.totalPages()).isEqualTo(1);
        assertThat(page.hasNext()).isFalse();
        assertThat(page.hasPrevious()).isFalse();
        assertThat(unassigned.schools().getFirst().schoolId()).isEqualTo(2L);
        assertThat(summary.schoolName()).isEqualTo("학교");
        assertThat(new SchoolNameInfo(2L, "학교").schoolName()).isEqualTo("학교");
        assertThat(SchoolNameInfo.from(School.create("변환 학교", null)).schoolName()).isEqualTo("변환 학교");
        assertThat(schoolInfo.withLogoImageUrl("new-logo").logoImageUrl()).isEqualTo("new-logo");
        assertThat(new UpdateSchoolInfo("새 학교", 1L, "서울", "비고", NOW, NOW).newSchoolName())
            .isEqualTo("새 학교");
        assertThat(new SchoolDeleteSearchCondition("학교", 1L).keyword()).isEqualTo("학교");
        assertThat(new SchoolAccessContext(3L, ChallengerPart.SPRINGBOOT).schoolId()).isEqualTo(3L);
    }

    @Test
    @DisplayName("기수 목록과 GraphQL 학교 상세는 null 링크·시간 및 중첩 링크를 안전하게 변환한다")
    void 기수와_GraphQL_응답을_변환한다() {
        var gisu = new GisuInfo(1L, 9L, NOW, NOW.plusSeconds(172_800), true);
        assertThat(GisuListResponse.from(List.of(gisu)).gisuList()).hasSize(1);
        assertThat(gisu.activityDays(NOW.plusSeconds(86_400))).isEqualTo(1L);
        var infoWithoutLinks = new SchoolDetailInfo(
            1L, "서울", "학교", 2L, "비고", "logo", null, true, null, null
        );
        var responseWithoutLinks = SchoolDetailGraphQlResponse.from(infoWithoutLinks);
        var organizationSchool = new GisuOrganizationInfo.SchoolOrganizationInfo(
            1L, "서울", 2L, "학교", "비고", "logo",
            List.of(new GisuOrganizationInfo.SchoolLinkInfo(
                "인스타", SchoolLinkType.INSTAGRAM, "https://instagram"
            )), true, NOW, NOW
        );
        var organizationResponse = SchoolDetailGraphQlResponse.from(organizationSchool);

        assertThat(responseWithoutLinks.links()).isEmpty();
        assertThat(responseWithoutLinks.createdAt()).isNull();
        assertThat(organizationResponse.links()).singleElement().satisfies(link -> {
            assertThat(link.title()).isEqualTo("인스타");
            assertThat(link.type()).isEqualTo(SchoolLinkType.INSTAGRAM);
            assertThat(link.url()).isEqualTo("https://instagram");
        });
        assertThat(organizationResponse.createdAt()).isEqualTo(NOW.toString());
    }

    @Test
    @DisplayName("UMC PRODUCT 소속·리더십·스쿼드 응답은 중첩 조회 모델을 변환한다")
    void UMC_PRODUCT_응답을_변환한다() {
        var chapter = new UmcProductChapterInfo(1L, "SERVER", "Server", "설명", 1, true);
        var squad = new UmcProductSquadInfo(2L, "PLATFORM", "Platform", "설명", TODAY, null, 1, true);
        var membership = new UmcProductChapterMembershipInfo(
            3L, 4L, 1L, chapter, UmcProductPosition.SERVER_DEVELOPER, "서버 개발자",
            "Backend", "API", TODAY, null
        );
        var leadership = new UmcProductLeadershipInfo(
            5L, 4L, UmcProductLeadershipRole.UMC_PRODUCT_LEAD, "리드", TODAY, null
        );
        var participation = new UmcProductSquadParticipationInfo(
            6L, 4L, 2L, squad, UmcProductSquadRole.MEMBER, "멤버",
            UmcProductPosition.SERVER_DEVELOPER, "서버 개발자", "Backend", "API", TODAY, null
        );

        assertThat(UmcProductChapterMembershipResponse.from(membership).chapter().code()).isEqualTo("SERVER");
        assertThat(UmcProductLeadershipResponse.from(leadership).leadershipId()).isEqualTo(5L);
        assertThat(UmcProductSquadParticipationResponse.from(participation).squad().code()).isEqualTo("PLATFORM");
        assertThat(UmcProductMemberActivityPeriodResponse.from(
            new UmcProductMemberActivityPeriodInfo(4L, TODAY, null)
        ).activityPeriodId()).isEqualTo(4L);

        assertThat(UmcProductChapterMembershipResponse.from(new UmcProductChapterMembershipInfo(
            3L, 4L, 1L, null, UmcProductPosition.SERVER_DEVELOPER, "서버 개발자",
            null, null, TODAY, null
        )).chapter()).isNull();
        assertThat(UmcProductSquadParticipationResponse.from(new UmcProductSquadParticipationInfo(
            6L, 4L, 2L, null, UmcProductSquadRole.MEMBER, "멤버",
            UmcProductPosition.SERVER_DEVELOPER, "서버 개발자", null, null, TODAY, null
        )).squad()).isNull();
    }
}
