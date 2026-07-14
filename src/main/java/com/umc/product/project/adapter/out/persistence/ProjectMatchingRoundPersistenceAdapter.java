package com.umc.product.project.adapter.out.persistence;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import com.umc.product.project.application.port.out.LoadProjectMatchingRoundPort;
import com.umc.product.project.application.port.out.SaveProjectMatchingRoundPort;
import com.umc.product.project.domain.ProjectMatchingRound;
import com.umc.product.project.domain.exception.ProjectDomainException;
import com.umc.product.project.domain.exception.ProjectErrorCode;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class ProjectMatchingRoundPersistenceAdapter
    implements LoadProjectMatchingRoundPort, SaveProjectMatchingRoundPort {

    private final ProjectMatchingRoundJpaRepository jpaRepository;

    @Override
    public ProjectMatchingRound getById(Long id) {
        return jpaRepository.findById(id)
            .orElseThrow(() -> new ProjectDomainException(ProjectErrorCode.PROJECT_MATCHING_ROUND_NOT_FOUND));
    }

    @Override
    public ProjectMatchingRound getByIdForUpdate(Long id) {
        return jpaRepository.findByIdForUpdate(id)
            .orElseThrow(() -> new ProjectDomainException(ProjectErrorCode.PROJECT_MATCHING_ROUND_NOT_FOUND));
    }

    @Override
    public Optional<ProjectMatchingRound> findById(Long id) {
        return jpaRepository.findById(id);
    }

    @Override
    public List<ProjectMatchingRound> listByIds(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return List.of();
        }
        return jpaRepository.findAllById(new LinkedHashSet<>(ids));
    }

    @Override
    public List<ProjectMatchingRound> batchGetByIds(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return List.of();
        }

        List<Long> distinctIds = new ArrayList<>(new LinkedHashSet<>(ids));
        List<ProjectMatchingRound> rounds = jpaRepository.findAllById(distinctIds);
        if (rounds.size() != distinctIds.size()) {
            throw new ProjectDomainException(ProjectErrorCode.PROJECT_MATCHING_ROUND_NOT_FOUND);
        }

        Map<Long, ProjectMatchingRound> roundMap = rounds.stream()
            .collect(Collectors.toMap(ProjectMatchingRound::getId, Function.identity()));
        return distinctIds.stream()
            .map(roundMap::get)
            .toList();
    }

    @Override
    public List<ProjectMatchingRound> listByChapterId(Long chapterId) {
        return jpaRepository.findAllByChapterIdOrderByStartsAtAsc(chapterId);
    }

    @Override
    public List<ProjectMatchingRound> listAll() {
        return jpaRepository.findAllByOrderByStartsAtAsc();
    }

    @Override
    public List<ProjectMatchingRound> listByFilters(Long gisuId, Long chapterId, Instant time) {
        return jpaRepository.findByFilters(gisuId, chapterId, time);
    }

    @Override
    public List<ProjectMatchingRound> listOpenAt(Long chapterId, Instant time) {
        return jpaRepository.findOpenAt(chapterId, time);
    }

    @Override
    public List<ProjectMatchingRound> listOverlapping(
        Long chapterId, Instant startsAt, Instant decisionDeadline
    ) {
        return jpaRepository.findOverlapping(chapterId, startsAt, decisionDeadline);
    }

    @Override
    public List<ProjectMatchingRound> listOverlappingExceptId(
        Long id, Long chapterId, Instant startsAt, Instant decisionDeadline
    ) {
        return jpaRepository.findOverlappingExceptId(id, chapterId, startsAt, decisionDeadline);
    }

    @Override
    public List<ProjectMatchingRound> listAllNotAutoDecided() {
        return jpaRepository.findAllByAutoDecisionExecutedAtIsNullOrderByDecisionDeadlineAsc();
    }

    @Override
    public ProjectMatchingRound save(ProjectMatchingRound matchingRound) {
        return jpaRepository.save(matchingRound);
    }

    @Override
    public List<ProjectMatchingRound> saveAll(List<ProjectMatchingRound> matchingRounds) {
        return jpaRepository.saveAll(matchingRounds);
    }

    @Override
    public void delete(ProjectMatchingRound matchingRound) {
        jpaRepository.delete(matchingRound);
    }

    @Override
    public void deleteAll(List<ProjectMatchingRound> matchingRounds) {
        jpaRepository.deleteAll(matchingRounds);
    }
}
