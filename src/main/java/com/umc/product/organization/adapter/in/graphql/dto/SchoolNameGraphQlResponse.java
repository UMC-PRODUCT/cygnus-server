package com.umc.product.organization.adapter.in.graphql.dto;

import com.umc.product.global.graphql.relay.GlobalId;
import com.umc.product.global.graphql.relay.GlobalIdTypes;
import com.umc.product.organization.application.port.in.query.dto.school.SchoolNameInfo;

/**
 * 스키마 {@code SchoolName} 타입 응답. Node가 아니며 schoolId는 School 전역 ID로 인코딩한다.
 */
public record SchoolNameGraphQlResponse(
    Long rawSchoolId,
    String schoolName
) {

    public static SchoolNameGraphQlResponse from(SchoolNameInfo info) {
        return new SchoolNameGraphQlResponse(info.schoolId(), info.schoolName());
    }

    public String schoolId() {
        return GlobalId.encode(GlobalIdTypes.SCHOOL, rawSchoolId);
    }
}
