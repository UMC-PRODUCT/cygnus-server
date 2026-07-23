package com.umc.product.challenger.adapter.out.persistence;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.springframework.stereotype.Component;

import com.umc.product.challenger.application.port.out.LoadChallengerPointPort;
import com.umc.product.challenger.application.port.out.SaveChallengerPointPort;
import com.umc.product.challenger.domain.ChallengerPoint;
import com.umc.product.challenger.domain.exception.ChallengerDomainException;
import com.umc.product.challenger.domain.exception.ChallengerErrorCode;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class ChallengerPointPersistenceAdapter implements LoadChallengerPointPort, SaveChallengerPointPort {

    private final ChallengerPointJpaRepository repository;
    private final ChallengerPointQueryRepository queryRepository;

    @Override
    public List<ChallengerPoint> findByChallengerId(Long challengerId) {
        return queryRepository.findAllByChallenger(challengerId);
    }

    @Override
    public List<ChallengerPoint> findByChallengerIdIn(Set<Long> challengerIds) {
        return queryRepository.findAllByChallengerIdIn(challengerIds);
    }

    @Override
    public Optional<ChallengerPoint> findById(Long id) {
        return repository.findById(id);
    }

    @Override
    public ChallengerPoint getById(Long id) {
        return repository.findById(id)
            .orElseThrow(() -> new ChallengerDomainException(ChallengerErrorCode.CHALLENGER_POINT_NOT_FOUND));
    }

    @Override
    public ChallengerPoint save(ChallengerPoint challengerPoint) {
        return repository.save(challengerPoint);
    }

    @Override
    public List<ChallengerPoint> saveAll(List<ChallengerPoint> challengerPoints) {
        return repository.saveAll(challengerPoints);
    }

    @Override
    public void deleteAllByChallengerId(Long challengerId) {
        repository.deleteAllByChallengerId(challengerId);
    }

    @Override
    public void delete(ChallengerPoint challengerPoint) {
        repository.delete(challengerPoint);
    }
}
