package com.umc.product.project.adapter.out.persistence;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Component;

import com.umc.product.project.application.port.out.LoadProjectApplicationPort;
import com.umc.product.project.application.port.out.SaveProjectApplicationPort;
import com.umc.product.project.application.port.out.dto.ProjectMemberMatchedRoundInfo;
import com.umc.product.project.domain.ProjectApplication;
import com.umc.product.project.domain.enums.MatchingType;
import com.umc.product.project.domain.enums.ProjectApplicationStatus;
import com.umc.product.project.domain.exception.ProjectDomainException;
import com.umc.product.project.domain.exception.ProjectErrorCode;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class ProjectApplicationPersistenceAdapter implements LoadProjectApplicationPort, SaveProjectApplicationPort {

    private final ProjectApplicationJpaRepository projectApplicationJpaRepository;
    private final ProjectApplicationQueryRepository projectApplicationQueryRepository;

    @Override
    public Optional<ProjectApplication> findById(Long id) {
        return projectApplicationJpaRepository.findById(id);
    }

    @Override
    public boolean existsByAppliedMatchingRoundId(Long matchingRoundId) {
        return projectApplicationJpaRepository.existsByAppliedMatchingRound_Id(matchingRoundId);
    }

    @Override
    public Optional<ProjectApplication> findByProjectIdAndApplicantMemberIdAndStatus(
        Long projectId,
        Long applicantMemberId,
        ProjectApplicationStatus status
    ) {
        return projectApplicationQueryRepository.findByProjectIdAndApplicantMemberIdAndStatus(
            projectId,
            applicantMemberId,
            status
        );
    }

    @Override
    public Optional<ProjectApplication> findByProjectIdAndApplicantMemberIdAndRoundIdAndStatus(
        Long projectId,
        Long applicantMemberId,
        Long roundId,
        ProjectApplicationStatus status
    ) {
        return projectApplicationQueryRepository.findByProjectIdAndApplicantMemberIdAndRoundIdAndStatus(
            projectId,
            applicantMemberId,
            roundId,
            status
        );
    }

    @Override
    public ProjectApplication getDraftByProjectAndMember(Long projectId, Long memberId) {
        return projectApplicationQueryRepository
            .findByProjectIdAndApplicantMemberIdAndStatus(
                projectId,
                memberId,
                ProjectApplicationStatus.DRAFT
            ).orElseThrow(() -> new ProjectDomainException(ProjectErrorCode.PROJECT_DRAFT_APPLICATION_NOT_FOUND));
    }

    @Override
    public boolean existsByRoundAndApplicantAndStatus(
        Long roundId,
        Long applicantMemberId,
        ProjectApplicationStatus status
    ) {
        return projectApplicationQueryRepository.existsByRoundAndApplicantAndStatus(
            roundId,
            applicantMemberId,
            status
        );
    }

    @Override
    public ProjectApplication save(ProjectApplication application) {
        return projectApplicationJpaRepository.save(application);
    }

    @Override
    public List<ProjectApplication> saveAll(Collection<ProjectApplication> applications) {
        return projectApplicationJpaRepository.saveAll(applications);
    }

    @Override
    public List<ProjectApplication> listByMatchingRoundId(Long matchingRoundId) {
        return projectApplicationJpaRepository.findAllByAppliedMatchingRound_Id(matchingRoundId);
    }

    @Override
    public List<ProjectApplication> listDecidableByMatchingRoundIdAndProjectId(Long matchingRoundId, Long projectId) {
        return projectApplicationQueryRepository.listDecidableByMatchingRoundIdAndProjectId(matchingRoundId, projectId);
    }

    @Override
    public Optional<ProjectApplication> findByIdWithDetails(Long applicationId) {
        return projectApplicationQueryRepository.findByIdWithDetails(applicationId);
    }

    @Override
    public List<ProjectApplication> batchGetByIdsWithDetails(Collection<Long> applicationIds) {
        if (applicationIds == null || applicationIds.isEmpty()) {
            return List.of();
        }

        List<ProjectApplication> applications =
            projectApplicationQueryRepository.findAllByIdInWithDetails(applicationIds);
        if (applications.size() != applicationIds.stream().distinct().count()) {
            throw new ProjectDomainException(ProjectErrorCode.PROJECT_APPLICATION_NOT_FOUND);
        }
        return applications;
    }

    @Override
    public List<ProjectApplication> searchMyApplications(
        Long applicantMemberId,
        Long gisuId,
        MatchingType matchingType,
        ProjectApplicationStatus status
    ) {
        return projectApplicationQueryRepository.searchMyApplications(applicantMemberId, gisuId, matchingType, status);
    }

    @Override
    public List<ProjectApplication> searchProjectApplications(
        Long projectId,
        Long matchingRoundId,
        ProjectApplicationStatus status,
        Instant now,
        boolean includeOngoingMatchingRounds
    ) {
        return projectApplicationQueryRepository.searchProjectApplications(
            projectId,
            matchingRoundId,
            status,
            now,
            includeOngoingMatchingRounds
        );
    }

    @Override
    public List<ProjectApplication> searchProjectApplicationsByProjectIds(
        Collection<Long> projectIds,
        Collection<Long> includeOngoingProjectIds,
        Long matchingRoundId,
        ProjectApplicationStatus status,
        Instant now
    ) {
        return projectApplicationQueryRepository.searchProjectApplicationsByProjectIds(
            projectIds,
            includeOngoingProjectIds,
            matchingRoundId,
            status,
            now
        );
    }

    @Override
    public List<ProjectMemberMatchedRoundInfo> listLatestApprovedMatchedRoundsByProjectIdsAndMemberIds(
        Collection<Long> projectIds,
        Collection<Long> memberIds
    ) {
        return projectApplicationQueryRepository.listLatestApprovedMatchedRoundsByProjectIdsAndMemberIds(
            projectIds, memberIds);
    }

    @Override
    public List<ProjectApplication> listInProgressByProjectId(Long projectId) {
        return projectApplicationJpaRepository.findAllByProjectIdAndStatusIn(
            projectId,
            List.of(ProjectApplicationStatus.DRAFT, ProjectApplicationStatus.SUBMITTED)
        );
    }
}
