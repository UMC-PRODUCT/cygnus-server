package com.umc.product.test.adapter.in.web.dto;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.umc.product.common.domain.enums.ChallengerPart;
import com.umc.product.common.domain.enums.ChallengerTrack;
import com.umc.product.test.application.port.in.command.dto.CreateSeedChallengerCommand;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotNull;

public record CreateSeedChallengerRequest(
    @NotNull(message = "회원 ID는 필수입니다") Long memberId,

    @NotNull(message = "기수 ID는 필수입니다") Long gisuId,

    ChallengerPart part,
    List<@NotNull ChallengerTrack> tracks
) {

    public CreateSeedChallengerCommand toCommand() {
        return CreateSeedChallengerCommand.of(memberId, gisuId, part, tracks);
    }

    @JsonIgnore
    @AssertTrue(message = "part 또는 tracks를 입력해주세요. 수강 없는 TRACK 기수 소속은 tracks를 빈 배열로 입력해주세요.") public boolean isLearningSelectionPresent() {
        return part != null || tracks != null;
    }
}
