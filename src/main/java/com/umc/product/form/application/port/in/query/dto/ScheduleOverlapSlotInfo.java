package com.umc.product.form.application.port.in.query.dto;

import java.time.Instant;
import java.util.Set;

/**
 * SCHEDULE 응답 교집합 계산 결과의 단일 15분 슬롯.
 *
 * @param startsAt 슬롯 시작 시각 (UTC Instant, 15분 배수)
 * @param availableResponseIds 이 슬롯을 가능하다고 표시한 FormResponse ID 집합
 */
public record ScheduleOverlapSlotInfo(
    Instant startsAt,
    Set<Long> availableResponseIds
) {
}
