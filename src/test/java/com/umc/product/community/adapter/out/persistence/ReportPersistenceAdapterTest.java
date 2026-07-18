package com.umc.product.community.adapter.out.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.Import;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import com.umc.product.community.domain.CommunityThread;
import com.umc.product.community.domain.Report;
import com.umc.product.community.domain.enums.CommunityThreadCategory;
import com.umc.product.community.domain.enums.ReportReason;
import com.umc.product.community.domain.enums.ReportTargetType;
import com.umc.product.community.domain.exception.CommunityDomainException;
import com.umc.product.community.domain.exception.CommunityErrorCode;
import com.umc.product.support.PersistenceAdapterTest;

@PersistenceAdapterTest
@Import(ReportPersistenceAdapter.class)
@DisplayName("ReportPersistenceAdapter")
class ReportPersistenceAdapterTest {

    private static final Instant NOW = Instant.parse("2026-07-18T00:00:00Z");

    @Autowired
    ReportPersistenceAdapter sut;

    @Autowired
    ReportRepository reportRepository;

    @Autowired
    CommunityThreadRepository threadRepository;

    @Autowired
    PlatformTransactionManager transactionManager;

    @Autowired
    TestEntityManager entityManager;

    @Test
    @DisplayName("legacy 신고와 스레드 메시지 신고를 같은 테이블에 보존한다")
    void save_legacy와_스레드_메시지를_보존한다() {
        // given
        Report legacy = Report.create(1L, ReportTargetType.COMMENT, 2L, "기존 사유");
        CommunityThread thread = threadRepository.save(createThread(130L));
        Report threadMessage = Report.createThreadMessage(3L, thread.getId(), 5L, ReportReason.SPAM);

        // when
        sut.save(legacy);
        sut.save(threadMessage);
        reportRepository.flush();
        Long legacyId = legacy.getId();
        Long threadMessageId = threadMessage.getId();
        entityManager.clear();

        // then
        Report reloadedLegacy = sut.findById(legacyId).orElseThrow();
        Report reloadedThreadMessage = sut.findById(threadMessageId).orElseThrow();
        assertThat(sut.existsByReporterIdAndTargetTypeAndTargetId(1L, ReportTargetType.COMMENT, 2L)).isTrue();
        assertThat(sut.existsThreadMessageReport(3L, 5L)).isTrue();
        assertThat(reloadedLegacy.getReason()).isEqualTo("기존 사유");
        assertThat(reloadedThreadMessage.getThreadId()).isEqualTo(thread.getId());
        assertThat(reloadedThreadMessage.getReasonCode()).isEqualTo(ReportReason.SPAM);
    }

    @Test
    @DisplayName("partial unique는 기존 POST COMMENT 중복 row를 제한하지 않는다")
    void partialUnique_legacy_중복을_보존한다() {
        // given
        Report first = Report.create(6L, ReportTargetType.POST, 7L, null);
        Report second = Report.create(6L, ReportTargetType.POST, 7L, null);

        // when
        reportRepository.saveAllAndFlush(List.of(first, second));

        // then
        assertThat(reportRepository.countByReporterIdAndTargetTypeAndTargetId(
            6L,
            ReportTargetType.POST,
            7L
        )).isEqualTo(2L);
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    @DisplayName("동시 스레드 메시지 중복 신고는 DB partial unique로 하나만 저장한다")
    void concurrentDuplicateReport_하나만_저장한다() throws Exception {
        // given
        TransactionTemplate transactionTemplate = new TransactionTemplate(transactionManager);
        Long threadId = transactionTemplate.execute(status -> threadRepository.save(createThread(131L)).getId());
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);
        ExecutorService executor = Executors.newFixedThreadPool(2);

        try {
            Future<SaveResult> first = executor.submit(
                () -> saveAfterSignal(transactionTemplate, ready, start, threadId)
            );
            Future<SaveResult> second = executor.submit(
                () -> saveAfterSignal(transactionTemplate, ready, start, threadId)
            );
            assertThat(ready.await(5, TimeUnit.SECONDS)).isTrue();

            // when
            start.countDown();
            List<SaveResult> results = List.of(first.get(5, TimeUnit.SECONDS), second.get(5, TimeUnit.SECONDS));

            // then
            assertThat(results).containsExactlyInAnyOrder(SaveResult.SAVED, SaveResult.DUPLICATE);
            Long count = transactionTemplate.execute(status -> reportRepository
                .countByReporterIdAndTargetTypeAndTargetId(10L, ReportTargetType.THREAD_MESSAGE, 30L));
            assertThat(count).isOne();
        } finally {
            start.countDown();
            executor.shutdownNow();
            transactionTemplate.executeWithoutResult(status -> {
                reportRepository.deleteAllInBatch();
                threadRepository.deleteById(threadId);
            });
        }
    }

    private SaveResult saveAfterSignal(
        TransactionTemplate transactionTemplate,
        CountDownLatch ready,
        CountDownLatch start,
        Long threadId
    ) throws InterruptedException {
        ready.countDown();
        if (!start.await(5, TimeUnit.SECONDS)) {
            throw new IllegalStateException("concurrent report start timed out");
        }
        try {
            transactionTemplate.executeWithoutResult(status -> sut.save(
                Report.createThreadMessage(10L, threadId, 30L, ReportReason.ABUSE)
            ));
            return SaveResult.SAVED;
        } catch (CommunityDomainException exception) {
            assertThat(exception.getBaseCode()).isEqualTo(CommunityErrorCode.REPORT_ALREADY_EXISTS);
            return SaveResult.DUPLICATE;
        }
    }

    private enum SaveResult {
        SAVED,
        DUPLICATE
    }

    private CommunityThread createThread(Long chatRoomId) {
        return CommunityThread.create(
            chatRoomId,
            "신고 스레드",
            null,
            CommunityThreadCategory.FREE,
            "🚨",
            10L,
            20L,
            NOW
        );
    }
}
