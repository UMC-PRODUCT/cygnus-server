package com.umc.product.recruiting.adapter.in.graphql.dto;

import java.util.List;
import java.util.Set;

import com.umc.product.global.graphql.relay.GlobalId;
import com.umc.product.global.graphql.relay.GlobalIdTypes;
import com.umc.product.recruiting.application.port.in.query.dto.RecruitingStatusSummaryQuery;

public record RecruitingStatusSummaryGraphQlRequest(
    String gisuId,
    List<String> schoolIds,
    List<String> roundIds,
    String schoolName
) {

    public RecruitingStatusSummaryQuery toQuery(Long requesterMemberId) {
        if (gisuId == null) {
            throw new IllegalArgumentException("gisuId는 필수입니다.");
        }
        return RecruitingStatusSummaryQuery.builder()
            .gisuId(GlobalId.decodeLong(gisuId, GlobalIdTypes.GISU))
            .schoolIds(decodePositive(schoolIds, GlobalIdTypes.SCHOOL, "schoolIds"))
            .roundIds(decodePositive(roundIds, GlobalIdTypes.RECRUITING_ROUND, "roundIds"))
            .schoolName(schoolName)
            .requesterMemberId(requesterMemberId)
            .build();
    }

    private static Set<Long> decodePositive(List<String> values, String typeName, String fieldName) {
        if (values == null) {
            return Set.of();
        }
        Set<Long> decoded = Set.copyOf(GlobalId.decodeLongs(values, typeName));
        if (decoded.stream().anyMatch(value -> value <= 0)) {
            throw new IllegalArgumentException(fieldName + "는 양수여야 합니다.");
        }
        return decoded;
    }
}
