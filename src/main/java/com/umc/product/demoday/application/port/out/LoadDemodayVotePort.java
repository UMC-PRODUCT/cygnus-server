package com.umc.product.demoday.application.port.out;

import java.util.List;
import java.util.Optional;

import com.umc.product.demoday.domain.DemodayVote;

public interface LoadDemodayVotePort {

    Optional<DemodayVote> findById(Long voteId);

    Optional<DemodayVote> findByPollIdAndMemberId(Long pollId, Long memberId);

    Optional<DemodayVote> findByEntryCodeId(Long entryCodeId);

    List<DemodayVote> listByPollId(Long pollId);
}
