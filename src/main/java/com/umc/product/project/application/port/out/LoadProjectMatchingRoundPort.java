package com.umc.product.project.application.port.out;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import com.umc.product.project.domain.ProjectMatchingRound;

public interface LoadProjectMatchingRoundPort {

    ProjectMatchingRound getById(Long id);

    ProjectMatchingRound getByIdForUpdate(Long id);

    Optional<ProjectMatchingRound> findById(Long id);

    List<ProjectMatchingRound> listByIds(List<Long> ids);

    List<ProjectMatchingRound> batchGetByIds(List<Long> ids);

    List<ProjectMatchingRound> listByChapterId(Long chapterId);

    List<ProjectMatchingRound> listAll();

    List<ProjectMatchingRound> listByFilters(Long gisuId, Long chapterId, Instant time);

    List<ProjectMatchingRound> listOpenAt(Long chapterId, Instant time);

    List<ProjectMatchingRound> listOverlapping(Long chapterId, Instant startsAt, Instant decisionDeadline);

    List<ProjectMatchingRound> listOverlappingExceptId(
        Long id, Long chapterId, Instant startsAt, Instant decisionDeadline
    );

    /**
     * 자동 선발이 아직 실행되지 않은 매칭 차수 전체를 조회합니다 (deadline 경과 여부 무관).
     * <p>
     * 동적 스케줄링 컴포넌트가 부팅 시 미처리 round 를 모두 등록하기 위해 사용합니다.
     */
    List<ProjectMatchingRound> listAllNotAutoDecided();
}
