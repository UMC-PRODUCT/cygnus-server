package com.umc.product.demoday.adapter.out.persistence;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;

import com.umc.product.demoday.application.port.out.LoadDemodayVotePort;
import com.umc.product.demoday.application.port.out.SaveDemodayVotePort;
import com.umc.product.demoday.domain.DemodayVote;
import com.umc.product.demoday.domain.exception.DemodayErrorCode;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class DemodayVotePersistenceAdapter implements LoadDemodayVotePort, SaveDemodayVotePort {

    private static final Sort VOTE_ORDER = Sort.by(Sort.Order.desc("id"));
    private static final Map<String, DemodayErrorCode> ERROR_CODES_BY_CONSTRAINT = Map.of(
        "uk_demoday_vote_poll_member", DemodayErrorCode.DEMODAY_VOTE_ALREADY_CAST,
        "uk_demoday_vote_entry_code", DemodayErrorCode.DEMODAY_VOTE_ALREADY_CAST
    );

    private final DemodayVoteJpaRepository repository;

    @Override
    public Optional<DemodayVote> findById(Long voteId) {
        return repository.findById(voteId);
    }

    @Override
    public Optional<DemodayVote> findMemberVote(Long pollId, Long memberId) {
        return repository.findByPollIdAndMemberId(pollId, memberId);
    }

    @Override
    public Optional<DemodayVote> findVisitorVote(Long entryCodeId) {
        return repository.findByEntryCodeId(entryCodeId);
    }

    @Override
    public List<DemodayVote> listVotes(Long pollId) {
        return repository.findAllByPollId(pollId, VOTE_ORDER);
    }

    @Override
    public DemodayVote save(DemodayVote vote) {
        try {
            return repository.saveAndFlush(vote);
        } catch (DataIntegrityViolationException exception) {
            throw DemodayConstraintViolationTranslator.translate(exception, ERROR_CODES_BY_CONSTRAINT);
        }
    }
}
