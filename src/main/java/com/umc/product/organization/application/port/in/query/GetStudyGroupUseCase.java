package com.umc.product.organization.application.port.in.query;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import com.umc.product.common.domain.enums.ChallengerPart;
import com.umc.product.organization.application.port.in.query.dto.OrganizationRoleScope;
import com.umc.product.organization.application.port.in.query.dto.studygroup.StudyGroupInfo;
import com.umc.product.organization.application.port.in.query.dto.studygroup.StudyGroupMemberInfo;
import com.umc.product.organization.application.port.in.query.dto.studygroup.StudyGroupMemberPageInfo;
import com.umc.product.organization.application.port.in.query.dto.studygroup.StudyGroupNameInfo;
import com.umc.product.organization.application.port.in.query.dto.studygroup.StudyGroupWithMemberAndMentorInfo;

/**
 * 스터디 그룹 조회 UseCase
 */
public interface GetStudyGroupUseCase {

    /**
     * 내 스터디 그룹 목록 조회.
     * <p>
     * memberId만으로 schoolId/활성 기수/역할을 내부에서 resolve 하여 역할 기반 Scope로 조회한다.
     * <ul>
     *   <li>학교 회장단 → 학교 멤버가 포함된 모든 그룹</li>
     *   <li>파트장 → 본인이 파트장인 그룹</li>
     *   <li>권한 없음 → 빈 리스트</li>
     * </ul>
     *
     * @param memberId 요청 주체 memberId
     * @param cursor   직전 페이지 마지막 groupId (첫 페이지는 null)
     * @param size     페이지 크기
     * @return 조회된 스터디 그룹 요약 목록 (권한 없으면 빈 리스트)
     */
    List<StudyGroupWithMemberAndMentorInfo> getMyStudyGroups(Long memberId, Long cursor, int size);

    /**
     * 스터디 그룹 이름 목록 조회 - memberId 기반으로 schoolId/part를 자동 resolve
     */
    List<StudyGroupNameInfo> getStudyGroupNames(Long memberId);

    StudyGroupInfo getById(Long studyGroupId);

    Optional<StudyGroupInfo> findById(Long studyGroupId);

    Optional<StudyGroupInfo> findByMemberIdAndGisuIdAndPart(
        Long memberId,
        Long gisuId,
        ChallengerPart part
    );

    StudyGroupWithMemberAndMentorInfo getWithMemberAndMentorInfoById(Long studyGroupId);

    /**
     * 스터디 그룹 ID 로 소속 스터디원 목록 조회.
     * <p>
     * 각 스터디원에 대해 memberId / 학교명 / 프로필 이미지 URL 을 반환한다. 대상은 {@code study_group_member} 테이블의 멤버이며 파트장(StudyGroupMentor)
     * 테이블과는 별개다.
     *
     * @param groupId 스터디 그룹 ID
     * @return 소속 스터디원 목록 (소속 없으면 빈 리스트)
     */
    List<StudyGroupMemberInfo> getStudyGroupMembers(Long groupId);

    /**
     * 요청자가 조회 권한을 가진 스터디 그룹들의 스터디원 목록을 커서 페이지네이션으로 조회한다.
     * <p>
     * {@link #getMyStudyGroups} 와 같은 역할 Scope 규칙(회장단/파트장)을 쓰되, 페이지 단위가 그룹이 아니라 *스터디원* 이다. 제출 현황처럼 인원이 행이 되는
     * 화면을 위해 분리했다.
     * <p>
     * 이름/학교/프로필은 담기지 않는다 (Member 도메인 소관). 호출 측이 memberId 로 batch 합성한다.
     *
     * @param requesterMemberId 요청 주체 memberId
     * @param studyGroupId      특정 그룹만 조회 (null 이면 권한 범위 내 전체 그룹)
     * @param cursor            직전 페이지 마지막 studyGroupMemberId (첫 페이지는 null)
     * @param size              조회 건수. hasNext 판별이 필요하면 호출 측에서 +1 하여 전달한다.
     * @return 조회된 스터디원 목록 (권한 범위가 비면 빈 리스트)
     * @throws com.umc.product.organization.exception.OrganizationDomainException {@code studyGroupId} 가 요청자의 권한
     *                                                                           범위 밖일 때
     */
    List<StudyGroupMemberPageInfo> getVisibleStudyGroupMembers(
        Long requesterMemberId, Long studyGroupId, Long cursor, int size
    );

    /**
     * 요청자가 조회 권한을 가진 스터디 그룹들의 distinct 파트 집합을 반환한다.
     * <p>
     * {@link #getVisibleStudyGroupMembers} 와 동일한 역할 Scope 규칙(회장단/파트장)을 쓰되, 멤버 페이지네이션 없이 파트만 뽑는다.
     * 제출 현황 주차 필터처럼 "가시 범위 그룹들의 파트별 커리큘럼" 만 필요한 화면을 위해 분리했다.
     *
     * @param requesterMemberId 요청 주체 memberId
     * @param studyGroupId      특정 그룹만 조회 (null 이면 권한 범위 내 전체 그룹)
     * @return distinct 파트 집합 (권한 범위가 비면 빈 집합)
     * @throws com.umc.product.organization.exception.OrganizationDomainException {@code studyGroupId} 가 요청자의 권한
     *                                                                           범위 밖일 때
     */
    Set<ChallengerPart> getVisibleStudyGroupParts(Long requesterMemberId, Long studyGroupId);

    /**
     * 특정 기수에서 해당 파트들의 스터디 그룹 ID 목록 조회 (파트장용)
     */
    List<Long> getStudyGroupIdsByParts(Long gisuId, Set<ChallengerPart> parts);

    /**
     * 사용자의 활성 기수 내 역할을 검사해 {@link OrganizationRoleScope} 리스트를 반환한다.
     * <p>
     * 다른 도메인(Schedule, StudyGroupSchedule 등) 이 사용자에게 보이는 데이터를 필터링할 때 이 scope 들을 받아 자기 데이터에 적용한다.
     * 권한 없는 일반 챌린저는 빈 리스트.
     */
    List<OrganizationRoleScope> resolveOrganizationRoleScopes(Long memberId);

    /**
     * 주어진 scope + 기수로 조회 가능한 스터디 그룹 ID 집합을 반환한다.
     * <p>
     * {@link #resolveOrganizationRoleScopes} 의 결과를 그대로 입력하면 됨. Schedule 같은 다른 aggregate 가 "사용자에게 보이는 스터디 그룹" 을
     * 알아내 schedule 필터링에 사용하는 케이스 등을 위해 분리.
     */
    Set<Long> findStudyGroupIds(List<OrganizationRoleScope> scopes, Long gisuId);
}
