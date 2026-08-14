package com.umc.product.demoday.adapter.out.persistence;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Component;

import com.umc.product.demoday.application.port.out.LoadDemodayVotePort;
import com.umc.product.demoday.application.port.out.SaveDemodayVotePort;
import com.umc.product.demoday.domain.DemodayVote;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class DemodayVotePersistenceAdapter implements LoadDemodayVotePort, SaveDemodayVotePort {

    private final DemodayVoteJpaRepository repository;

    @Override
    public Optional<DemodayVote> findById(Long voteId) {
        return repository.findById(voteId);
    }

    @Override
    public Optional<DemodayVote> findByPollIdAndMemberId(Long pollId, Long memberId) {
        return repository.findByPollIdAndMemberId(pollId, memberId);
    }

    @Override
    public Optional<DemodayVote> findByEntryCodeId(Long entryCodeId) {
        return repository.findByEntryCodeId(entryCodeId);
    }

    @Override
    public List<DemodayVote> listByPollId(Long pollId) {
        return repository.findAllByPollIdOrderByIdDesc(pollId);
    }

    @Override
    public DemodayVote save(DemodayVote vote) {
        return repository.save(vote);
    }
}
