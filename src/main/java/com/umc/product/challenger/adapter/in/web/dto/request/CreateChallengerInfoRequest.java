package com.umc.product.challenger.adapter.in.web.dto.request;

import java.util.List;

import com.umc.product.challenger.application.port.in.command.dto.CreateChallengerCommand;
import com.umc.product.common.domain.enums.ChallengerPart;
import com.umc.product.common.domain.enums.ChallengerTrack;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotNull;

public record CreateChallengerInfoRequest(
    @NotNull(message = "회원 ID는 필수입니다") Long memberId,
    ChallengerPart part,
    List<@NotNull(message = "챌린저 트랙에는 null을 포함할 수 없습니다") ChallengerTrack> tracks,
    @NotNull(message = "기수 ID는 필수입니다") Long gisuId
) {
    public CreateChallengerCommand toCommand() {
        return CreateChallengerCommand.builder()
            .memberId(memberId)
            .part(part)
            .tracks(tracks)
            .gisuId(gisuId)
            .build();
    }

    @AssertTrue(message = "챌린저 파트 또는 트랙이 필요합니다") public boolean isPartOrTracksPresent() {
        return part != null || (tracks != null && !tracks.isEmpty());
    }
}
