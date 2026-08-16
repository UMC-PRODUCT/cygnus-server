package com.umc.product.demoday.application.port.in.query.dto;

import java.util.List;

import com.umc.product.demoday.domain.enums.DemodayPollStatus;

/**
 * 운영자가 부스 등록 결과를 확인하는 읽기 모델이다.
 *
 * <p>참여자용 목록과 달리 투표의 운영 상태와 부스 수를 함께 담는다. 등록 직후 운영자가 확인하는 것이
 * 개별 부스가 아니라 "몇 개가 등록됐고 아직 더 넣을 수 있는가"이기 때문이다. {@code boothAddable}은
 * 상태에서 파생되지만, 잠금 규칙을 클라이언트가 다시 계산하지 않도록 서버가 판단한 결과를 그대로 내려준다.
 */
public record DemodayAdminBoothListInfo(
    Long pollId,
    DemodayPollStatus pollStatus,
    boolean boothAddable,
    int boothCount,
    List<DemodayBoothInfo> booths
) {
}
