package com.umc.product.demoday.application.port.out;

import java.util.List;
import java.util.Optional;

import com.umc.product.demoday.domain.DemodayVote;

public interface LoadDemodayVotePort {

    Optional<DemodayVote> findById(Long voteId);

    Optional<DemodayVote> findByIdInPollForUpdate(Long pollId, Long voteId);

    Optional<DemodayVote> findMemberVote(Long pollId, Long memberId);

    Optional<DemodayVote> findVisitorVote(Long entryCodeId);

    List<DemodayVote> listVotes(Long pollId);
}
