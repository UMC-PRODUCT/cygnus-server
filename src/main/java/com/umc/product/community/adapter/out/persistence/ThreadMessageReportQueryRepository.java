package com.umc.product.community.adapter.out.persistence;

import static com.umc.product.community.domain.QReport.report;

import java.util.List;

import org.springframework.stereotype.Repository;

import com.querydsl.core.BooleanBuilder;
import com.querydsl.jpa.impl.JPAQueryFactory;
import com.umc.product.community.application.port.out.report.SearchThreadMessageReportPort;
import com.umc.product.community.application.port.out.report.dto.ThreadMessageReportSearchQuery;
import com.umc.product.community.application.port.out.report.dto.ThreadMessageReportSearchResult;
import com.umc.product.community.domain.Report;
import com.umc.product.community.domain.enums.ReportTargetType;

import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class ThreadMessageReportQueryRepository implements SearchThreadMessageReportPort {

    private final JPAQueryFactory queryFactory;

    @Override
    public ThreadMessageReportSearchResult search(ThreadMessageReportSearchQuery query) {
        BooleanBuilder condition = buildCondition(query);
        List<Report> reports = queryFactory
            .selectFrom(report)
            .where(condition)
            .orderBy(report.createdAt.desc(), report.id.desc())
            .offset(query.offset())
            .limit(query.limit())
            .fetch();
        Long total = queryFactory
            .select(report.id.count())
            .from(report)
            .where(condition)
            .fetchOne();
        return new ThreadMessageReportSearchResult(reports, total == null ? 0L : total);
    }

    private BooleanBuilder buildCondition(ThreadMessageReportSearchQuery query) {
        BooleanBuilder condition = new BooleanBuilder()
            .and(report.targetType.eq(ReportTargetType.THREAD_MESSAGE));
        if (query.status() != null) {
            condition.and(report.status.eq(query.status()));
        }
        if (query.reason() != null) {
            condition.and(report.reasonCode.eq(query.reason()));
        }
        if (query.threadId() != null) {
            condition.and(report.threadId.eq(query.threadId()));
        }
        if (query.reporterId() != null) {
            condition.and(report.reporterId.eq(query.reporterId()));
        }
        return condition;
    }
}
