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
     * 지정된 FormResponse 들의 SCHEDULE 답변 교집합 조회.
     *
     * @param formId 이 응답들이 속해야 할 Form ID
     * @param formResponseIds 계산 대상 FormResponse ID 집합. 빈 Set 이면 빈 List 반환.
     * @return 각 15분 슬롯별 available responseId 집합, startsAt 오름차순.
     *
     * @throws com.umc.product.form.domain.exception.FormDomainException
     *   FORM_RESPONSE_NOT_FOUND — 존재하지 않는 responseId 포함<br>
     *   FORM_RESPONSE_NOT_IN_FORM — formId 에 속하지 않는 responseId 포함<br>
     *   FORM_RESPONSE_NOT_SUBMITTED — SUBMITTED 상태가 아닌 responseId 포함
     */
    List<ScheduleOverlapSlotInfo> getOverlap(Long formId, Set<Long> formResponseIds);
}
