package com.umc.product.form.application.port.in.query;

import java.util.List;
import java.util.Optional;

import com.umc.product.form.application.port.in.query.dto.QuestionInfo;

/**
 * Question 조회 UseCase.
 */
public interface GetQuestionUseCase {

    /**
     * 질문 ID로 단건 조회. 없으면 Optional.empty.
     */
    Optional<QuestionInfo> findById(Long questionId);

    /**
     * 질문 ID로 단건 조회. 없으면 QUESTION_NOT_FOUND 예외.
     */
    QuestionInfo getById(Long questionId);

    /**
     * 섹션에 속한 모든 질문을 orderNo 오름차순으로 조회.
     */
    List<QuestionInfo> listBySectionId(Long sectionId);
}
