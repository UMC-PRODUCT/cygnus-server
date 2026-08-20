package com.umc.product.demoday.application.port.in.command.dto;

import java.util.List;

/**
 * 부스 일괄 등록 요청이다. 항목 하나하나는 {@link RegisterDemodayBoothCommand}와 같은 규칙을 따른다.
 *
 * <p>등록 결과는 입력 순서와 인덱스로 대응하며, 한 항목이라도 규칙을 어기면 전체가 저장되지 않는다.
 * 부분 성공을 허용하면 운영자가 무엇이 들어갔는지 확인하러 목록을 다시 훑어야 하는데, 부스는 투표를
 * OPEN하기 전에 한 번에 준비하는 자원이라 그 확인 비용이 재시도 비용보다 크다.
 */
public record RegisterDemodayBoothBatchCommand(
    Long memberId,
    Long pollId,
    List<BoothRegistration> registrations
) {

    public record BoothRegistration(
        Integer boothCode,
        Long projectId,
        String displayName
    ) {
    }
}
