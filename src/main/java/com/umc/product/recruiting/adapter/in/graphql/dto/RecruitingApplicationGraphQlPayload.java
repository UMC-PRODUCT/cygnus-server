package com.umc.product.recruiting.adapter.in.graphql.dto;

import com.umc.product.recruiting.application.port.in.query.dto.RecruitingApplicationInfo;

/**
 * 지원서 노드를 담는 뮤테이션 payload. 지원서를 반환하는 모든 payload 스키마 타입에 매핑된다.
 */
public record RecruitingApplicationGraphQlPayload(RecruitingApplicationGraphQlResponse application) {

    public static RecruitingApplicationGraphQlPayload from(RecruitingApplicationInfo info) {
        return new RecruitingApplicationGraphQlPayload(RecruitingApplicationGraphQlResponse.from(info));
    }
}
