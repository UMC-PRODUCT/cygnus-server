package com.umc.product.demoday.adapter.out.persistence;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.umc.product.demoday.domain.DemodayVote;

public interface DemodayVoteJpaRepository extends JpaRepository<DemodayVote, Long> {

    Optional<DemodayVote> findByPollIdAndMemberId(Long pollId, Long memberId);

    Optional<DemodayVote> findByEntryCodeId(Long entryCodeId);

    List<DemodayVote> findAllByPollIdOrderByIdDesc(Long pollId);
}
