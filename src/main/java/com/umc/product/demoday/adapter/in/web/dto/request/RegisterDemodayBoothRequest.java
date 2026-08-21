package com.umc.product.demoday.adapter.in.web.dto.request;

import com.umc.product.demoday.application.port.in.command.dto.RegisterDemodayBoothCommand;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

@Schema(
    description = """
        데모데이 부스 등록 요청.

        projectId와 displayName 중 **정확히 하나만** 채웁니다. 둘 다 채우거나 둘 다 비우면 400으로 거부합니다.
        """
)
public record RegisterDemodayBoothRequest(
    @Schema(
        description = "행사에서 사용하는 부스 코드 번호. 같은 투표 안에서 중복될 수 없는 1 이상의 정수입니다.",
        example = "11",
        requiredMode = Schema.RequiredMode.REQUIRED
    )
    @NotNull @Positive Integer boothCode,

    @Schema(
        description = "UPMS에 등록된 프로젝트 ID. 등록된 프로젝트를 부스로 올릴 때만 채웁니다.",
        example = "101",
        nullable = true
    )
    Long projectId,

    @Schema(
        description = "외부 참가팀의 부스 표시 이름. UPMS에 프로젝트가 없을 때만 채우며 1자 이상 255자 이하입니다.",
        example = "외부 참가팀 A",
        nullable = true
    )
    String displayName
) {

    public RegisterDemodayBoothCommand toCommand(Long pollId, Long memberId) {
        return new RegisterDemodayBoothCommand(memberId, pollId, boothCode, projectId, displayName);
    }
}
