package com.umc.product.organization.application.port.service.query;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.umc.product.authorization.application.port.in.query.GetChallengerRoleUseCase;
import com.umc.product.common.domain.enums.ChallengerPart;
import com.umc.product.common.domain.enums.ChallengerRoleType;
import com.umc.product.member.application.port.in.query.GetMemberUseCase;
import com.umc.product.member.application.port.in.query.dto.MemberInfo;
import com.umc.product.organization.application.port.in.query.GetGisuUseCase;
import com.umc.product.organization.application.port.in.query.dto.OrganizationRoleScope;
import com.umc.product.organization.application.port.in.query.dto.studygroup.StudyGroupHeaderInfo;
import com.umc.product.organization.application.port.in.query.dto.studygroup.StudyGroupNameInfo;
import com.umc.product.organization.application.port.out.query.LoadStudyGroupPort;
import com.umc.product.organization.domain.StudyGroup;

@ExtendWith(MockitoExtension.class)
@DisplayName("StudyGroupQueryService 잔여 경로")
class StudyGroupQueryServiceResidualTest {

    @Mock
    GetGisuUseCase getGisuUseCase;

    @Mock
    GetMemberUseCase getMemberUseCase;

    @Mock
    GetChallengerRoleUseCase getChallengerRoleUseCase;

    @Mock
    LoadStudyGroupPort loadStudyGroupPort;

    StudyGroupQueryService service;

    @BeforeEach
    void setUp() {
        service = new StudyGroupQueryService(
            getGisuUseCase, getMemberUseCase, getChallengerRoleUseCase, loadStudyGroupPort
        );
    }

    @Test
    @DisplayName("내 스터디 목록은 헤더·멤버·멘토를 batch 조회하고 존재하는 회원만 조립한다")
    void 내_스터디_목록을_조립한다() {
        givenScope(1L, 10L, 100L, true);
        var header = new StudyGroupHeaderInfo(
            20L, "스프링 스터디", 10L, ChallengerPart.SPRINGBOOT, Instant.EPOCH
        );
        given(loadStudyGroupPort.findStudyGroupHeaders(any(), eq(10L), eq(30L), eq(3)))
            .willReturn(List.of(header));
        given(loadStudyGroupPort.findMemberIdsByStudyGroupIds(List.of(20L)))
            .willReturn(Map.of(20L, List.of(2L, 999L)));
        given(loadStudyGroupPort.findMentorIdsByStudyGroupIds(List.of(20L)))
            .willReturn(Map.of(20L, List.of(3L)));
        given(getMemberUseCase.findAllByIds(Set.of(2L, 3L, 999L))).willReturn(Map.of(
            2L, memberInfo(2L, "스터디원"),
            3L, memberInfo(3L, "멘토")
        ));

        var result = service.getMyStudyGroups(1L, 30L, 2);

        assertThat(result).singleElement().satisfies(group -> {
            assertThat(group.groupId()).isEqualTo(20L);
            assertThat(group.members()).extracting(member -> member.memberId()).containsExactly(2L);
            assertThat(group.mentors()).extracting(member -> member.memberId()).containsExactly(3L);
        });
    }

    @Test
    @DisplayName("스터디 이름 목록은 권한 없음과 파트장 권한을 구분한다")
    void 스터디_이름을_조회한다() {
        givenScope(1L, 10L, 100L, false);

        assertThat(service.getStudyGroupNames(1L)).isEmpty();

        given(getChallengerRoleUseCase.hasRoleTypeInGisu(
            1L, 10L, ChallengerRoleType.SCHOOL_PART_LEADER
        )).willReturn(true);
        given(loadStudyGroupPort.findStudyGroupNames(any(), eq(10L)))
            .willReturn(List.of(new StudyGroupNameInfo(20L, "스프링 스터디")));

        assertThat(service.getStudyGroupNames(1L)).singleElement()
            .satisfies(info -> assertThat(info.groupId()).isEqualTo(20L));
    }

    @Test
    @DisplayName("조회 가능한 그룹 ID는 빈 scope를 차단하고 유효 scope만 위임한다")
    void 조회_가능한_그룹_ID를_반환한다() {
        var scopes = List.<OrganizationRoleScope>of(new OrganizationRoleScope.AsPartLeader(1L));
        given(loadStudyGroupPort.findStudyGroupIds(scopes, 10L)).willReturn(Set.of(20L));

        assertThat(service.findStudyGroupIds(null, 10L)).isEmpty();
        assertThat(service.findStudyGroupIds(List.of(), 10L)).isEmpty();
        assertThat(service.findStudyGroupIds(scopes, 10L)).containsExactly(20L);
    }

    @Test
    @DisplayName("기수와 파트 조건에 맞는 스터디 ID 목록을 저장소에 위임한다")
    void 기수와_파트로_스터디_ID를_조회한다() {
        Set<ChallengerPart> parts = Set.of(ChallengerPart.SPRINGBOOT);
        given(loadStudyGroupPort.findIdsByGisuIdAndPartIn(10L, parts)).willReturn(List.of(20L));

        assertThat(service.getStudyGroupIdsByParts(10L, parts)).containsExactly(20L);
    }

    @Test
    @DisplayName("멤버가 없는 스터디는 batch 회원 조회를 생략하고 빈 목록을 반환한다")
    void 멤버가_없는_스터디를_조회한다() {
        var group = emptyGroup();
        given(loadStudyGroupPort.getEntityById(20L)).willReturn(group);

        assertThat(service.getStudyGroupMembers(20L)).isEmpty();
        then(getMemberUseCase).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("단건 상세에서 멤버와 멘토가 없으면 빈 batch 결과로 조립한다")
    void 빈_단건_상세를_조립한다() {
        var group = emptyGroup();
        given(loadStudyGroupPort.getEntityById(20L)).willReturn(group);

        var result = service.getWithMemberAndMentorInfoById(20L);

        assertThat(result.members()).isEmpty();
        assertThat(result.mentors()).isEmpty();
    }

    private void givenScope(Long memberId, Long gisuId, Long schoolId, boolean partLeader) {
        given(getMemberUseCase.getById(memberId)).willReturn(MemberInfo.builder()
            .id(memberId)
            .schoolId(schoolId)
            .build());
        given(getGisuUseCase.getActiveGisuId()).willReturn(gisuId);
        given(getChallengerRoleUseCase.isSchoolCoreInGisu(memberId, gisuId, schoolId)).willReturn(false);
        given(getChallengerRoleUseCase.hasRoleTypeInGisu(
            memberId, gisuId, ChallengerRoleType.SCHOOL_PART_LEADER
        )).willReturn(partLeader);
    }

    private MemberInfo memberInfo(Long id, String name) {
        return MemberInfo.builder()
            .id(id)
            .name(name)
            .schoolId(100L)
            .schoolName("테스트대학교")
            .profileImageId("profile-" + id)
            .profileImageLink("https://profile/" + id)
            .build();
    }

    private StudyGroup emptyGroup() {
        StudyGroup group = StudyGroup.create("빈 스터디", 10L, ChallengerPart.SPRINGBOOT);
        ReflectionTestUtils.setField(group, "id", 20L);
        return group;
    }
}
