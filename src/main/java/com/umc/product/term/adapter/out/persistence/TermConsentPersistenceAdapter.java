package com.umc.product.term.adapter.out.persistence;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Component;

import com.umc.product.term.application.port.out.LoadTermConsentPort;
import com.umc.product.term.application.port.out.SaveTermConsentPort;
import com.umc.product.term.domain.TermConsent;
import com.umc.product.term.domain.enums.TermType;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class TermConsentPersistenceAdapter implements LoadTermConsentPort, SaveTermConsentPort {

    private final TermConsentRepository repository;

    @Override
    public List<TermConsent> findByMemberId(Long memberId) {
        return repository.findByMemberId(memberId);
    }

    @Override
    public List<TermConsent> listByMemberIdAndTermIds(Long memberId, List<Long> termIds) {
        return repository.findByMemberIdAndTermIdIn(memberId, termIds);
    }

    @Override
    public Optional<TermConsent> findByMemberIdAndTermType(Long memberId, TermType termType) {
        return repository.findByMemberIdAndTermType(memberId, termType);
    }

    @Override
    public Optional<TermConsent> findByMemberIdAndTermId(Long memberId, Long termId) {
        return repository.findByMemberIdAndTermId(memberId, termId);
    }

    @Override
    public boolean existsByMemberIdAndTermType(Long memberId, TermType termType) {
        return repository.existsByMemberIdAndTermType(memberId, termType);
    }

    @Override
    public boolean existsByMemberIdAndTermId(Long memberId, Long termId) {
        return repository.existsByMemberIdAndTermId(memberId, termId);
    }

    @Override
    public TermConsent save(TermConsent termConsent) {
        return repository.save(termConsent);
    }

    @Override
    public boolean saveIfAbsent(TermConsent termConsent) {
        return repository.insertIfAbsent(
            termConsent.getMemberId(),
            termConsent.getTermId(),
            termConsent.getTermType().name(),
            termConsent.getAgreedAt()
        ) == 1;
    }

    @Override
    public void delete(TermConsent termConsent) {
        repository.delete(termConsent);
    }
}
