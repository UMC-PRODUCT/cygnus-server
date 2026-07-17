package com.umc.product.form.application.port.in.query;

import java.util.List;
import java.util.Optional;

import com.umc.product.form.application.port.in.FormActorContext;
import com.umc.product.form.application.port.in.query.dto.QuestionOptionInfo;
import com.umc.product.form.domain.FormOwnerReference;

/**
 * QuestionOption 조회 UseCase.
 */
public interface GetQuestionOptionUseCase {

    /**
     * 선택지 ID로 단건 조회. 없으면 Optional.empty.
     * expected owner binding과 READ policy는 선택지 존재 여부와 무관하게 먼저 검증한다.
     */
    Optional<QuestionOptionInfo> findById(
        FormOwnerReference expectedOwner,
        FormActorContext actorContext,
        Long optionId
    );

    /**
     * 선택지 ID로 단건 조회. 없으면 예외.
     */
    QuestionOptionInfo getById(FormOwnerReference expectedOwner, FormActorContext actorContext, Long optionId);

    /**
     * 질문에 속한 모든 선택지를 orderNo 오름차순으로 조회.
     * 반환된 각 선택지는 요청한 질문과 expected owner Form root에 정확히 속해야 한다.
     */
    List<QuestionOptionInfo> listByQuestionId(
        FormOwnerReference expectedOwner,
        FormActorContext actorContext,
        Long questionId
    );
}
