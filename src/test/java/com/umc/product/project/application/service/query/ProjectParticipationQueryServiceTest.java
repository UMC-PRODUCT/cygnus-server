package com.umc.product.project.application.service.query;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
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

import com.umc.product.project.application.port.out.LoadProjectMemberPort;
import com.umc.product.project.application.port.out.LoadProjectPort;
import com.umc.product.project.domain.Project;
import com.umc.product.project.domain.ProjectMember;

@ExtendWith(MockitoExtension.class)
@DisplayName("프로젝트 참여자 조회 서비스")
class ProjectParticipationQueryServiceTest {

    private static final Long MEMBER_ID = 10L;
    private static final Long OWNER_PROJECT_ID = 20L;
    private static final Long MEMBER_PROJECT_ID = 30L;
    private static final Long OTHER_PROJECT_ID = 40L;

    @Mock
    private LoadProjectPort loadProjectPort;

    @Mock
    private LoadProjectMemberPort loadProjectMemberPort;

    @InjectMocks
    private ProjectParticipationQueryService service;

    @Test
    @DisplayName("메인 PM 또는 활성 팀원인 프로젝트 ID를 모두 반환한다")
    void listParticipatingProjectIds() {
        // given
        Set<Long> projectIds = Set.of(OWNER_PROJECT_ID, MEMBER_PROJECT_ID, OTHER_PROJECT_ID);
        Project ownerProject = project(MEMBER_ID);
        given(ownerProject.getId()).willReturn(OWNER_PROJECT_ID);
        Project memberProject = project(100L);
        Project otherProject = project(200L);
        ProjectMember projectMember = mock(ProjectMember.class);
        given(projectMember.getMemberId()).willReturn(MEMBER_ID);
        given(loadProjectPort.listByIds(projectIds))
            .willReturn(List.of(ownerProject, memberProject, otherProject));
        given(loadProjectMemberPort.listByProjectIds(projectIds))
            .willReturn(Map.of(MEMBER_PROJECT_ID, List.of(projectMember)));

        // when
        Set<Long> result = service.listParticipatingProjectIds(projectIds, MEMBER_ID);

        // then
        assertThat(result).containsExactlyInAnyOrder(OWNER_PROJECT_ID, MEMBER_PROJECT_ID);
    }

    @Test
    @DisplayName("후보 프로젝트가 없으면 영속 계층을 조회하지 않는다")
    void emptyCandidates() {
        // when
        Set<Long> result = service.listParticipatingProjectIds(Set.of(), MEMBER_ID);

        // then
        assertThat(result).isEmpty();
        verifyNoInteractions(loadProjectPort, loadProjectMemberPort);
    }

    private Project project(Long ownerMemberId) {
        Project project = mock(Project.class);
        given(project.getProductOwnerMemberId()).willReturn(ownerMemberId);
        return project;
    }
}
