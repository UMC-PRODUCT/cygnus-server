package com.umc.product.recruiting.adapter.out.persistence;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.umc.product.recruiting.domain.RecruitingApplicationForm;
import com.umc.product.recruiting.domain.enums.RecruitingApplicationFormStatus;

public interface RecruitingApplicationFormJpaRepository extends JpaRepository<RecruitingApplicationForm, Long> {

    Optional<RecruitingApplicationForm> findByRound_IdAndFormId(Long roundId, Long formId);

    Optional<RecruitingApplicationForm> findFirstByFormIdOrderByIdAsc(Long formId);

    List<RecruitingApplicationForm> findAllByRound_IdOrderByIdAsc(Long roundId);

    @Query("""
        select applicationForm
        from RecruitingApplicationForm applicationForm
        join fetch applicationForm.round round
        join fetch round.season
        where round.id in :roundIds
          and applicationForm.status = :status
        order by round.roundNo asc, applicationForm.id asc
        """)
    List<RecruitingApplicationForm> findAllByRoundIdsAndStatus(
        @Param("roundIds") List<Long> roundIds,
        @Param("status") RecruitingApplicationFormStatus status
    );
}
