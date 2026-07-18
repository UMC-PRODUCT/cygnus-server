package com.umc.product.community.adapter.out.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.Import;

import com.umc.product.community.application.port.out.report.dto.ThreadMessageReportSearchQuery;
import com.umc.product.community.application.port.out.report.dto.ThreadMessageReportSearchResult;
import com.umc.product.community.domain.CommunityThread;
import com.umc.product.community.domain.Report;
import com.umc.product.community.domain.enums.CommunityThreadCategory;
import com.umc.product.community.domain.enums.ReportReason;
import com.umc.product.community.domain.enums.ReportStatus;
import com.umc.product.community.domain.enums.ReportTargetType;
import com.umc.product.support.PersistenceAdapterTest;

@PersistenceAdapterTest
@Import(ThreadMessageReportQueryRepository.class)
@DisplayName("ThreadMessageReportQueryRepository")
class ThreadMessageReportQueryRepositoryTest {

    private static final Instant NOW = Instant.parse("2026-07-18T00:00:00Z");

    @Autowired
    ThreadMessageReportQueryRepository sut;

    @Autowired
    TestEntityManager entityManager;

    @Autowired
    CommunityThreadRepository threadRepository;

    @Autowired
    ReportRepository reportRepository;

    @Test
    @DisplayName("THREAD_MESSAGE만 상태/사유/스레드/Challenger 필터로 조회하고 exact total을 반환한다")
    void search_스레드_메시지_신고는_모든_필터와_total을_적용한다() {
        // given
        CommunityThread firstThread = threadRepository.saveAndFlush(createThread(110L, "첫 스레드"));
        CommunityThread secondThread = threadRepository.saveAndFlush(createThread(111L, "둘째 스레드"));
        Report first = threadMessageReport(11L, firstThread.getId(), 1001L, ReportReason.ABUSE);
        Report second = threadMessageReport(12L, firstThread.getId(), 1002L, ReportReason.ABUSE);
        Report third = threadMessageReport(13L, firstThread.getId(), 1003L, ReportReason.ABUSE);
        third.approve();
        Report fourth = threadMessageReport(14L, firstThread.getId(), 1004L, ReportReason.ABUSE);
        fourth.approve();
        Report fifth = threadMessageReport(15L, secondThread.getId(), 1005L, ReportReason.SPAM);
        Report legacyPost = Report.create(99L, ReportTargetType.POST, 1001L, null);
        Report legacyComment = Report.create(99L, ReportTargetType.COMMENT, 1001L, null);
        reportRepository.saveAllAndFlush(List.of(
            first,
            second,
            third,
            fourth,
            fifth,
            legacyPost,
            legacyComment
        ));
        alignCreatedAt(List.of(first, second, third, fourth, fifth, legacyPost, legacyComment));
        entityManager.clear();

        // when
        ThreadMessageReportSearchResult result = sut.search(new ThreadMessageReportSearchQuery(
            ReportStatus.PENDING,
            ReportReason.ABUSE,
            firstThread.getId(),
            null,
            0,
            20
        ));

        // then
        assertThat(result.total()).isEqualTo(2L);
        assertThat(result.reports())
            .extracting(Report::getId)
            .containsExactly(second.getId(), first.getId());
        assertThat(result.reports())
            .allMatch(report -> report.getTargetType() == ReportTargetType.THREAD_MESSAGE);
    }

    @Test
    @DisplayName("createdAt 동률은 id 내림차순 tie-breaker로 고정하고 legacy POST/COMMENT를 total과 결과에서 제외한다")
    void search_동일_createdAt은_id_내림차순이고_legacy는_제외한다() {
        // given
        CommunityThread firstThread = threadRepository.saveAndFlush(createThread(120L, "정렬 스레드"));
        Report first = threadMessageReport(21L, firstThread.getId(), 2001L, ReportReason.SPAM);
        Report second = threadMessageReport(22L, firstThread.getId(), 2002L, ReportReason.SPAM);
        Report third = threadMessageReport(23L, firstThread.getId(), 2003L, ReportReason.SPAM);
        Report legacyPost = Report.create(21L, ReportTargetType.POST, 2001L, null);
        Report legacyComment = Report.create(21L, ReportTargetType.COMMENT, 2002L, null);
        reportRepository.saveAllAndFlush(List.of(first, second, third, legacyPost, legacyComment));
        alignCreatedAt(List.of(first, second, third, legacyPost, legacyComment));
        entityManager.clear();

        // when
        ThreadMessageReportSearchResult result = sut.search(new ThreadMessageReportSearchQuery(
            ReportStatus.PENDING,
            null,
            null,
            null,
            1,
            1
        ));

        // then
        assertThat(result.total()).isEqualTo(3L);
        assertThat(result.reports())
            .extracting(Report::getId)
            .containsExactly(second.getId());
        assertThat(result.reports())
            .extracting(Report::getTargetType)
            .containsOnly(ReportTargetType.THREAD_MESSAGE);
    }

    @Test
    @DisplayName("상태와 사유와 Challenger reporterId를 함께 걸면 일치하는 신고만 반환한다")
    void search_상태_사유_Challenger_필터를_함께_적용한다() {
        // given
        CommunityThread thread = threadRepository.saveAndFlush(createThread(130L, "복합 필터 스레드"));
        Report approvedMatch = threadMessageReport(31L, thread.getId(), 3001L, ReportReason.ETC);
        approvedMatch.approve();
        Report approvedDifferentReporter = threadMessageReport(32L, thread.getId(), 3002L, ReportReason.ETC);
        approvedDifferentReporter.approve();
        Report pendingSameReporter = threadMessageReport(31L, thread.getId(), 3003L, ReportReason.ETC);
        reportRepository.saveAllAndFlush(List.of(
            approvedMatch,
            approvedDifferentReporter,
            pendingSameReporter
        ));
        entityManager.clear();

        // when
        ThreadMessageReportSearchResult result = sut.search(new ThreadMessageReportSearchQuery(
            ReportStatus.APPROVED,
            ReportReason.ETC,
            thread.getId(),
            31L,
            0,
            10
        ));

        // then
        assertThat(result.total()).isEqualTo(1L);
        assertThat(result.reports())
            .extracting(Report::getId)
            .containsExactly(approvedMatch.getId());
    }

    private Report threadMessageReport(
        Long reporterId,
        Long threadId,
        Long messageId,
        ReportReason reason
    ) {
        return Report.createThreadMessage(reporterId, threadId, messageId, reason);
    }

    private CommunityThread createThread(Long chatRoomId, String title) {
        return CommunityThread.create(
            chatRoomId,
            title,
            null,
            CommunityThreadCategory.FREE,
            "🚨",
            10L,
            20L,
            NOW
        );
    }

    private void alignCreatedAt(List<Report> reports) {
        for (Report report : reports) {
            entityManager.getEntityManager()
                .createNativeQuery("""
                    update report
                    set created_at = :createdAt,
                        updated_at = :createdAt
                    where id = :id
                    """)
                .setParameter("createdAt", Timestamp.from(NOW))
                .setParameter("id", report.getId())
                .executeUpdate();
        }
    }
}
