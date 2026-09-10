package com.umc.product.organization.adapter.out.persistence.studygroup;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.Import;
import org.springframework.test.util.ReflectionTestUtils;

import com.umc.product.common.domain.enums.ChallengerPart;
import com.umc.product.common.domain.enums.ChallengerTrack;
import com.umc.product.organization.application.port.in.query.dto.OrganizationRoleScope;
import com.umc.product.organization.application.port.in.query.dto.OrganizationRoleScope.AsPartLeader;
import com.umc.product.organization.application.port.in.query.dto.OrganizationRoleScope.AsSchoolCore;
import com.umc.product.organization.application.port.in.query.dto.studygroup.StudyGroupHeaderInfo;
import com.umc.product.organization.domain.StudyGroup;
import com.umc.product.organization.domain.StudyGroupMember;
import com.umc.product.organization.domain.StudyGroupMentor;
import com.umc.product.support.PersistenceAdapterTest;

@PersistenceAdapterTest
@Import({
    StudyGroupQueryRepository.class
})
class StudyGroupQueryRepositoryTest {

    @Autowired
    TestEntityManager em;

    @Autowired
    StudyGroupQueryRepository sut;

    @Test
    void 트랙별_조회와_중복검사는_다른_트랙의_소속을_섞지_않는다() {
        // given
        StudyGroup plan = em.persist(StudyGroup.create("기획", 1L, null, ChallengerTrack.PLAN,
            Set.of(100L), Set.of(200L)));
        StudyGroup web = em.persist(StudyGroup.create("웹", 1L, null, ChallengerTrack.WEB_PRODUCT_ENGINEER,
            Set.of(100L), Set.of(200L)));
        em.flush();
        em.clear();

        // when & then
        assertThat(sut.findEntityByMemberIdAndGisuIdAndTrack(100L, 1L, ChallengerTrack.PLAN))
            .get().extracting(StudyGroup::getId).isEqualTo(plan.getId());
        assertThat(sut.findEntityByMemberIdAndGisuIdAndTrack(100L, 1L, ChallengerTrack.WEB_PRODUCT_ENGINEER))
            .get().extracting(StudyGroup::getId).isEqualTo(web.getId());
        assertThat(sut.findConflictedTrackMemberIds(1L, ChallengerTrack.PLAN, Set.of(100L), null))
            .containsExactly(100L);
        assertThat(sut.findConflictedTrackMemberIds(1L, ChallengerTrack.PLAN, Set.of(100L), plan.getId()))
            .isEmpty();
        assertThat(sut.findConflictedTrackMemberIds(2L, ChallengerTrack.PLAN, Set.of(100L), null)).isEmpty();
        assertThat(sut.findStudyGroupHeaders(List.of(new AsPartLeader(200L)), 1L, null, 20))
            .extracting(StudyGroupHeaderInfo::track)
            .containsExactlyInAnyOrder(ChallengerTrack.PLAN, ChallengerTrack.WEB_PRODUCT_ENGINEER);
        assertThat(sut.findStudyGroupMemberPage(Set.of(plan.getId()), null, 20)).singleElement()
            .satisfies(info -> assertThat(info.track()).isEqualTo(ChallengerTrack.PLAN));
    }

    @Test
    void findStudyGroupHeaders_AsSchoolCore_scope_학교_멤버가_멤버로_등록된_그룹만_반환() {
        // given
        Long gisuId = 1L;
        Long schoolMemberA = 100L;
        Long schoolMemberB = 101L;
        Long outsider = 999L;

        StudyGroup hit1 = persistGroup("hit1", gisuId, ChallengerPart.SPRINGBOOT,
            Set.of(), Set.of(schoolMemberA));
        StudyGroup hit2 = persistGroup("hit2", gisuId, ChallengerPart.SPRINGBOOT,
            Set.of(), Set.of(schoolMemberB));
        StudyGroup miss = persistGroup("miss", gisuId, ChallengerPart.SPRINGBOOT,
            Set.of(), Set.of(outsider));
        em.flush();
        em.clear();

        List<OrganizationRoleScope> scopes = List.of(new AsSchoolCore(Set.of(schoolMemberA, schoolMemberB)));

        // when
        List<StudyGroupHeaderInfo> result = sut.findStudyGroupHeaders(scopes, gisuId, null, 20);

        // then
        assertThat(result).extracting(StudyGroupHeaderInfo::groupId)
            .containsExactlyInAnyOrder(hit1.getId(), hit2.getId())
            .doesNotContain(miss.getId());
    }

    @Test
    void findStudyGroupHeaders_AsPartLeader_scope_memberId가_멘토로_등록된_그룹만_반환() {
        // given
        Long gisuId = 1L;
        Long me = 100L;
        Long otherMentor = 200L;
        Long someMember = 300L;

        StudyGroup hit = persistGroup("hit", gisuId, ChallengerPart.SPRINGBOOT,
            Set.of(me), Set.of(someMember));
        StudyGroup miss = persistGroup("miss", gisuId, ChallengerPart.SPRINGBOOT,
            Set.of(otherMentor), Set.of(someMember));
        em.flush();
        em.clear();

        List<OrganizationRoleScope> scopes = List.of(new AsPartLeader(me));

        // when
        List<StudyGroupHeaderInfo> result = sut.findStudyGroupHeaders(scopes, gisuId, null, 20);

        // then
        assertThat(result).extracting(StudyGroupHeaderInfo::groupId)
            .containsExactly(hit.getId())
            .doesNotContain(miss.getId());
    }

    @Test
    void findStudyGroupHeaders_두_scope_OR_결합_중복은_한_번만_반환() {
        // given — schoolOnly: 회장 scope만 / mentorOnly: 파트장 scope만 / both: 둘 다 잡힘
        Long gisuId = 1L;
        Long me = 100L;
        Long schoolMember = 101L;
        Long otherMentor = 200L;
        Long outsider = 999L;

        StudyGroup schoolOnly = persistGroup("schoolOnly", gisuId, ChallengerPart.SPRINGBOOT,
            Set.of(otherMentor), Set.of(schoolMember));
        StudyGroup mentorOnly = persistGroup("mentorOnly", gisuId, ChallengerPart.SPRINGBOOT,
            Set.of(me), Set.of(outsider));
        StudyGroup both = persistGroup("both", gisuId, ChallengerPart.SPRINGBOOT,
            Set.of(me), Set.of(schoolMember));
        em.flush();
        em.clear();

        List<OrganizationRoleScope> scopes = List.of(
            new AsSchoolCore(Set.of(schoolMember)),
            new AsPartLeader(me)
        );

        // when
        List<StudyGroupHeaderInfo> result = sut.findStudyGroupHeaders(scopes, gisuId, null, 20);

        // then
        assertThat(result).extracting(StudyGroupHeaderInfo::groupId)
            .containsExactlyInAnyOrder(schoolOnly.getId(), mentorOnly.getId(), both.getId());
    }

    @Test
    void findStudyGroupHeaders_다른_기수_그룹은_제외() {
        // given
        Long activeGisu = 1L;
        Long otherGisu = 2L;
        Long me = 100L;
        Long someMember = 200L;

        StudyGroup activeGroup = persistGroup("active", activeGisu, ChallengerPart.SPRINGBOOT,
            Set.of(me), Set.of(someMember));
        StudyGroup otherGroup = persistGroup("other", otherGisu, ChallengerPart.SPRINGBOOT,
            Set.of(me), Set.of(someMember));
        em.flush();
        em.clear();

        List<OrganizationRoleScope> scopes = List.of(new AsPartLeader(me));

        // when
        List<StudyGroupHeaderInfo> result = sut.findStudyGroupHeaders(scopes, activeGisu, null, 20);

        // then
        assertThat(result).extracting(StudyGroupHeaderInfo::groupId)
            .containsExactly(activeGroup.getId())
            .doesNotContain(otherGroup.getId());
    }

    @Test
    void findStudyGroupHeaders_커서_페이지네이션_id_DESC로_lt_cursor만_반환() {
        // given — id가 더 작은(=오래된) 그룹만 다음 페이지에 잡혀야 함
        Long gisuId = 1L;
        Long me = 100L;
        Long someMember = 200L;

        StudyGroup oldest = persistGroup("oldest", gisuId, ChallengerPart.SPRINGBOOT,
            Set.of(me), Set.of(someMember));
        StudyGroup middle = persistGroup("middle", gisuId, ChallengerPart.SPRINGBOOT,
            Set.of(me), Set.of(someMember));
        StudyGroup newest = persistGroup("newest", gisuId, ChallengerPart.SPRINGBOOT,
            Set.of(me), Set.of(someMember));
        em.flush();
        em.clear();

        List<OrganizationRoleScope> scopes = List.of(new AsPartLeader(me));

        // when — newest 다음 페이지
        List<StudyGroupHeaderInfo> result = sut.findStudyGroupHeaders(scopes, gisuId, newest.getId(), 20);

        // then
        assertThat(result).extracting(StudyGroupHeaderInfo::groupId)
            .containsExactly(middle.getId(), oldest.getId())
            .doesNotContain(newest.getId());
    }

    @Test
    void findStudyGroupHeaders_모든_scope의_predicate가_null이면_빈_결과() {
        // given — buildScopePredicate 가 null 반환하는 경로 (AsSchoolCore 의 schoolMemberIds 가 비어있음).
        //         이런 입력이 들어와도 풀스캔 없이 빈 리스트가 되어야 한다.
        Long gisuId = 1L;
        persistGroup("g1", gisuId, ChallengerPart.SPRINGBOOT, Set.of(), Set.of(100L));
        em.flush();
        em.clear();

        List<OrganizationRoleScope> scopes = List.of(new AsSchoolCore(Set.of()));

        // when
        List<StudyGroupHeaderInfo> result = sut.findStudyGroupHeaders(scopes, gisuId, null, 20);

        // then
        assertThat(result).isEmpty();
    }

    @Test
    void findMemberIdsByStudyGroupIds_그룹별_memberIds_매핑() {
        // given
        Long gisuId = 1L;
        StudyGroup g1 = persistGroup("g1", gisuId, ChallengerPart.SPRINGBOOT,
            Set.of(), Set.of(100L, 101L));
        StudyGroup g2 = persistGroup("g2", gisuId, ChallengerPart.SPRINGBOOT,
            Set.of(), Set.of(200L));
        StudyGroup g3 = persistGroup("g3", gisuId, ChallengerPart.SPRINGBOOT,
            Set.of(), Set.of(300L));   // 호출에 포함 안 시킬 그룹
        em.flush();
        em.clear();

        // when — g1, g2 만 조회
        Map<Long, List<Long>> result = sut.findMemberIdsByStudyGroupIds(List.of(g1.getId(), g2.getId()));

        // then
        assertThat(result).containsOnlyKeys(g1.getId(), g2.getId());
        assertThat(result.get(g1.getId())).containsExactlyInAnyOrder(100L, 101L);
        assertThat(result.get(g2.getId())).containsExactly(200L);
        assertThat(result).doesNotContainKey(g3.getId());
    }

    @Test
    void findMentorIdsByStudyGroupIds_그룹별_mentorIds_매핑() {
        // given
        Long gisuId = 1L;
        StudyGroup g1 = persistGroup("g1", gisuId, ChallengerPart.SPRINGBOOT,
            Set.of(10L, 11L), Set.of(100L));
        StudyGroup g2 = persistGroup("g2", gisuId, ChallengerPart.SPRINGBOOT,
            Set.of(20L), Set.of(100L));
        em.flush();
        em.clear();

        // when
        Map<Long, List<Long>> result = sut.findMentorIdsByStudyGroupIds(List.of(g1.getId(), g2.getId()));

        // then
        assertThat(result.get(g1.getId())).containsExactlyInAnyOrder(10L, 11L);
        assertThat(result.get(g2.getId())).containsExactly(20L);
    }

    @Test
    void findMemberIdsByStudyGroupIds_빈_입력은_빈_맵() {
        assertThat(sut.findMemberIdsByStudyGroupIds(List.of())).isEmpty();
        assertThat(sut.findMentorIdsByStudyGroupIds(List.of())).isEmpty();
    }

    @Test
    void findEntityById_mentors와_members_컬렉션을_초기화한_상태로_반환() {
        // given
        Long gisuId = 1L;
        StudyGroup target = persistGroup("target", gisuId, ChallengerPart.SPRINGBOOT,
            Set.of(10L, 11L), Set.of(100L, 101L));
        em.flush();
        em.clear();

        // when
        Optional<StudyGroup> result = sut.findEntityById(target.getId());
        em.clear();   // 영속성 컨텍스트 비움 — fetch join 안 됐다면 이후 컬렉션 접근에서 LazyInitException

        // then
        assertThat(result).isPresent();
        StudyGroup loaded = result.get();

        // detached 상태에서 컬렉션 접근 가능 → fetch join 동작 검증
        assertThat(loaded.getMembers())
            .extracting(StudyGroupMember::getMemberId)
            .containsExactlyInAnyOrder(100L, 101L);
        assertThat(loaded.getMentors())
            .extracting(StudyGroupMentor::getMemberId)
            .containsExactlyInAnyOrder(10L, 11L);
    }

    @Test
    void findEntityById_존재하지_않으면_Optional_empty() {
        // when
        Optional<StudyGroup> result = sut.findEntityById(99999L);

        // then
        assertThat(result).isEmpty();
    }

    @Test
    void findStudyGroupHeaders_AsSchoolCore_학교_멤버가_멘토로_등록된_그룹도_포함() {
        // given — V1 회장단 spec 확장: 멤버 OR 멘토에 학교 인원이 있으면 visible.
        Long gisuId = 1L;
        Long schoolMember = 100L;
        Long outsider = 999L;

        StudyGroup groupByMember = persistGroup("byMember", gisuId, ChallengerPart.SPRINGBOOT,
            Set.of(outsider), Set.of(schoolMember));      // 멤버에 학교 인원
        StudyGroup groupByMentor = persistGroup("byMentor", gisuId, ChallengerPart.SPRINGBOOT,
            Set.of(schoolMember), Set.of(outsider));      // 멘토에 학교 인원
        StudyGroup miss = persistGroup("miss", gisuId, ChallengerPart.SPRINGBOOT,
            Set.of(outsider), Set.of(outsider));          // 둘 다 학교 인원 아님
        em.flush();
        em.clear();

        List<OrganizationRoleScope> scopes = List.of(new AsSchoolCore(Set.of(schoolMember)));

        // when
        List<StudyGroupHeaderInfo> result = sut.findStudyGroupHeaders(scopes, gisuId, null, 20);

        // then
        assertThat(result).extracting(StudyGroupHeaderInfo::groupId)
            .containsExactlyInAnyOrder(groupByMember.getId(), groupByMentor.getId())
            .doesNotContain(miss.getId());
    }

    @Test
    void findStudyGroupIds_scope_적용된_studyGroupIds_Set_반환() {
        // given
        Long gisuId = 1L;
        Long me = 100L;
        Long otherMentor = 200L;

        StudyGroup hit1 = persistGroup("hit1", gisuId, ChallengerPart.SPRINGBOOT,
            Set.of(me), Set.of(300L));
        StudyGroup hit2 = persistGroup("hit2", gisuId, ChallengerPart.SPRINGBOOT,
            Set.of(me), Set.of(301L));
        StudyGroup miss = persistGroup("miss", gisuId, ChallengerPart.SPRINGBOOT,
            Set.of(otherMentor), Set.of(302L));
        em.flush();
        em.clear();

        List<OrganizationRoleScope> scopes = List.of(new AsPartLeader(me));

        // when
        Set<Long> result = sut.findStudyGroupIds(scopes, gisuId);

        // then
        assertThat(result)
            .containsExactlyInAnyOrder(hit1.getId(), hit2.getId())
            .doesNotContain(miss.getId());
    }

    @Test
    void findStudyGroupIds_scope_predicate_null이면_빈_Set() {
        // given
        Long gisuId = 1L;
        persistGroup("g1", gisuId, ChallengerPart.SPRINGBOOT, Set.of(), Set.of(100L));
        em.flush();
        em.clear();

        // when — 빈 schoolMemberIds → predicate null → 풀스캔 방지
        Set<Long> result = sut.findStudyGroupIds(List.of(new AsSchoolCore(Set.of())), gisuId);

        // then
        assertThat(result).isEmpty();
    }

    @Test
    void findStudyGroupIds_다른_기수_그룹은_제외() {
        // given
        Long activeGisu = 1L;
        Long otherGisu = 2L;
        Long me = 100L;

        StudyGroup activeGroup = persistGroup("active", activeGisu, ChallengerPart.SPRINGBOOT,
            Set.of(me), Set.of(200L));
        StudyGroup otherGroup = persistGroup("other", otherGisu, ChallengerPart.SPRINGBOOT,
            Set.of(me), Set.of(200L));
        em.flush();
        em.clear();

        // when
        Set<Long> result = sut.findStudyGroupIds(List.of(new AsPartLeader(me)), activeGisu);

        // then
        assertThat(result).containsExactly(activeGroup.getId()).doesNotContain(otherGroup.getId());
    }

    // ========== Helper Methods ==========

    private StudyGroup persistGroup(
        String name, Long gisuId, ChallengerPart part,
        Set<Long> mentorIds, Set<Long> memberIds
    ) {
        StudyGroup group = newStudyGroup(name, gisuId, part);
        em.persist(group);
        memberIds.forEach(memberId -> persistMember(group, memberId));
        mentorIds.forEach(mentorId -> persistMentor(group, mentorId));
        return group;
    }

    private StudyGroup newStudyGroup(String name, Long gisuId, ChallengerPart part) {
        StudyGroup group;
        try {
            var constructor = StudyGroup.class.getDeclaredConstructor();
            constructor.setAccessible(true);
            group = constructor.newInstance();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        ReflectionTestUtils.setField(group, "name", name);
        ReflectionTestUtils.setField(group, "gisuId", gisuId);
        ReflectionTestUtils.setField(group, "part", part);
        return group;
    }

    private void persistMember(StudyGroup group, Long memberId) {
        StudyGroupMember entity;
        try {
            var constructor = StudyGroupMember.class.getDeclaredConstructor();
            constructor.setAccessible(true);
            entity = constructor.newInstance();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        ReflectionTestUtils.setField(entity, "studyGroup", group);
        ReflectionTestUtils.setField(entity, "memberId", memberId);
        ReflectionTestUtils.setField(entity, "isLeader", false);
        em.persist(entity);
    }

    private void persistMentor(StudyGroup group, Long memberId) {
        StudyGroupMentor entity;
        try {
            var constructor = StudyGroupMentor.class.getDeclaredConstructor();
            constructor.setAccessible(true);
            entity = constructor.newInstance();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        ReflectionTestUtils.setField(entity, "studyGroup", group);
        ReflectionTestUtils.setField(entity, "memberId", memberId);
        em.persist(entity);
    }
}
