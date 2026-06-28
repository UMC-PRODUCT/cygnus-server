package com.umc.product.analytics.adapter.out.persistence;

import java.time.Instant;

import org.springframework.stereotype.Component;

import com.umc.product.analytics.application.port.in.query.dto.AdminOperationsAttendanceInfo;
import com.umc.product.analytics.application.port.in.query.dto.AdminOperationsOverviewInfo;
import com.umc.product.analytics.application.port.in.query.dto.AdminOperationsOverviewQuery;
import com.umc.product.analytics.application.port.in.query.dto.AdminOperationsPointsInfo;
import com.umc.product.analytics.application.port.in.query.dto.AdminOperationsSchoolsInfo;
import com.umc.product.analytics.application.port.in.query.dto.AdminOperationsSignupsInfo;
import com.umc.product.analytics.application.port.in.query.dto.AdminOperationsStudyGroupsInfo;
import com.umc.product.analytics.application.port.in.query.dto.AdminOperationsAttendanceChaptersInfo;
import com.umc.product.analytics.application.port.in.query.dto.AdminOperationsAttendancePartsInfo;
import com.umc.product.analytics.application.port.in.query.dto.AdminOperationsAttendanceTagsInfo;
import com.umc.product.analytics.application.port.out.LoadAdminOperationsAnalyticsPort;
import com.umc.product.analytics.application.port.out.LoadAdminOperationsAttendanceChaptersPort;
import com.umc.product.analytics.application.port.out.LoadAdminOperationsAttendancePartsPort;
import com.umc.product.analytics.application.port.out.LoadAdminOperationsAttendancePort;
import com.umc.product.analytics.application.port.out.LoadAdminOperationsAttendanceTagsPort;
import com.umc.product.analytics.application.port.out.LoadAdminOperationsPointsPort;
import com.umc.product.analytics.application.port.out.LoadAdminOperationsSchoolsPort;
import com.umc.product.analytics.application.port.out.LoadAdminOperationsSignupsPort;
import com.umc.product.analytics.application.port.out.LoadAdminOperationsStudyGroupsPort;
import com.umc.product.analytics.domain.AdminAnalyticsScope;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class AdminOperationsAnalyticsPersistenceAdapter implements
    LoadAdminOperationsAnalyticsPort,
    LoadAdminOperationsSchoolsPort,
    LoadAdminOperationsPointsPort,
    LoadAdminOperationsAttendancePort,
    LoadAdminOperationsAttendanceChaptersPort,
    LoadAdminOperationsAttendanceTagsPort,
    LoadAdminOperationsAttendancePartsPort,
    LoadAdminOperationsStudyGroupsPort,
    LoadAdminOperationsSignupsPort {

    private final AdminOperationsAnalyticsQueryRepository queryRepository;

    @Override
    public AdminOperationsOverviewInfo getOperationsOverview(
        AdminAnalyticsScope scope,
        AdminOperationsOverviewQuery query
    ) {
        return queryRepository.getOperationsOverview(scope, query);
    }

    @Override
    public AdminOperationsSchoolsInfo getOperationsSchools(AdminAnalyticsScope scope) {
        return queryRepository.getOperationsSchools(scope);
    }

    @Override
    public AdminOperationsPointsInfo getOperationsPoints(AdminAnalyticsScope scope, Instant from, Instant to) {
        return queryRepository.getOperationsPoints(scope, from, to);
    }

    @Override
    public AdminOperationsAttendanceInfo getOperationsAttendance(AdminAnalyticsScope scope, Instant from, Instant to) {
        return queryRepository.getOperationsAttendance(scope, from, to);
    }

    @Override
    public AdminOperationsStudyGroupsInfo getOperationsStudyGroups(AdminAnalyticsScope scope, Instant from, Instant to) {
        return queryRepository.getOperationsStudyGroups(scope, from, to);
    }

    @Override
    public AdminOperationsSignupsInfo getOperationsSignups(AdminAnalyticsScope scope, Instant from, Instant to) {
        return queryRepository.getOperationsSignups(scope, from, to);
    }

    @Override
    public AdminOperationsAttendanceChaptersInfo getAttendanceByChapters(AdminAnalyticsScope scope, Instant from, Instant to) {
        return queryRepository.getAttendanceByChapters(scope, from, to);
    }

    @Override
    public AdminOperationsAttendanceTagsInfo getAttendanceByTags(AdminAnalyticsScope scope, Instant from, Instant to) {
        return queryRepository.getAttendanceByTags(scope, from, to);
    }

    @Override
    public AdminOperationsAttendancePartsInfo getAttendanceByParts(AdminAnalyticsScope scope, Instant from, Instant to) {
        return queryRepository.getAttendanceByParts(scope, from, to);
    }
}
