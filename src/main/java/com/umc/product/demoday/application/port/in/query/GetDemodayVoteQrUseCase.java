package com.umc.product.demoday.application.port.in.query;

import com.umc.product.demoday.application.port.in.query.dto.DemodayVoteQrInfo;

public interface GetDemodayVoteQrUseCase {

    /**
     * 현재 시간 구간에서 유효한 INFO QR credential을 조회한다. 호출할 때마다 새로 만들지 않는다.
     */
    DemodayVoteQrInfo get(Long pollId, Long memberId);
}
