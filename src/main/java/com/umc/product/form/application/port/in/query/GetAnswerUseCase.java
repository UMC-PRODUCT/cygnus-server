package com.umc.product.form.application.port.in.query;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import com.umc.product.form.application.port.in.query.dto.AnswerInfo;

/**
 * Answer 조회 UseCase.
 * <p>
 * 개별 답변 조회와 FormResponse 단위 전체 답변 조회를 모두 지원한다.
 */
public interface GetAnswerUseCase {

    /**
     * 답변 ID 로 단건 조회. 없으면 Optional.empty.
     */
    Optional<AnswerInfo> findById(Long answerId);

    /**
     * 답변 ID 로 단건 조회. 없으면 예외.
     */
    AnswerInfo getById(Long answerId);

    /**
     * 특정 FormResponse 에 속한 모든 답변을 반환한다. (질문 orderNo 순)
     */
    List<AnswerInfo> listByFormResponseId(Long formResponseId);

    /**
     * 여러 FormResponse 에 속한 답변을 한 번에 조회한다.
     *
     * @return formResponseId -> 답변 목록
     */
    Map<Long, List<AnswerInfo>> listByFormResponseIds(Set<Long> formResponseIds);
}
