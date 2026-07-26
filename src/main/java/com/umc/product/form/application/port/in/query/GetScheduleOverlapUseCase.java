package com.umc.product.form.application.port.in.query;

import java.util.List;
import java.util.Set;

import com.umc.product.form.application.port.in.query.dto.ScheduleOverlapSlotInfo;

/**
 * 여러 SUBMITTED FormResponse 의 SCHEDULE 답변을 15분 슬롯 단위로 뒤집어
 * 각 슬롯에 어떤 응답이 가능하다고 표시했는지 계산한다.
 *
 * <p>소비자 (recruiting 면접 조율, timepick 스타일 그룹 시간 조율 등) 와 무관하게 동작한다.
 * raw 15분 슬롯 결과만 반환하며, "전원 교집합" / "N명 이상" / "특정 조합" 같은 필터링은 소비자 책임이다.
 *
 * <p>병합(merge)/후처리 없음. 결과는 startsAt 오름차순. 아무도 표시하지 않은 슬롯은 결과에 포함되지 않는다.
 */
public interface GetScheduleOverlapUseCase {

    /**
     * 지정된 SCHEDULE 질문에 대한 FormResponse 들의 답변 교집합 조회.
     * <p>
     * 한 폼에 SCHEDULE 질문이 여러 개일 수 있으므로 (예: 1차/2차 면접 시간) 소비자는 대상 질문을 명시해야 한다.
     * 다른 질문의 답변은 결과에 포함되지 않는다.
     *
     * @param formId 응답과 질문이 속해야 할 Form ID
     * @param questionId 교집합을 계산할 SCHEDULE 질문 ID
     * @param formResponseIds 계산 대상 FormResponse ID 집합. 빈 Set 이면 빈 List 반환.
     * @return 각 15분 슬롯별 available responseId 집합, startsAt 오름차순.
     *
     * @throws com.umc.product.form.domain.exception.FormDomainException
     *   QUESTION_NOT_FOUND — 존재하지 않는 questionId<br>
     *   QUESTION_IS_NOT_OWNED_BY_FORM — questionId 가 formId 에 속하지 않음<br>
     *   QUESTION_TYPE_MISMATCH — questionId 가 SCHEDULE 타입이 아님<br>
     *   FORM_RESPONSE_NOT_FOUND — 존재하지 않는 responseId 포함<br>
     *   FORM_RESPONSE_NOT_IN_FORM — formId 에 속하지 않는 responseId 포함<br>
     *   FORM_RESPONSE_NOT_SUBMITTED — SUBMITTED 상태가 아닌 responseId 포함
     */
    List<ScheduleOverlapSlotInfo> getOverlap(Long formId, Long questionId, Set<Long> formResponseIds);
}
