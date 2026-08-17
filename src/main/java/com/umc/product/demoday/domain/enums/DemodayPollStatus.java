package com.umc.product.demoday.domain.enums;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "데모데이 투표 운영 상태. READY는 행사 전 준비, OPEN은 투표 진행, CLOSED는 투표 종료를 의미합니다.")
public enum DemodayPollStatus {
    READY,
    OPEN,
    CLOSED
}
