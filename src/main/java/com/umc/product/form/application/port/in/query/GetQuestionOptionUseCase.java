package com.umc.product.form.application.port.in.query;

import java.util.List;
import java.util.Optional;

import com.umc.product.form.application.port.in.query.dto.QuestionOptionInfo;

/**
 * QuestionOption 조회 UseCase.
 */
public interface GetQuestionOptionUseCase {

    /**
     * 선택지 ID로 단건 조회. 없으면 Optional.empty.
     */
    Optional<QuestionOptionInfo> findById(Long optionId);

    /**
     * 선택지 ID로 단건 조회. 없으면 예외.
     */
    QuestionOptionInfo getById(Long optionId);

    /**
     * 질문에 속한 모든 선택지를 orderNo 오름차순으로 조회.
     */
    List<QuestionOptionInfo> listByQuestionId(Long questionId);
}
