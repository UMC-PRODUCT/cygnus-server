package com.umc.product.challenger.adapter.out.persistence;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

import com.umc.product.challenger.application.port.in.query.dto.SearchChallengerQuery;
import com.umc.product.challenger.application.port.out.LoadChallengerPort;
import com.umc.product.challenger.application.port.out.SaveChallengerPort;
import com.umc.product.challenger.application.port.out.SearchChallengerPort;
import com.umc.product.challenger.application.port.out.dto.ChallengerSearchBundle;
import com.umc.product.challenger.domain.Challenger;
import com.umc.product.challenger.domain.exception.ChallengerDomainException;
import com.umc.product.challenger.domain.exception.ChallengerErrorCode;
import com.umc.product.common.domain.enums.ChallengerPart;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class ChallengerPersistenceAdapter implements LoadChallengerPort, SaveChallengerPort, SearchChallengerPort {

    private final ChallengerJpaRepository repository;
    private final ChallengerQueryRepository queryRepository;

    @Override
    public Optional<Challenger> findById(Long id) {
        return repository.findById(id);
    }

    @Override
    public Challenger getById(Long id) {
        return repository.findById(id)
            .orElseThrow(() -> new ChallengerDomainException(ChallengerErrorCode.CHALLENGER_NOT_FOUND));
    }

    @Override
    public Optional<Challenger> findByMemberIdAndGisuId(Long memberId, Long gisuId) {
        return repository.findByMemberIdAndGisuId(memberId, gisuId);
    }

    @Override
    public List<Challenger> getAllByMemberId(Long memberId) {
        return repository.findByMemberId(memberId);
    }

    @Override
    public boolean existsByMemberId(Long memberId) {
        return memberId != null && repository.existsByMemberId(memberId);
    }

    @Override
    public List<Challenger> listAllByMemberIds(Set<Long> memberIds) {
        if (memberIds == null || memberIds.isEmpty()) {
            return List.of();
        }
        return repository.findByMemberIdIn(memberIds);
    }

    @Override
    public List<Challenger> getAllByGisuId(Long gisuId) {
        return repository.findByGisuId(gisuId);
    }

    @Override
    public List<Challenger> getAllByGisuIds(List<Long> gisuIds) {
        return repository.findByGisuIdIn(gisuIds);
    }

    @Override
    public List<Challenger> listByChapterId(Long chapterId) {
        return queryRepository.listByChapterId(chapterId);
    }

    @Override
    public Long countByIdIn(Set<Long> ids) {
        return (long) ids.size();
    }

    @Override
    public Challenger findTopByMemberIdOrderByCreatedAtDesc(Long memberId) {
        return repository.findTopByMemberIdOrderByCreatedAtDesc(memberId)
            .orElseThrow(() -> new ChallengerDomainException(ChallengerErrorCode.CHALLENGER_NOT_FOUND));
    }

    @Override
    public List<Challenger> getAllByIds(Set<Long> ids) {
        return repository.findByIdIn(ids);
    }

    @Override
    public List<Challenger> batchGetByMemberIdsAndGisuId(Set<Long> memberIds, Long gisuId) {
        if (memberIds == null || memberIds.isEmpty()) {
            return List.of();
        }
        List<Challenger> challengers = repository.findByMemberIdInAndGisuId(memberIds, gisuId);
        if (challengers.size() != memberIds.size()) {
            throw new ChallengerDomainException(ChallengerErrorCode.NO_CHALLENGER_IN_MEMBER_GISU);
        }
        return challengers;
    }

    @Override
    public List<Challenger> listByMemberIdsAndGisuId(Set<Long> memberIds, Long gisuId) {
        if (memberIds == null || memberIds.isEmpty() || gisuId == null) {
            return List.of();
        }
        return repository.findByMemberIdInAndGisuId(memberIds, gisuId);
    }

    @Override
    public List<Challenger> findLatestPerMember() {
        return queryRepository.getAllLatestGisuPerMember();
    }

    @Override
    public Page<Challenger> search(SearchChallengerQuery query, Pageable pageable) {
        return queryRepository.pagingSearch(query, pageable);
    }

    @Override
    public List<Challenger> cursorSearch(SearchChallengerQuery query, Long cursor, int size) {
        return queryRepository.cursorSearch(query, cursor, size);
    }

    @Override
    public Map<ChallengerPart, Long> countByPart(SearchChallengerQuery query) {
        return queryRepository.countByPart(query);
    }

    @Override
    public Map<Long, Double> sumPointsByChallengerIds(Set<Long> challengerIds) {
        return queryRepository.sumPointsByChallengerIds(challengerIds);
    }

    @Override
    public ChallengerSearchBundle cursorSearchWithCounts(SearchChallengerQuery query, Long cursor, int size) {
        return queryRepository.cursorSearchWithCounts(query, cursor, size);
    }

    @Override
    public ChallengerSearchBundle pagingSearchWithCounts(SearchChallengerQuery query, Pageable pageable) {
        return queryRepository.pagingSearchWithCounts(query, pageable);
    }

    @Override
    public Challenger save(Challenger challenger) {
        return repository.save(challenger);
    }

    @Override
    public List<Challenger> saveAll(List<Challenger> challengers) {
        return repository.saveAll(challengers);
    }

    @Override
    public void delete(Challenger challenger) {
        repository.delete(challenger);
    }
}
