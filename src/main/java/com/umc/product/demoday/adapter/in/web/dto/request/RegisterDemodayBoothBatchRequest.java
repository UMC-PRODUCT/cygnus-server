package com.umc.product.demoday.adapter.in.web.dto.request;

import java.util.List;

import com.umc.product.demoday.application.port.in.command.dto.RegisterDemodayBoothBatchCommand;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;

@Schema(
    description = """
        데모데이 부스 일괄 등록 요청.

        항목마다 boothCode를 채우고 projectId와 displayName 중 정확히 하나만 채웁니다.
        한 항목이라도 규칙을 어기면 전체가 저장되지 않습니다.
        """
)
public record RegisterDemodayBoothBatchRequest(
    @Schema(description = "등록할 부스 목록. 최소 1건이어야 합니다.")
    @NotEmpty List<@Valid RegisterDemodayBoothRequest> booths
) {

    public RegisterDemodayBoothBatchCommand toCommand(Long pollId, Long memberId) {
        return new RegisterDemodayBoothBatchCommand(
            memberId,
            pollId,
            booths.stream()
                .map(booth -> new RegisterDemodayBoothBatchCommand.BoothRegistration(
                    booth.boothCode(),
                    booth.projectId(),
                    booth.displayName()))
                .toList()
        );
    }
}
