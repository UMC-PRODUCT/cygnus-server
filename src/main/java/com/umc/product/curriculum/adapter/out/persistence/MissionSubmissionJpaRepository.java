package com.umc.product.curriculum.adapter.out.persistence;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.umc.product.curriculum.domain.MissionSubmission;

import jakarta.persistence.LockModeType;

public interface MissionSubmissionJpaRepository extends JpaRepository<MissionSubmission, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT s FROM MissionSubmission s WHERE s.id = :missionSubmissionId")
    Optional<MissionSubmission> findByIdForUpdate(@Param("missionSubmissionId") Long missionSubmissionId);

    List<MissionSubmission> findByChallengerWorkbook_IdAndWithdrawnAtIsNull(Long challengerWorkbookId);

    List<MissionSubmission> findByChallengerWorkbook_IdInAndWithdrawnAtIsNull(List<Long> challengerWorkbookIds);

    boolean existsByOriginalWorkbookMission_Id(Long originalWorkbookMissionId);

    boolean existsByOriginalWorkbookMission_IdAndChallengerWorkbook_Id(
        Long originalWorkbookMissionId,
        Long challengerWorkbookId
    );

    boolean existsByChallengerWorkbook_Id(Long challengerWorkbookId);
}
