package com.umc.product.organization.adapter.out.persistence;

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
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;

import com.umc.product.common.domain.enums.ChallengerPart;
import com.umc.product.organization.adapter.out.persistence.chapter.ChapterJpaRepository;
import com.umc.product.organization.adapter.out.persistence.chapter.ChapterPersistenceAdapter;
import com.umc.product.organization.adapter.out.persistence.chapter.ChapterSchoolJpaRepository;
import com.umc.product.organization.adapter.out.persistence.chapter.ChapterSchoolPersistenceAdapter;
import com.umc.product.organization.adapter.out.persistence.chapter.ChapterSchoolQueryRepository;
import com.umc.product.organization.adapter.out.persistence.gisu.GisuJpaRepository;
import com.umc.product.organization.adapter.out.persistence.gisu.GisuPersistenceAdapter;
import com.umc.product.organization.adapter.out.persistence.gisu.GisuQueryRepository;
import com.umc.product.organization.adapter.out.persistence.school.SchoolJpaRepository;
import com.umc.product.organization.adapter.out.persistence.school.SchoolLinkJpaRepository;
import com.umc.product.organization.adapter.out.persistence.school.SchoolPersistenceAdapter;
import com.umc.product.organization.adapter.out.persistence.school.SchoolQueryRepository;
import com.umc.product.organization.adapter.out.persistence.studygroup.StudyGroupJpaRepository;
import com.umc.product.organization.adapter.out.persistence.studygroup.StudyGroupPersistenceAdapter;
import com.umc.product.organization.adapter.out.persistence.studygroup.StudyGroupQueryRepository;
import com.umc.product.organization.application.port.in.query.dto.OrganizationRoleScope;
import com.umc.product.organization.application.port.in.query.dto.school.SchoolChapterInfo;
import com.umc.product.organization.domain.StudyGroupSchedule;
import com.umc.product.organization.exception.OrganizationDomainException;

@DisplayName("Organization Persistence Adapter 잔여 경로")
class OrganizationPersistenceAdapterResidualTest {

    @Nested
    @ExtendWith(MockitoExtension.class)
    @DisplayName("스터디 그룹")
    class StudyGroupAdapter {

        @Mock
        StudyGroupJpaRepository jpaRepository;

        @Mock
        StudyGroupQueryRepository queryRepository;

        @InjectMocks
        StudyGroupPersistenceAdapter adapter;

        @Test
        @DisplayName("단건 조회는 Optional 계약과 not-found 예외를 보장한다")
        void 단건_조회_계약을_보장한다() {
            var group = 스터디_그룹(1L, 2L, Set.of(3L), Set.of(4L));
            given(queryRepository.findEntityById(1L)).willReturn(Optional.of(group));
            given(queryRepository.findEntityById(9L)).willReturn(Optional.empty());
            given(jpaRepository.findByName("스프링 스터디")).willReturn(Optional.of(group));
            given(jpaRepository.findByName("없음")).willReturn(Optional.empty());

            assertThat(adapter.getEntityById(1L)).isSameAs(group);
            assertThat(adapter.findEntityById(1L)).contains(group);
            assertThat(adapter.getByName("스프링 스터디")).isSameAs(group);
            assertThatThrownBy(() -> adapter.getEntityById(9L)).isInstanceOf(OrganizationDomainException.class);
            assertThatThrownBy(() -> adapter.getByName("없음")).isInstanceOf(OrganizationDomainException.class);
        }

        @Test
        @DisplayName("scope·ID batch 조회는 null과 빈 입력을 차단하고 유효 입력만 위임한다")
        void batch_조회의_경계값을_처리한다() {
            var scopes = List.<OrganizationRoleScope>of(new OrganizationRoleScope.AsPartLeader(1L));
            given(queryRepository.findStudyGroupHeaders(scopes, 2L, null, 10)).willReturn(List.of());
            given(queryRepository.findStudyGroupNames(scopes, 2L)).willReturn(List.of());
            given(queryRepository.findStudyGroupIds(scopes, 2L)).willReturn(Set.of(1L));
            given(queryRepository.findMemberIdsByStudyGroupIds(List.of(1L))).willReturn(Map.of(1L, List.of(3L)));
            given(queryRepository.findMentorIdsByStudyGroupIds(List.of(1L))).willReturn(Map.of(1L, List.of(4L)));
            given(jpaRepository.findIdsByGisuIdAndPartIn(2L, Set.of(ChallengerPart.SPRINGBOOT)))
                .willReturn(List.of(1L));
            given(queryRepository.findConflictedMemberIds(
                2L, ChallengerPart.SPRINGBOOT, Set.of(3L), null
            )).willReturn(Set.of(3L));

            assertThat(adapter.findStudyGroupHeaders(null, 2L, null, 10)).isEmpty();
            assertThat(adapter.findStudyGroupHeaders(List.of(), 2L, null, 10)).isEmpty();
            assertThat(adapter.findStudyGroupHeaders(scopes, 2L, null, 10)).isEmpty();
            assertThat(adapter.findStudyGroupNames(null, 2L)).isEmpty();
            assertThat(adapter.findStudyGroupNames(scopes, 2L)).isEmpty();
            assertThat(adapter.findStudyGroupIds(null, 2L)).isEmpty();
            assertThat(adapter.findStudyGroupIds(scopes, 2L)).containsExactly(1L);
            assertThat(adapter.findMemberIdsByStudyGroupIds(null)).isEmpty();
            assertThat(adapter.findMemberIdsByStudyGroupIds(List.of(1L))).containsKey(1L);
            assertThat(adapter.findMentorIdsByStudyGroupIds(List.of())).isEmpty();
            assertThat(adapter.findMentorIdsByStudyGroupIds(List.of(1L))).containsKey(1L);
            assertThat(adapter.findIdsByGisuIdAndPartIn(2L, null)).isEmpty();
            assertThat(adapter.findIdsByGisuIdAndPartIn(2L, Set.of(ChallengerPart.SPRINGBOOT)))
                .containsExactly(1L);
            assertThat(adapter.findConflictedMemberIds(2L, ChallengerPart.SPRINGBOOT, null, null)).isEmpty();
            assertThat(adapter.findConflictedMemberIds(
                2L, ChallengerPart.SPRINGBOOT, Set.of(3L), null
            )).containsExactly(3L);
        }

        @Test
        @DisplayName("저장과 삭제를 JPA 저장소에 위임한다")
        void 저장하고_삭제한다() {
            var group = 스터디_그룹(1L, 2L, Set.of(3L), Set.of(4L));
            given(jpaRepository.save(group)).willReturn(group);

            assertThat(adapter.save(group)).isSameAs(group);
            adapter.delete(group);

            then(jpaRepository).should().delete(group);
        }
    }

    @Nested
    @ExtendWith(MockitoExtension.class)
    @DisplayName("학교")
    class SchoolAdapter {

        @Mock
        SchoolJpaRepository jpaRepository;

        @Mock
        SchoolLinkJpaRepository linkRepository;

        @Mock
        SchoolQueryRepository queryRepository;

        @InjectMocks
        SchoolPersistenceAdapter adapter;

        @Test
        @DisplayName("학교 저장·삭제·단건·일괄·존재 검사를 위임하고 not-found를 변환한다")
        void 학교_기본_영속성_계약을_보장한다() {
            var school = 학교(1L, "테스트대학교");
            given(jpaRepository.save(school)).willReturn(school);
            given(jpaRepository.findByIdWithDetails(1L)).willReturn(Optional.of(school));
            given(jpaRepository.findByIdWithDetails(9L)).willReturn(Optional.empty());
            given(jpaRepository.findById(1L)).willReturn(Optional.of(school));
            given(jpaRepository.findById(9L)).willReturn(Optional.empty());
            given(jpaRepository.findAllByIdIn(List.of(1L))).willReturn(List.of(school));
            given(jpaRepository.existsById(1L)).willReturn(true);
            given(jpaRepository.existsById(9L)).willReturn(false);
            given(jpaRepository.findUnassignedByGisuId(2L)).willReturn(List.of(school));

            assertThat(adapter.save(school)).isSameAs(school);
            adapter.deleteAllByIds(List.of(1L));
            adapter.deleteAllLinksBySchoolIds(List.of(1L));
            assertThat(adapter.findSchoolDetailById(1L)).isSameAs(school);
            assertThat(adapter.findById(1L)).isSameAs(school);
            assertThat(adapter.findAllByIds(List.of(1L))).containsExactly(school);
            assertThat(adapter.existsById(1L)).isTrue();
            adapter.throwIfNotExists(1L);
            assertThat(adapter.findUnassignedByGisuId(2L)).containsExactly(school);
            assertThatThrownBy(() -> adapter.findSchoolDetailById(9L)).isInstanceOf(OrganizationDomainException.class);
            assertThatThrownBy(() -> adapter.findById(9L)).isInstanceOf(OrganizationDomainException.class);
            assertThatThrownBy(() -> adapter.throwIfNotExists(9L)).isInstanceOf(OrganizationDomainException.class);
        }

        @Test
        @DisplayName("학교 조회 모델은 QueryDSL 저장소에 위임하고 없는 상세를 변환한다")
        void 학교_조회_모델을_위임한다() {
            var detail = new SchoolChapterInfo(2L, "서울", "학교", 1L, null, null, true, null, null);
            given(queryRepository.getSchoolDetail(1L)).willReturn(detail);
            given(queryRepository.getSchoolDetail(9L)).willReturn(null);

            adapter.findSchools(null, PageRequest.of(0, 10));
            adapter.findAllNames();
            adapter.findSchoolDetailsByGisuId(2L);
            adapter.findSchoolDetailsByGisuIds(Set.of(2L));
            adapter.findSchoolDetailsByIds(Set.of(1L));
            adapter.findLinksBySchoolIds(List.of(1L));
            assertThat(adapter.findSchoolDetailByIdWithActiveChapter(1L)).isSameAs(detail);
            adapter.findLinksBySchoolId(1L);
            assertThatThrownBy(() -> adapter.findSchoolDetailByIdWithActiveChapter(9L))
                .isInstanceOf(OrganizationDomainException.class);
        }
    }

    @Nested
    @ExtendWith(MockitoExtension.class)
    @DisplayName("지부와 기수")
    class ChapterAndGisuAdapter {

        @Mock
        ChapterJpaRepository chapterJpaRepository;
        @Mock
        ChapterSchoolJpaRepository chapterSchoolJpaRepository;
        @Mock
        ChapterSchoolQueryRepository chapterSchoolQueryRepository;
        @Mock
        GisuJpaRepository gisuJpaRepository;
        @Mock
        GisuQueryRepository gisuQueryRepository;

        @Test
        @DisplayName("지부 adapter는 존재·조회·목록·저장·삭제 계약을 보장한다")
        void 지부_adapter_계약을_보장한다() {
            var gisu = 기수(1L, 9L, true);
            var chapter = 지부(2L, gisu, "서울");
            var adapter = new ChapterPersistenceAdapter(chapterJpaRepository);
            given(chapterJpaRepository.existsById(2L)).willReturn(true);
            given(chapterJpaRepository.existsById(9L)).willReturn(false);
            given(chapterJpaRepository.findById(2L)).willReturn(Optional.of(chapter));
            given(chapterJpaRepository.findById(9L)).willReturn(Optional.empty());
            given(chapterJpaRepository.save(chapter)).willReturn(chapter);

            adapter.validateExists(2L);
            assertThatThrownBy(() -> adapter.validateExists(9L)).isInstanceOf(OrganizationDomainException.class);
            assertThat(adapter.findById(2L)).isSameAs(chapter);
            assertThatThrownBy(() -> adapter.findById(9L)).isInstanceOf(OrganizationDomainException.class);
            adapter.findAll();
            adapter.findByGisuId(1L);
            adapter.findByGisuIds(Set.of(1L));
            adapter.existsByGisuId(1L);
            assertThat(adapter.save(chapter)).isSameAs(chapter);
            adapter.delete(chapter);
        }

        @Test
        @DisplayName("지부-학교 adapter는 모든 조회·저장·삭제를 위임하고 not-found를 변환한다")
        void 지부_학교_adapter_계약을_보장한다() {
            var assignment = 지부_학교(3L, 지부(2L, 기수(1L, 9L, true), "서울"), 학교(4L, "학교"));
            var adapter = new ChapterSchoolPersistenceAdapter(
                chapterSchoolJpaRepository, chapterSchoolQueryRepository
            );
            given(chapterSchoolJpaRepository.save(assignment)).willReturn(assignment);
            given(chapterSchoolQueryRepository.findByChapterIdAndSchoolId(2L, 4L))
                .willReturn(Optional.of(assignment));
            given(chapterSchoolQueryRepository.findByChapterIdAndSchoolId(9L, 9L))
                .willReturn(Optional.empty());

            adapter.findByGisuId(1L);
            adapter.findByGisuIds(Set.of(1L));
            assertThat(adapter.save(assignment)).isSameAs(assignment);
            adapter.deleteAllBySchoolIds(List.of(4L));
            adapter.deleteAllByChapterId(2L);
            assertThat(adapter.findByChapterIdAndSchoolId(2L, 4L)).isSameAs(assignment);
            assertThatThrownBy(() -> adapter.findByChapterIdAndSchoolId(9L, 9L))
                .isInstanceOf(OrganizationDomainException.class);
            adapter.findBySchoolId(4L);
            adapter.findBySchoolIds(List.of(4L));
            adapter.findByGisuIdsAndSchoolIds(Set.of(1L), Set.of(4L));
        }

        @Test
        @DisplayName("기수 adapter는 활성·단건·목록·날짜·저장·삭제 계약과 not-found를 보장한다")
        void 기수_adapter_계약을_보장한다() {
            var gisu = 기수(1L, 9L, true);
            var adapter = new GisuPersistenceAdapter(gisuJpaRepository, gisuQueryRepository);
            given(gisuJpaRepository.findByIsActiveTrue()).willReturn(Optional.of(gisu));
            given(gisuJpaRepository.findActiveWithLock()).willReturn(Optional.of(gisu));
            given(gisuJpaRepository.findById(1L)).willReturn(Optional.of(gisu));
            given(gisuJpaRepository.findById(9L)).willReturn(Optional.empty());
            given(gisuJpaRepository.save(gisu)).willReturn(gisu);
            given(gisuQueryRepository.findGisuByDate(Instant.EPOCH)).willReturn(Optional.of(gisu));

            assertThat(adapter.getActiveGisu()).isSameAs(gisu);
            assertThat(adapter.findActiveGisu()).contains(gisu);
            assertThat(adapter.findActiveGisuWithLock()).contains(gisu);
            assertThat(adapter.getById(1L)).isSameAs(gisu);
            assertThatThrownBy(() -> adapter.getById(9L)).isInstanceOf(OrganizationDomainException.class);
            adapter.listByIds(Set.of(1L));
            adapter.listByGenerations(Set.of(9L));
            adapter.findAll();
            adapter.findAll(PageRequest.of(0, 10));
            assertThat(adapter.save(gisu)).isSameAs(gisu);
            adapter.existsByGeneration(9L);
            assertThat(adapter.findGisuByDate(Instant.EPOCH)).contains(gisu);
            adapter.delete(gisu);

            given(gisuJpaRepository.findByIsActiveTrue()).willReturn(Optional.empty());
            assertThatThrownBy(adapter::getActiveGisu).isInstanceOf(OrganizationDomainException.class);
        }
    }

    @Nested
    @ExtendWith(MockitoExtension.class)
    @DisplayName("스터디 일정")
    class ScheduleAdapter {

        @Mock
        StudyGroupScheduleJpaRepository jpaRepository;

        @InjectMocks
        StudyGroupSchedulePersistenceAdapter adapter;

        @Mock
        StudyGroupSchedule schedule;

        @Test
        @DisplayName("일정을 저장하고 그룹 ID 입력의 빈값을 차단하며 ID를 중복 제거한다")
        void 일정_adapter_계약을_보장한다() {
            given(jpaRepository.save(schedule)).willReturn(schedule);
            given(jpaRepository.findScheduleIdsByStudyGroupIdIn(List.of(1L, 2L)))
                .willReturn(List.of(10L, 10L, 20L));

            assertThat(adapter.save(schedule)).isSameAs(schedule);
            assertThat(adapter.findScheduleIdsByStudyGroupIds(null)).isEmpty();
            assertThat(adapter.findScheduleIdsByStudyGroupIds(List.of())).isEmpty();
            assertThat(adapter.findScheduleIdsByStudyGroupIds(List.of(1L, 2L)))
                .containsExactlyInAnyOrder(10L, 20L);
        }
    }
}
