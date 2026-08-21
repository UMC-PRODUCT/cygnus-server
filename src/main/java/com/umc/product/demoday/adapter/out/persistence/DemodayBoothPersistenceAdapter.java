package com.umc.product.demoday.adapter.out.persistence;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;

import com.umc.product.demoday.application.port.out.LoadDemodayBoothPort;
import com.umc.product.demoday.application.port.out.SaveDemodayBoothPort;
import com.umc.product.demoday.domain.DemodayBooth;
import com.umc.product.demoday.domain.exception.DemodayErrorCode;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class DemodayBoothPersistenceAdapter implements LoadDemodayBoothPort, SaveDemodayBoothPort {

    private static final Sort BOOTH_ORDER = Sort.by(Sort.Order.asc("boothCode"));
    private static final Map<String, DemodayErrorCode> ERROR_CODES_BY_CONSTRAINT = Map.of(
        "uk_demoday_booth_poll_code", DemodayErrorCode.DEMODAY_BOOTH_CODE_DUPLICATED
    );

    private final DemodayBoothJpaRepository repository;

    @Override
    public Optional<DemodayBooth> findById(Long boothId) {
        return repository.findById(boothId);
    }

    @Override
    public Optional<DemodayBooth> findByStampCredentialHash(String stampCredentialHash) {
        return repository.findByStampCredentialHash(stampCredentialHash);
    }

    @Override
    public List<DemodayBooth> listByPollId(Long pollId) {
        return repository.findAllByPollId(pollId, BOOTH_ORDER);
    }

    @Override
    public DemodayBooth save(DemodayBooth booth) {
        try {
            return repository.saveAndFlush(booth);
        } catch (DataIntegrityViolationException exception) {
            throw DemodayConstraintViolationTranslator.translate(exception, ERROR_CODES_BY_CONSTRAINT);
        }
    }

    @Override
    public List<DemodayBooth> saveAll(List<DemodayBooth> booths) {
        try {
            return repository.saveAllAndFlush(booths);
        } catch (DataIntegrityViolationException exception) {
            throw DemodayConstraintViolationTranslator.translate(exception, ERROR_CODES_BY_CONSTRAINT);
        }
    }
}
