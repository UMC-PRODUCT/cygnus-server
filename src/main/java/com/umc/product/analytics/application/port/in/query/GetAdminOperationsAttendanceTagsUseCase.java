package com.umc.product.analytics.application.port.in.query;

import com.umc.product.analytics.application.port.in.query.dto.AdminOperationsAttendanceTagsInfo;
import com.umc.product.analytics.application.port.in.query.dto.AdminOperationsAttendanceTagsQuery;

public interface GetAdminOperationsAttendanceTagsUseCase {

    AdminOperationsAttendanceTagsInfo getOperationsAttendanceByTags(AdminOperationsAttendanceTagsQuery query);
}
