package com.umc.product.project.adapter.in.graphql.dto;

import java.util.List;

import org.springframework.data.domain.Pageable;

import com.umc.product.common.domain.enums.ChallengerPart;
import com.umc.product.project.application.port.in.query.dto.SearchProjectQuery;
import com.umc.product.project.domain.enums.ProjectStatus;

public record ProjectSearchGraphQlRequest(
    Long gisuId,
    String keyword,
    Long chapterId,
    List<Long> productOwnerSchoolIds,
    List<ChallengerPart> parts,
    ProjectPartQuotaStatus partQuotaStatus,
    List<ProjectStatus> statuses
) {
    public SearchProjectQuery toQuery(Pageable pageable) {
        List<ProjectStatus> effectiveStatuses = (statuses == null || statuses.isEmpty())
            ? List.of(ProjectStatus.IN_PROGRESS)
            : statuses;

        return SearchProjectQuery.builder()
            .gisuId(gisuId)
            .keyword(keyword)
            .chapterId(chapterId)
            .productOwnerSchoolIds(productOwnerSchoolIds)
            .parts(parts)
            .partQuotaStatus(partQuotaStatus == null ? null : partQuotaStatus.toDomain())
            .statuses(effectiveStatuses)
            .pageable(pageable)
            .build();
    }
}
