package com.umc.product.project.adapter.out.persistence;

import java.util.Collection;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import com.umc.product.common.domain.enums.ChallengerPart;
import com.umc.product.project.application.port.out.LoadProjectMemberPort;
import com.umc.product.project.application.port.out.SaveProjectMemberPort;
import com.umc.product.project.domain.ProjectMember;
import com.umc.product.project.domain.enums.MatchingType;
import com.umc.product.project.domain.enums.ProjectMemberStatus;
import com.umc.product.project.domain.exception.ProjectDomainException;
import com.umc.product.project.domain.exception.ProjectErrorCode;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class ProjectMemberPersistenceAdapter implements LoadProjectMemberPort, SaveProjectMemberPort {

    private final ProjectMemberJpaRepository repository;
    private final ProjectMemberQueryRepository queryRepository;

    @Override
    public ProjectMember save(ProjectMember member) {
        return repository.save(member);
    }

    @Override
    public List<ProjectMember> saveAll(Collection<ProjectMember> members) {
        return repository.saveAll(members);
    }

    @Override
    public void hardDelete(Long projectMemberId) {
        repository.deleteById(projectMemberId);
    }

    @Override
    public void deleteAllByProjectId(Long projectId) {
        repository.deleteAllByProjectId(projectId);
    }

    @Override
    public List<ProjectMember> listByProjectId(Long projectId) {
        return repository.findByProjectIdAndStatusOrderByCreatedAtAscIdAsc(projectId, ProjectMemberStatus.ACTIVE);
    }

    @Override
    public Map<Long, List<ProjectMember>> listByProjectIds(Collection<Long> projectIds) {
        return repository.findByProjectIdInAndStatusOrderByCreatedAtAscIdAsc(projectIds, ProjectMemberStatus.ACTIVE)
            .stream()
            .collect(Collectors.groupingBy(pm -> pm.getProject().getId()));
    }

    @Override
    public List<ProjectMember> listByProjectIdAndPart(Long projectId, ChallengerPart part) {
        return repository.findByProjectIdAndPartAndStatusOrderByCreatedAtAscIdAsc(
            projectId, part, ProjectMemberStatus.ACTIVE);
    }

    @Override
    public Map<Long, List<ProjectMember>> listByProjectIdsAndPartGroupedByProjectId(
        Collection<Long> projectIds, ChallengerPart part
    ) {
        return queryRepository.listByProjectIdsAndPartGroupedByProjectId(projectIds, part);
    }

    @Override
    public Map<ChallengerPart, Long> countByProjectIdGroupByPart(Long projectId) {
        List<Object[]> rows = repository.countByProjectIdGroupByPartRaw(projectId, ProjectMemberStatus.ACTIVE);

        Map<ChallengerPart, Long> result = new EnumMap<>(ChallengerPart.class);
        for (Object[] row : rows) {
            result.put((ChallengerPart) row[0], (Long) row[1]);
        }
        return result;
    }

    @Override
    public Map<Long, Map<ChallengerPart, Long>> countByProjectIdsGroupByProjectIdAndPart(
        Collection<Long> projectIds
    ) {
        return queryRepository.countByProjectIdsGroupByProjectIdAndPart(projectIds);
    }

    @Override
    public Optional<ProjectMember> findByProjectIdAndMemberId(Long projectId, Long memberId) {
        return repository.findByProjectIdAndMemberId(projectId, memberId);
    }

    @Override
    public ProjectMember getByProjectIdAndMemberId(Long projectId, Long memberId) {
        return repository.findByProjectIdAndMemberId(projectId, memberId)
            .orElseThrow(() -> new ProjectDomainException(ProjectErrorCode.PROJECT_MEMBER_NOT_FOUND));
    }

    @Override
    public boolean existsByGisuAndMember(Long gisuId, Long memberId) {
        return repository.existsByProject_GisuIdAndMemberIdAndStatus(gisuId, memberId, ProjectMemberStatus.ACTIVE);
    }

    @Override
    public boolean isActivePlanMember(Long projectId, Long memberId) {
        return repository.existsByProjectIdAndMemberIdAndPartAndStatus(
            projectId, memberId, ChallengerPart.PLAN, ProjectMemberStatus.ACTIVE
        );
    }

    @Override
    public List<Long> listProjectIdsByActivePlanMember(Collection<Long> projectIds, Long memberId) {
        return queryRepository.listProjectIdsByActivePlanMember(projectIds, memberId);
    }

    @Override
    public Optional<ProjectMember> findActiveWithoutApplicationByMemberIdAndGisuIdAndMatchingType(
        Long memberId, Long gisuId, MatchingType matchingType
    ) {
        return queryRepository.findActiveWithoutApplicationByMemberIdAndGisuIdAndMatchingType(
            memberId, gisuId, matchingType);
    }
}
