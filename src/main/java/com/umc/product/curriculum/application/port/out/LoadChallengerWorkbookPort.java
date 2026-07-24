package com.umc.product.curriculum.application.port.out;

import java.util.List;
import java.util.Optional;

import com.umc.product.curriculum.domain.ChallengerWorkbook;

public interface LoadChallengerWorkbookPort {

    ChallengerWorkbook getById(Long id);

    /**
     * memberId + originalWorkbookId로 ChallengerWorkbook 단건 조회
     * <p>
     * (member_id, original_workbook_id) UNIQUE 제약이 있으므로 Optional 반환
     */
    Optional<ChallengerWorkbook> findByMemberIdAndOriginalWorkbookId(Long memberId, Long originalWorkbookId);

    boolean existsByOriginalWorkbookId(Long originalWorkbookId);

    /**
     * memberId + 여러 originalWorkbookId에 해당하는 ChallengerWorkbook 목록 일괄 조회 (N+1 방지)
     */
    List<ChallengerWorkbook> listByMemberIdAndOriginalWorkbookIdIn(Long memberId, List<Long> originalWorkbookIds);

    List<ChallengerWorkbook> listByLookupKeys(List<ChallengerWorkbookLookupKey> keys);

    record ChallengerWorkbookLookupKey(Long memberId, Long weeklyCurriculumId, Long studyGroupId) {
    }
}
