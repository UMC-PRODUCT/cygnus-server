package com.umc.product.form.application.port.out;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import com.umc.product.form.domain.QuestionOption;

public interface LoadQuestionOptionPort {

    Optional<QuestionOption> findById(Long optionId);

    boolean existsByIdAndQuestionId(Long optionId, Long questionId);

    List<QuestionOption> listByQuestionId(Long questionId);

    /**
     * 여러 질문의 모든 선택지를 한 번에 조회 (벌크). orderNo 오름차순 정렬.
     * 폼 전체 구조 조회 등 N+1 회피 용도.
     */
    List<QuestionOption> listByQuestionIdIn(Set<Long> questionIds);
}
