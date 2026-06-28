package com.umc.product.analytics.application.port.in.query;

import com.umc.product.analytics.application.port.in.query.dto.AdminOperationsAttendancePartsInfo;
import com.umc.product.analytics.application.port.in.query.dto.AdminOperationsAttendancePartsQuery;

public interface GetAdminOperationsAttendancePartsUseCase {

    AdminOperationsAttendancePartsInfo getOperationsAttendanceByParts(AdminOperationsAttendancePartsQuery query);
}
