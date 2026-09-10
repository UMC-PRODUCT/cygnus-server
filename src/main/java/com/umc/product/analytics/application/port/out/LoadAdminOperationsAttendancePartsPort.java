package com.umc.product.analytics.application.port.out;

import java.time.Instant;

import com.umc.product.analytics.application.port.in.query.dto.AdminOperationsAttendancePartsInfo;
import com.umc.product.analytics.domain.AdminAnalyticsScope;

public interface LoadAdminOperationsAttendancePartsPort {

    AdminOperationsAttendancePartsInfo getAttendanceByParts(AdminAnalyticsScope scope, Instant from, Instant to);
}
