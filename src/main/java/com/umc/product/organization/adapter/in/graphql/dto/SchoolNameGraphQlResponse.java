package com.umc.product.organization.adapter.in.graphql.dto;

import com.umc.product.organization.application.port.in.query.dto.school.SchoolNameInfo;

public record SchoolNameGraphQlResponse(
    Long schoolId,
    String schoolName
) {

    public static SchoolNameGraphQlResponse from(SchoolNameInfo info) {
        return new SchoolNameGraphQlResponse(info.schoolId(), info.schoolName());
    }
}
