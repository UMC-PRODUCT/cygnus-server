package com.umc.product.challenger.application.port.in.command.dto;

import lombok.Builder;

@Builder
public record ConsumeChallengerRecordCommand(
    Long targetMemberId,
    String code
) {
}
