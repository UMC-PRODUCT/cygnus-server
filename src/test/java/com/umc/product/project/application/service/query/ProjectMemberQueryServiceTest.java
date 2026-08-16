package com.umc.product.project.application.service.query;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.umc.product.challenger.application.port.in.query.GetChallengerUseCase;
import com.umc.product.common.domain.enums.ChallengerPart;
import com.umc.product.project.application.port.in.query.dto.ProjectMemberInfo;
import com.umc.product.project.application.port.out.LoadProjectMemberPort;
import com.umc.product.project.domain.Project;
import com.umc.product.project.domain.ProjectApplication;
import com.umc.product.project.domain.ProjectMember;
import com.umc.product.project.domain.enums.ProjectMemberStatus;
import com.umc.product.project.domain.enums.ProjectStatus;

@ExtendWith(MockitoExtension.class)
class ProjectMemberQueryServiceTest {

    @Mock
    LoadProjectMemberPort loadProjectMemberPort;

    @Mock
    GetChallengerUseCase getChallengerUseCase;

    @Test
    @DisplayName("여러 프로젝트 멤버를 중복 제거해 조회하고 지원서 ID를 함께 매핑한다")
    void 여러_프로젝트_멤버를_중복_제거해_조회하고_지원서_ID를_함께_매핑한다() {
        ProjectMemberQueryService sut = new ProjectMemberQueryService(
            loadProjectMemberPort,
            getChallengerUseCase
        );
        Project firstProject = project(1L);
        Project secondProject = project(2L);
        ProjectMember memberWithApplication = member(10L, firstProject, 100L, application(1000L));
        ProjectMember memberWithoutApplication = member(20L, secondProject, 200L, null);

        given(loadProjectMemberPort.listByProjectIds(List.of(1L, 2L))).willReturn(Map.of(
            1L, List.of(memberWithApplication),
            2L, List.of(memberWithoutApplication)
        ));

        Map<Long, List<ProjectMemberInfo>> result = sut.listByProjectIds(List.of(1L, 1L, 2L));

        assertThat(result).containsOnlyKeys(1L, 2L);
        assertThat(result.get(1L)).extracting(ProjectMemberInfo::applicationId).containsExactly(1000L);
        assertThat(result.get(2L)).extracting(ProjectMemberInfo::applicationId).containsExactly((Long)null);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<Collection<Long>> captor = ArgumentCaptor.forClass(Collection.class);
        then(loadProjectMemberPort).should().listByProjectIds(captor.capture());
        assertThat(captor.getValue()).containsExactly(1L, 2L);
    }

    private Project project(Long projectId) {
        Project project = Project.createDraft(1L, 1L, 10L, 1L, 10L);
        ReflectionTestUtils.setField(project, "id", projectId);
        ReflectionTestUtils.setField(project, "status", ProjectStatus.IN_PROGRESS);
        return project;
    }

    private ProjectMember member(Long projectMemberId, Project project, Long memberId, ProjectApplication application) {
        ProjectMember member = ProjectMember.create(project, memberId, ChallengerPart.WEB, 999L);
        ReflectionTestUtils.setField(member, "id", projectMemberId);
        ReflectionTestUtils.setField(member, "status", ProjectMemberStatus.ACTIVE);
        ReflectionTestUtils.setField(member, "application", application);
        return member;
    }

    private ProjectApplication application(Long applicationId) {
        try {
            var constructor = ProjectApplication.class.getDeclaredConstructor();
            constructor.setAccessible(true);
            ProjectApplication application = constructor.newInstance();
            ReflectionTestUtils.setField(application, "id", applicationId);
            return application;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
