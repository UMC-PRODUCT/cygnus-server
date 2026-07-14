package com.umc.product.project.adapter.out.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.util.ReflectionTestUtils;

import com.umc.product.common.domain.enums.ChallengerPart;
import com.umc.product.project.application.access.ScopeClause;
import com.umc.product.project.application.port.in.query.dto.SearchProjectQuery;
import com.umc.product.project.domain.Project;
import com.umc.product.project.domain.ProjectMember;
import com.umc.product.project.domain.ProjectPartQuota;
import com.umc.product.project.domain.enums.PartQuotaStatus;
import com.umc.product.project.domain.enums.ProjectMemberStatus;
import com.umc.product.project.domain.enums.ProjectStatus;
import com.umc.product.support.PersistenceAdapterTest;

@PersistenceAdapterTest
@Import({ProjectQueryRepository.class})
class ProjectQueryRepositoryTest {

    @Autowired
    TestEntityManager em;

    @Autowired
    ProjectQueryRepository sut;

    private Long gisuId;
    private Long alphaId;
    private Long betaId;
    private Long gammaId;
    private Long deltaId;

    @BeforeEach
    void setUp() {
        gisuId = 1L;

        alphaId = persistProject("프로젝트 알파", ProjectStatus.IN_PROGRESS, gisuId, 1L, 10L).getId();
        betaId = persistProject("프로젝트 베타", ProjectStatus.IN_PROGRESS, gisuId, 1L, 11L).getId();
        gammaId = persistProject("프로젝트 감마", ProjectStatus.DRAFT, gisuId, 1L, 12L).getId();
        deltaId = persistProject("프로젝트 델타", ProjectStatus.IN_PROGRESS, gisuId, 2L, 13L).getId();
        persistProject("다른기수 프로젝트", ProjectStatus.IN_PROGRESS, 2L, 1L, 14L);

        em.flush();
        em.clear();
    }

    @Test
    void 기수_필터로_조회() {
        SearchProjectQuery query = SearchProjectQuery.forChallenger(
            gisuId, null, null, null, null, null, PageRequest.of(0, 20));

        Page<Project> result = sut.search(query);

        assertThat(result.getContent()).hasSize(3);
        assertThat(result.getContent())
            .allMatch(p -> p.getGisuId().equals(gisuId))
            .allMatch(p -> p.getStatus() == ProjectStatus.IN_PROGRESS);
    }

    @Test
    void 이름_키워드_검색() {
        SearchProjectQuery query = SearchProjectQuery.forChallenger(
            gisuId, "알파", null, null, null, null, PageRequest.of(0, 20));

        Page<Project> result = sut.search(query);

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getName()).isEqualTo("프로젝트 알파");
    }

    @Test
    void 지부_필터() {
        SearchProjectQuery query = SearchProjectQuery.forChallenger(
            gisuId, null, 2L, null, null, null, PageRequest.of(0, 20));

        Page<Project> result = sut.search(query);

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getName()).isEqualTo("프로젝트 델타");
    }

    @Test
    void 상태_필터_Admin용() {
        SearchProjectQuery query = SearchProjectQuery.forAdmin(
            gisuId, null, null, null, null, null,
            List.of(ProjectStatus.DRAFT), PageRequest.of(0, 20));

        Page<Project> result = sut.search(query);

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getStatus()).isEqualTo(ProjectStatus.DRAFT);
    }

    @Test
    void 조합_필터_키워드와_지부() {
        SearchProjectQuery query = SearchProjectQuery.forChallenger(
            gisuId, "프로젝트", 1L, null, null, null, PageRequest.of(0, 20));

        Page<Project> result = sut.search(query);

        assertThat(result.getContent()).hasSize(2);
    }

    @Test
    void 페이지네이션() {
        SearchProjectQuery query = SearchProjectQuery.forChallenger(
            gisuId, null, null, null, null, null, PageRequest.of(0, 2));

        Page<Project> result = sut.search(query);

        assertThat(result.getContent()).hasSize(2);
        assertThat(result.getTotalElements()).isEqualTo(3);
        assertThat(result.getTotalPages()).isEqualTo(2);
        assertThat(result.hasNext()).isTrue();
    }

    @Test
    void unpaged_요청시_offset_limit_없이_전건_반환() {
        // given — Pageable.unpaged() 로 호출 (시딩 등 전체 조회 시나리오)
        SearchProjectQuery query = SearchProjectQuery.forChallenger(
            gisuId, null, null, null, null, null, Pageable.unpaged());

        // when — Unpaged 의 getOffset/getPageSize 호출로 인한 UnsupportedOperationException 이 없어야 한다
        Page<Project> result = sut.search(query);

        // then — 조건에 맞는 전건(기수 1 IN_PROGRESS 3건) 을 페이징 절단 없이 반환
        assertThat(result.getContent()).hasSize(3);
        assertThat(result.getTotalElements()).isEqualTo(3);
        assertThat(result.getContent())
            .allMatch(p -> p.getStatus() == ProjectStatus.IN_PROGRESS);
    }

    @Test
    void 정렬_createdAt_ASC() {
        SearchProjectQuery query = SearchProjectQuery.forChallenger(
            gisuId, null, null, null, null, null, PageRequest.of(0, 20));

        Page<Project> result = sut.search(query);

        List<Project> content = result.getContent();
        for (int i = 0; i < content.size() - 1; i++) {
            assertThat(content.get(i).getCreatedAt())
                .isBeforeOrEqualTo(content.get(i + 1).getCreatedAt());
        }
    }

    @Test
    void 정렬_createdAt이_같으면_이름_오름차순() {
        Long targetGisuId = 77L;
        Project charlie = persistProject("Charlie", ProjectStatus.IN_PROGRESS, targetGisuId, 1L, 301L);
        Project alpha = persistProject("Alpha", ProjectStatus.IN_PROGRESS, targetGisuId, 1L, 302L);
        Project bravo = persistProject("Bravo", ProjectStatus.IN_PROGRESS, targetGisuId, 1L, 303L);
        em.flush();

        Instant sameCreatedAt = Instant.parse("2026-01-01T00:00:00Z");
        updateAuditTimestamp(charlie.getId(), sameCreatedAt);
        updateAuditTimestamp(alpha.getId(), sameCreatedAt);
        updateAuditTimestamp(bravo.getId(), sameCreatedAt);
        em.flush();
        em.clear();

        SearchProjectQuery query = SearchProjectQuery.forChallenger(
            targetGisuId, null, null, null, null, null, PageRequest.of(0, 20));

        Page<Project> result = sut.search(query);

        assertThat(result.getContent())
            .extracting(Project::getName)
            .containsExactly("Alpha", "Bravo", "Charlie");
    }

    @Test
    void 빈_결과() {
        SearchProjectQuery query = SearchProjectQuery.forChallenger(
            999L, null, null, null, null, null, PageRequest.of(0, 20));

        Page<Project> result = sut.search(query);

        assertThat(result.getContent()).isEmpty();
        assertThat(result.getTotalElements()).isZero();
    }

    // ========== parts / partQuotaStatus 상관 필터 ==========

    @Test
    void 파트_단일_선택_해당_파트_있는_프로젝트만() {
        // given — 알파에만 WEB quota
        persistQuota(alphaId, ChallengerPart.WEB, 2);
        em.flush();
        em.clear();

        SearchProjectQuery query = SearchProjectQuery.forChallenger(
            gisuId, null, null, null, List.of(ChallengerPart.WEB), null, PageRequest.of(0, 20));

        // when
        Page<Project> result = sut.search(query);

        // then
        assertThat(result.getContent())
            .extracting(Project::getName)
            .containsExactly("프로젝트 알파");
    }

    @Test
    void 파트_여러개_AND로_모두_포함하는_프로젝트만() {
        // given — 알파: WEB만 / 베타: WEB+SPRINGBOOT / 델타: WEB만
        persistQuota(alphaId, ChallengerPart.WEB, 2);
        persistQuota(betaId, ChallengerPart.WEB, 2);
        persistQuota(betaId, ChallengerPart.SPRINGBOOT, 1);
        persistQuota(deltaId, ChallengerPart.WEB, 2);
        em.flush();
        em.clear();

        SearchProjectQuery query = SearchProjectQuery.forChallenger(
            gisuId, null, null, null,
            List.of(ChallengerPart.WEB, ChallengerPart.SPRINGBOOT), null,
            PageRequest.of(0, 20));

        // when
        Page<Project> result = sut.search(query);

        // then
        assertThat(result.getContent())
            .extracting(Project::getName)
            .containsExactly("프로젝트 베타");
    }

    @Test
    void 파트_RECRUITING_해당_파트가_아직_정원_미달() {
        // given — 알파 WEB 2/0 (recruiting), 베타 WEB 2/2 (full)
        persistQuota(alphaId, ChallengerPart.WEB, 2);
        persistQuota(betaId, ChallengerPart.WEB, 2);
        persistMember(betaId, ChallengerPart.WEB, 200L);
        persistMember(betaId, ChallengerPart.WEB, 201L);
        em.flush();
        em.clear();

        SearchProjectQuery query = SearchProjectQuery.forChallenger(
            gisuId, null, null, null,
            List.of(ChallengerPart.WEB), PartQuotaStatus.RECRUITING,
            PageRequest.of(0, 20));

        // when
        Page<Project> result = sut.search(query);

        // then
        assertThat(result.getContent())
            .extracting(Project::getName)
            .containsExactly("프로젝트 알파");
    }

    @Test
    void 파트_COMPLETED_해당_파트가_정원_달성() {
        // given
        persistQuota(alphaId, ChallengerPart.WEB, 2);
        persistQuota(betaId, ChallengerPart.WEB, 2);
        persistMember(betaId, ChallengerPart.WEB, 200L);
        persistMember(betaId, ChallengerPart.WEB, 201L);
        em.flush();
        em.clear();

        SearchProjectQuery query = SearchProjectQuery.forChallenger(
            gisuId, null, null, null,
            List.of(ChallengerPart.WEB), PartQuotaStatus.COMPLETED,
            PageRequest.of(0, 20));

        // when
        Page<Project> result = sut.search(query);

        // then
        assertThat(result.getContent())
            .extracting(Project::getName)
            .containsExactly("프로젝트 베타");
    }

    @Test
    void 파트_여러개_RECRUITING_선택한_파트_모두_모집중() {
        // given — 베타: WEB+SPRINGBOOT 둘 다 미달 / 델타: WEB만 미달, SPRINGBOOT 정원 달성
        persistQuota(betaId, ChallengerPart.WEB, 2);
        persistQuota(betaId, ChallengerPart.SPRINGBOOT, 1);
        persistQuota(deltaId, ChallengerPart.WEB, 2);
        persistQuota(deltaId, ChallengerPart.SPRINGBOOT, 1);
        persistMember(deltaId, ChallengerPart.SPRINGBOOT, 300L);
        em.flush();
        em.clear();

        SearchProjectQuery query = SearchProjectQuery.forChallenger(
            gisuId, null, null, null,
            List.of(ChallengerPart.WEB, ChallengerPart.SPRINGBOOT),
            PartQuotaStatus.RECRUITING,
            PageRequest.of(0, 20));

        // when
        Page<Project> result = sut.search(query);

        // then
        assertThat(result.getContent())
            .extracting(Project::getName)
            .containsExactly("프로젝트 베타");
    }

    @Test
    void 파트_여러개_COMPLETED_선택한_파트_모두_정원_달성() {
        // given — 베타: WEB+SPRINGBOOT 둘 다 풀 / 델타: WEB 풀, SPRINGBOOT 미달
        persistQuota(betaId, ChallengerPart.WEB, 2);
        persistQuota(betaId, ChallengerPart.SPRINGBOOT, 1);
        persistMember(betaId, ChallengerPart.WEB, 200L);
        persistMember(betaId, ChallengerPart.WEB, 201L);
        persistMember(betaId, ChallengerPart.SPRINGBOOT, 202L);
        persistQuota(deltaId, ChallengerPart.WEB, 2);
        persistQuota(deltaId, ChallengerPart.SPRINGBOOT, 3);
        persistMember(deltaId, ChallengerPart.WEB, 300L);
        persistMember(deltaId, ChallengerPart.WEB, 301L);
        em.flush();
        em.clear();

        SearchProjectQuery query = SearchProjectQuery.forChallenger(
            gisuId, null, null, null,
            List.of(ChallengerPart.WEB, ChallengerPart.SPRINGBOOT),
            PartQuotaStatus.COMPLETED,
            PageRequest.of(0, 20));

        // when
        Page<Project> result = sut.search(query);

        // then
        assertThat(result.getContent())
            .extracting(Project::getName)
            .containsExactly("프로젝트 베타");
    }

    @Test
    void 파트_없이_RECRUITING_아무_파트라도_모집중이면_포함() {
        // given — 알파: 모집중 파트 있음 / 베타: 모든 파트 풀 / 델타: 일부 풀, 일부 모집중
        persistQuota(alphaId, ChallengerPart.WEB, 2);
        persistQuota(betaId, ChallengerPart.WEB, 1);
        persistMember(betaId, ChallengerPart.WEB, 200L);
        persistQuota(deltaId, ChallengerPart.WEB, 1);
        persistQuota(deltaId, ChallengerPart.SPRINGBOOT, 2);
        persistMember(deltaId, ChallengerPart.WEB, 300L);
        em.flush();
        em.clear();

        SearchProjectQuery query = SearchProjectQuery.forChallenger(
            gisuId, null, null, null, null,
            PartQuotaStatus.RECRUITING,
            PageRequest.of(0, 20));

        // when
        Page<Project> result = sut.search(query);

        // then
        assertThat(result.getContent())
            .extracting(Project::getName)
            .containsExactlyInAnyOrder("프로젝트 알파", "프로젝트 델타");
    }

    @Test
    void 파트_없이_COMPLETED_모든_파트가_정원_달성이면_포함() {
        // given — 알파: 모집중 / 베타: 모두 풀 / 델타: 일부 모집중
        persistQuota(alphaId, ChallengerPart.WEB, 2);
        persistQuota(betaId, ChallengerPart.WEB, 1);
        persistMember(betaId, ChallengerPart.WEB, 200L);
        persistQuota(deltaId, ChallengerPart.WEB, 1);
        persistQuota(deltaId, ChallengerPart.SPRINGBOOT, 2);
        persistMember(deltaId, ChallengerPart.WEB, 300L);
        em.flush();
        em.clear();

        SearchProjectQuery query = SearchProjectQuery.forChallenger(
            gisuId, null, null, null, null,
            PartQuotaStatus.COMPLETED,
            PageRequest.of(0, 20));

        // when
        Page<Project> result = sut.search(query);

        // then
        assertThat(result.getContent())
            .extracting(Project::getName)
            .containsExactly("프로젝트 베타");
    }

    @Test
    void COMPLETED_quota가_없는_프로젝트는_제외된다() {
        // given — Admin 뷰로 DRAFT 포함. 감마는 DRAFT + quota row 없음 → 잘못된 COMPLETED 분류 회피
        persistQuota(alphaId, ChallengerPart.WEB, 1);
        persistMember(alphaId, ChallengerPart.WEB, 100L);
        em.flush();
        em.clear();

        SearchProjectQuery query = SearchProjectQuery.forAdmin(
            gisuId, null, null, null, null,
            PartQuotaStatus.COMPLETED,
            List.of(ProjectStatus.IN_PROGRESS, ProjectStatus.DRAFT),
            PageRequest.of(0, 20));

        // when
        Page<Project> result = sut.search(query);

        // then
        assertThat(result.getContent())
            .extracting(Project::getName)
            .containsExactly("프로젝트 알파");
    }

    @Test
    void productOwnerSchoolIds_필터로_조회() {
        // 한양대(7L) 1건, 인하대(8L) 1건 추가
        persistProject("한양대 프로젝트", ProjectStatus.IN_PROGRESS, gisuId, 1L, 20L, 7L);
        persistProject("인하대 프로젝트", ProjectStatus.IN_PROGRESS, gisuId, 1L, 21L, 8L);

        SearchProjectQuery query = SearchProjectQuery.forChallenger(
            gisuId, null, null, List.of(7L), null, null, PageRequest.of(0, 20));

        Page<Project> result = sut.search(query);

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getName()).isEqualTo("한양대 프로젝트");
    }

    @Test
    void 상위_scope_검색에서도_본인_PO_DRAFT는_추가_포함한다() {
        persistProject("내 초안", ProjectStatus.DRAFT, gisuId, 2L, 99L, 9L);
        persistProject("다른 사람 초안", ProjectStatus.DRAFT, gisuId, 2L, 100L, 9L);
        em.flush();
        em.clear();

        SearchProjectQuery query = SearchProjectQuery.builder()
            .gisuId(gisuId)
            .chapterId(1L)
            .statuses(List.of(ProjectStatus.PENDING_REVIEW, ProjectStatus.IN_PROGRESS,
                ProjectStatus.COMPLETED, ProjectStatus.ABORTED))
            .pageable(PageRequest.of(0, 20))
            .build()
            .withIncludedOwner(99L, java.util.EnumSet.allOf(ProjectStatus.class));

        Page<Project> result = sut.search(query);

        assertThat(result.getContent())
            .extracting(Project::getName)
            .contains("내 초안")
            .doesNotContain("다른 사람 초안");
    }

    @Test
    void scope_clause_내부는_AND_clause_간은_OR로_조합한다() {
        // given — (gisu=1 AND chapter=10) OR owner=7 조건의 두 분기와 평면 교차 반례
        persistProject("기수-지부 분기", ProjectStatus.IN_PROGRESS, 1L, 10L, 8L);
        persistProject("owner 분기", ProjectStatus.IN_PROGRESS, 2L, 99L, 7L);
        persistProject("평면 교차 반례", ProjectStatus.IN_PROGRESS, 1L, 99L, 8L);
        em.flush();
        em.clear();

        SearchProjectQuery query = SearchProjectQuery.builder()
            .gisuId(1L)
            .chapterId(10L)
            .statuses(List.of(ProjectStatus.IN_PROGRESS))
            .pageable(PageRequest.of(0, 20))
            .build()
            .withIncludedOwner(7L, java.util.EnumSet.allOf(ProjectStatus.class));

        // when
        Page<Project> result = sut.search(query);

        // then — 독립 owner 분기는 유지하고 평면 교차 반례는 제외한다
        assertThat(result.getContent())
            .extracting(Project::getName)
            .containsExactlyInAnyOrder("기수-지부 분기", "owner 분기")
            .doesNotContain("평면 교차 반례");
    }

    @Test
    void target_scope의_허용_지부와_요청_지부가_다르면_결과가_없다() {
        SearchProjectQuery query = SearchProjectQuery.forChallenger(
            gisuId, null, 2L, null, null, null, PageRequest.of(0, 20))
            .withScopeClauses(List.of(
                ScopeClause.gisu(Set.of(gisuId), Set.of(ProjectStatus.IN_PROGRESS))
                    .andChapterIds(Set.of(1L))
            ));

        Page<Project> result = sut.search(query);

        assertThat(result.getContent()).isEmpty();
        assertThat(result.getTotalElements()).isZero();
    }

    // ========== Helper ==========

    private Project persistProject(String name, ProjectStatus status, Long gisuId, Long chapterId, Long ownerId) {
        return persistProject(name, status, gisuId, chapterId, ownerId, 1L);
    }

    private Project persistProject(String name, ProjectStatus status, Long gisuId, Long chapterId,
                                   Long ownerId, Long ownerSchoolId) {
        Project project;
        try {
            var constructor = Project.class.getDeclaredConstructor();
            constructor.setAccessible(true);
            project = constructor.newInstance();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        ReflectionTestUtils.setField(project, "gisuId", gisuId);
        ReflectionTestUtils.setField(project, "chapterId", chapterId);
        ReflectionTestUtils.setField(project, "status", status);
        ReflectionTestUtils.setField(project, "name", name);
        ReflectionTestUtils.setField(project, "productOwnerMemberId", ownerId);
        ReflectionTestUtils.setField(project, "productOwnerSchoolId", ownerSchoolId);
        ReflectionTestUtils.setField(project, "creatorMemberId", ownerId);
        em.persist(project);
        return project;
    }

    private void persistQuota(Long projectId, ChallengerPart part, long quota) {
        Project project = em.find(Project.class, projectId);
        ProjectPartQuota pq;
        try {
            var constructor = ProjectPartQuota.class.getDeclaredConstructor();
            constructor.setAccessible(true);
            pq = constructor.newInstance();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        ReflectionTestUtils.setField(pq, "project", project);
        ReflectionTestUtils.setField(pq, "part", part);
        ReflectionTestUtils.setField(pq, "quota", quota);
        em.persist(pq);
    }

    private void persistMember(Long projectId, ChallengerPart part, Long memberId) {
        Project project = em.find(Project.class, projectId);
        ProjectMember pm;
        try {
            var constructor = ProjectMember.class.getDeclaredConstructor();
            constructor.setAccessible(true);
            pm = constructor.newInstance();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        ReflectionTestUtils.setField(pm, "project", project);
        ReflectionTestUtils.setField(pm, "memberId", memberId);
        ReflectionTestUtils.setField(pm, "part", part);
        ReflectionTestUtils.setField(pm, "status", ProjectMemberStatus.ACTIVE);
        em.persist(pm);
    }

    private void updateAuditTimestamp(Long projectId, Instant createdAt) {
        em.getEntityManager()
            .createNativeQuery("""
                update project
                set created_at = :createdAt,
                    updated_at = :createdAt
                where id = :id
                """)
            .setParameter("createdAt", Timestamp.from(createdAt))
            .setParameter("id", projectId)
            .executeUpdate();
    }
}
