package com.umc.product.project.adapter.out.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.Import;
import org.springframework.test.util.ReflectionTestUtils;

import com.umc.product.common.domain.enums.ChallengerPart;
import com.umc.product.project.domain.Project;
import com.umc.product.project.domain.ProjectMember;
import com.umc.product.project.domain.enums.ProjectStatus;
import com.umc.product.support.PersistenceAdapterTest;

@PersistenceAdapterTest
@Import({ProjectMemberQueryRepository.class})
class ProjectMemberQueryRepositoryTest {

    @Autowired
    TestEntityManager em;

    @Autowired
    ProjectMemberQueryRepository sut;

    @Test
    @DisplayName("listProjectIdsByActivePlanMember_ACTIVE_PLAN_멤버인_프로젝트_ID만_반환")
    void listProjectIdsByActivePlanMemberReturnsOnlyActivePlanProjects() {
        // given
        Long memberId = 10L;
        Project planProject = persistProject("플랜 프로젝트", 100L);
        Project webProject = persistProject("웹 프로젝트", 101L);
        Project dismissedProject = persistProject("종료 프로젝트", 102L);
        Project otherMemberProject = persistProject("타인 프로젝트", 103L);

        em.persist(ProjectMember.create(planProject, memberId, ChallengerPart.PLAN, 999L));
        em.persist(ProjectMember.create(webProject, memberId, ChallengerPart.WEB, 999L));
        ProjectMember dismissed = ProjectMember.create(dismissedProject, memberId, ChallengerPart.PLAN, 999L);
        dismissed.dismiss("권한 제거", 999L);
        em.persist(dismissed);
        em.persist(ProjectMember.create(otherMemberProject, 11L, ChallengerPart.PLAN, 999L));
        em.flush();
        em.clear();

        // when
        List<Long> result = sut.listProjectIdsByActivePlanMember(
            Set.of(planProject.getId(), webProject.getId(), dismissedProject.getId(), otherMemberProject.getId()),
            memberId
        );

        // then
        assertThat(result).containsExactly(planProject.getId());
    }

    private Project persistProject(String name, Long ownerId) {
        Project project;
        try {
            var constructor = Project.class.getDeclaredConstructor();
            constructor.setAccessible(true);
            project = constructor.newInstance();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        ReflectionTestUtils.setField(project, "gisuId", 1L);
        ReflectionTestUtils.setField(project, "chapterId", 1L);
        ReflectionTestUtils.setField(project, "status", ProjectStatus.IN_PROGRESS);
        ReflectionTestUtils.setField(project, "name", name);
        ReflectionTestUtils.setField(project, "productOwnerMemberId", ownerId);
        ReflectionTestUtils.setField(project, "productOwnerSchoolId", 1L);
        ReflectionTestUtils.setField(project, "creatorMemberId", ownerId);
        em.persist(project);
        return project;
    }
}
