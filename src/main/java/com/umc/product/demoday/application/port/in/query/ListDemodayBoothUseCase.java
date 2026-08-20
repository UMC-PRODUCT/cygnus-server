package com.umc.product.demoday.application.port.in.query;

import java.util.List;

import com.umc.product.demoday.application.port.in.query.dto.DemodayBoothInfo;
import com.umc.product.demoday.application.port.in.query.participant.DemodayParticipant;

public interface ListDemodayBoothUseCase {

    /**
     * 참여자가 투표할 수 있는 프로젝트 부스만 조회한다. 외부 부스와 회원의 소속 프로젝트 부스는 제외한다.
     */
    List<DemodayBoothInfo> listBooths(Long pollId, DemodayParticipant participant);
}
