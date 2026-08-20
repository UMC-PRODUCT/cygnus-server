package com.umc.product.demoday.adapter.in.web.dto.response;

import java.util.List;

import com.umc.product.demoday.application.port.in.query.dto.DemodayAdminBoothListInfo;
import com.umc.product.demoday.domain.enums.DemodayPollStatus;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "운영자용 데모데이 부스 등록 현황")
public record DemodayAdminBoothListResponse(
    @Schema(description = "데모데이 투표 ID", example = "1")
    Long pollId,

    @Schema(description = "투표의 현재 운영 상태", example = "CLOSED")
    DemodayPollStatus pollStatus,

    @Schema(
        description = "부스를 더 등록할 수 있는지 여부. 투표가 OPEN이 되는 순간부터 false가 됩니다.",
        example = "true"
    )
    boolean boothAddable,

    @Schema(description = "등록된 부스 수", example = "8")
    int boothCount,

    @Schema(description = "등록된 부스 목록. 부스 코드 오름차순입니다.")
    List<DemodayBoothResponse> booths
) {

    public static DemodayAdminBoothListResponse from(DemodayAdminBoothListInfo info) {
        return new DemodayAdminBoothListResponse(
            info.pollId(),
            info.pollStatus(),
            info.boothAddable(),
            info.boothCount(),
            info.booths().stream().map(DemodayBoothResponse::from).toList()
        );
    }
}
