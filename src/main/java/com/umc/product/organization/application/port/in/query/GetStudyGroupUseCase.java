package com.umc.product.organization.application.port.in.query;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import com.umc.product.common.domain.enums.ChallengerPart;
import com.umc.product.common.domain.enums.ChallengerTrack;
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

    List<StudyGroupWithMemberAndMentorInfo> getMyStudyGroups(Long memberId, Long cursor, int size, Long gisuId);

    /**
     * 스터디 그룹 이름 목록 조회 - memberId 기반으로 schoolId/part를 자동 resolve
     */
    List<StudyGroupNameInfo> getStudyGroupNames(Long memberId);

    List<StudyGroupNameInfo> getStudyGroupNames(Long memberId, Long gisuId);

    StudyGroupInfo getById(Long studyGroupId);

    Optional<StudyGroupInfo> findById(Long studyGroupId);

    Optional<StudyGroupInfo> findByMemberIdAndGisuIdAndPart(
        Long memberId,
        Long gisuId,
        ChallengerPart part
    );

    Optional<StudyGroupInfo> findByMemberIdAndGisuIdAndTrack(
        Long memberId, Long gisuId, ChallengerTrack track
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
     * 특정 기수에서 해당 파트들의 스터디 그룹 ID 목록 조회 (파트장용)
     */
    List<StudyGroupMemberPageInfo> getVisibleStudyGroupMembers(
        Long requesterMemberId, Long studyGroupId, Long cursor, int size, Long gisuId
    );

    List<Long> getStudyGroupIdsByParts(Long gisuId, Set<ChallengerPart> parts);

    /**
     * 사용자에게 보이는 활성 기수 스터디 그룹 ID 집합을 반환한다.
     * <p>
     * 호출자는 memberId 만 넘기면 된다. 역할별 Scope 판단, 회장단·파트장 겸직 시 합집합 처리, 활성 기수 결정을 모두 Organization 안에서 하므로 호출자가
     * Organization 의 권한 계산 방식을 알 필요가 없다.
     * <p>
     * Schedule 등 다른 aggregate 가 "사용자에게 보이는 스터디 그룹" 만 필요할 때 사용한다. 권한 없는 일반 챌린저는 빈 Set.
     *
     * @param memberId 요청 주체 memberId
     * @return 조회 가능한 스터디 그룹 ID 집합 (권한 없으면 빈 Set)
     */
    Set<Long> findVisibleStudyGroupIds(Long memberId);
}
