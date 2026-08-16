package com.umc.product.organization.adapter.in.graphql.dto;

import com.umc.product.organization.application.port.in.query.dto.chapter.ChapterWithSchoolsInfo;

public record ChapterSchoolGraphQlResponse(
    Long schoolId,
    String schoolName
) {

    public static ChapterSchoolGraphQlResponse from(ChapterWithSchoolsInfo.SchoolInfo info) {
        return new ChapterSchoolGraphQlResponse(info.schoolId(), info.schoolName());
    }
}
