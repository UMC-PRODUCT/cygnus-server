package com.umc.product.demoday.adapter.out.persistence;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.umc.product.demoday.domain.DemodayVote;

import jakarta.persistence.LockModeType;

public interface DemodayVoteJpaRepository extends JpaRepository<DemodayVote, Long> {

    Optional<DemodayVote> findByPollIdAndMemberId(Long pollId, Long memberId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select vote from DemodayVote vote where vote.pollId = :pollId and vote.id = :voteId")
    Optional<DemodayVote> findByPollIdAndIdForUpdate(
        @Param("pollId") Long pollId,
        @Param("voteId") Long voteId
    );

    Optional<DemodayVote> findByEntryCodeId(Long entryCodeId);

    List<DemodayVote> findAllByPollId(Long pollId, Sort sort);
}
