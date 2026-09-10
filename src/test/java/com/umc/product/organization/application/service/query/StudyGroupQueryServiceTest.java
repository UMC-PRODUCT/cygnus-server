package com.umc.product.organization.application.service.query;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.umc.product.authorization.application.port.in.query.GetChallengerRoleUseCase;
import com.umc.product.common.domain.enums.ChallengerPart;
import com.umc.product.common.domain.enums.ChallengerRoleType;
import com.umc.product.common.domain.enums.GisuLearningType;
import com.umc.product.common.domain.enums.MemberStatus;
import com.umc.product.member.application.port.in.query.GetMemberUseCase;
import com.umc.product.member.application.port.in.query.dto.MemberInfo;
import com.umc.product.organization.application.port.in.query.GetGisuUseCase;
import com.umc.product.organization.application.port.in.query.dto.OrganizationRoleScope;
import com.umc.product.organization.application.port.in.query.dto.OrganizationRoleScope.AsPartLeader;
import com.umc.product.organization.application.port.in.query.dto.OrganizationRoleScope.AsSchoolCore;
import com.umc.product.organization.application.port.in.query.dto.gisu.GisuInfo;
import com.umc.product.organization.application.port.in.query.dto.studygroup.StudyGroupInfo;
import com.umc.product.organization.application.port.in.query.dto.studygroup.StudyGroupMemberInfo;
import com.umc.product.organization.application.port.in.query.dto.studygroup.StudyGroupWithMemberAndMentorInfo;
import com.umc.product.organization.application.port.out.query.LoadStudyGroupPort;
import com.umc.product.organization.application.port.service.query.StudyGroupQueryService;
import com.umc.product.organization.domain.StudyGroup;
import com.umc.product.organization.domain.StudyGroupMember;
import com.umc.product.organization.domain.StudyGroupMentor;

@ExtendWith(MockitoExtension.class)
class StudyGroupQueryServiceTest {

    @Mock
    GetGisuUseCase getGisuUseCase;
    @Mock
    GetMemberUseCase getMemberUseCase;
    @Mock
    GetChallengerRoleUseCase getChallengerRoleUseCase;
    @Mock
    LoadStudyGroupPort loadStudyGroupPort;

    @InjectMocks
    StudyGroupQueryService sut;

    @Test
    void 준비기수_조회는_선택한_기수의_권한과_그룹을_사용한다() {
        // given
        given(getMemberUseCase.getById(1L)).willReturn(memberInfo(1L, 100L));
        given(getGisuUseCase.getById(11L)).willReturn(new GisuInfo(11L, 11L,
            Instant.parse("2026-09-01T00:00:00Z"), Instant.parse("2027-03-01T00:00:00Z"),
            false, GisuLearningType.TRACK));
        given(getChallengerRoleUseCase.isSchoolCoreInGisu(1L, 11L, 100L)).willReturn(true);
        given(getMemberUseCase.listIdsBySchoolId(100L)).willReturn(Set.of(101L));

        // when
        sut.getMyStudyGroups(1L, null, 20, 11L);
        sut.getStudyGroupNames(1L, 11L);
        sut.getVisibleStudyGroupMembers(1L, null, null, 20, 11L);

        // then
        verify(getGisuUseCase, never()).getActiveGisuId();
        verify(loadStudyGroupPort).findStudyGroupHeaders(any(), eq(11L), eq(null), eq(21));
        verify(loadStudyGroupPort).findStudyGroupNames(any(), eq(11L));
        verify(loadStudyGroupPort).findStudyGroupIds(any(), eq(11L));
        verify(getChallengerRoleUseCase, times(3)).isSchoolCoreInGisu(1L, 11L, 100L);
    }

    @Test
    void getMyStudyGroups_회장은_AsSchoolCore_scope로_조회() {
        // given
        Long memberId = 1L;
        Long schoolId = 100L;
        Long gisuId = 10L;
        Set<Long> schoolMemberIds = Set.of(101L, 102L, 103L);

        given(getMemberUseCase.getById(memberId)).willReturn(memberInfo(memberId, schoolId));
        given(getGisuUseCase.getActiveGisuId()).willReturn(gisuId);
        given(getChallengerRoleUseCase.isSchoolCoreInGisu(memberId, gisuId, schoolId)).willReturn(true);
        given(getMemberUseCase.listIdsBySchoolId(schoolId)).willReturn(schoolMemberIds);
        given(getChallengerRoleUseCase.hasRoleTypeInGisu(memberId, gisuId, ChallengerRoleType.SCHOOL_PART_LEADER))
            .willReturn(false);
        given(loadStudyGroupPort.findStudyGroupHeaders(any(), eq(gisuId), eq(null), anyInt()))
            .willReturn(List.of());   // headers 빈 결과 — scope 검증에 집중

        // when
        sut.getMyStudyGroups(memberId, null, 20);

        // then
        List<OrganizationRoleScope> capturedScopes = captureScopes();
        assertThat(capturedScopes).hasSize(1);
        assertThat(capturedScopes.get(0)).isInstanceOfSatisfying(AsSchoolCore.class,
            scope -> assertThat(scope.schoolMemberIds()).isEqualTo(schoolMemberIds));
    }

    @Test
    void getMyStudyGroups_파트장은_AsPartLeader_scope로_조회() {
        // given
        Long memberId = 2L;
        Long schoolId = 200L;
        Long gisuId = 10L;

        given(getMemberUseCase.getById(memberId)).willReturn(memberInfo(memberId, schoolId));
        given(getGisuUseCase.getActiveGisuId()).willReturn(gisuId);
        given(getChallengerRoleUseCase.isSchoolCoreInGisu(memberId, gisuId, schoolId)).willReturn(false);
        given(getChallengerRoleUseCase.hasRoleTypeInGisu(memberId, gisuId, ChallengerRoleType.SCHOOL_PART_LEADER))
            .willReturn(true);

        given(loadStudyGroupPort.findStudyGroupHeaders(any(), eq(gisuId), eq(null), anyInt()))
            .willReturn(List.of());

        // when
        sut.getMyStudyGroups(memberId, null, 20);

        // then
        List<OrganizationRoleScope> capturedScopes = captureScopes();
        assertThat(capturedScopes).hasSize(1);
        assertThat(capturedScopes.get(0)).isInstanceOfSatisfying(AsPartLeader.class,
            scope -> assertThat(scope.memberId()).isEqualTo(memberId));
    }

    @Test
    void getMyStudyGroups_회장과_파트장_겸직시_두_scope_조립() {
        // given
        Long memberId = 3L;
        Long schoolId = 300L;
        Long gisuId = 10L;
        Set<Long> schoolMemberIds = Set.of(301L, 302L);

        given(getMemberUseCase.getById(memberId)).willReturn(memberInfo(memberId, schoolId));
        given(getGisuUseCase.getActiveGisuId()).willReturn(gisuId);
        given(getChallengerRoleUseCase.isSchoolCoreInGisu(memberId, gisuId, schoolId)).willReturn(true);
        given(getMemberUseCase.listIdsBySchoolId(schoolId)).willReturn(schoolMemberIds);
        given(getChallengerRoleUseCase.hasRoleTypeInGisu(memberId, gisuId, ChallengerRoleType.SCHOOL_PART_LEADER))
            .willReturn(true);

        given(loadStudyGroupPort.findStudyGroupHeaders(any(), eq(gisuId), eq(null), anyInt()))
            .willReturn(List.of());

        // when
        sut.getMyStudyGroups(memberId, null, 20);

        // then
        List<OrganizationRoleScope> capturedScopes = captureScopes();
        assertThat(capturedScopes).hasSize(2);
        assertThat(capturedScopes).hasAtLeastOneElementOfType(AsSchoolCore.class);
        assertThat(capturedScopes).hasAtLeastOneElementOfType(AsPartLeader.class);
    }

    @Test
    void getMyStudyGroups_권한이_없으면_port_호출없이_빈_리스트() {
        // given — 회장도 파트장도 아닌 일반 챌린저
        Long memberId = 4L;
        Long schoolId = 400L;
        Long gisuId = 10L;

        given(getMemberUseCase.getById(memberId)).willReturn(memberInfo(memberId, schoolId));
        given(getGisuUseCase.getActiveGisuId()).willReturn(gisuId);
        given(getChallengerRoleUseCase.isSchoolCoreInGisu(memberId, gisuId, schoolId)).willReturn(false);
        given(getChallengerRoleUseCase.hasRoleTypeInGisu(memberId, gisuId, ChallengerRoleType.SCHOOL_PART_LEADER))
            .willReturn(false);

        // when
        List<StudyGroupWithMemberAndMentorInfo> result = sut.getMyStudyGroups(memberId, null, 20);

        // then
        assertThat(result).isEmpty();
        verify(loadStudyGroupPort, never()).findStudyGroupHeaders(any(), any(), any(), anyInt());
    }

    @Test
    void getMyStudyGroups_회장이지만_학교_멤버가_없으면_AsSchoolCore_scope_제외() {
        // given — 회장 권한 있지만 학교 멤버 집합이 비어있어 AsSchoolCore가 EXISTS=false가 될 것이 자명. 비용 절감 위해 scope 자체를 만들지 않음.
        //         파트장 권한을 같이 부여해 결과적으로 AsPartLeader만 조립되는지 검증한다.
        Long memberId = 5L;
        Long schoolId = 500L;
        Long gisuId = 10L;

        given(getMemberUseCase.getById(memberId)).willReturn(memberInfo(memberId, schoolId));
        given(getGisuUseCase.getActiveGisuId()).willReturn(gisuId);
        given(getChallengerRoleUseCase.isSchoolCoreInGisu(memberId, gisuId, schoolId)).willReturn(true);
        given(getMemberUseCase.listIdsBySchoolId(schoolId)).willReturn(Set.of());
        given(getChallengerRoleUseCase.hasRoleTypeInGisu(memberId, gisuId, ChallengerRoleType.SCHOOL_PART_LEADER))
            .willReturn(true);
        given(loadStudyGroupPort.findStudyGroupHeaders(any(), eq(gisuId), eq(null), anyInt()))
            .willReturn(List.of());

        // when
        sut.getMyStudyGroups(memberId, null, 20);

        // then
        List<OrganizationRoleScope> capturedScopes = captureScopes();
        assertThat(capturedScopes).hasSize(1);
        assertThat(capturedScopes.get(0)).isInstanceOf(AsPartLeader.class);
    }

    @Test
    void getMyStudyGroups_컨트롤러_hasNext_판단을_위해_size_plus_1로_port에_전달() {
        // given
        Long memberId = 6L;
        Long schoolId = 600L;
        Long gisuId = 10L;
        int requestedSize = 20;

        given(getMemberUseCase.getById(memberId)).willReturn(memberInfo(memberId, schoolId));
        given(getGisuUseCase.getActiveGisuId()).willReturn(gisuId);
        given(getChallengerRoleUseCase.isSchoolCoreInGisu(memberId, gisuId, schoolId)).willReturn(false);
        given(getChallengerRoleUseCase.hasRoleTypeInGisu(memberId, gisuId, ChallengerRoleType.SCHOOL_PART_LEADER))
            .willReturn(true);
        given(loadStudyGroupPort.findStudyGroupHeaders(any(), any(), any(), anyInt()))
            .willReturn(List.of());

        // when
        sut.getMyStudyGroups(memberId, null, requestedSize);

        // then
        verify(loadStudyGroupPort).findStudyGroupHeaders(any(), eq(gisuId), eq(null), eq(requestedSize + 1));
    }

    @Test
    void getById_StudyGroupInfo에_기본_정보와_멘토_멤버_ID를_담는다() {
        // given
        Long groupId = 42L;
        Long mentorId = 10L;
        Long memberId = 20L;
        StudyGroup group = studyGroup(
            groupId, "스프링 스터디", 1L, ChallengerPart.SPRINGBOOT,
            List.of(mentorId), List.of(memberId)
        );
        given(loadStudyGroupPort.getEntityById(groupId)).willReturn(group);

        // when
        StudyGroupInfo result = sut.getById(groupId);

        // then
        assertThat(result.groupId()).isEqualTo(groupId);
        assertThat(result.name()).isEqualTo("스프링 스터디");
        assertThat(result.mentorIds()).containsExactly(mentorId);
        assertThat(result.memberIds()).containsExactly(memberId);
        verify(getMemberUseCase, never()).findAllByIds(any());
    }

    @Test
    void getWithMemberAndMentorInfoById_헤더와_멘토_멤버를_조립한다() {
        // given — Aggregate 가 자식 mentor/member 를 캡슐화. Service 는 group.getMentors/getMembers 로 접근.
        Long groupId = 42L;
        Long mentor1 = 10L;
        Long mentor2 = 20L;
        Long member1 = 30L;
        Long member2 = 40L;

        StudyGroup group = studyGroup(
            groupId, "스프링 스터디", 1L, ChallengerPart.SPRINGBOOT,
            List.of(mentor1, mentor2), List.of(member1, member2)
        );
        given(loadStudyGroupPort.getEntityById(groupId)).willReturn(group);
        given(getMemberUseCase.findAllByIds(Set.of(mentor1, mentor2, member1, member2)))
            .willReturn(Map.of(
                mentor1, memberInfo(mentor1, 100L),
                mentor2, memberInfo(mentor2, 100L),
                member1, memberInfo(member1, 200L),
                member2, memberInfo(member2, 200L)
            ));

        // when
        StudyGroupWithMemberAndMentorInfo result = sut.getWithMemberAndMentorInfoById(groupId);

        // then
        assertThat(result.groupId()).isEqualTo(groupId);
        assertThat(result.name()).isEqualTo("스프링 스터디");
        assertThat(result.mentors())
            .extracting(StudyGroupMemberInfo::memberId)
            .containsExactly(mentor1, mentor2);
        assertThat(result.members())
            .extracting(StudyGroupMemberInfo::memberId)
            .containsExactly(member1, member2);
        assertThat(result.members())
            .allSatisfy(m -> {
                assertThat(m.studyGroupId()).isEqualTo(groupId);
                assertThat(m.memberName()).isEqualTo("테스트");
                assertThat(m.schoolName()).isEqualTo("테스트학교");
            });
    }

    @Test
    void getWithMemberAndMentorInfoById_Member_조회_누락분은_결과에서_제외된다() {
        // given — INNER JOIN 의 silent drop 과 동일 동작: memberMap 에 없는 ID 는 결과 리스트에서 빠진다.
        Long groupId = 42L;
        Long existing = 10L;
        Long withdrawn = 99L;

        StudyGroup group = studyGroup(
            groupId, "g", 1L, ChallengerPart.SPRINGBOOT,
            List.of(), List.of(existing, withdrawn)
        );
        given(loadStudyGroupPort.getEntityById(groupId)).willReturn(group);
        given(getMemberUseCase.findAllByIds(Set.of(existing, withdrawn)))
            .willReturn(Map.of(existing, memberInfo(existing, 100L)));   // withdrawn 누락

        // when
        StudyGroupWithMemberAndMentorInfo result = sut.getWithMemberAndMentorInfoById(groupId);

        // then
        assertThat(result.members())
            .extracting(StudyGroupMemberInfo::memberId)
            .containsExactly(existing)
            .doesNotContain(withdrawn);
    }

    @Test
    void getWithMemberAndMentorInfoById_mentor와_member_둘다_없으면_findAllByIds_호출없이_빈_리스트() {
        // given
        Long groupId = 42L;
        StudyGroup group = studyGroup(
            groupId, "g", 1L, ChallengerPart.SPRINGBOOT,
            List.of(), List.of()
        );
        given(loadStudyGroupPort.getEntityById(groupId)).willReturn(group);

        // when
        StudyGroupWithMemberAndMentorInfo result = sut.getWithMemberAndMentorInfoById(groupId);

        // then
        assertThat(result.mentors()).isEmpty();
        assertThat(result.members()).isEmpty();
        verify(getMemberUseCase, never()).findAllByIds(any());
    }

    @Test
    void findById_존재하면_StudyGroupInfo를_반환한다() {
        // given
        Long groupId = 42L;
        Long mentorId = 10L;
        Long memberId = 20L;
        StudyGroup group = studyGroup(
            groupId, "g", 1L, ChallengerPart.SPRINGBOOT,
            List.of(mentorId), List.of(memberId)
        );
        given(loadStudyGroupPort.findEntityById(groupId)).willReturn(Optional.of(group));

        // when
        Optional<StudyGroupInfo> result = sut.findById(groupId);

        // then
        assertThat(result).isPresent();
        assertThat(result.get().mentorIds()).containsExactly(mentorId);
        assertThat(result.get().memberIds()).containsExactly(memberId);
        verify(getMemberUseCase, never()).findAllByIds(any());
    }

    @Test
    void findById_존재하지_않으면_Optional_empty를_반환한다() {
        // given
        Long groupId = 999L;
        given(loadStudyGroupPort.findEntityById(groupId)).willReturn(Optional.empty());

        // when
        Optional<StudyGroupInfo> result = sut.findById(groupId);

        // then
        assertThat(result).isEmpty();
    }

    @Test
    void getStudyGroupMembers_그룹_멤버를_MemberInfo로_조립한다() {
        // given
        Long groupId = 42L;
        Long member1 = 30L;
        Long member2 = 40L;

        StudyGroup group = studyGroup(
            groupId, "g", 1L, ChallengerPart.SPRINGBOOT,
            List.of(), List.of(member1, member2)
        );
        given(loadStudyGroupPort.getEntityById(groupId)).willReturn(group);
        given(getMemberUseCase.findAllByIds(Set.of(member1, member2)))
            .willReturn(Map.of(
                member1, memberInfo(member1, 200L),
                member2, memberInfo(member2, 200L)
            ));

        // when
        List<StudyGroupMemberInfo> result = sut.getStudyGroupMembers(groupId);

        // then
        assertThat(result)
            .extracting(StudyGroupMemberInfo::memberId)
            .containsExactly(member1, member2);
        assertThat(result).allSatisfy(m -> {
            assertThat(m.studyGroupId()).isEqualTo(groupId);
            assertThat(m.memberName()).isEqualTo("테스트");
            assertThat(m.schoolName()).isEqualTo("테스트학교");
        });
    }

    @Test
    void getStudyGroupMembers_멤버가_없으면_findAllByIds_호출없이_빈_리스트() {
        // given
        Long groupId = 42L;
        StudyGroup group = studyGroup(
            groupId, "g", 1L, ChallengerPart.SPRINGBOOT,
            List.of(), List.of()
        );
        given(loadStudyGroupPort.getEntityById(groupId)).willReturn(group);

        // when
        List<StudyGroupMemberInfo> result = sut.getStudyGroupMembers(groupId);

        // then
        assertThat(result).isEmpty();
        verify(getMemberUseCase, never()).findAllByIds(any());
    }

    @Test
    void getStudyGroupMembers_Member_조회_누락분은_결과에서_제외된다() {
        // given — getById 와 동일하게 INNER JOIN 의 silent drop 동작 유지
        Long groupId = 42L;
        Long existing = 30L;
        Long withdrawn = 99L;

        StudyGroup group = studyGroup(
            groupId, "g", 1L, ChallengerPart.SPRINGBOOT,
            List.of(), List.of(existing, withdrawn)
        );
        given(loadStudyGroupPort.getEntityById(groupId)).willReturn(group);
        given(getMemberUseCase.findAllByIds(Set.of(existing, withdrawn)))
            .willReturn(Map.of(existing, memberInfo(existing, 200L)));

        // when
        List<StudyGroupMemberInfo> result = sut.getStudyGroupMembers(groupId);

        // then
        assertThat(result)
            .extracting(StudyGroupMemberInfo::memberId)
            .containsExactly(existing)
            .doesNotContain(withdrawn);
    }

    @Test
    @DisplayName("회장단과 파트장을 겸직하면 두 역할의 scope 합집합으로 스터디 그룹을 조회한다")
    void findVisibleStudyGroupIdsMergesScopesWhenMemberHoldsBothRoles() {
        // given
        Long memberId = 1L;
        Long schoolId = 100L;
        Long gisuId = 10L;
        Set<Long> schoolMemberIds = Set.of(101L, 102L);

        given(getMemberUseCase.getById(memberId)).willReturn(memberInfo(memberId, schoolId));
        given(getGisuUseCase.getActiveGisuId()).willReturn(gisuId);
        given(getChallengerRoleUseCase.isSchoolCoreInGisu(memberId, gisuId, schoolId)).willReturn(true);
        given(getMemberUseCase.listIdsBySchoolId(schoolId)).willReturn(schoolMemberIds);
        given(getChallengerRoleUseCase.hasRoleTypeInGisu(memberId, gisuId, ChallengerRoleType.SCHOOL_PART_LEADER))
            .willReturn(true);
        given(loadStudyGroupPort.findStudyGroupIds(any(), eq(gisuId))).willReturn(Set.of(100L, 200L));

        // when
        Set<Long> result = sut.findVisibleStudyGroupIds(memberId);

        // then
        assertThat(result).containsExactlyInAnyOrder(100L, 200L);
        assertThat(captureVisibleScopes())
            .hasSize(2)
            .hasAtLeastOneElementOfType(AsSchoolCore.class)
            .hasAtLeastOneElementOfType(AsPartLeader.class);
    }

    @Test
    @DisplayName("회장단도 파트장도 아니면 port 호출 없이 빈 Set을 반환한다")
    void findVisibleStudyGroupIdsReturnsEmptyWithoutPortCallWhenNoRole() {
        // given
        Long memberId = 1L;
        Long schoolId = 100L;
        Long gisuId = 10L;

        given(getMemberUseCase.getById(memberId)).willReturn(memberInfo(memberId, schoolId));
        given(getGisuUseCase.getActiveGisuId()).willReturn(gisuId);
        given(getChallengerRoleUseCase.isSchoolCoreInGisu(memberId, gisuId, schoolId)).willReturn(false);
        given(getChallengerRoleUseCase.hasRoleTypeInGisu(memberId, gisuId, ChallengerRoleType.SCHOOL_PART_LEADER))
            .willReturn(false);

        // when
        Set<Long> result = sut.findVisibleStudyGroupIds(memberId);

        // then
        assertThat(result).isEmpty();
        verify(loadStudyGroupPort, never()).findStudyGroupIds(any(), any());
    }

    @Test
    @DisplayName("활성 기수를 한 번만 읽어 scope 판단 기수와 조회 기수가 어긋나지 않는다")
    void findVisibleStudyGroupIdsReadsActiveGisuOnce() {
        // given - scope 조립과 그룹 조회가 서로 다른 기수를 보면 권한 범위가 어긋난다
        Long memberId = 1L;
        Long schoolId = 100L;
        Long activeGisuId = 10L;

        given(getMemberUseCase.getById(memberId)).willReturn(memberInfo(memberId, schoolId));
        given(getGisuUseCase.getActiveGisuId()).willReturn(activeGisuId);
        given(getChallengerRoleUseCase.isSchoolCoreInGisu(memberId, activeGisuId, schoolId)).willReturn(false);
        given(getChallengerRoleUseCase.hasRoleTypeInGisu(
            memberId, activeGisuId, ChallengerRoleType.SCHOOL_PART_LEADER)).willReturn(true);
        given(loadStudyGroupPort.findStudyGroupIds(any(), eq(activeGisuId))).willReturn(Set.of(100L));

        // when
        sut.findVisibleStudyGroupIds(memberId);

        // then
        verify(getGisuUseCase, times(1)).getActiveGisuId();
        verify(loadStudyGroupPort).findStudyGroupIds(any(), eq(activeGisuId));
    }

    // ========== Helper Methods ==========

    @SuppressWarnings("unchecked")
    private List<OrganizationRoleScope> captureScopes() {
        ArgumentCaptor<List<OrganizationRoleScope>> captor = ArgumentCaptor.forClass(List.class);
        verify(loadStudyGroupPort).findStudyGroupHeaders(captor.capture(), any(), any(), anyInt());
        return captor.getValue();
    }

    @SuppressWarnings("unchecked")
    private List<OrganizationRoleScope> captureVisibleScopes() {
        ArgumentCaptor<List<OrganizationRoleScope>> captor = ArgumentCaptor.forClass(List.class);
        verify(loadStudyGroupPort).findStudyGroupIds(captor.capture(), any());
        return captor.getValue();
    }

    private MemberInfo memberInfo(Long memberId, Long schoolId) {
        return MemberInfo.builder()
            .id(memberId)
            .name("테스트")
            .nickname("테스트")
            .email("test@example.com")
            .schoolId(schoolId)
            .schoolName("테스트학교")
            .profileImageId(null)
            .profileImageLink(null)
            .status(MemberStatus.ACTIVE)
            .roles(List.of())
            .build();
    }

    private StudyGroup studyGroup(
        Long id, String name, Long gisuId, ChallengerPart part,
        List<Long> mentorIds, List<Long> memberIds
    ) {
        StudyGroup group = newStudyGroup(id, name, gisuId, part);

        List<StudyGroupMentor> mentors = new ArrayList<>();
        mentorIds.forEach(mid -> mentors.add(newStudyGroupMentor(group, mid)));
        ReflectionTestUtils.setField(group, "mentors", mentors);

        List<StudyGroupMember> members = new ArrayList<>();
        memberIds.forEach(mid -> members.add(newStudyGroupMember(group, mid)));
        ReflectionTestUtils.setField(group, "members", members);

        return group;
    }

    private StudyGroup newStudyGroup(Long id, String name, Long gisuId, ChallengerPart part) {
        StudyGroup group;
        try {
            var constructor = StudyGroup.class.getDeclaredConstructor();
            constructor.setAccessible(true);
            group = constructor.newInstance();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        ReflectionTestUtils.setField(group, "id", id);
        ReflectionTestUtils.setField(group, "name", name);
        ReflectionTestUtils.setField(group, "gisuId", gisuId);
        ReflectionTestUtils.setField(group, "part", part);
        return group;
    }

    private StudyGroupMember newStudyGroupMember(StudyGroup group, Long memberId) {
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
        return entity;
    }

    private StudyGroupMentor newStudyGroupMentor(StudyGroup group, Long memberId) {
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
        return entity;
    }
}
