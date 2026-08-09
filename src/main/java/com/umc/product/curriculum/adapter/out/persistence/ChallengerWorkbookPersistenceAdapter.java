package com.umc.product.curriculum.adapter.out.persistence;

import java.util.List;
import java.util.Optional;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;

import com.umc.product.curriculum.application.port.out.LoadChallengerWorkbookPort;
import com.umc.product.curriculum.application.port.out.SaveChallengerWorkbookPort;
import com.umc.product.curriculum.domain.ChallengerWorkbook;
import com.umc.product.curriculum.domain.exception.CurriculumDomainException;
import com.umc.product.curriculum.domain.exception.CurriculumErrorCode;
import com.umc.product.global.persistence.ConstraintViolationInspector;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class ChallengerWorkbookPersistenceAdapter implements LoadChallengerWorkbookPort, SaveChallengerWorkbookPort {

    private static final String UNIQUE_MEMBER_WORKBOOK_CONSTRAINT =
        "uk_challenger_workbook_member_id_original_workbook_id";
    private static final String SUBMISSION_WORKBOOK_FOREIGN_KEY =
        "fk_mission_submission_on_challenger_workbook";

    private final ChallengerWorkbookJpaRepository challengerWorkbookJpaRepository;
    private final ChallengerWorkbookQueryRepository challengerWorkbookQueryRepository;

    @Override
    public ChallengerWorkbook getById(Long id) {
        return challengerWorkbookJpaRepository.findById(id)
            .orElseThrow(() -> new CurriculumDomainException(CurriculumErrorCode.CHALLENGER_WORKBOOK_NOT_FOUND));
    }

    @Override
    public Optional<ChallengerWorkbook> findByMemberIdAndOriginalWorkbookId(Long memberId, Long originalWorkbookId) {
        return challengerWorkbookJpaRepository.findByMemberIdAndOriginalWorkbookId(memberId, originalWorkbookId);
    }

    @Override
    public boolean existsByOriginalWorkbookId(Long originalWorkbookId) {
        return challengerWorkbookJpaRepository.existsByOriginalWorkbookId(originalWorkbookId);
    }

    @Override
    public List<ChallengerWorkbook> listByMemberIdAndOriginalWorkbookIdIn(
        Long memberId,
        List<Long> originalWorkbookIds
    ) {
        if (originalWorkbookIds.isEmpty()) {
            return List.of();
        }
        return challengerWorkbookJpaRepository.findByMemberIdAndOriginalWorkbookIdIn(memberId, originalWorkbookIds);
    }

    @Override
    public List<ChallengerWorkbook> listByLookupKeys(List<ChallengerWorkbookLookupKey> keys) {
        return challengerWorkbookQueryRepository.findByLookupKeys(keys);
    }

    @Override
    public ChallengerWorkbook save(ChallengerWorkbook challengerWorkbook) {
        try {
            return challengerWorkbookJpaRepository.saveAndFlush(challengerWorkbook);
        } catch (DataIntegrityViolationException e) {
            if (ConstraintViolationInspector.matches(e, UNIQUE_MEMBER_WORKBOOK_CONSTRAINT)) {
                throw new CurriculumDomainException(CurriculumErrorCode.CHALLENGER_WORKBOOK_ALREADY_EXISTS, e);
            }
            throw e;
        }
    }

    @Override
    public void delete(ChallengerWorkbook challengerWorkbook) {
        try {
            challengerWorkbookJpaRepository.delete(challengerWorkbook);
            challengerWorkbookJpaRepository.flush();
        } catch (DataIntegrityViolationException e) {
            if (ConstraintViolationInspector.matches(e, SUBMISSION_WORKBOOK_FOREIGN_KEY)) {
                throw new CurriculumDomainException(CurriculumErrorCode.WORKBOOK_HAS_SUBMISSIONS, e);
            }
            throw e;
        }
    }
}
