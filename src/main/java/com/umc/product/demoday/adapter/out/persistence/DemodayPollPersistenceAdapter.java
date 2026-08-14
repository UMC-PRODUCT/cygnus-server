package com.umc.product.demoday.adapter.out.persistence;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Component;

import com.umc.product.demoday.application.port.out.LoadDemodayPollPort;
import com.umc.product.demoday.application.port.out.SaveDemodayPollPort;
import com.umc.product.demoday.domain.DemodayPoll;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class DemodayPollPersistenceAdapter implements LoadDemodayPollPort, SaveDemodayPollPort {

    private final DemodayPollJpaRepository repository;

    @Override
    public Optional<DemodayPoll> findById(Long pollId) {
        return repository.findById(pollId);
    }

    @Override
    public List<DemodayPoll> listAll() {
        return repository.findAllByOrderByOpensAtDescIdDesc();
    }

    @Override
    public DemodayPoll save(DemodayPoll poll) {
        return repository.save(poll);
    }
}
