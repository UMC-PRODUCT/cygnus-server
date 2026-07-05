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

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class ChallengerWorkbookPersistenceAdapter implements LoadChallengerWorkbookPort, SaveChallengerWorkbookPort {

    private final ChallengerWorkbookJpaRepository challengerWorkbookJpaRepository;

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
    public ChallengerWorkbook save(ChallengerWorkbook challengerWorkbook) {
        return challengerWorkbookJpaRepository.save(challengerWorkbook);
    }

    @Override
    public void delete(ChallengerWorkbook challengerWorkbook) {
        try {
            challengerWorkbookJpaRepository.delete(challengerWorkbook);
            challengerWorkbookJpaRepository.flush();
        } catch (DataIntegrityViolationException e) {
            throw new CurriculumDomainException(
                CurriculumErrorCode.WORKBOOK_HAS_SUBMISSIONS,
                "챌린저 워크북에 연결된 미션 제출 또는 피드백이 있어 삭제할 수 없어요."
            );
        }
    }
}
