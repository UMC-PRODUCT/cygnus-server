package com.umc.product.recruiting.adapter.out.persistence;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.umc.product.recruiting.domain.RecruitingApplication;
import com.umc.product.recruiting.domain.enums.RecruitingApplicationStatus;

public interface RecruitingApplicationJpaRepository extends JpaRepository<RecruitingApplication, Long> {

    Optional<RecruitingApplication> findByApplicationNo(String applicationNo);

    List<RecruitingApplication> findAllByRound_IdOrderBySubmittedAtAscIdAsc(Long roundId);

    List<RecruitingApplication> findAllByRound_IdAndStatusInOrderBySubmittedAtAscIdAsc(
        Long roundId,
        Collection<RecruitingApplicationStatus> statuses
    );
}
