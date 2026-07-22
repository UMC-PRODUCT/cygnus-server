package com.umc.product.project.adapter.in.web.assembler;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;

import java.lang.reflect.RecordComponent;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.util.ReflectionTestUtils;

import com.umc.product.authorization.application.port.in.CheckPermissionUseCase;
import com.umc.product.authorization.domain.ResourcePermission;
import com.umc.product.common.domain.enums.ChallengerPart;
import com.umc.product.global.response.PageResponse;
import com.umc.product.member.application.port.in.query.GetMemberUseCase;
import com.umc.product.member.application.port.in.query.dto.MemberInfo;
import com.umc.product.project.adapter.in.web.dto.common.MemberBrief;
import com.umc.product.project.adapter.in.web.dto.response.DraftProjectResponse;
import com.umc.product.project.adapter.in.web.dto.response.ManagedProjectSummaryResponse;
import com.umc.product.project.adapter.in.web.dto.response.ProjectDetailResponse;
import com.umc.product.project.adapter.in.web.dto.response.ProjectMembersResponse;
import com.umc.product.project.application.port.in.query.GetProjectStatisticsUseCase;
import com.umc.product.project.application.port.in.query.GetProjectUseCase;
import com.umc.product.project.application.port.in.query.SearchManagedProjectUseCase;
import com.umc.product.project.application.port.in.query.SearchProjectUseCase;
import com.umc.product.project.application.port.in.query.dto.ProjectInfo;
import com.umc.product.project.application.port.in.query.dto.SearchManagedProjectQuery;
import com.umc.product.project.application.port.in.query.dto.SearchProjectQuery;
import com.umc.product.project.application.port.in.query.dto.statistics.ChapterProjectMatchingStatisticsInfo;
import com.umc.product.project.application.port.in.query.dto.statistics.ChapterProjectStatisticsInfo;
import com.umc.product.project.application.port.in.query.dto.statistics.ChapterProjectStatisticsSummaryInfo;
import com.umc.product.project.application.port.in.query.dto.statistics.ProjectMatchingCountInfo;
import com.umc.product.project.application.port.in.query.dto.statistics.ProjectMatchingRoundStatisticsInfo;
import com.umc.product.project.application.port.in.query.dto.statistics.ProjectMemberApplicationStatisticsInfo;
import com.umc.product.project.application.port.in.query.dto.statistics.ProjectMemberStatisticsInfo;
import com.umc.product.project.application.port.in.query.dto.statistics.ProjectRoundMemberCountInfo;
import com.umc.product.project.application.port.in.query.dto.statistics.ProjectRoundMemberStatisticsInfo;
import com.umc.product.project.application.port.in.query.dto.statistics.ProjectStatisticsInfo;
import com.umc.product.project.application.port.in.query.dto.statistics.RoundApplicationStatisticsInfo;
import com.umc.product.project.application.port.in.query.dto.statistics.RoundMatchingStatisticsInfo;
import com.umc.product.project.application.port.in.query.dto.statistics.RoundSchoolApplicationStatisticsInfo;
import com.umc.product.project.application.port.in.query.dto.statistics.SchoolApplicationMatchingStatisticsInfo;
import com.umc.product.project.application.port.in.query.dto.statistics.SchoolApplicationStatisticsInfo;
import com.umc.product.project.application.port.in.query.dto.statistics.SchoolMatchingStatisticsInfo;
import com.umc.product.project.application.port.in.query.dto.statistics.UnclassifiedMatchingStatisticsInfo;
import com.umc.product.project.application.port.out.LoadProjectApplicationFormPort;
import com.umc.product.project.application.port.out.LoadProjectApplicationPort;
import com.umc.product.project.application.port.out.LoadProjectMemberPort;
import com.umc.product.project.application.port.out.dto.ProjectMemberMatchedRoundInfo;
import com.umc.product.project.domain.Project;
import com.umc.product.project.domain.ProjectApplicationForm;
import com.umc.product.project.domain.ProjectMember;
import com.umc.product.project.domain.enums.MatchingPhase;
import com.umc.product.project.domain.enums.MatchingType;
import com.umc.product.project.domain.enums.ProjectApplicationStatus;
import com.umc.product.project.domain.enums.ProjectMemberStatus;
import com.umc.product.project.domain.enums.ProjectStatus;

@ExtendWith(MockitoExtension.class)
class ProjectResponseAssemblerTest {

    @Mock
    GetProjectUseCase getProjectUseCase;
    @Mock
    SearchProjectUseCase searchProjectUseCase;
    @Mock
    SearchManagedProjectUseCase searchManagedProjectUseCase;
    @Mock
    GetProjectStatisticsUseCase getProjectStatisticsUseCase;
    @Mock
    GetMemberUseCase getMemberUseCase;
    @Mock
    LoadProjectApplicationFormPort loadProjectApplicationFormPort;
    @Mock
    LoadProjectApplicationPort loadProjectApplicationPort;
    @Mock
    LoadProjectMemberPort loadProjectMemberPort;
    @Mock
    CheckPermissionUseCase checkPermissionUseCase;

    @InjectMocks
    ProjectResponseAssembler sut;

    @Test
    void detailFor_폼이_있으면_applicationFormId가_채워진다() {
        ProjectInfo info = projectInfoWithCoOwner(42L, 101L);
        given(getProjectUseCase.getById(42L)).willReturn(info);
        given(getMemberUseCase.findAllByIds(java.util.Set.of(99L, 101L)))
            .willReturn(Map.of(99L, memberInfo(99L), 101L, memberInfo(101L)));

        Project project = project(42L);
        ProjectApplicationForm form = applicationForm(project, 100L, 500L);
        given(loadProjectApplicationFormPort.findByProjectId(42L)).willReturn(Optional.of(form));

        ProjectDetailResponse response = sut.detailFor(42L);

        assertThat(response.applicationFormId()).isEqualTo(100L);
        assertThat(response.coProductOwners()).hasSize(1);
    }

    @Test
    void detailFor_폼이_없으면_applicationFormId가_null() {
        ProjectInfo info = projectInfo(42L);
        given(getProjectUseCase.getById(42L)).willReturn(info);
        given(getMemberUseCase.findAllByIds(java.util.Set.of(99L)))
            .willReturn(Map.of(99L, memberInfo(99L)));
        given(loadProjectApplicationFormPort.findByProjectId(42L)).willReturn(Optional.empty());

        ProjectDetailResponse response = sut.detailFor(42L);

        assertThat(response.applicationFormId()).isNull();
    }

    @Test
    void draftFor_Draft가_있으면_applicationFormId_hydrate() {
        ProjectInfo info = projectInfoWithCoOwner(42L, 101L);
        given(getProjectUseCase.findDraftByCreatorAndGisu(99L, 1L)).willReturn(Optional.of(info));
        given(getMemberUseCase.findAllByIds(java.util.Set.of(99L, 101L)))
            .willReturn(Map.of(99L, memberInfo(99L), 101L, memberInfo(101L)));

        Project project = project(42L);
        ProjectApplicationForm form = applicationForm(project, 100L, 500L);
        given(loadProjectApplicationFormPort.findByProjectId(42L)).willReturn(Optional.of(form));

        DraftProjectResponse response = sut.draftFor(99L, 1L);

        assertThat(response.applicationFormId()).isEqualTo(100L);
        assertThat(response.coProductOwners()).hasSize(1);
    }

    @Test
    void draftFor_Draft가_없으면_null_반환() {
        given(getProjectUseCase.findDraftByCreatorAndGisu(99L, 1L)).willReturn(Optional.empty());

        DraftProjectResponse response = sut.draftFor(99L, 1L);

        assertThat(response).isNull();
    }

    @Test
    void membersFor_PM과_보조PM_그룹화_및_실명_노출() {
        ProjectInfo info = projectInfo(42L);
        given(getProjectUseCase.getById(42L)).willReturn(info);
        given(loadProjectMemberPort.listByProjectId(42L)).willReturn(List.of(
            projectMember(99L, ChallengerPart.PLAN),
            projectMember(101L, ChallengerPart.PLAN),
            projectMember(102L, ChallengerPart.SPRINGBOOT)
        ));
        given(getMemberUseCase.findAllByIds(Set.of(99L, 101L, 102L))).willReturn(Map.of(
            99L, memberInfoOf(99L, "메인PM", "김메인"),
            101L, memberInfoOf(101L, "코PM가", "박가나"),
            102L, memberInfoOf(102L, "백엔드", "이다라")
        ));

        ProjectMembersResponse response = sut.membersFor(42L);

        assertThat(response.productOwner().memberId()).isEqualTo(99L);
        assertThat(response.productOwner().name()).isEqualTo("김메인");
        assertThat(response.productOwner().matchedRoundInfo()).isNull();
        assertThat(response.coProductOwners()).hasSize(1);
        assertThat(response.coProductOwners().get(0).memberId()).isEqualTo(101L);
        assertThat(response.coProductOwners().get(0).name()).isEqualTo("박가나");
        assertThat(response.partGroups()).hasSize(1);
        assertThat(response.partGroups().get(0).part()).isEqualTo(ChallengerPart.SPRINGBOOT);
    }

    @Test
    void membersFor_보조PM_createdAt_오름차순_정렬() {
        ProjectInfo info = projectInfo(42L);
        given(getProjectUseCase.getById(42L)).willReturn(info);
        given(loadProjectMemberPort.listByProjectId(42L)).willReturn(List.of(
            projectMember(101L, ChallengerPart.PLAN, Instant.parse("2026-01-01T00:00:00Z")),
            projectMember(102L, ChallengerPart.PLAN, Instant.parse("2026-01-03T00:00:00Z")),
            projectMember(103L, ChallengerPart.PLAN, Instant.parse("2026-01-02T00:00:00Z"))
        ));
        given(getMemberUseCase.findAllByIds(Set.of(99L, 101L, 102L, 103L))).willReturn(Map.of(
            99L, memberInfoOf(99L, "메인", "김메인"),
            101L, memberInfoOf(101L, "다람쥐", "박다람"),
            102L, memberInfoOf(102L, "가람", "이가람"),
            103L, memberInfoOf(103L, "나무", "최나무")
        ));

        ProjectMembersResponse response = sut.membersFor(42L);

        List<String> nicknames = response.coProductOwners().stream()
            .map(ProjectMembersResponse.ProjectMemberBrief::nickname)
            .toList();
        assertThat(nicknames).containsExactly("다람쥐", "나무", "가람");
    }

    @Test
    @DisplayName("membersFor는 파트별 멤버를 ProjectMember createdAt 오름차순으로 응답한다")
    void membersFor_파트별_멤버_createdAt_오름차순_정렬() {
        ProjectInfo info = projectInfo(42L);
        given(getProjectUseCase.getById(42L)).willReturn(info);
        given(loadProjectMemberPort.listByProjectId(42L)).willReturn(List.of(
            projectMember(101L, ChallengerPart.SPRINGBOOT, Instant.parse("2026-01-02T00:00:00Z")),
            projectMember(102L, ChallengerPart.SPRINGBOOT, Instant.parse("2026-01-01T00:00:00Z"))
        ));
        given(getMemberUseCase.findAllByIds(Set.of(99L, 101L, 102L))).willReturn(Map.of(
            99L, memberInfoOf(99L, "메인", "김메인"),
            101L, memberInfoOf(101L, "가람", "이가람"),
            102L, memberInfoOf(102L, "다람쥐", "박다람")
        ));

        ProjectMembersResponse response = sut.membersFor(42L);

        List<Long> memberIds = response.partGroups().get(0).members().stream()
            .map(ProjectMembersResponse.ProjectMemberBrief::memberId)
            .toList();
        assertThat(memberIds).containsExactly(102L, 101L);
    }

    @Test
    @DisplayName("membersFor는 APPROVED 지원서의 최신 매칭차수를 프로젝트 멤버 응답에만 포함한다")
    void membersForIncludesLatestApprovedMatchedRoundOnlyInProjectMemberResponse() {
        ProjectInfo info = projectInfo(42L);
        given(getProjectUseCase.getById(42L)).willReturn(info);
        given(loadProjectMemberPort.listByProjectId(42L)).willReturn(List.of(
            projectMember(101L, ChallengerPart.PLAN),
            projectMember(102L, ChallengerPart.SPRINGBOOT)
        ));
        given(getMemberUseCase.findAllByIds(Set.of(99L, 101L, 102L))).willReturn(Map.of(
            99L, memberInfoOf(99L, "메인", "김메인"),
            101L, memberInfoOf(101L, "코PM", "박코피"),
            102L, memberInfoOf(102L, "백엔드", "이다라")
        ));
        given(loadProjectApplicationPort.listLatestApprovedMatchedRoundsByProjectIdsAndMemberIds(any(), any()))
            .willReturn(List.of(new ProjectMemberMatchedRoundInfo(
                42L,
                102L,
                7L,
                MatchingType.PLAN_DEVELOPER,
                MatchingPhase.SECOND
            )));

        ProjectMembersResponse response = sut.membersFor(42L);

        assertThat(response.coProductOwners().get(0).matchedRoundInfo()).isNull();
        ProjectMembersResponse.ProjectMemberBrief springMember = response.partGroups().get(0).members().get(0);
        assertThat(springMember.memberId()).isEqualTo(102L);
        assertThat(springMember.matchedRoundInfo())
            .isEqualTo(new ProjectMembersResponse.MatchedRoundInfo(
                7L,
                MatchingType.PLAN_DEVELOPER,
                MatchingPhase.SECOND
            ));

        List<String> memberBriefFields = Arrays.stream(MemberBrief.class.getRecordComponents())
            .map(RecordComponent::getName)
            .toList();
        assertThat(memberBriefFields).doesNotContain("matchedRoundInfo");
    }

    @Test
    @DisplayName("listProjectMembers는 프로젝트와 멤버 쌍별로 APPROVED 매칭차수를 매핑한다")
    void listProjectMembersMapsApprovedMatchedRoundByProjectAndMember() {
        ProjectInfo project42 = projectInfo(42L);
        ProjectInfo project43 = projectInfoWithOwner(43L, 199L);
        given(checkPermissionUseCase.check(anyLong(), any(ResourcePermission.class))).willReturn(true);
        given(getProjectUseCase.getById(42L)).willReturn(project42);
        given(getProjectUseCase.getById(43L)).willReturn(project43);
        given(loadProjectMemberPort.listByProjectIds(Set.of(42L, 43L))).willReturn(Map.of(
            42L, List.of(projectMember(102L, ChallengerPart.SPRINGBOOT)),
            43L, List.of(projectMember(102L, ChallengerPart.SPRINGBOOT))
        ));
        given(getMemberUseCase.findAllByIds(Set.of(99L, 199L, 102L))).willReturn(Map.of(
            99L, memberInfoOf(99L, "메인42", "김메인"),
            199L, memberInfoOf(199L, "메인43", "이메인"),
            102L, memberInfoOf(102L, "백엔드", "이다라")
        ));
        given(loadProjectApplicationPort.listLatestApprovedMatchedRoundsByProjectIdsAndMemberIds(any(), any()))
            .willReturn(List.of(
                new ProjectMemberMatchedRoundInfo(42L, 102L, 7L, MatchingType.PLAN_DEVELOPER, MatchingPhase.FIRST),
                new ProjectMemberMatchedRoundInfo(43L, 102L, 8L, MatchingType.PLAN_DEVELOPER, MatchingPhase.THIRD)
            ));

        Map<Long, ProjectMembersResponse> responses = sut.listProjectMembers(List.of(42L, 43L), 900L);

        assertThat(responses.get(42L).partGroups().get(0).members().get(0).matchedRoundInfo())
            .isEqualTo(new ProjectMembersResponse.MatchedRoundInfo(
                7L,
                MatchingType.PLAN_DEVELOPER,
                MatchingPhase.FIRST
            ));
        assertThat(responses.get(43L).partGroups().get(0).members().get(0).matchedRoundInfo())
            .isEqualTo(new ProjectMembersResponse.MatchedRoundInfo(
                8L,
                MatchingType.PLAN_DEVELOPER,
                MatchingPhase.THIRD
            ));
    }

    @Test
    void searchManagedFor_status_필드_포함_실명_노출() {
        ProjectInfo info = projectInfoWithStatus(42L, ProjectStatus.PENDING_REVIEW);
        SearchManagedProjectQuery query = SearchManagedProjectQuery.builder()
            .gisuId(1L).pageable(org.springframework.data.domain.PageRequest.of(0, 20)).build();

        given(searchManagedProjectUseCase.searchManaged(query, 99L))
            .willReturn(new org.springframework.data.domain.PageImpl<>(java.util.List.of(info),
                query.pageable(), 1));
        given(getMemberUseCase.findAllByIds(java.util.Set.of(99L)))
            .willReturn(Map.of(99L, memberInfo(99L)));

        PageResponse<ManagedProjectSummaryResponse> response = sut.searchManagedFor(query, 99L);

        assertThat(response.content()).hasSize(1);
        assertThat(response.content().get(0).status()).isEqualTo(ProjectStatus.PENDING_REVIEW);
        assertThat(response.content().get(0).productOwner().name()).isEqualTo("이예원");
    }

    @Test
    @DisplayName("searchFor는 빈 페이지에서 회원 batch 조회를 생략한다")
    void searchFor_빈_페이지() {
        SearchProjectQuery query = SearchProjectQuery.forChallenger(
            1L, null, null, null, null, null, PageRequest.of(0, 20));
        given(searchProjectUseCase.search(query, 99L))
            .willReturn(new PageImpl<>(List.of(), query.pageable(), 0));

        PageResponse<?> response = sut.searchFor(query, 99L);

        assertThat(response.content()).isEmpty();
    }

    @Test
    @DisplayName("searchFor는 조회된 프로젝트의 PM 정보를 조립한다")
    void searchFor_PM_정보_조립() {
        SearchProjectQuery query = SearchProjectQuery.forChallenger(
            1L, null, null, null, null, null, PageRequest.of(0, 20));
        ProjectInfo info = projectInfo(42L);
        given(searchProjectUseCase.search(query, 99L))
            .willReturn(new PageImpl<>(List.of(info), query.pageable(), 1));
        given(getMemberUseCase.findAllByIds(Set.of(99L)))
            .willReturn(Map.of(99L, memberInfo(99L)));

        PageResponse<?> response = sut.searchFor(query, 99L);

        assertThat(response.content()).hasSize(1);
    }

    @Test
    @DisplayName("관리 목록이 비어 있으면 회원 batch 조회 없이 빈 페이지를 반환한다")
    void searchManagedFor_빈_페이지() {
        SearchManagedProjectQuery query = SearchManagedProjectQuery.builder()
            .gisuId(1L).pageable(PageRequest.of(0, 20)).build();
        given(searchManagedProjectUseCase.searchManaged(query, 99L))
            .willReturn(new PageImpl<>(List.of(), query.pageable(), 0));

        assertThat(sut.searchManagedFor(query, 99L).content()).isEmpty();
    }

    @Test
    @DisplayName("일괄 팀원 조회는 권한 거부와 조회 실패 프로젝트를 제외한다")
    void listProjectMembers_권한_거부와_조회_실패() {
        given(checkPermissionUseCase.check(eq(900L), any(ResourcePermission.class)))
            .willReturn(false, true);
        given(getProjectUseCase.getById(43L)).willThrow(new IllegalStateException("조회 실패"));

        Map<Long, ProjectMembersResponse> result = sut.listProjectMembers(List.of(42L, 43L), 900L);

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("일괄 팀원 조회는 누락 회원을 제외하고 중복 매칭 행 중 첫 값을 보존한다")
    void listProjectMembers_누락_회원과_중복_매칭_행() {
        ProjectInfo info = projectInfo(42L);
        ProjectMember missing = projectMember(101L, ChallengerPart.PLAN);
        ProjectMember included = projectMember(102L, ChallengerPart.SPRINGBOOT);
        given(checkPermissionUseCase.check(anyLong(), any(ResourcePermission.class))).willReturn(true);
        given(getProjectUseCase.getById(42L)).willReturn(info);
        given(loadProjectMemberPort.listByProjectIds(Set.of(42L)))
            .willReturn(Map.of(42L, List.of(missing, included)));
        given(getMemberUseCase.findAllByIds(Set.of(99L, 101L, 102L))).willReturn(Map.of(
            99L, memberInfo(99L),
            102L, memberInfoOf(102L, "백엔드", "이다라")
        ));
        given(loadProjectApplicationPort.listLatestApprovedMatchedRoundsByProjectIdsAndMemberIds(any(), any()))
            .willReturn(List.of(
                new ProjectMemberMatchedRoundInfo(42L, 102L, 7L, MatchingType.PLAN_DEVELOPER, MatchingPhase.FIRST),
                new ProjectMemberMatchedRoundInfo(42L, 102L, 8L, MatchingType.PLAN_DEVELOPER, MatchingPhase.SECOND)
            ));

        ProjectMembersResponse response = sut.listProjectMembers(List.of(42L), 900L).get(42L);

        assertThat(response.coProductOwners()).isEmpty();
        assertThat(response.partGroups().get(0).members().get(0).matchedRoundInfo().id())
            .isEqualTo(7L);
    }

    @Test
    @DisplayName("통계 응답 조립은 모든 중첩 통계 값을 보존한다")
    void statistics_응답_조립() {
        ProjectMatchingRoundStatisticsInfo round = new ProjectMatchingRoundStatisticsInfo(
            7L, MatchingType.PLAN_DEVELOPER, MatchingPhase.FIRST);
        ProjectStatisticsInfo project = new ProjectStatisticsInfo(
            42L,
            List.of(new ProjectMemberStatisticsInfo(1L, 99L, ChallengerPart.PLAN,
                ProjectMemberStatus.ACTIVE,
                List.of(new ProjectMemberApplicationStatisticsInfo(
                    10L, ProjectApplicationStatus.APPROVED, round)))),
            List.of(new RoundApplicationStatisticsInfo(round, 2, 3)),
            List.of(new RoundSchoolApplicationStatisticsInfo(
                round, List.of(new SchoolApplicationStatisticsInfo(5L, 2))))
        );
        ChapterProjectStatisticsInfo chapter = new ChapterProjectStatisticsInfo(
            3L,
            List.of(project),
            new ChapterProjectStatisticsSummaryInfo(
                project.roundApplicationStatistics(),
                project.schoolApplicationStatistics(),
                List.of(new SchoolApplicationMatchingStatisticsInfo(5L, 1, 4, 2)),
                List.of(new ProjectRoundMemberStatisticsInfo(
                    42L, List.of(new ProjectRoundMemberCountInfo(round, 2, 1))))
            )
        );
        ChapterProjectMatchingStatisticsInfo matching = new ChapterProjectMatchingStatisticsInfo(
            3L,
            List.of(new RoundMatchingStatisticsInfo(
                round, 1, 4, List.of(new ProjectMatchingCountInfo(42L, 1)))),
            List.of(new SchoolMatchingStatisticsInfo(5L, 1, 4)),
            new UnclassifiedMatchingStatisticsInfo(1, List.of(new ProjectMatchingCountInfo(42L, 1)))
        );
        given(getProjectStatisticsUseCase.getByProjectId(42L, 99L)).willReturn(project);
        given(getProjectStatisticsUseCase.getByChapterId(3L, 99L)).willReturn(chapter);
        given(getProjectStatisticsUseCase.getByProjectIds(List.of(42L), 99L)).willReturn(chapter);
        given(getProjectStatisticsUseCase.getPublicMatchingStatisticsByChapterId(3L)).willReturn(matching);

        assertThat(sut.statisticsForProject(42L, 99L).projectMembers()).hasSize(1);
        assertThat(sut.statisticsForChapter(3L, 99L).summary().projectRoundStatistics()).hasSize(1);
        assertThat(sut.statisticsForProjects(List.of(42L), 99L).projects()).hasSize(1);
        assertThat(sut.matchingStatisticsForChapter(3L).roundMatchingStatistics()).hasSize(1);
    }

    private ProjectInfo projectInfoWithStatus(Long projectId, ProjectStatus status) {
        return ProjectInfo.builder()
            .id(projectId)
            .status(status)
            .name("Triple")
            .description(null)
            .gisuId(1L)
            .chapterId(1L)
            .productOwnerMemberId(99L)
            .coProductOwnerMemberIds(List.of())
            .partQuotas(List.of())
            .build();
    }

    private ProjectInfo projectInfoWithOwner(Long projectId, Long ownerMemberId) {
        return ProjectInfo.builder()
            .id(projectId)
            .status(ProjectStatus.DRAFT)
            .name("Triple")
            .description(null)
            .gisuId(1L)
            .chapterId(1L)
            .productOwnerMemberId(ownerMemberId)
            .coProductOwnerMemberIds(List.of())
            .partQuotas(List.of())
            .build();
    }

    private ProjectInfo projectInfoWithCoOwner(Long projectId, Long coOwnerMemberId) {
        return ProjectInfo.builder()
            .id(projectId)
            .status(ProjectStatus.DRAFT)
            .name("Triple")
            .gisuId(1L)
            .chapterId(1L)
            .productOwnerMemberId(99L)
            .coProductOwnerMemberIds(List.of(coOwnerMemberId))
            .partQuotas(List.of())
            .build();
    }

    private MemberInfo memberInfoOf(Long memberId, String nickname, String name) {
        return MemberInfo.builder()
            .id(memberId)
            .nickname(nickname)
            .name(name)
            .schoolName("학교")
            .build();
    }

    private ProjectMember projectMember(Long memberId, ChallengerPart part) {
        return projectMember(memberId, part, null);
    }

    private ProjectMember projectMember(Long memberId, ChallengerPart part, Instant createdAt) {
        try {
            var c = ProjectMember.class.getDeclaredConstructor();
            c.setAccessible(true);
            ProjectMember pm = c.newInstance();
            ReflectionTestUtils.setField(pm, "memberId", memberId);
            ReflectionTestUtils.setField(pm, "part", part);
            ReflectionTestUtils.setField(pm, "createdAt", createdAt);
            return pm;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private ProjectInfo projectInfo(Long projectId) {
        return ProjectInfo.builder()
            .id(projectId)
            .status(ProjectStatus.DRAFT)
            .name("Triple")
            .description(null)
            .gisuId(1L)
            .chapterId(1L)
            .productOwnerMemberId(99L)
            .coProductOwnerMemberIds(List.of())
            .partQuotas(List.of())
            .build();
    }

    private MemberInfo memberInfo(Long memberId) {
        return MemberInfo.builder()
            .id(memberId)
            .nickname("이방토")
            .name("이예원")
            .schoolName("한양대 ERICA")
            .build();
    }

    private Project project(Long id) {
        Project p;
        try {
            var c = Project.class.getDeclaredConstructor();
            c.setAccessible(true);
            p = c.newInstance();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        ReflectionTestUtils.setField(p, "id", id);
        return p;
    }

    private ProjectApplicationForm applicationForm(Project project, Long id, Long formId) {
        ProjectApplicationForm form = ProjectApplicationForm.create(project, formId);
        ReflectionTestUtils.setField(form, "id", id);
        return form;
    }
}
