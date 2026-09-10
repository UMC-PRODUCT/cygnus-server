package com.umc.product.organization.adapter.out.persistence.studygroup;


import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.springframework.stereotype.Component;

import com.umc.product.common.domain.enums.ChallengerPart;
import com.umc.product.common.domain.enums.ChallengerTrack;
import com.umc.product.organization.application.port.in.query.dto.OrganizationRoleScope;
import com.umc.product.organization.application.port.in.query.dto.studygroup.StudyGroupHeaderInfo;
import com.umc.product.organization.application.port.in.query.dto.studygroup.StudyGroupMemberPageInfo;
import com.umc.product.organization.application.port.in.query.dto.studygroup.StudyGroupNameInfo;
import com.umc.product.organization.application.port.out.command.SaveStudyGroupPort;
import com.umc.product.organization.application.port.out.query.LoadStudyGroupPort;
import com.umc.product.organization.domain.StudyGroup;
import com.umc.product.organization.exception.OrganizationDomainException;
import com.umc.product.organization.exception.OrganizationErrorCode;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class StudyGroupPersistenceAdapter implements SaveStudyGroupPort, LoadStudyGroupPort {

    private final StudyGroupJpaRepository studyGroupJpaRepository;
    private final StudyGroupQueryRepository studyGroupQueryRepository;

    @Override
    public StudyGroup getEntityById(Long id) {
        return findEntityById(id).orElseThrow(
            () -> new OrganizationDomainException(OrganizationErrorCode.STUDY_GROUP_NOT_FOUND));
    }

    @Override
    public Optional<StudyGroup> findEntityById(Long id) {
        return studyGroupQueryRepository.findEntityById(id);
    }

    @Override
    public Optional<StudyGroup> findEntityByMemberIdAndGisuIdAndPart(
        Long memberId,
        Long gisuId,
        ChallengerPart part
    ) {
        return studyGroupQueryRepository.findEntityByMemberIdAndGisuIdAndPart(memberId, gisuId, part);
    }

    @Override
    public Optional<StudyGroup> findEntityByMemberIdAndGisuIdAndTrack(
        Long memberId,
        Long gisuId,
        ChallengerTrack track
    ) {
        return studyGroupQueryRepository.findEntityByMemberIdAndGisuIdAndTrack(memberId, gisuId, track);
    }

    @Override
    public StudyGroup getByName(String name) {
        return studyGroupJpaRepository.findByName(name).orElseThrow(
            () -> new OrganizationDomainException(OrganizationErrorCode.STUDY_GROUP_NOT_FOUND));
    }

    /**
     * 역할 Scope 기반 헤더 목록 조회로 위임. scopes 비어있으면 풀스캔 방지를 위해 즉시 빈 리스트.
     */
    @Override
    public List<StudyGroupHeaderInfo> findStudyGroupHeaders(
        List<OrganizationRoleScope> scopes, Long gisuId,
        Long cursor, int size
    ) {
        if (scopes == null || scopes.isEmpty()) {
            return List.of();
        }
        return studyGroupQueryRepository.findStudyGroupHeaders(scopes, gisuId, cursor, size);
    }

    @Override
    public List<StudyGroupNameInfo> findStudyGroupNames(List<OrganizationRoleScope> scopes, Long gisuId) {
        if (scopes == null || scopes.isEmpty()) {
            return List.of();
        }
        return studyGroupQueryRepository.findStudyGroupNames(scopes, gisuId);
    }

    @Override
    public Set<Long> findStudyGroupIds(List<OrganizationRoleScope> scopes, Long gisuId) {
        if (scopes == null || scopes.isEmpty()) {
            return Set.of();
        }
        return studyGroupQueryRepository.findStudyGroupIds(scopes, gisuId);
    }

    @Override
    public Map<Long, List<Long>> findMemberIdsByStudyGroupIds(Collection<Long> groupIds) {
        if (groupIds == null || groupIds.isEmpty()) {
            return Map.of();
        }
        return studyGroupQueryRepository.findMemberIdsByStudyGroupIds(groupIds);
    }

    @Override
    public List<StudyGroupMemberPageInfo> findStudyGroupMemberPage(
        Collection<Long> groupIds, Long cursor, int size
    ) {
        if (groupIds == null || groupIds.isEmpty()) {
            return List.of();
        }
        return studyGroupQueryRepository.findStudyGroupMemberPage(groupIds, cursor, size);
    }

    @Override
    public Map<Long, List<Long>> findMentorIdsByStudyGroupIds(Collection<Long> groupIds) {
        if (groupIds == null || groupIds.isEmpty()) {
            return Map.of();
        }
        return studyGroupQueryRepository.findMentorIdsByStudyGroupIds(groupIds);
    }

    @Override
    public List<Long> findIdsByGisuIdAndPartIn(Long gisuId, Set<ChallengerPart> parts) {
        if (parts == null || parts.isEmpty()) {
            return List.of();
        }
        return studyGroupJpaRepository.findIdsByGisuIdAndPartIn(gisuId, parts);
    }

    @Override
    public Set<Long> findConflictedMemberIds(
        Long gisuId, ChallengerPart part, Set<Long> memberIds, Long excludedStudyGroupId
    ) {
        if (memberIds == null || memberIds.isEmpty()) {
            return Set.of();
        }

        return studyGroupQueryRepository.findConflictedMemberIds(gisuId, part, memberIds, excludedStudyGroupId);
    }

    @Override
    public Set<Long> findConflictedTrackMemberIds(
        Long gisuId, ChallengerTrack track, Set<Long> memberIds, Long excludedStudyGroupId
    ) {
        if (memberIds == null || memberIds.isEmpty()) {
            return Set.of();
        }

        return studyGroupQueryRepository.findConflictedTrackMemberIds(gisuId, track, memberIds, excludedStudyGroupId);
    }

    @Override
    public StudyGroup save(StudyGroup studyGroup) {
        return studyGroupJpaRepository.save(studyGroup);
    }

    @Override
    public void delete(StudyGroup studyGroup) {
        studyGroupJpaRepository.delete(studyGroup);
    }
}
