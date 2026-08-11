package com.umc.product.project.adapter.in.graphql.dto;

import java.util.List;

import com.umc.product.global.graphql.relay.GlobalId;
import com.umc.product.global.graphql.relay.GlobalIdTypes;
import com.umc.product.global.graphql.relay.RelayNode;
import com.umc.product.project.application.port.in.query.dto.ProjectInfo;
import com.umc.product.project.domain.enums.ProjectStatus;

/**
 * Project Node 응답. {@code id}는 전역 ID이며, {@code projectId}/{@code productOwnerMemberId}/
 * {@code coProductOwnerMemberIds}는 배치 로딩·권한 검증용 raw ID로 스키마에 노출하지 않는다.
 */
public record ProjectGraphQlResponse(
    String id,
    Long projectId,
    ProjectStatus status,
    String name,
    String description,
    String thumbnailImageUrl,
    String logoImageUrl,
    String externalLink,
    String gisuId,
    String chapterId,
    Long productOwnerMemberId,
    List<Long> coProductOwnerMemberIds,
    List<ProjectPartQuotaGraphQlResponse> partQuotas,
    String createdAt,
    String updatedAt
) implements RelayNode {
    public static ProjectGraphQlResponse from(ProjectInfo info) {
        return new ProjectGraphQlResponse(
            GlobalId.encode(GlobalIdTypes.PROJECT, info.id()),
            info.id(),
            info.status(),
            info.name(),
            info.description(),
            info.thumbnailImageUrl(),
            info.logoImageUrl(),
            info.externalLink(),
            info.gisuId() == null ? null : GlobalId.encode(GlobalIdTypes.GISU, info.gisuId()),
            info.chapterId() == null ? null : GlobalId.encode(GlobalIdTypes.CHAPTER, info.chapterId()),
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
