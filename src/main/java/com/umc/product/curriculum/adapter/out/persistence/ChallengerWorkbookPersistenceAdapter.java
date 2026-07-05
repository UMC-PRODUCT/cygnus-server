package com.umc.product.curriculum.adapter.out.persistence;

import com.umc.product.curriculum.application.port.out.LoadChallengerWorkbookPort;
import com.umc.product.curriculum.application.port.out.SaveChallengerWorkbookPort;
import com.umc.product.curriculum.domain.ChallengerWorkbook;
import com.umc.product.curriculum.domain.exception.CurriculumDomainException;
import com.umc.product.curriculum.domain.exception.CurriculumErrorCode;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

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
    public List<ChallengerWorkbook> findByMemberIdAndOriginalWorkbookIdIn(Long memberId, List<Long> originalWorkbookIds) {
        if (originalWorkbookIds.isEmpty()) {
            return List.of();
        }
        return challengerWorkbookJpaRepository.findByMemberIdAndOriginalWorkbookIdIn(memberId, originalWorkbookIds);
    }

    @Override
    public ChallengerWorkbook save(ChallengerWorkbook challengerWorkbook) {
        return challengerWorkbookJpaRepository.save(challengerWorkbook);
    }
}
