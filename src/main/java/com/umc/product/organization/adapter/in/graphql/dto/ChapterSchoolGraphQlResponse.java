package com.umc.product.organization.adapter.in.graphql.dto;

import com.umc.product.global.graphql.relay.GlobalId;
import com.umc.product.global.graphql.relay.GlobalIdTypes;
import com.umc.product.organization.application.port.in.query.dto.chapter.ChapterWithSchoolsInfo;

/**
 * 스키마 {@code ChapterSchool} 타입 응답. Node가 아니며 schoolId는 School 전역 ID로 인코딩한다.
 */
public record ChapterSchoolGraphQlResponse(
    Long rawSchoolId,
    String schoolName
) {

    public static ChapterSchoolGraphQlResponse from(ChapterWithSchoolsInfo.SchoolInfo info) {
        return new ChapterSchoolGraphQlResponse(info.schoolId(), info.schoolName());
    }

    public String schoolId() {
        return GlobalId.encode(GlobalIdTypes.SCHOOL, rawSchoolId);
    }
}
