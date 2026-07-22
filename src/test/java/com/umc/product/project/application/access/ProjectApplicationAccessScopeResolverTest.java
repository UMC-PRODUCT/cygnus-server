package com.umc.product.project.application.access;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verifyNoInteractions;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.umc.product.authorization.application.port.in.query.GetChallengerRoleUseCase;
import com.umc.product.authorization.application.port.in.query.dto.ChallengerRoleInfo;
import com.umc.product.common.domain.enums.ChallengerRoleType;
import com.umc.product.common.domain.enums.OrganizationType;
import com.umc.product.organization.application.port.in.query.GetChapterUseCase;
import com.umc.product.organization.application.port.in.query.dto.chapter.ChapterInfo;
import com.umc.product.project.application.access.ProjectApplicationAccessScope.None;
import com.umc.product.project.application.access.ProjectApplicationAccessScope.ProjectScoped;
import com.umc.product.project.application.port.out.LoadProjectMemberPort;
import com.umc.product.project.domain.Project;

@ExtendWith(MockitoExtension.class)
class ProjectApplicationAccessScopeResolverTest {

    private static final Long PROJECT_ID = 100L;
    private static final Long MEMBER_ID = 10L;
    private static final Long OWNER_ID = 99L;
    private static final Long GISU_ID = 1L;
    private static final Long CHAPTER_ID = 5L;
    private static final Long SCHOOL_ID = 7L;
    private static final Long OTHER_GISU_ID = 999L;
    private static final Long OTHER_CHAPTER_ID = 888L;
    private static final Long OTHER_SCHOOL_ID = 777L;

    @Mock
    GetChallengerRoleUseCase getChallengerRoleUseCase;
    @Mock
    LoadProjectMemberPort loadProjectMemberPort;
    @Mock
    GetChapterUseCase getChapterUseCase;

    @InjectMocks
    ProjectApplicationAccessScopeResolver sut;

    @Test
    @DisplayName("본인 지원 내역 scope는 호출자 owner로 고정한다")
    void applicant_scope는_본인으로_제한한다() {
        assertThat(sut.resolveForApplicant(MEMBER_ID))
            .isEqualTo(new ProjectApplicationAccessScope.OwnerOnly(MEMBER_ID));
    }

    // --- PO / Sub-PM (프로젝트 단위 권한) ---

    private static Project project(Long ownerMemberId, Long gisuId, Long chapterId, Long schoolId) {
        return project(PROJECT_ID, ownerMemberId, gisuId, chapterId, schoolId);
    }

    private static Project project(Long projectId, Long ownerMemberId, Long gisuId, Long chapterId, Long schoolId) {
        Project p = newInstance(Project.class);
        ReflectionTestUtils.setField(p, "id", projectId);
        ReflectionTestUtils.setField(p, "productOwnerMemberId", ownerMemberId);
        ReflectionTestUtils.setField(p, "gisuId", gisuId);
        ReflectionTestUtils.setField(p, "chapterId", chapterId);
        ReflectionTestUtils.setField(p, "productOwnerSchoolId", schoolId);
        return p;
    }

    private static <T> T newInstance(Class<T> clazz) {
        try {
            var constructor = clazz.getDeclaredConstructor();
            constructor.setAccessible(true);
            return constructor.newInstance();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    // --- SUPER_ADMIN (기수 무관) ---

    private static ChallengerRoleInfo roleInfo(
        ChallengerRoleType type, OrganizationType orgType, Long orgId, Long gisuId
    ) {
        return ChallengerRoleInfo.builder()
            .id(1L)
            .challengerId(1L)
            .roleType(type)
            .organizationType(orgType)
            .organizationId(orgId)
            .responsiblePart(null)
            .gisuId(gisuId)
            .build();
    }

    // --- Central Core (총괄 / 부총괄) ---

    @Test
    @DisplayName("projectApplicantList_PO_본인이면_ProjectScoped")
    void projectApplicantList_PO_통과() {
        // PO == 호출자 -- short-circuit 으로 isActivePlanMember 미호출
        Project project = project(MEMBER_ID, GISU_ID, CHAPTER_ID, SCHOOL_ID);

        ProjectApplicationAccessScope scope =
            sut.resolveForProjectApplicantList(MEMBER_ID, project);

        assertThat(scope).isInstanceOf(ProjectScoped.class);
        assertThat(((ProjectScoped) scope).projectId()).isEqualTo(PROJECT_ID);
        assertThat(((ProjectScoped) scope).includeOngoingMatchingRounds()).isFalse();
    }

    @Test
    @DisplayName("projectApplicantList_Sub_PM_이면_ProjectScoped")
    void projectApplicantList_SubPM_통과() {
        Project project = project(OWNER_ID, GISU_ID, CHAPTER_ID, SCHOOL_ID);
        given(loadProjectMemberPort.isActivePlanMember(PROJECT_ID, MEMBER_ID)).willReturn(true);

        ProjectApplicationAccessScope scope =
            sut.resolveForProjectApplicantList(MEMBER_ID, project);

        assertThat(scope).isInstanceOf(ProjectScoped.class);
    }

    @Test
    @DisplayName("projectApplicantList_SUPER_ADMIN_이면_기수_무관_ProjectScoped")
    void projectApplicantList_SUPER_ADMIN_통과() {
        Project project = project(OWNER_ID, GISU_ID, CHAPTER_ID, SCHOOL_ID);
        given(loadProjectMemberPort.isActivePlanMember(PROJECT_ID, MEMBER_ID)).willReturn(false);
        given(getChallengerRoleUseCase.isSuperAdmin(MEMBER_ID)).willReturn(true);

        ProjectApplicationAccessScope scope =
            sut.resolveForProjectApplicantList(MEMBER_ID, project);

        assertThat(scope).isInstanceOf(ProjectScoped.class);
        assertThat(((ProjectScoped) scope).includeOngoingMatchingRounds()).isTrue();
    }

    @Test
    @DisplayName("projectApplicantList_같은_기수_총괄이면_ProjectScoped")
    void projectApplicantList_총괄_통과() {
        Project project = project(OWNER_ID, GISU_ID, CHAPTER_ID, SCHOOL_ID);
        given(loadProjectMemberPort.isActivePlanMember(PROJECT_ID, MEMBER_ID)).willReturn(false);
        given(getChallengerRoleUseCase.findAllByMemberId(MEMBER_ID)).willReturn(List.of(
            roleInfo(ChallengerRoleType.CENTRAL_PRESIDENT, OrganizationType.CENTRAL, null, GISU_ID)
        ));

        ProjectApplicationAccessScope scope =
            sut.resolveForProjectApplicantList(MEMBER_ID, project);

        assertThat(scope).isInstanceOf(ProjectScoped.class);
        assertThat(((ProjectScoped) scope).includeOngoingMatchingRounds()).isTrue();
    }

    // --- 지부장 ---

    @Test
    @DisplayName("projectApplicantList_같은_기수_부총괄도_ProjectScoped")
    void projectApplicantList_부총괄_통과() {
        Project project = project(OWNER_ID, GISU_ID, CHAPTER_ID, SCHOOL_ID);
        given(loadProjectMemberPort.isActivePlanMember(PROJECT_ID, MEMBER_ID)).willReturn(false);
        given(getChallengerRoleUseCase.findAllByMemberId(MEMBER_ID)).willReturn(List.of(
            roleInfo(ChallengerRoleType.CENTRAL_VICE_PRESIDENT, OrganizationType.CENTRAL, null, GISU_ID)
        ));

        ProjectApplicationAccessScope scope =
            sut.resolveForProjectApplicantList(MEMBER_ID, project);

        assertThat(scope).isInstanceOf(ProjectScoped.class);
        assertThat(((ProjectScoped) scope).includeOngoingMatchingRounds()).isTrue();
    }

    @Test
    @DisplayName("projectApplicantList_타_기수_총괄이면_None")
    void projectApplicantList_타기수_총괄_거부() {
        Project project = project(OWNER_ID, GISU_ID, CHAPTER_ID, SCHOOL_ID);
        given(loadProjectMemberPort.isActivePlanMember(PROJECT_ID, MEMBER_ID)).willReturn(false);
        given(getChallengerRoleUseCase.findAllByMemberId(MEMBER_ID)).willReturn(List.of(
            roleInfo(ChallengerRoleType.CENTRAL_PRESIDENT, OrganizationType.CENTRAL, null, OTHER_GISU_ID)
        ));

        ProjectApplicationAccessScope scope =
            sut.resolveForProjectApplicantList(MEMBER_ID, project);

        assertThat(scope).isInstanceOf(None.class);
    }

    @Test
    @DisplayName("projectApplicantList_중앙_운영국원은_권한_없음_None")
    void projectApplicantList_운영국원_거부() {
        // CENTRAL_OPERATING_TEAM_MEMBER 는 isAtLeastCentralCore() 미포함 -> 거부
        Project project = project(OWNER_ID, GISU_ID, CHAPTER_ID, SCHOOL_ID);
        given(loadProjectMemberPort.isActivePlanMember(PROJECT_ID, MEMBER_ID)).willReturn(false);
        given(getChallengerRoleUseCase.findAllByMemberId(MEMBER_ID)).willReturn(List.of(
            roleInfo(ChallengerRoleType.CENTRAL_OPERATING_TEAM_MEMBER, OrganizationType.CENTRAL, null, GISU_ID)
        ));

        ProjectApplicationAccessScope scope =
            sut.resolveForProjectApplicantList(MEMBER_ID, project);

        assertThat(scope).isInstanceOf(None.class);
    }

    // --- 학교 회장단 (SCHOOL_PRESIDENT / SCHOOL_VICE_PRESIDENT) ---

    @Test
    @DisplayName("projectApplicantList_같은_기수_지부의_지부장이면_ProjectScoped")
    void projectApplicantList_지부장_통과() {
        Project project = project(OWNER_ID, GISU_ID, CHAPTER_ID, SCHOOL_ID);
        given(loadProjectMemberPort.isActivePlanMember(PROJECT_ID, MEMBER_ID)).willReturn(false);
        given(getChallengerRoleUseCase.findAllByMemberId(MEMBER_ID)).willReturn(List.of(
            roleInfo(ChallengerRoleType.CHAPTER_PRESIDENT, OrganizationType.CHAPTER, CHAPTER_ID, GISU_ID)
        ));

        ProjectApplicationAccessScope scope =
            sut.resolveForProjectApplicantList(MEMBER_ID, project);

        assertThat(scope).isInstanceOf(ProjectScoped.class);
    }

    @Test
    @DisplayName("projectApplicantList_타_지부_지부장이면_None")
    void projectApplicantList_타지부_지부장_거부() {
        Project project = project(OWNER_ID, GISU_ID, CHAPTER_ID, SCHOOL_ID);
        given(loadProjectMemberPort.isActivePlanMember(PROJECT_ID, MEMBER_ID)).willReturn(false);
        given(getChallengerRoleUseCase.findAllByMemberId(MEMBER_ID)).willReturn(List.of(
            roleInfo(ChallengerRoleType.CHAPTER_PRESIDENT, OrganizationType.CHAPTER, OTHER_CHAPTER_ID, GISU_ID)
        ));

        ProjectApplicationAccessScope scope =
            sut.resolveForProjectApplicantList(MEMBER_ID, project);

        assertThat(scope).isInstanceOf(None.class);
    }

    @Test
    @DisplayName("projectApplicantList_타_기수_지부장이면_None")
    void projectApplicantList_타기수_지부장_거부() {
        Project project = project(OWNER_ID, GISU_ID, CHAPTER_ID, SCHOOL_ID);
        given(loadProjectMemberPort.isActivePlanMember(PROJECT_ID, MEMBER_ID)).willReturn(false);
        given(getChallengerRoleUseCase.findAllByMemberId(MEMBER_ID)).willReturn(List.of(
            roleInfo(ChallengerRoleType.CHAPTER_PRESIDENT, OrganizationType.CHAPTER, CHAPTER_ID, OTHER_GISU_ID)
        ));

        ProjectApplicationAccessScope scope =
            sut.resolveForProjectApplicantList(MEMBER_ID, project);

        assertThat(scope).isInstanceOf(None.class);
    }

    @Test
    @DisplayName("projectApplicantList_같은_기수_같은_지부_학교_회장이면_ProjectScoped")
    void projectApplicantList_같은지부_학교회장_통과() {
        Project project = project(OWNER_ID, GISU_ID, CHAPTER_ID, SCHOOL_ID);
        given(loadProjectMemberPort.isActivePlanMember(PROJECT_ID, MEMBER_ID)).willReturn(false);
        given(getChallengerRoleUseCase.findAllByMemberId(MEMBER_ID)).willReturn(List.of(
            roleInfo(ChallengerRoleType.SCHOOL_PRESIDENT, OrganizationType.SCHOOL, OTHER_SCHOOL_ID, GISU_ID)
        ));
        given(getChapterUseCase.getChapterMapByGisuIdsAndSchoolIds(Set.of(GISU_ID), Set.of(OTHER_SCHOOL_ID)))
            .willReturn(Map.of(GISU_ID, Map.of(OTHER_SCHOOL_ID, new ChapterInfo(CHAPTER_ID, "서울"))));

        ProjectApplicationAccessScope scope =
            sut.resolveForProjectApplicantList(MEMBER_ID, project);

        assertThat(scope).isInstanceOf(ProjectScoped.class);
    }

    // --- 권한 없음 (역할 0건) ---

    @Test
    @DisplayName("projectApplicantList_같은_기수_같은_지부_학교_부회장이면_ProjectScoped")
    void projectApplicantList_같은지부_학교부회장_통과() {
        Project project = project(OWNER_ID, GISU_ID, CHAPTER_ID, SCHOOL_ID);
        given(loadProjectMemberPort.isActivePlanMember(PROJECT_ID, MEMBER_ID)).willReturn(false);
        given(getChallengerRoleUseCase.findAllByMemberId(MEMBER_ID)).willReturn(List.of(
            roleInfo(ChallengerRoleType.SCHOOL_VICE_PRESIDENT, OrganizationType.SCHOOL, SCHOOL_ID, GISU_ID)
        ));
        given(getChapterUseCase.getChapterMapByGisuIdsAndSchoolIds(Set.of(GISU_ID), Set.of(SCHOOL_ID)))
            .willReturn(Map.of(GISU_ID, Map.of(SCHOOL_ID, new ChapterInfo(CHAPTER_ID, "서울"))));

        ProjectApplicationAccessScope scope =
            sut.resolveForProjectApplicantList(MEMBER_ID, project);

        assertThat(scope).isInstanceOf(ProjectScoped.class);
    }

    // --- helpers ---

    @Test
    @DisplayName("projectApplicantList_타_지부_학교_회장이면_None")
    void projectApplicantList_타지부_학교회장_거부() {
        Project project = project(OWNER_ID, GISU_ID, CHAPTER_ID, SCHOOL_ID);
        given(loadProjectMemberPort.isActivePlanMember(PROJECT_ID, MEMBER_ID)).willReturn(false);
        given(getChallengerRoleUseCase.findAllByMemberId(MEMBER_ID)).willReturn(List.of(
            roleInfo(ChallengerRoleType.SCHOOL_PRESIDENT, OrganizationType.SCHOOL, OTHER_SCHOOL_ID, GISU_ID)
        ));
        given(getChapterUseCase.getChapterMapByGisuIdsAndSchoolIds(Set.of(GISU_ID), Set.of(OTHER_SCHOOL_ID)))
            .willReturn(Map.of(GISU_ID, Map.of(OTHER_SCHOOL_ID, new ChapterInfo(OTHER_CHAPTER_ID, "인천"))));

        ProjectApplicationAccessScope scope =
            sut.resolveForProjectApplicantList(MEMBER_ID, project);

        assertThat(scope).isInstanceOf(None.class);
    }

    @Test
    @DisplayName("projectApplicantList_타_지부_학교_부회장이면_None")
    void projectApplicantList_타지부_학교부회장_거부() {
        Project project = project(OWNER_ID, GISU_ID, CHAPTER_ID, SCHOOL_ID);
        given(loadProjectMemberPort.isActivePlanMember(PROJECT_ID, MEMBER_ID)).willReturn(false);
        given(getChallengerRoleUseCase.findAllByMemberId(MEMBER_ID)).willReturn(List.of(
            roleInfo(ChallengerRoleType.SCHOOL_VICE_PRESIDENT, OrganizationType.SCHOOL, OTHER_SCHOOL_ID, GISU_ID)
        ));
        given(getChapterUseCase.getChapterMapByGisuIdsAndSchoolIds(Set.of(GISU_ID), Set.of(OTHER_SCHOOL_ID)))
            .willReturn(Map.of(GISU_ID, Map.of(OTHER_SCHOOL_ID, new ChapterInfo(OTHER_CHAPTER_ID, "인천"))));

        ProjectApplicationAccessScope scope =
            sut.resolveForProjectApplicantList(MEMBER_ID, project);

        assertThat(scope).isInstanceOf(None.class);
    }

    @Test
    @DisplayName("projectApplicantList_타_기수_학교_회장이면_None")
    void projectApplicantList_타기수_학교회장_거부() {
        Project project = project(OWNER_ID, GISU_ID, CHAPTER_ID, SCHOOL_ID);
        given(loadProjectMemberPort.isActivePlanMember(PROJECT_ID, MEMBER_ID)).willReturn(false);
        given(getChallengerRoleUseCase.findAllByMemberId(MEMBER_ID)).willReturn(List.of(
            roleInfo(ChallengerRoleType.SCHOOL_PRESIDENT, OrganizationType.SCHOOL, SCHOOL_ID, OTHER_GISU_ID)
        ));

        ProjectApplicationAccessScope scope =
            sut.resolveForProjectApplicantList(MEMBER_ID, project);

        assertThat(scope).isInstanceOf(None.class);
    }

    @Test
    @DisplayName("projectApplicantList_프로젝트_기수가_null이면_학교_회장단_scope는_None")
    void projectApplicantList_프로젝트_기수_null_학교회장_거부() {
        Project project = project(OWNER_ID, null, CHAPTER_ID, SCHOOL_ID);
        given(loadProjectMemberPort.isActivePlanMember(PROJECT_ID, MEMBER_ID)).willReturn(false);
        given(getChallengerRoleUseCase.findAllByMemberId(MEMBER_ID)).willReturn(List.of(
            roleInfo(ChallengerRoleType.SCHOOL_PRESIDENT, OrganizationType.SCHOOL, SCHOOL_ID, null)
        ));

        ProjectApplicationAccessScope scope =
            sut.resolveForProjectApplicantList(MEMBER_ID, project);

        assertThat(scope).isInstanceOf(None.class);
        verifyNoInteractions(getChapterUseCase);
    }

    @Test
    @DisplayName("projectApplicantList_역할이_없는_일반_챌린저면_None")
    void projectApplicantList_일반_챌린저_거부() {
        Project project = project(OWNER_ID, GISU_ID, CHAPTER_ID, SCHOOL_ID);
        given(loadProjectMemberPort.isActivePlanMember(PROJECT_ID, MEMBER_ID)).willReturn(false);
        given(getChallengerRoleUseCase.findAllByMemberId(MEMBER_ID)).willReturn(List.of());

        ProjectApplicationAccessScope scope =
            sut.resolveForProjectApplicantList(MEMBER_ID, project);

        assertThat(scope).isInstanceOf(None.class);
    }

    @Test
    @DisplayName("projectApplicantLists_batch는_PO_SubPM_중앙총괄_scope를_프로젝트별로_반환")
    void projectApplicantLists_batch_scope_판정() {
        // given
        Project ownerProject = project(100L, MEMBER_ID, GISU_ID, CHAPTER_ID, SCHOOL_ID);
        Project subPmProject = project(101L, OWNER_ID, GISU_ID, CHAPTER_ID, SCHOOL_ID);
        Project centralProject = project(102L, OWNER_ID, GISU_ID, OTHER_CHAPTER_ID, SCHOOL_ID);
        Project deniedProject = project(103L, OWNER_ID, OTHER_GISU_ID, OTHER_CHAPTER_ID, SCHOOL_ID);
        List<Project> projects = List.of(ownerProject, subPmProject, centralProject, deniedProject);

        given(loadProjectMemberPort.listProjectIdsByActivePlanMember(
            Set.of(100L, 101L, 102L, 103L), MEMBER_ID))
            .willReturn(List.of(101L));
        given(getChallengerRoleUseCase.findAllByMemberId(MEMBER_ID)).willReturn(List.of(
            roleInfo(ChallengerRoleType.CENTRAL_PRESIDENT, OrganizationType.CENTRAL, null, GISU_ID)
        ));

        // when
        Map<Long, ProjectApplicationAccessScope> result =
            sut.resolveForProjectApplicantLists(MEMBER_ID, projects);

        // then
        assertThat(result.keySet()).containsExactly(100L, 101L, 102L, 103L);
        assertThat(result.get(100L)).isEqualTo(new ProjectScoped(100L));
        assertThat(result.get(101L)).isEqualTo(new ProjectScoped(101L));
        assertThat(result.get(102L)).isEqualTo(new ProjectScoped(102L, true));
        assertThat(result.get(103L)).isInstanceOf(None.class);
    }

    @Test
    @DisplayName("projectApplicantLists_batch에서_프로젝트_기수가_null이면_학교_회장단_scope는_None")
    void projectApplicantLists_batch_프로젝트_기수_null_학교회장_거부() {
        // given
        Project project = project(PROJECT_ID, OWNER_ID, null, CHAPTER_ID, SCHOOL_ID);
        given(loadProjectMemberPort.listProjectIdsByActivePlanMember(Set.of(PROJECT_ID), MEMBER_ID))
            .willReturn(List.of());
        given(getChallengerRoleUseCase.findAllByMemberId(MEMBER_ID)).willReturn(List.of(
            roleInfo(ChallengerRoleType.SCHOOL_PRESIDENT, OrganizationType.SCHOOL, SCHOOL_ID, null)
        ));

        // when
        Map<Long, ProjectApplicationAccessScope> result =
            sut.resolveForProjectApplicantLists(MEMBER_ID, List.of(project));

        // then
        assertThat(result).containsEntry(PROJECT_ID, new None());
        verifyNoInteractions(getChapterUseCase);
    }

    @Test
    @DisplayName("projectApplicantLists_batch는 null·빈 프로젝트를 조회 없이 단축한다")
    void projectApplicantLists_batch_빈_입력() {
        assertThat(sut.resolveForProjectApplicantLists(MEMBER_ID, null)).isEmpty();
        assertThat(sut.resolveForProjectApplicantLists(MEMBER_ID, List.of())).isEmpty();
        verifyNoInteractions(loadProjectMemberPort, getChallengerRoleUseCase, getChapterUseCase);
    }

    @Test
    @DisplayName("projectApplicantLists_batch SUPER_ADMIN은 진행 중 차수까지 조회한다")
    void projectApplicantLists_batch_SUPER_ADMIN() {
        Project project = project(OWNER_ID, GISU_ID, CHAPTER_ID, SCHOOL_ID);
        given(loadProjectMemberPort.listProjectIdsByActivePlanMember(Set.of(PROJECT_ID), MEMBER_ID))
            .willReturn(List.of());
        given(getChallengerRoleUseCase.isSuperAdmin(MEMBER_ID)).willReturn(true);

        ProjectScoped scope = (ProjectScoped) sut.resolveForProjectApplicantLists(
            MEMBER_ID, List.of(project)).get(PROJECT_ID);

        assertThat(scope.includeOngoingMatchingRounds()).isTrue();
    }

    @Test
    @DisplayName("projectApplicantLists_batch는 같은 지부의 지부장과 학교 회장단만 허용한다")
    void projectApplicantLists_batch_지부장과_학교회장단() {
        Project chapterProject = project(100L, OWNER_ID, GISU_ID, CHAPTER_ID, SCHOOL_ID);
        Project schoolProject = project(101L, OWNER_ID, GISU_ID, 6L, SCHOOL_ID);
        Project otherChapterProject = project(102L, OWNER_ID, GISU_ID, OTHER_CHAPTER_ID, SCHOOL_ID);
        Set<Long> ids = Set.of(100L, 101L, 102L);
        given(loadProjectMemberPort.listProjectIdsByActivePlanMember(ids, MEMBER_ID)).willReturn(List.of());
        given(getChallengerRoleUseCase.findAllByMemberId(MEMBER_ID)).willReturn(List.of(
            roleInfo(ChallengerRoleType.CHAPTER_PRESIDENT, OrganizationType.CHAPTER, CHAPTER_ID, GISU_ID),
            roleInfo(ChallengerRoleType.SCHOOL_PRESIDENT, OrganizationType.SCHOOL, SCHOOL_ID, GISU_ID)
        ));
        given(getChapterUseCase.getChapterMapByGisuIdsAndSchoolIds(Set.of(GISU_ID), Set.of(SCHOOL_ID)))
            .willReturn(Map.of(GISU_ID, Map.of(SCHOOL_ID, new ChapterInfo(6L, "경기"))));

        Map<Long, ProjectApplicationAccessScope> result = sut.resolveForProjectApplicantLists(
            MEMBER_ID, List.of(chapterProject, schoolProject, otherChapterProject));

        assertThat(result.get(100L)).isEqualTo(new ProjectScoped(100L));
        assertThat(result.get(101L)).isEqualTo(new ProjectScoped(101L));
        assertThat(result.get(102L)).isInstanceOf(None.class);
    }

    @Test
    @DisplayName("projectApplicantLists_batch는 학교의 지부 매핑이 누락되면 fail-closed한다")
    void projectApplicantLists_batch_학교_지부_매핑_누락() {
        Project project = project(OWNER_ID, GISU_ID, CHAPTER_ID, SCHOOL_ID);
        given(loadProjectMemberPort.listProjectIdsByActivePlanMember(Set.of(PROJECT_ID), MEMBER_ID))
            .willReturn(List.of());
        given(getChallengerRoleUseCase.findAllByMemberId(MEMBER_ID)).willReturn(List.of(
            roleInfo(ChallengerRoleType.SCHOOL_VICE_PRESIDENT, OrganizationType.SCHOOL, SCHOOL_ID, GISU_ID)
        ));
        given(getChapterUseCase.getChapterMapByGisuIdsAndSchoolIds(Set.of(GISU_ID), Set.of(SCHOOL_ID)))
            .willReturn(Map.of());

        assertThat(sut.resolveForProjectApplicantLists(MEMBER_ID, List.of(project)))
            .containsEntry(PROJECT_ID, new None());
    }

    @Test
    @DisplayName("management scope는 SUPER_ADMIN·총괄·지부장·일반 회원을 구분한다")
    void management_scope_역할_행렬() {
        given(getChallengerRoleUseCase.isSuperAdmin(1L)).willReturn(true);
        given(getChallengerRoleUseCase.findAllByMemberId(2L)).willReturn(List.of(
            roleInfo(ChallengerRoleType.CENTRAL_VICE_PRESIDENT, OrganizationType.CENTRAL, null, GISU_ID)));
        given(getChallengerRoleUseCase.findAllByMemberId(3L)).willReturn(List.of(
            roleInfo(ChallengerRoleType.CHAPTER_PRESIDENT, OrganizationType.CHAPTER, CHAPTER_ID, GISU_ID),
            roleInfo(ChallengerRoleType.CHAPTER_PRESIDENT, OrganizationType.CHAPTER, OTHER_CHAPTER_ID, GISU_ID),
            roleInfo(ChallengerRoleType.CENTRAL_PRESIDENT, OrganizationType.CENTRAL, null, OTHER_GISU_ID)));
        given(getChallengerRoleUseCase.findAllByMemberId(4L)).willReturn(List.of());

        assertThat(sut.resolveForManagement(1L, GISU_ID))
            .isEqualTo(new ProjectApplicationAccessScope.All());
        assertThat(sut.resolveForManagement(2L, GISU_ID))
            .isEqualTo(new ProjectApplicationAccessScope.AllInGisu(GISU_ID));
        assertThat(sut.resolveForManagement(3L, GISU_ID))
            .isEqualTo(new ProjectApplicationAccessScope.ChapterScoped(
                List.of(CHAPTER_ID, OTHER_CHAPTER_ID), GISU_ID));
        assertThat(sut.resolveForManagement(4L, GISU_ID)).isEqualTo(new None());
    }
}
