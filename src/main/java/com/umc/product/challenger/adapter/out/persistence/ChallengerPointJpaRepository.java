package com.umc.product.challenger.adapter.out.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.umc.product.challenger.domain.ChallengerPoint;

public interface ChallengerPointJpaRepository extends JpaRepository<ChallengerPoint, Long> {

    @Modifying
    @Query("DELETE FROM ChallengerPoint point WHERE point.challenger.id = :challengerId")
    void deleteAllByChallengerId(@Param("challengerId") Long challengerId);
}
