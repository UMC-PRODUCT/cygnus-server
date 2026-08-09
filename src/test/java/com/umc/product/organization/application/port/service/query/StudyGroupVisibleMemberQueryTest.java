package com.umc.product.organization.application.port.service.query;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.never;
import static org.mockito.BDDMockito.then;

import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import com.umc.product.authorization.application.port.in.query.GetChallengerRoleUseCase;
import com.umc.product.common.domain.enums.ChallengerPart;
import com.umc.product.common.domain.enums.ChallengerRoleType;
import com.umc.product.member.application.port.in.query.GetMemberUseCase;
import com.umc.product.member.application.port.in.query.dto.MemberInfo;
import com.umc.product.organization.application.port.in.query.GetGisuUseCase;
import com.umc.product.organization.application.port.in.query.dto.studygroup.StudyGroupMemberPageInfo;
import com.umc.product.organization.application.port.out.query.LoadStudyGroupPort;
import com.umc.product.organization.exception.OrganizationDomainException;
import com.umc.product.organization.exception.OrganizationErrorCode;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("권한 범위 내 스터디원 조회")
class StudyGroupVisibleMemberQueryTest {

    private static final Long REQUESTER_ID = 1L;
    private static final Long SCHOOL_ID = 5L;
    private static final Long GISU_ID = 9L;
    private static final Long VISIBLE_GROUP = 10L;
    private static final Long OUT_OF_SCOPE_GROUP = 99L;

    @Mock
    private GetGisuUseCase getGisuUseCase;
    @Mock
    private GetMemberUseCase getMemberUseCase;
    @Mock
    private GetChallengerRoleUseCase getChallengerRoleUseCase;
    @Mock
    private LoadStudyGroupPort loadStudyGroupPort;
    @InjectMocks
    private StudyGroupQueryService service;

    @BeforeEach
    void setUp() {
        given(getMemberUseCase.getById(REQUESTER_ID))
            .willReturn(MemberInfo.builder().id(REQUESTER_ID).schoolId(SCHOOL_ID).build());
        given(getGisuUseCase.getActiveGisuId()).willReturn(GISU_ID);
        given(loadStudyGroupPort.findStudyGroupIds(any(), anyLong())).willReturn(Set.of(VISIBLE_GROUP));
        given(loadStudyGroupPort.findStudyGroupMemberPage(any(), any(), anyInt()))
            .willReturn(List.of(new StudyGroupMemberPageInfo(
                51L, VISIBLE_GROUP, "SpringBoot 스터디", ChallengerPart.SPRINGBOOT, 100L
            )));
    }

    @Test
    @DisplayName("파트장이면 본인 그룹 Scope 로 조회한다")
    void partLeaderScope() {
        givenPartLeader();

        assertThat(service.getVisibleStudyGroupMembers(REQUESTER_ID, null, null, 20)).hasSize(1);

        ArgumentCaptor<Set<Long>> groupIds = ArgumentCaptor.forClass(Set.class);
        then(loadStudyGroupPort).should().findStudyGroupMemberPage(groupIds.capture(), any(), anyInt());
        assertThat(groupIds.getValue()).containsExactly(VISIBLE_GROUP);
    }

    @Test
    @DisplayName("권한이 없으면 스터디원 쿼리를 아예 실행하지 않고 빈 목록을 반환한다")
    void noRole_returnsEmptyWithoutQuery() {
        givenNoRole();

        assertThat(service.getVisibleStudyGroupMembers(REQUESTER_ID, null, null, 20)).isEmpty();

        then(loadStudyGroupPort).should(never()).findStudyGroupMemberPage(any(), any(), anyInt());
    }

    @Test
    @DisplayName("권한 범위 밖 그룹을 지정하면 빈 목록이 아니라 403 으로 끊는다")
    void outOfScopeGroup_throwsAccessDenied() {
        givenPartLeader();

        assertThatThrownBy(() ->
            service.getVisibleStudyGroupMembers(REQUESTER_ID, OUT_OF_SCOPE_GROUP, null, 20))
            .isInstanceOf(OrganizationDomainException.class)
            .extracting("baseCode")
            .isEqualTo(OrganizationErrorCode.STUDY_GROUP_ACCESS_DENIED);
    }

    @Test
    @DisplayName("권한이 아예 없는 사용자가 그룹을 지정해도 403 이다")
    void noRoleWithGroupId_throwsAccessDenied() {
        givenNoRole();

        assertThatThrownBy(() ->
            service.getVisibleStudyGroupMembers(REQUESTER_ID, VISIBLE_GROUP, null, 20))
            .isInstanceOf(OrganizationDomainException.class)
            .extracting("baseCode")
            .isEqualTo(OrganizationErrorCode.STUDY_GROUP_ACCESS_DENIED);
    }

    @Test
    @DisplayName("권한 범위 안 그룹을 지정하면 그 그룹만 조회한다")
    void inScopeGroup_narrowsToThatGroup() {
        givenPartLeader();

        assertThat(service.getVisibleStudyGroupMembers(REQUESTER_ID, VISIBLE_GROUP, null, 20)).hasSize(1);

        then(loadStudyGroupPort).should().findStudyGroupMemberPage(Set.of(VISIBLE_GROUP), null, 20);
    }

    @Test
    @DisplayName("가시 파트 조회는 권한 범위 그룹들의 distinct 파트를 반환한다")
    void visibleParts_returnsPartsOfVisibleGroups() {
        givenPartLeader();
        given(loadStudyGroupPort.findPartsByStudyGroupIds(Set.of(VISIBLE_GROUP)))
            .willReturn(Set.of(ChallengerPart.SPRINGBOOT));

        assertThat(service.getVisibleStudyGroupParts(REQUESTER_ID, null))
            .containsExactly(ChallengerPart.SPRINGBOOT);
    }

    @Test
    @DisplayName("가시 파트 조회에서 범위 밖 그룹을 지정하면 403 이다")
    void visibleParts_outOfScopeGroup_throwsAccessDenied() {
        givenPartLeader();

        assertThatThrownBy(() -> service.getVisibleStudyGroupParts(REQUESTER_ID, OUT_OF_SCOPE_GROUP))
            .isInstanceOf(OrganizationDomainException.class)
            .extracting("baseCode")
            .isEqualTo(OrganizationErrorCode.STUDY_GROUP_ACCESS_DENIED);
    }

    @Test
    @DisplayName("가시 파트 조회에서 그룹을 지정하면 그 그룹의 파트만 조회한다")
    void visibleParts_inScopeGroup_narrowsToThatGroup() {
        givenPartLeader();
        given(loadStudyGroupPort.findPartsByStudyGroupIds(Set.of(VISIBLE_GROUP)))
            .willReturn(Set.of(ChallengerPart.SPRINGBOOT));

        assertThat(service.getVisibleStudyGroupParts(REQUESTER_ID, VISIBLE_GROUP))
            .containsExactly(ChallengerPart.SPRINGBOOT);

        then(loadStudyGroupPort).should().findPartsByStudyGroupIds(Set.of(VISIBLE_GROUP));
    }

    @Test
    @DisplayName("권한이 없으면 가시 파트는 빈 집합이다")
    void visibleParts_noRole_returnsEmpty() {
        givenNoRole();

        assertThat(service.getVisibleStudyGroupParts(REQUESTER_ID, null)).isEmpty();
    }

    private void givenPartLeader() {
        given(getChallengerRoleUseCase.isSchoolCoreInGisu(REQUESTER_ID, GISU_ID, SCHOOL_ID)).willReturn(false);
        given(getChallengerRoleUseCase.hasRoleTypeInGisu(
            REQUESTER_ID, GISU_ID, ChallengerRoleType.SCHOOL_PART_LEADER
        )).willReturn(true);
    }

    private void givenNoRole() {
        given(getChallengerRoleUseCase.isSchoolCoreInGisu(REQUESTER_ID, GISU_ID, SCHOOL_ID)).willReturn(false);
        given(getChallengerRoleUseCase.hasRoleTypeInGisu(
            REQUESTER_ID, GISU_ID, ChallengerRoleType.SCHOOL_PART_LEADER
        )).willReturn(false);
    }
}
