package com.umc.product.recruiting.adapter.out.persistence;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.springframework.data.jpa.repository.JpaRepository;

import com.umc.product.recruiting.domain.RecruitingRoundEvaluator;
public interface RecruitingRoundEvaluatorJpaRepository extends JpaRepository<RecruitingRoundEvaluator, Long> {

    Optional<RecruitingRoundEvaluator> findByRound_IdAndMemberId(Long roundId, Long memberId);

    List<RecruitingRoundEvaluator> findAllByRound_IdOrderByMemberIdAscIdAsc(Long roundId);

    List<RecruitingRoundEvaluator> findAllByRound_IdInOrderByRound_IdAscMemberIdAscIdAsc(Set<Long> roundIds);

    boolean existsByRound_IdAndMemberId(Long roundId, Long memberId);

    void deleteAllByRound_Id(Long roundId);
}
