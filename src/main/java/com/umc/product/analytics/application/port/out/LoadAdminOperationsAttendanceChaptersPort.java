package com.umc.product.analytics.application.port.out;

import java.time.Instant;

import com.umc.product.analytics.application.port.in.query.dto.AdminOperationsAttendanceChaptersInfo;
import com.umc.product.analytics.domain.AdminAnalyticsScope;

public interface LoadAdminOperationsAttendanceChaptersPort {

    AdminOperationsAttendanceChaptersInfo getAttendanceByChapters(AdminAnalyticsScope scope, Instant from, Instant to);
}
