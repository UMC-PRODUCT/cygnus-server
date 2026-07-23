package com.umc.product.recruiting.application.port.in.command.dto;

import java.util.List;

import lombok.Builder;

@Builder
public record FindRecruitingInterviewScheduleCandidatesCommand(
    Long formId,
    List<Long> formResponseIds
) {
}
