package com.umc.product.recruiting.adapter.out.persistence;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.umc.product.recruiting.domain.RecruitingRound;
import com.umc.product.recruiting.domain.enums.RecruitingRoundType;

public interface RecruitingRoundJpaRepository extends JpaRepository<RecruitingRound, Long> {

    List<RecruitingRound> findAllBySeason_IdOrderByRoundNoAscIdAsc(Long seasonId);

    boolean existsBySeason_IdAndTypeAndRoundNo(Long seasonId, RecruitingRoundType type, Integer roundNo);
}
