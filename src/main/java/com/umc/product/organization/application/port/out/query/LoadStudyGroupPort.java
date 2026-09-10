package com.umc.product.organization.application.port.out.query;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import com.umc.product.common.domain.enums.ChallengerPart;
import com.umc.product.common.domain.enums.ChallengerTrack;
import com.umc.product.organization.application.port.in.query.dto.OrganizationRoleScope;
import com.umc.product.organization.application.port.in.query.dto.studygroup.StudyGroupHeaderInfo;
import com.umc.product.organization.application.port.in.query.dto.studygroup.StudyGroupMemberPageInfo;
import com.umc.product.organization.application.port.in.query.dto.studygroup.StudyGroupNameInfo;
import com.umc.product.organization.domain.StudyGroup;

public interface LoadStudyGroupPort {
    // TODO: 의존성 역전 있음, 수정 필요

    StudyGroup getEntityById(Long id);

    Optional<StudyGroup> findEntityById(Long id);

    Optional<StudyGroup> findEntityByMemberIdAndGisuIdAndPart(
        Long memberId,
        Long gisuId,
        ChallengerPart part
    );

    Optional<StudyGroup> findEntityByMemberIdAndGisuIdAndTrack(
        Long memberId, Long gisuId, ChallengerTrack track
    );

    Set<Long> findConflictedTrackMemberIds(
        Long gisuId, ChallengerTrack track, Set<Long> memberIds, Long excludedStudyGroupId
    );

    StudyGroup getByName(String name);

    /**
     * 운영진(회장단/파트장)의 권한 Scope 기반 스터디 그룹 헤더 목록을 커서 페이지네이션으로 조회한다.
     * <p>
     * scopes 를 OR 로 합쳐 EXISTS 서브쿼리로 필터링. 정렬은 id DESC (신규 우선), 커서는 {@code studyGroup.id.lt(cursor)}. 헤더 정보만 반환하며,
     * 각 그룹의 멘토/멤버 상세는 Service 가 {@link #findMemberIdsByStudyGroupIds} / {@link #findMentorIdsByStudyGroupIds} + Member 도메인
     * batch 호출로 합성한다 (cross-domain JOIN 회피).
     *
     * @param scopes 역할 기반 조회 범위. 비어있으면 빈 리스트.
     * @param gisuId 활성 기수 ID
     * @param cursor 직전 페이지 마지막 groupId. 첫 페이지는 null.
     * @param size   조회 사이즈 (hasNext 판단용 +1 포함)
     */
    List<StudyGroupHeaderInfo> findStudyGroupHeaders(
        List<OrganizationRoleScope> scopes, Long gisuId, Long cursor, int size);

    /**
     * 운영진(회장단/파트장) 권한에 따라 활성 기수의 스터디 그룹 이름 목록을 조회한다.
     * <p>
     * {@link #findStudyGroupHeaders} 와 동일한 Scope OR 합성 규칙을 사용하지만, 페이지네이션 없이 (groupId, name) 만 반환 (토글/드롭다운 용도).
     * scopes 가 null/빈 리스트이면 구현체는 즉시 빈 리스트를 반환해야 한다.
     */
    List<StudyGroupNameInfo> findStudyGroupNames(List<OrganizationRoleScope> scopes, Long gisuId);

    /**
     * 역할 Scope 기반 조회 대상이 되는 스터디 그룹의 ID 집합을 반환한다.
     * <p>
     * {@link #findStudyGroupHeaders} 와 동일한 scope 필터링 규칙. Schedule 같은 다른 aggregate 가 "사용자에게 보이는 그룹들" 만 알면 될 때 사용.
     * scope 가 모두 비어 합성 predicate 가 null 이면 빈 집합 반환 (풀스캔 방지).
     */
    Set<Long> findStudyGroupIds(List<OrganizationRoleScope> scopes, Long gisuId);

    /**
     * 여러 스터디 그룹의 멤버 ID 목록을 한 번에 batch 조회. cross-domain JOIN 없이 study_group_member 테이블만 본다.
     *
     * @param groupIds 조회 대상 그룹 ID 들. 비어있으면 빈 맵.
     * @return {groupId → 해당 그룹 memberId 리스트}. 멤버가 없는 그룹은 맵에 키가 없을 수 있음.
     */
    Map<Long, List<Long>> findMemberIdsByStudyGroupIds(Collection<Long> groupIds);

    /**
     * 여러 스터디 그룹의 스터디원을 커서 페이지네이션으로 조회한다 (studyGroupMemberId 오름차순).
     *
     * @param size hasNext 판별이 필요하면 호출 측에서 +1 하여 전달한다.
     */
    List<StudyGroupMemberPageInfo> findStudyGroupMemberPage(Collection<Long> groupIds, Long cursor, int size);

    /**
     * 여러 스터디 그룹의 멘토(파트장) memberId 목록을 한 번에 batch 조회. cross-domain JOIN 없이 study_group_mentor 테이블만 본다.
     *
     * @param groupIds 조회 대상 그룹 ID 들. 비어있으면 빈 맵.
     * @return {groupId → 해당 그룹 멘토 memberId 리스트}. 멘토가 없는 그룹은 맵에 키가 없을 수 있음.
     */
    Map<Long, List<Long>> findMentorIdsByStudyGroupIds(Collection<Long> groupIds);

    /**
     * 특정 기수에서 해당 파트들의 스터디 그룹 ID 목록 조회 (파트장용)
     */
    List<Long> findIdsByGisuIdAndPartIn(Long gisuId, Set<ChallengerPart> parts);

    /**
     * 지정한 기수/파트에서 이미 다른 스터디 그룹에 소속된 멤버 ID를 반환한다.
     * <p>
     * 스터디 그룹 생성/멤버 추가 시 "동일 기수+파트 내 중복 소속" 여부를 서비스가 검증하기 위한 조회이다. 반환 집합이 비어있으면 모두 신규 가입 가능한 멤버이다.
     *
     * @param id                   기수 ID
     * @param part                 파트
     * @param memberIds            검사 대상 memberId 집합
     * @param excludedStudyGroupId 중복 검사에서 제외할 스터디 그룹 ID
     * @return 이미 다른 그룹에 소속된 memberId 집합 (비어있으면 충돌 없음)
     */
    Set<Long> findConflictedMemberIds(
        Long id, ChallengerPart part, Set<Long> memberIds, Long excludedStudyGroupId);
}
