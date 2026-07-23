package com.umc.product.term.adapter.out.persistence;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Component;

import com.umc.product.term.application.port.out.LoadTermPort;
import com.umc.product.term.application.port.out.SaveTermPort;
import com.umc.product.term.domain.Term;
import com.umc.product.term.domain.enums.TermType;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class TermPersistenceAdapter implements LoadTermPort, SaveTermPort {

    private final TermRepository repository;
    private final TermQueryRepository queryRepository;

    @Override
    public Optional<Term> findById(Long id) {
        return repository.findById(id);
    }

    @Override
    public Optional<Term> findByIdWithSharedLock(Long id) {
        return repository.findByIdWithSharedLock(id);
    }

    @Override
    public Optional<Term> findActiveByType(TermType type) {
        return queryRepository.findActiveByType(type);
    }

    @Override
    public List<Term> listActive() {
        return repository.findAllByActiveIsTrueOrderByIdAsc();
    }

    @Override
    public boolean existsById(Long id) {
        return repository.existsById(id);
    }

    @Override
    public List<Term> findAllActiveByTypes(List<TermType> types) {
        return repository.findAllByTypeInAndActiveIsTrue(types);
    }

    @Override
    public List<Term> listByIds(List<Long> ids) {
        return repository.findAllById(ids);
    }

    public List<Term> findAllActiveRequired() {
        return queryRepository.findAllActiveRequired();
    }

    @Override
    public Term save(Term term) {
        return repository.save(term);
    }

    @Override
    public void delete(Term term) {
        repository.delete(term);
    }
}
