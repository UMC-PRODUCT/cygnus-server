package com.umc.product.organization.application.port.service.query;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.umc.product.authorization.application.port.in.query.GetChallengerRoleUseCase;
import com.umc.product.common.domain.enums.ChallengerPart;
import com.umc.product.common.domain.enums.ChallengerRoleType;
import com.umc.product.common.domain.enums.ChallengerTrack;
import com.umc.product.member.application.port.in.query.GetMemberUseCase;
import com.umc.product.member.application.port.in.query.dto.MemberInfo;
import com.umc.product.organization.application.port.in.query.GetGisuUseCase;
import com.umc.product.organization.application.port.in.query.GetStudyGroupUseCase;
import com.umc.product.organization.application.port.in.query.dto.OrganizationRoleScope;
import com.umc.product.organization.application.port.in.query.dto.studygroup.StudyGroupHeaderInfo;
import com.umc.product.organization.application.port.in.query.dto.studygroup.StudyGroupInfo;
import com.umc.product.organization.application.port.in.query.dto.studygroup.StudyGroupMemberInfo;
import com.umc.product.organization.application.port.in.query.dto.studygroup.StudyGroupMemberPageInfo;
import com.umc.product.organization.application.port.in.query.dto.studygroup.StudyGroupNameInfo;
import com.umc.product.organization.application.port.in.query.dto.studygroup.StudyGroupWithMemberAndMentorInfo;
import com.umc.product.organization.application.port.out.query.LoadStudyGroupPort;
import com.umc.product.organization.domain.StudyGroup;
import com.umc.product.organization.domain.StudyGroupMember;
import com.umc.product.organization.domain.StudyGroupMentor;
import com.umc.product.organization.exception.OrganizationDomainException;
import com.umc.product.organization.exception.OrganizationErrorCode;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class StudyGroupQueryService implements GetStudyGroupUseCase {

    private final GetGisuUseCase getGisuUseCase;
    private final GetMemberUseCase getMemberUseCase;
    private final GetChallengerRoleUseCase getChallengerRoleUseCase;

    private final LoadStudyGroupPort loadStudyGroupPort;

    /**
     * 내 스터디 그룹 목록 조회
     * <p>
     * 사용자의 활성 기수 역할로 Scope 를 조립하여 권한 범위의 스터디 그룹을 커서 페이지네이션으로 반환한다.
     * <ul>
     *   <li>학교 회장단(SCHOOL_PRESIDENT/VICE) → 해당 학교 멤버가 포함된 모든 스터디 그룹</li>
     *   <li>파트장(SCHOOL_PART_LEADER) → 본인이 mentor 로 등록된 스터디 그룹</li>
     *   <li>일반 챌린저 / 권한 없음 → 빈 리스트</li>
     * </ul>
     * <p>
     * <b>조립 흐름:</b>
     * <ol>
     *   <li>역할 Scope 합성 (없으면 빈 결과)</li>
     *   <li>{@link LoadStudyGroupPort#findStudyGroupHeaders} — 헤더만 페이징 (멤버/멘토 미포함)</li>
     *   <li>{@link LoadStudyGroupPort#findMemberIdsByStudyGroupIds} / {@code findMentorIdsByStudyGroupIds}
     *       — 헤더 groupId 들로 batch 조회 (cross-domain JOIN 없음)</li>
     *   <li>{@link GetMemberUseCase#findAllByIds} — 전체 memberId 집합으로 이름/학교/프로필 batch 합성</li>
     *   <li>헤더별로 mentor/member 리스트를 조립해 {@link StudyGroupWithMemberAndMentorInfo} 생성</li>
     * </ol>
     * fetchSize 는 Controller 의 hasNext 판단 위해 size + 1 로 조회.
     */
    @Override
    public List<StudyGroupWithMemberAndMentorInfo> getMyStudyGroups(Long memberId, Long cursor, int size) {
        return getMyStudyGroups(memberId, cursor, size, null);
    }

    @Override
    public List<StudyGroupWithMemberAndMentorInfo> getMyStudyGroups(
        Long memberId, Long cursor, int size, Long gisuId
    ) {
        Long schoolId = getMemberUseCase.getById(memberId).schoolId();
        Long activeGisuId = gisuId == null ? getGisuUseCase.getActiveGisuId() : getGisuUseCase.getById(gisuId).gisuId();

        List<OrganizationRoleScope> scopes = resolveScopes(memberId, activeGisuId, schoolId);
        if (scopes.isEmpty()) {
            return List.of();
        }

        List<StudyGroupHeaderInfo> headers = loadStudyGroupPort.findStudyGroupHeaders(
            scopes, activeGisuId, cursor, size + 1
        );
        if (headers.isEmpty()) {
            return List.of();
        }

        List<Long> groupIds = headers.stream().map(StudyGroupHeaderInfo::groupId).toList();
        Map<Long, List<Long>> memberIdsByGroup = loadStudyGroupPort.findMemberIdsByStudyGroupIds(groupIds);
        Map<Long, List<Long>> mentorIdsByGroup = loadStudyGroupPort.findMentorIdsByStudyGroupIds(groupIds);

        Map<Long, MemberInfo> memberMap = batchGetMembers(collectAllMemberIds(memberIdsByGroup, mentorIdsByGroup));

        return headers.stream()
            .map(header -> StudyGroupWithMemberAndMentorInfo.create(
                header.groupId(), header.name(),
                header.gisuId(), header.part(), header.createdAt(),
                assembleStudyGroupMembers(header.groupId(),
                    mentorIdsByGroup.getOrDefault(header.groupId(), List.of()), memberMap),
                assembleStudyGroupMembers(header.groupId(),
                    memberIdsByGroup.getOrDefault(header.groupId(), List.of()), memberMap),
                header.track()
            ))
            .toList();
    }

    /**
     * 권한 범위 내 스터디원 목록 (커서 페이지네이션).
     * <p>
     * {@code studyGroupId} 가 주어지면 그 그룹이 권한 범위에 있는지 먼저 확인한다. 범위 밖이면 빈 목록이 아니라 403 으로 끊는다 — 존재하지 않는 그룹과
     * 권한 없는 그룹을 호출 측이 구분할 수 있어야 하기 때문.
     */
    @Override
    public List<StudyGroupMemberPageInfo> getVisibleStudyGroupMembers(
        Long requesterMemberId, Long studyGroupId, Long cursor, int size
    ) {
        return getVisibleStudyGroupMembers(requesterMemberId, studyGroupId, cursor, size, null);
    }

    @Override
    public List<StudyGroupMemberPageInfo> getVisibleStudyGroupMembers(
        Long requesterMemberId, Long studyGroupId, Long cursor, int size, Long gisuId
    ) {
        Long schoolId = getMemberUseCase.getById(requesterMemberId).schoolId();
        Long activeGisuId = gisuId == null ? getGisuUseCase.getActiveGisuId() : getGisuUseCase.getById(gisuId).gisuId();

        List<OrganizationRoleScope> scopes = resolveScopes(requesterMemberId, activeGisuId, schoolId);
        Set<Long> visibleGroupIds = scopes.isEmpty()
            ? Set.of()
            : loadStudyGroupPort.findStudyGroupIds(scopes, activeGisuId);

        if (studyGroupId != null) {
            if (!visibleGroupIds.contains(studyGroupId)) {
                throw new OrganizationDomainException(OrganizationErrorCode.STUDY_GROUP_ACCESS_DENIED);
            }
            return loadStudyGroupPort.findStudyGroupMemberPage(Set.of(studyGroupId), cursor, size);
        }

        if (visibleGroupIds.isEmpty()) {
            return List.of();
        }
        return loadStudyGroupPort.findStudyGroupMemberPage(visibleGroupIds, cursor, size);
    }

    /**
     * memberId 만으로 "사용자에게 보이는 활성 기수 스터디 그룹 ID" 를 반환 (UseCase 표면).
     * <p>
     * Scope 조립과 그룹 조회에 같은 activeGisuId 를 쓰기 위해 기수를 한 번만 읽는다. 두 단계를 나눠 부르면 그 사이에 활성 기수가 바뀌었을 때 scope 와 조회 기수가
     * 어긋날 수 있어 여기서 묶는다.
     */
    @Override
    public Set<Long> findVisibleStudyGroupIds(Long memberId) {
        Long schoolId = getMemberUseCase.getById(memberId).schoolId();
        Long activeGisuId = getGisuUseCase.getActiveGisuId();

        List<OrganizationRoleScope> scopes = resolveScopes(memberId, activeGisuId, schoolId);

        return findStudyGroupIdsByScopes(scopes, activeGisuId);
    }

    /**
     * Scope + gisuId 로 조회 가능한 스터디 그룹 ID 집합 반환 (내부 helper).
     * <p>
     * Scope 가 하나도 없으면 조회할 것이 없으므로 쿼리 없이 빈 Set 을 반환한다 (풀스캔 방지).
     */
    private Set<Long> findStudyGroupIdsByScopes(List<OrganizationRoleScope> scopes, Long gisuId) {
        if (scopes.isEmpty()) {
            return Set.of();
        }
        return loadStudyGroupPort.findStudyGroupIds(scopes, gisuId);
    }

    /**
     * 활성 기수 내 역할을 검사해 조회 Scope 리스트 구성 (내부 helper).
     * <p>
     * 새 역할 추가 시 이 메서드에만 분기 추가. 학교 회장단 Scope 의 학교 멤버 집합이 비어있으면 EXISTS subquery 가 항상 false 이므로 Scope 자체를 생략 (쿼리 비용 절감).
     */
    private List<OrganizationRoleScope> resolveScopes(Long memberId, Long gisuId, Long schoolId) {
        List<OrganizationRoleScope> scopes = new ArrayList<>();

        if (getChallengerRoleUseCase.isSchoolCoreInGisu(memberId, gisuId, schoolId)) {
            Set<Long> schoolMemberIds = getMemberUseCase.listIdsBySchoolId(schoolId);
            if (!schoolMemberIds.isEmpty()) {
                scopes.add(new OrganizationRoleScope.AsSchoolCore(schoolMemberIds));
            }
        }

        if (getChallengerRoleUseCase.hasRoleTypeInGisu(memberId, gisuId, ChallengerRoleType.SCHOOL_PART_LEADER)) {
            scopes.add(new OrganizationRoleScope.AsPartLeader(memberId));
        }

        return scopes;
    }

    /**
     * 권한 기반 스터디 그룹 이름 목록 조회. {@link #getMyStudyGroups} 와 동일한 Scope 규칙을 쓰지만 페이징/멤버 합성 없이 (groupId, name) 만 반환 (드롭다운 용).
     */
    @Override
    public List<StudyGroupNameInfo> getStudyGroupNames(Long memberId) {
        return getStudyGroupNames(memberId, null);
    }

    @Override
    public List<StudyGroupNameInfo> getStudyGroupNames(Long memberId, Long gisuId) {
        Long schoolId = getMemberUseCase.getById(memberId).schoolId();
        Long activeGisuId = gisuId == null ? getGisuUseCase.getActiveGisuId() : getGisuUseCase.getById(gisuId).gisuId();

        List<OrganizationRoleScope> scopes = resolveScopes(memberId, activeGisuId, schoolId);
        if (scopes.isEmpty()) {
            return List.of();
        }

        return loadStudyGroupPort.findStudyGroupNames(scopes, activeGisuId);
    }

    @Override
    public StudyGroupInfo getById(Long studyGroupId) {
        return StudyGroupInfo.from(loadStudyGroupPort.getEntityById(studyGroupId));
    }

    @Override
    public Optional<StudyGroupInfo> findById(Long studyGroupId) {
        return loadStudyGroupPort.findEntityById(studyGroupId)
            .map(StudyGroupInfo::from);
    }

    @Override
    public Optional<StudyGroupInfo> findByMemberIdAndGisuIdAndPart(
        Long memberId,
        Long gisuId,
        ChallengerPart part
    ) {
        return loadStudyGroupPort.findEntityByMemberIdAndGisuIdAndPart(memberId, gisuId, part)
            .map(StudyGroupInfo::from);
    }

    @Override
    public Optional<StudyGroupInfo> findByMemberIdAndGisuIdAndTrack(
        Long memberId,
        Long gisuId,
        ChallengerTrack track
    ) {
        return loadStudyGroupPort.findEntityByMemberIdAndGisuIdAndTrack(memberId, gisuId, track)
            .map(StudyGroupInfo::from);
    }

    /**
     * 스터디 그룹 단건 조회 — Aggregate root 를 fetch join 으로 통째로 로드, Member 도메인 batch 호출로 이름/학교/프로필 합성.
     * Member 가 존재하지 않는 memberId 는 결과에서 제외 (INNER JOIN 의 silent drop 과 동일 동작).
     */
    @Override
    public StudyGroupWithMemberAndMentorInfo getWithMemberAndMentorInfoById(Long studyGroupId) {
        StudyGroup group = loadStudyGroupPort.getEntityById(studyGroupId);

        List<Long> mentorIds = group.getMentors().stream()
            .map(StudyGroupMentor::getMemberId)
            .toList();
        List<Long> memberIds = group.getMembers().stream()
            .map(StudyGroupMember::getMemberId)
            .toList();

        Set<Long> allIds = new HashSet<>(mentorIds.size() + memberIds.size());
        allIds.addAll(mentorIds);
        allIds.addAll(memberIds);
        Map<Long, MemberInfo> memberMap = batchGetMembers(allIds);

        return StudyGroupWithMemberAndMentorInfo.create(
            group.getId(), group.getName(),
            group.getGisuId(), group.getPart(), group.getCreatedAt(),
            assembleStudyGroupMembers(studyGroupId, mentorIds, memberMap),
            assembleStudyGroupMembers(studyGroupId, memberIds, memberMap),
            group.getTrack()
        );
    }

    @Override
    public List<Long> getStudyGroupIdsByParts(Long gisuId, Set<ChallengerPart> parts) {
        return loadStudyGroupPort.findIdsByGisuIdAndPartIn(gisuId, parts);
    }

    /**
     * 스터디 그룹 ID 로 소속 스터디원 목록 조회.
     * Aggregate root 의 자식에서 memberId 들을 얻고 Member 도메인 batch 조회로 이름/학교/프로필 합성.
     */
    @Override
    public List<StudyGroupMemberInfo> getStudyGroupMembers(Long groupId) {
        StudyGroup group = loadStudyGroupPort.getEntityById(groupId);
        List<Long> memberIds = group.getMembers().stream()
            .map(StudyGroupMember::getMemberId)
            .toList();
        if (memberIds.isEmpty()) {
            return List.of();
        }

        Map<Long, MemberInfo> memberMap = batchGetMembers(new HashSet<>(memberIds));
        return assembleStudyGroupMembers(groupId, memberIds, memberMap);
    }

    // ============================================================================
    // Member 도메인 batch 조회 + StudyGroupMemberInfo 조립 (공통 헬퍼)
    // ============================================================================

    /**
     * 여러 그룹의 memberId 들을 평탄화해 batch 조회용 단일 Set 으로 합친다.
     */
    private Set<Long> collectAllMemberIds(
        Map<Long, List<Long>> memberIdsByGroup,
        Map<Long, List<Long>> mentorIdsByGroup
    ) {
        Set<Long> all = new HashSet<>();
        memberIdsByGroup.values().forEach(all::addAll);
        mentorIdsByGroup.values().forEach(all::addAll);
        return all;
    }

    /**
     * Member 도메인의 batch 조회 위임. 입력이 비어있으면 호출 자체를 생략.
     */
    private Map<Long, MemberInfo> batchGetMembers(Set<Long> ids) {
        if (ids.isEmpty()) {
            return Map.of();
        }
        return getMemberUseCase.findAllByIds(ids);
    }

    /**
     * memberId 리스트 + MemberInfo 맵 → StudyGroupMemberInfo 리스트. 맵에 없는 memberId 는 결과에서 제외 (silent drop).
     */
    private List<StudyGroupMemberInfo> assembleStudyGroupMembers(
        Long studyGroupId, List<Long> memberIds, Map<Long, MemberInfo> memberMap
    ) {
        return memberIds.stream()
            .map(memberMap::get)
            .filter(Objects::nonNull)
            .map(m -> StudyGroupMemberInfo.create(
                studyGroupId,
                m.id(), m.name(),
                m.schoolId(), m.schoolName(),
                m.profileImageId(), m.profileImageLink()
            ))
            .toList();
    }
}
