package com.umc.product.project.adapter.in.web.dto.request;

import java.util.List;

import com.umc.product.project.application.port.in.command.dto.CompleteProjectsCommand;

import jakarta.validation.constraints.NotEmpty;

/**
 * 프로젝트 완료(complete) 요청 DTO. 기수 종료 시 여러 프로젝트를 한 번에 완료 처리합니다.
 */
public record CompleteProjectsRequest(
    @NotEmpty List<Long> projectIds
) {
    public CompleteProjectsCommand toCommand(Long requesterMemberId) {
        return CompleteProjectsCommand.builder()
            .projectIds(projectIds)
            .requesterMemberId(requesterMemberId)
            .build();
    }
}
