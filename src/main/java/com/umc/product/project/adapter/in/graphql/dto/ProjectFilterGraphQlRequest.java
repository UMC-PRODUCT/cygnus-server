package com.umc.product.project.adapter.in.graphql.dto;

import java.util.List;

import org.springframework.data.domain.Pageable;

import com.umc.product.common.domain.enums.ChallengerPart;
import com.umc.product.global.graphql.relay.GlobalId;
import com.umc.product.global.graphql.relay.GlobalIdTypes;
import com.umc.product.project.application.port.in.query.dto.SearchProjectQuery;
import com.umc.product.project.domain.enums.PartQuotaStatus;
import com.umc.product.project.domain.enums.ProjectStatus;

/**
 * projects 검색 필터. ID 필드는 전역 ID로 받아 어댑터 경계에서 raw ID로 디코딩한다.
 */
public record ProjectFilterGraphQlRequest(
    String gisuId,
    String keyword,
    String chapterId,
    List<String> productOwnerSchoolIds,
    List<ChallengerPart> parts,
    PartQuotaStatus partQuotaStatus,
    List<ProjectStatus> statuses
) {
    public SearchProjectQuery toQuery(Pageable pageable) {
        List<ProjectStatus> effectiveStatuses = (statuses == null || statuses.isEmpty())
            ? List.of(ProjectStatus.IN_PROGRESS)
            : statuses;

        return SearchProjectQuery.builder()
            .gisuId(GlobalId.decodeLong(gisuId, GlobalIdTypes.GISU))
            .keyword(keyword)
            .chapterId(chapterId == null ? null : GlobalId.decodeLong(chapterId, GlobalIdTypes.CHAPTER))
            .productOwnerSchoolIds(productOwnerSchoolIds == null
                ? null
                : GlobalId.decodeLongs(productOwnerSchoolIds, GlobalIdTypes.SCHOOL))
            .parts(parts)
            .partQuotaStatus(partQuotaStatus)
            .statuses(effectiveStatuses)
            .pageable(pageable)
            .build();
    }
}
