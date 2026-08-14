package com.umc.product.demoday.adapter.out.persistence;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;

import com.umc.product.demoday.application.port.out.LoadDemodayBoothPort;
import com.umc.product.demoday.application.port.out.SaveDemodayBoothPort;
import com.umc.product.demoday.domain.DemodayBooth;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class DemodayBoothPersistenceAdapter implements LoadDemodayBoothPort, SaveDemodayBoothPort {

    private static final Sort BOOTH_ORDER = Sort.by(Sort.Order.asc("id"));

    private final DemodayBoothJpaRepository repository;

    @Override
    public Optional<DemodayBooth> findById(Long boothId) {
        return repository.findById(boothId);
    }

    @Override
    public List<DemodayBooth> listByPollId(Long pollId) {
        return repository.findAllByPollId(pollId, BOOTH_ORDER);
    }

    @Override
    public DemodayBooth save(DemodayBooth booth) {
        return repository.save(booth);
    }

    @Override
    public List<DemodayBooth> saveAll(List<DemodayBooth> booths) {
        return repository.saveAll(booths);
    }
}
