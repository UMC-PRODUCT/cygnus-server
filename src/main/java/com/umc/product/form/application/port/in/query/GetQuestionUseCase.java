package com.umc.product.form.application.port.in.query;

import java.util.List;
import java.util.Optional;

import com.umc.product.form.application.port.in.FormActorContext;
import com.umc.product.form.application.port.in.query.dto.QuestionInfo;
import com.umc.product.form.domain.FormOwnerReference;

/**
 * Question 조회 UseCase.
 */
public interface GetQuestionUseCase {

    /**
     * 질문 ID로 단건 조회. 없으면 Optional.empty.
     * expected owner binding과 READ policy는 질문 존재 여부와 무관하게 먼저 검증한다.
     */
    Optional<QuestionInfo> findById(FormOwnerReference expectedOwner, FormActorContext actorContext, Long questionId);

    /**
     * 질문 ID로 단건 조회. 없으면 QUESTION_NOT_FOUND 예외.
     */
    QuestionInfo getById(FormOwnerReference expectedOwner, FormActorContext actorContext, Long questionId);

    /**
     * 섹션에 속한 모든 질문을 orderNo 오름차순으로 조회.
     * 반환된 각 질문은 요청한 섹션과 expected owner Form root에 정확히 속해야 한다.
     */
    List<QuestionInfo> listBySectionId(FormOwnerReference expectedOwner, FormActorContext actorContext, Long sectionId);
}
