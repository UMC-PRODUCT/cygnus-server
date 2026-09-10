package com.umc.product.analytics.application.port.in.query;

import com.umc.product.analytics.application.port.in.query.dto.AdminOperationsAttendanceChaptersInfo;
import com.umc.product.analytics.application.port.in.query.dto.AdminOperationsAttendanceChaptersQuery;

public interface GetAdminOperationsAttendanceChaptersUseCase {

    AdminOperationsAttendanceChaptersInfo getOperationsAttendanceByChapters(AdminOperationsAttendanceChaptersQuery query);
}
