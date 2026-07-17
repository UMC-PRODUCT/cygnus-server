package com.umc.product.form.application.port.in.command.dto;

import java.util.List;

import lombok.Builder;

/**
 * 투표용 설문 생성 명령 DTO
 */
@Builder
public record CreateVoteCommand(
    String title,
    boolean isAnonymous,
    boolean allowMultipleChoice,
    List<String> options
) {
}
