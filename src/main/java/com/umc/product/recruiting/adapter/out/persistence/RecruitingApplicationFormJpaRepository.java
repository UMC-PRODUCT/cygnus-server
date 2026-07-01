package com.umc.product.recruiting.adapter.out.persistence;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.umc.product.recruiting.domain.RecruitingApplicationForm;

public interface RecruitingApplicationFormJpaRepository extends JpaRepository<RecruitingApplicationForm, Long> {

    Optional<RecruitingApplicationForm> findByRound_IdAndFormId(Long roundId, Long formId);

    Optional<RecruitingApplicationForm> findFirstByFormIdOrderByIdAsc(Long formId);

    List<RecruitingApplicationForm> findAllByRound_IdOrderByIdAsc(Long roundId);
}
