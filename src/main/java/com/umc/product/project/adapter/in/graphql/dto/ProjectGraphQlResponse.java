package com.umc.product.project.adapter.in.graphql.dto;

import java.util.List;

import com.umc.product.project.application.port.in.query.dto.ProjectInfo;
import com.umc.product.project.domain.enums.ProjectStatus;

public record ProjectGraphQlResponse(
    Long id,
    ProjectStatus status,
    String name,
    String description,
    String thumbnailImageUrl,
    String logoImageUrl,
    String externalLink,
    Long gisuId,
    Long chapterId,
    Long productOwnerMemberId,
    List<Long> coProductOwnerMemberIds,
    List<ProjectPartQuotaGraphQlResponse> partQuotas,
    String createdAt,
    String updatedAt
) {
    public static ProjectGraphQlResponse from(ProjectInfo info) {
        return new ProjectGraphQlResponse(
            info.id(),
            info.status(),
            info.name(),
            info.description(),
            info.thumbnailImageUrl(),
            info.logoImageUrl(),
            info.externalLink(),
            info.gisuId(),
            info.chapterId(),
            info.productOwnerMemberId(),
            info.coProductOwnerMemberIds() == null ? List.of() : info.coProductOwnerMemberIds(),
            info.partQuotas() == null
                ? List.of()
                : info.partQuotas().stream().map(ProjectPartQuotaGraphQlResponse::from).toList(),
            info.createdAt() == null ? null : info.createdAt().toString(),
            info.updatedAt() == null ? null : info.updatedAt().toString()
        );
    }
}
