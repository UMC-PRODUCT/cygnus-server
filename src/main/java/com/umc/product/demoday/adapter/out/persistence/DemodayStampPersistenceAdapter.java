package com.umc.product.demoday.adapter.out.persistence;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Component;

import com.umc.product.demoday.application.port.out.LoadDemodayStampPort;
import com.umc.product.demoday.application.port.out.SaveDemodayStampPort;
import com.umc.product.demoday.domain.DemodayStamp;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class DemodayStampPersistenceAdapter implements LoadDemodayStampPort, SaveDemodayStampPort {

    private final DemodayStampJpaRepository repository;

    @Override
    public Optional<DemodayStamp> findById(Long stampId) {
        return repository.findById(stampId);
    }

    @Override
    public Optional<DemodayStamp> findByMemberIdAndBoothId(Long memberId, Long boothId) {
        return repository.findByMemberIdAndBoothId(memberId, boothId);
    }

    @Override
    public Optional<DemodayStamp> findByEntryCodeIdAndBoothId(Long entryCodeId, Long boothId) {
        return repository.findByEntryCodeIdAndBoothId(entryCodeId, boothId);
    }

    @Override
    public List<DemodayStamp> listByMemberId(Long memberId) {
        return repository.findAllByMemberIdOrderByCreatedAtDescIdDesc(memberId);
    }

    @Override
    public List<DemodayStamp> listByEntryCodeId(Long entryCodeId) {
        return repository.findAllByEntryCodeIdOrderByCreatedAtDescIdDesc(entryCodeId);
    }

    @Override
    public DemodayStamp save(DemodayStamp stamp) {
        return repository.save(stamp);
    }
}
