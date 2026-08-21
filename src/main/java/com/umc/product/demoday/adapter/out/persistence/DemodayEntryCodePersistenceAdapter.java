package com.umc.product.demoday.adapter.out.persistence;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Component;

import com.umc.product.demoday.application.port.out.LoadDemodayEntryCodePort;
import com.umc.product.demoday.application.port.out.SaveDemodayEntryCodePort;
import com.umc.product.demoday.domain.DemodayEntryCode;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class DemodayEntryCodePersistenceAdapter
    implements LoadDemodayEntryCodePort, SaveDemodayEntryCodePort {

    private final DemodayEntryCodeJpaRepository repository;

    @Override
    public Optional<DemodayEntryCode> findById(Long entryCodeId) {
        return repository.findById(entryCodeId);
    }

    @Override
    public Optional<DemodayEntryCode> findByCodeHash(String codeHash) {
        return repository.findByCodeHash(codeHash);
    }

    @Override
    public Optional<DemodayEntryCode> findByCodeHashForRedemption(String codeHash) {
        return repository.findByCodeHashForUpdate(codeHash);
    }

    @Override
    public List<DemodayEntryCode> listRedeemedByPollId(Long pollId) {
        return repository.findAllByPollIdAndRedeemedAtIsNotNullOrderByRedeemedAtAscIdAsc(pollId);
    }

    @Override
    public DemodayEntryCode save(DemodayEntryCode entryCode) {
        return repository.save(entryCode);
    }

    @Override
    public List<DemodayEntryCode> saveAll(List<DemodayEntryCode> entryCodes) {
        return repository.saveAll(entryCodes);
    }
}
