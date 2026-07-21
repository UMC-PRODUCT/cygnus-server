package com.umc.product.recruiting.adapter.out.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import com.umc.product.common.domain.enums.ChallengerTrack;
import com.umc.product.form.application.port.in.command.ManageFormUseCase;
import com.umc.product.form.application.port.in.query.GetFormResponseUseCase;
import com.umc.product.form.application.port.in.query.GetFormUseCase;
import com.umc.product.recruiting.application.port.in.command.CloseRecruitingApplicationFormUseCase;
import com.umc.product.recruiting.application.port.in.command.PublishRecruitingApplicationFormUseCase;
import com.umc.product.recruiting.application.port.in.command.UnpublishRecruitingApplicationFormUseCase;
import com.umc.product.recruiting.application.port.in.command.dto.CreateRecruitingRoundCommand;
import com.umc.product.recruiting.application.port.in.command.dto.RecruitingRoundConfigurationCommand;
import com.umc.product.recruiting.application.port.out.LoadRecruitingApplicationFormPort;
import com.umc.product.recruiting.application.port.out.LoadRecruitingApplicationPort;
import com.umc.product.recruiting.application.service.command.RecruitingRoundCommandService;
import com.umc.product.recruiting.domain.RecruitingRound;
import com.umc.product.recruiting.domain.RecruitingSeason;
import com.umc.product.recruiting.domain.RecruitingSeasonTrackQuota;
import com.umc.product.recruiting.domain.enums.RecruitingRoundType;
import com.umc.product.recruiting.domain.exception.RecruitingDomainException;
import com.umc.product.recruiting.domain.exception.RecruitingErrorCode;
import com.umc.product.support.PersistenceAdapterTest;

@PersistenceAdapterTest
@Transactional(propagation = Propagation.NOT_SUPPORTED)
@Import({
    RecruitingSeasonPersistenceAdapter.class,
    RecruitingSeasonTrackQuotaPersistenceAdapter.class,
    RecruitingRoundPersistenceAdapter.class,
    RecruitingRoundCommandService.class
})
class RecruitingRoundCreationConcurrencyTest {

    @Autowired
    RecruitingSeasonPersistenceAdapter seasonAdapter;
    @Autowired
    RecruitingSeasonTrackQuotaPersistenceAdapter quotaAdapter;
    @Autowired
    RecruitingRoundPersistenceAdapter roundAdapter;
    @Autowired
    RecruitingRoundCommandService roundCommandService;
    @Autowired
    PlatformTransactionManager transactionManager;

    @MockitoBean
    LoadRecruitingApplicationPort loadApplicationPort;
    @MockitoBean
    LoadRecruitingApplicationFormPort loadApplicationFormPort;
    @MockitoBean
    PublishRecruitingApplicationFormUseCase publishApplicationFormUseCase;
    @MockitoBean
    CloseRecruitingApplicationFormUseCase closeApplicationFormUseCase;
    @MockitoBean
    UnpublishRecruitingApplicationFormUseCase unpublishApplicationFormUseCase;
    @MockitoBean
    ManageFormUseCase manageFormUseCase;
    @MockitoBean
    GetFormUseCase getFormUseCase;
    @MockitoBean
    GetFormResponseUseCase getFormResponseUseCase;

    @Test
    @DisplayName("동시 추가모집 생성은 Season lock으로 차수를 1부터 순차 배정한다")
    void concurrentAdditionalRoundsReceiveSequentialNumbers() throws Exception {
        Long seasonId = persistSeason(701L, 702L);

        List<CreateResult> results = createConcurrently(
            command(seasonId, "첫 번째 추가모집"),
            command(seasonId, "두 번째 추가모집")
        );

        assertThat(results).allMatch(CreateResult::success);
        assertThat(rounds(seasonId))
            .extracting(RecruitingRound::getRoundNo)
            .containsExactly(1, 2);
    }

    @Test
    @DisplayName("동일 제목의 동시 Round 생성은 하나만 성공한다")
    void concurrentDuplicateTitlesAllowOnlyOneRound() throws Exception {
        Long seasonId = persistSeason(703L, 704L);

        List<CreateResult> results = createConcurrently(
            command(seasonId, "동일 제목"),
            command(seasonId, "동일 제목")
        );

        assertThat(results.stream().filter(CreateResult::success)).hasSize(1);
        assertThat(results.stream().filter(result -> result.errorCode()
            == RecruitingErrorCode.RECRUITING_ROUND_TITLE_ALREADY_EXISTS)).hasSize(1);
        assertThat(rounds(seasonId)).hasSize(1);
    }

    private List<CreateResult> createConcurrently(
        CreateRecruitingRoundCommand firstCommand,
        CreateRecruitingRoundCommand secondCommand
    ) throws Exception {
        CountDownLatch workersReady = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);
        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            Future<CreateResult> first = executor.submit(() -> create(firstCommand, workersReady, start));
            Future<CreateResult> second = executor.submit(() -> create(secondCommand, workersReady, start));
            workersReady.await();
            start.countDown();
            return List.of(first.get(), second.get());
        } finally {
            executor.shutdownNow();
        }
    }

    private CreateResult create(
        CreateRecruitingRoundCommand command,
        CountDownLatch workersReady,
        CountDownLatch start
    ) throws Exception {
        workersReady.countDown();
        start.await();
        try {
            return new CreateResult(roundCommandService.createRound(command), null);
        } catch (RecruitingDomainException exception) {
            return new CreateResult(null, (RecruitingErrorCode) exception.getBaseCode());
        }
    }

    private Long persistSeason(Long gisuId, Long schoolId) {
        TransactionTemplate transaction = new TransactionTemplate(transactionManager);
        return transaction.execute(status -> {
            RecruitingSeason season = seasonAdapter.save(RecruitingSeason.create(gisuId, schoolId));
            quotaAdapter.saveAll(List.of(RecruitingSeasonTrackQuota.create(
                season,
                ChallengerTrack.PLAN,
                3
            )));
            return season.getId();
        });
    }

    private List<RecruitingRound> rounds(Long seasonId) {
        TransactionTemplate transaction = new TransactionTemplate(transactionManager);
        return transaction.execute(status -> roundAdapter.listBySeasonId(seasonId));
    }

    private CreateRecruitingRoundCommand command(Long seasonId, String title) {
        return CreateRecruitingRoundCommand.builder()
            .seasonId(seasonId)
            .type(RecruitingRoundType.ADDITIONAL)
            .title(title)
            .configuration(RecruitingRoundConfigurationCommand.of(
                List.of(ChallengerTrack.PLAN),
                false,
                Instant.parse("2026-08-01T00:00:00Z"),
                Instant.parse("2026-08-08T00:00:00Z"),
                Instant.parse("2026-08-10T00:00:00Z"),
                false,
                null,
                null,
                Instant.parse("2026-08-16T00:00:00Z"),
                null,
                null,
                null
            ))
            .build();
    }

    private record CreateResult(Long roundId, RecruitingErrorCode errorCode) {

        private boolean success() {
            return roundId != null;
        }
    }
}
