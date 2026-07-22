package com.umc.product.project.application.service.policy;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verifyNoInteractions;

import java.util.List;
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
import com.umc.product.project.application.port.out.LoadProjectMemberPort;
import com.umc.product.project.domain.Project;

@ExtendWith(MockitoExtension.class)
class ProjectStatisticsAccessPolicyTest {

    private static final Long MEMBER_ID = 10L;
    private static final Long PROJECT_ID = 100L;
    private static final Long CHAPTER_ID = 7L;
    private static final Long SCHOOL_ID = 3L;

    @Mock
    LoadProjectMemberPort loadProjectMemberPort;
    @Mock
    GetChallengerRoleUseCase getChallengerRoleUseCase;
    @Mock
    GetChapterUseCase getChapterUseCase;

    @InjectMocks
    ProjectStatisticsAccessPolicy sut;

    @Test
    @DisplayName("프로젝트 PO는 통계를 조회할 수 있다")
    void 프로젝트_PO는_통계를_조회할_수_있다() {
        Project project = project(MEMBER_ID);

        boolean result = sut.canReadProjectStatistics(MEMBER_ID, project);

        assertThat(result).isTrue();
        verifyNoInteractions(loadProjectMemberPort, getChallengerRoleUseCase, getChapterUseCase);
    }

    @Test
    @DisplayName("보조 PM은 프로젝트 통계를 조회할 수 있다")
    void 보조_PM은_프로젝트_통계를_조회할_수_있다() {
        Project project = project(999L);
        given(loadProjectMemberPort.isActivePlanMember(PROJECT_ID, MEMBER_ID)).willReturn(true);

        boolean result = sut.canReadProjectStatistics(MEMBER_ID, project);

        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("PO와 보조 PM이 아니면 지부 통계 권한으로 fallback한다")
    void 프로젝트_통계는_지부_권한으로_fallback한다() {
        Project project = project(999L);
        given(loadProjectMemberPort.isActivePlanMember(PROJECT_ID, MEMBER_ID)).willReturn(false);
        given(getChallengerRoleUseCase.isSuperAdmin(MEMBER_ID)).willReturn(true);

        assertThat(sut.canReadProjectStatistics(MEMBER_ID, project)).isTrue();
    }

    @Test
    @DisplayName("SUPER_ADMIN은 지부 통계를 전역 조회할 수 있다")
    void SUPER_ADMIN은_지부_통계를_조회할_수_있다() {
        given(getChallengerRoleUseCase.isSuperAdmin(MEMBER_ID)).willReturn(true);

        assertThat(sut.canReadChapterStatistics(MEMBER_ID, CHAPTER_ID)).isTrue();
        verifyNoInteractions(getChapterUseCase);
    }

    @Test
    @DisplayName("중앙 운영진은 지부 통계를 조회할 수 있다")
    void 중앙_운영진은_지부_통계를_조회할_수_있다() {
        given(getChallengerRoleUseCase.findAllByMemberId(MEMBER_ID))
            .willReturn(List.of(role(ChallengerRoleType.CENTRAL_PRESIDENT, OrganizationType.CENTRAL, null)));

        boolean result = sut.canReadChapterStatistics(MEMBER_ID, CHAPTER_ID);

        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("해당 지부장은 지부 통계를 조회할 수 있다")
    void 해당_지부장은_지부_통계를_조회할_수_있다() {
        given(getChallengerRoleUseCase.findAllByMemberId(MEMBER_ID))
            .willReturn(List.of(role(ChallengerRoleType.CHAPTER_PRESIDENT, OrganizationType.CHAPTER, CHAPTER_ID)));

        boolean result = sut.canReadChapterStatistics(MEMBER_ID, CHAPTER_ID);

        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("해당 지부 소속 학교 회장단은 지부 통계를 조회할 수 있다")
    void 해당_지부_소속_학교_회장단은_지부_통계를_조회할_수_있다() {
        given(getChallengerRoleUseCase.findAllByMemberId(MEMBER_ID))
            .willReturn(List.of(role(ChallengerRoleType.SCHOOL_PRESIDENT, OrganizationType.SCHOOL, SCHOOL_ID)));
        given(getChapterUseCase.getChaptersBySchoolIds(Set.of(SCHOOL_ID)))
            .willReturn(List.of(new ChapterInfo(CHAPTER_ID, "테스트 지부")));

        boolean result = sut.canReadChapterStatistics(MEMBER_ID, CHAPTER_ID);

        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("역할이 없으면 지부 통계를 조회할 수 없다")
    void 역할이_없으면_지부_통계를_조회할_수_없다() {
        given(getChallengerRoleUseCase.findAllByMemberId(MEMBER_ID)).willReturn(List.of());

        boolean result = sut.canReadChapterStatistics(MEMBER_ID, CHAPTER_ID);

        assertThat(result).isFalse();
    }

    private static Project project(Long ownerMemberId) {
        Project project = Project.createDraft(1L, CHAPTER_ID, ownerMemberId, 1L, ownerMemberId);
        ReflectionTestUtils.setField(project, "id", PROJECT_ID);
        return project;
    }

    private static ChallengerRoleInfo role(
        ChallengerRoleType roleType,
        OrganizationType organizationType,
        Long organizationId
    ) {
        return ChallengerRoleInfo.builder()
            .roleType(roleType)
            .organizationType(organizationType)
            .organizationId(organizationId)
            .gisuId(1L)
            .build();
    }
}
