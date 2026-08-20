package com.umc.product.demoday.adapter.out.persistence;

import java.util.Map;

import org.springframework.stereotype.Component;

import com.umc.product.demoday.application.port.out.LoadDemodayDashboardPort;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class DemodayDashboardPersistenceAdapter implements LoadDemodayDashboardPort {

    private final DemodayDashboardQueryRepository queryRepository;

    @Override
    public Map<Long, Long> countActiveVotesByBooth(Long pollId) {
        return queryRepository.countActiveVotesByBooth(pollId);
    }

    @Override
    public Map<Long, Long> countActiveStampsByBooth(Long pollId) {
        return queryRepository.countActiveStampsByBooth(pollId);
    }
}
