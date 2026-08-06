package com.umc.product.recruiting.adapter.out.persistence;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.umc.product.recruiting.domain.RecruitingSeason;

import jakarta.persistence.LockModeType;

public interface RecruitingSeasonJpaRepository extends JpaRepository<RecruitingSeason, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT season FROM RecruitingSeason season WHERE season.id = :id")
    Optional<RecruitingSeason> findByIdForUpdate(@Param("id") Long id);

    Optional<RecruitingSeason> findByGisuIdAndSchoolId(Long gisuId, Long schoolId);

    boolean existsByGisuIdAndSchoolId(Long gisuId, Long schoolId);

    List<RecruitingSeason> findAllByGisuIdOrderBySchoolIdAscIdAsc(Long gisuId);
}
