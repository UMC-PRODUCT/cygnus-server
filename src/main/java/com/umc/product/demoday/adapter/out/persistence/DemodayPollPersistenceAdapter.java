package com.umc.product.demoday.adapter.out.persistence;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;

import com.umc.product.demoday.application.port.out.LoadDemodayPollPort;
import com.umc.product.demoday.application.port.out.SaveDemodayPollPort;
import com.umc.product.demoday.domain.DemodayPoll;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class DemodayPollPersistenceAdapter implements LoadDemodayPollPort, SaveDemodayPollPort {

    private static final Sort POLL_ORDER = Sort.by(
        Sort.Order.desc("opensAt"),
        Sort.Order.desc("id")
    );

    private final DemodayPollJpaRepository repository;

    @Override
    public Optional<DemodayPoll> findById(Long pollId) {
        return repository.findById(pollId);
    }

    @Override
    public List<DemodayPoll> listAll() {
        return repository.findAll(POLL_ORDER);
    }

    @Override
    public DemodayPoll save(DemodayPoll poll) {
        return repository.save(poll);
    }
}
