package com.umc.product.challenger.adapter.out.persistence;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.springframework.data.jpa.repository.JpaRepository;

import com.umc.product.challenger.domain.Challenger;

public interface ChallengerJpaRepository extends JpaRepository<Challenger, Long> {

    /**
     * memberId와 gisuId로 챌린저 조회
     */
    Optional<Challenger> findByMemberIdAndGisuId(Long memberId, Long gisuId);

    /**
     * memberId로 챌린저 목록 조회
     */
    List<Challenger> findByMemberId(Long memberId);

    boolean existsByMemberId(Long memberId);

    /**
     * 여러 memberId로 챌린저 목록 IN 쿼리 1회 조회
     */
    List<Challenger> findByMemberIdIn(Set<Long> memberIds);

    /**
     * gisuId로 챌린저 목록 조회
     */
    List<Challenger> findByGisuId(Long gisuId);

    /**
     * 여러 gisuId로 챌린저 목록 조회
     */
    List<Challenger> findByGisuIdIn(List<Long> gisuIds);

    Optional<Challenger> findTopByMemberIdOrderByCreatedAtDesc(Long memberId);

    List<Challenger> findByIdIn(Set<Long> ids);

    /**
     * 특정 기수 내에서 여러 멤버 ID 에 해당하는 챌린저들을 일괄 조회한다.
     */
    List<Challenger> findByMemberIdInAndGisuId(Set<Long> memberIds, Long gisuId);
}
