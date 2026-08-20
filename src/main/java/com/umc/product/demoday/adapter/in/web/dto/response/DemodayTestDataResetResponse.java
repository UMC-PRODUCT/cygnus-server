package com.umc.product.demoday.adapter.in.web.dto.response;

import com.umc.product.demoday.application.port.in.command.dto.DemodayTestDataResetInfo;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "데모데이 테스트 데이터 초기화 결과")
public record DemodayTestDataResetResponse(
    @Schema(description = "삭제된 Poll 수", example = "1")
    int deletedPolls,

    @Schema(description = "삭제된 부스 수", example = "20")
    int deletedBooths,

    @Schema(description = "삭제된 외부인 입장 코드 수", example = "200")
    int deletedEntryCodes,

    @Schema(description = "삭제된 스탬프 수", example = "500")
    int deletedStamps,

    @Schema(description = "삭제된 표 수", example = "150")
    int deletedVotes
) {

    public static DemodayTestDataResetResponse from(DemodayTestDataResetInfo info) {
        return new DemodayTestDataResetResponse(
            info.deletedPolls(),
            info.deletedBooths(),
            info.deletedEntryCodes(),
            info.deletedStamps(),
            info.deletedVotes()
        );
    }
}
