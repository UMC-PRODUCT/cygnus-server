package com.umc.product.challenger.adapter.out.persistence;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.umc.product.challenger.domain.ChallengerRecord;

import jakarta.persistence.LockModeType;

public interface ChallengerRecordJpaRepository extends JpaRepository<ChallengerRecord, Long> {

    Optional<ChallengerRecord> findByCode(String code);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select record from ChallengerRecord record where record.code = :code")
    Optional<ChallengerRecord> findByCodeForUpdate(@Param("code") String code);

    boolean existsByCode(String code);

    List<ChallengerRecord> findBySchoolId(Long schoolId);

    List<ChallengerRecord> findByChapterId(Long chapterId);
}
