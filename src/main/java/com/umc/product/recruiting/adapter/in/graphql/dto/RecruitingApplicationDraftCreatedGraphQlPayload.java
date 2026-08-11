package com.umc.product.recruiting.adapter.in.graphql.dto;

import com.umc.product.global.graphql.relay.GlobalId;
import com.umc.product.global.graphql.relay.GlobalIdTypes;
import com.umc.product.recruiting.application.port.in.query.dto.RecruitingApplicationCreatedInfo;
import com.umc.product.recruiting.domain.enums.RecruitingApplicationStatus;

/**
 * 지원서 초안 생성 payload. 회원/익명 초안 생성 payload 스키마 타입 모두에 매핑된다.
 */
public record RecruitingApplicationDraftCreatedGraphQlPayload(
    String applicationId,
    String applicationKey,
    RecruitingApplicationStatus status
) {

    public static RecruitingApplicationDraftCreatedGraphQlPayload from(RecruitingApplicationCreatedInfo info) {
        return new RecruitingApplicationDraftCreatedGraphQlPayload(
            GlobalId.encode(GlobalIdTypes.RECRUITING_APPLICATION, info.applicationId()),
            info.applicationKey(),
            info.status()
        );
    }
}
