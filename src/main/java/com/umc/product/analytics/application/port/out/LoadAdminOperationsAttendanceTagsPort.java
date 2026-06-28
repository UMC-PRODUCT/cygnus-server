package com.umc.product.analytics.application.port.out;

import java.time.Instant;

import com.umc.product.analytics.application.port.in.query.dto.AdminOperationsAttendanceTagsInfo;
import com.umc.product.analytics.domain.AdminAnalyticsScope;

public interface LoadAdminOperationsAttendanceTagsPort {

    AdminOperationsAttendanceTagsInfo getAttendanceByTags(AdminAnalyticsScope scope, Instant from, Instant to);
}
