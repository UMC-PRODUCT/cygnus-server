package com.umc.product.form.application.port.in.command.dto;

import lombok.Builder;

import java.util.List;

/**
 * 투표용 설문 생성 명령 DTO
 */
@Builder
public record CreateVoteCommand(
    Long createdMemberId,
    String title,
    boolean isAnonymous,
    boolean allowMultipleChoice,
    List<String> options
) {
}
