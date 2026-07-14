package com.umc.product.project.application.service.command;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.TestPropertySource;

import com.fasterxml.jackson.databind.JsonNode;
import com.umc.product.common.domain.enums.ChallengerPart;
import com.umc.product.global.event.adapter.out.persistence.EventOutboxJpaRepository;
import com.umc.product.global.event.domain.EventOutbox;
import com.umc.product.member.domain.Member;
import com.umc.product.organization.domain.Chapter;
import com.umc.product.organization.domain.Gisu;
import com.umc.product.project.adapter.out.persistence.ProjectApplicationJpaRepository;
import com.umc.product.project.adapter.out.persistence.ProjectMemberJpaRepository;
import com.umc.product.project.application.authorization.ProjectPolicyAction;
import com.umc.product.project.application.authorization.rollout.ProjectAuthorizationRolloutMode;
import com.umc.product.project.application.authorization.rollout.ProjectAuthorizationRolloutModeResolver;
import com.umc.product.project.application.port.in.command.AutoDecideProjectMatchingRoundUseCase;
import com.umc.product.project.application.port.in.command.AutoDecisionActor;
import com.umc.product.project.application.port.out.LoadProjectMatchingRoundPort;
import com.umc.product.project.application.port.out.SaveProjectApplicationFormPort;
import com.umc.product.project.application.port.out.SaveProjectMatchingRoundPort;
import com.umc.product.project.application.port.out.SaveProjectPartQuotaPort;
import com.umc.product.project.application.port.out.SaveProjectPort;
import com.umc.product.project.domain.Project;
import com.umc.product.project.domain.ProjectApplication;
import com.umc.product.project.domain.ProjectApplicationForm;
import com.umc.product.project.domain.ProjectMatchingRound;
import com.umc.product.project.domain.ProjectPartQuota;
import com.umc.product.project.domain.enums.MatchingPhase;
import com.umc.product.project.domain.enums.MatchingType;
import com.umc.product.project.domain.enums.ProjectApplicationStatus;
import com.umc.product.project.domain.enums.ProjectMemberStatus;
import com.umc.product.project.domain.exception.ProjectDomainException;
import com.umc.product.project.domain.exception.ProjectErrorCode;
import com.umc.product.support.IntegrationTestSupport;
import com.umc.product.support.fixture.ChallengerFixture;
import com.umc.product.support.fixture.ChapterFixture;
import com.umc.product.support.fixture.GisuFixture;
import com.umc.product.support.fixture.MemberFixture;

import io.micrometer.core.instrument.MeterRegistry;

@TestPropertySource(properties = {
    "app.event-outbox.enabled=true",
    "spring.task.scheduling.enabled=false"
})
class ProjectMatchingRoundFinalizationConcurrencyIntegrationTest extends IntegrationTestSupport {

    private static final int CONCURRENT_INVOCATIONS = 2;
    private static final long WAIT_TIMEOUT_SECONDS = 20L;

    @Autowired
    private AutoDecideProjectMatchingRoundUseCase autoDecideUseCase;

    @Autowired
    private GisuFixture gisuFixture;

    @Autowired
    private ChapterFixture chapterFixture;

    @Autowired
    private MemberFixture memberFixture;

    @Autowired
    private ChallengerFixture challengerFixture;

    @Autowired
    private SaveProjectPort saveProjectPort;

    @Autowired
    private SaveProjectMatchingRoundPort saveProjectMatchingRoundPort;

    @Autowired
    private SaveProjectApplicationFormPort saveProjectApplicationFormPort;

    @Autowired
    private SaveProjectPartQuotaPort saveProjectPartQuotaPort;

    @Autowired
    private LoadProjectMatchingRoundPort loadProjectMatchingRoundPort;

    @Autowired
    private ProjectApplicationJpaRepository projectApplicationJpaRepository;

    @Autowired
    private ProjectMemberJpaRepository projectMemberJpaRepository;

    @Autowired
    private EventOutboxJpaRepository eventOutboxJpaRepository;

    @Autowired
    private ProjectAuthorizationRolloutModeResolver rolloutModeResolver;

    @Autowired
    private MeterRegistry meterRegistry;

    @Test
    @DisplayName("동시 SYSTEM 자동 확정 호출은 모든 영속 효과와 감사 outbox를 정확히 한 번만 만든다")
    void 동시_SYSTEM_자동_확정은_모든_효과를_정확히_한_번만_만든다() throws Exception {
        // given
        FinalizationFixture fixture = finalizableRoundWithOneRequiredApproval();
        CountDownLatch workersReady = new CountDownLatch(CONCURRENT_INVOCATIONS);
        CountDownLatch startGate = new CountDownLatch(1);
        ExecutorService executor = Executors.newFixedThreadPool(CONCURRENT_INVOCATIONS);
        double rolloutDecisionsBefore = rolloutDecisionCount();

        assertThat(rolloutModeResolver.resolve(ProjectPolicyAction.MATCHING_SYSTEM_AUTO_DECIDE))
            .isEqualTo(ProjectAuthorizationRolloutMode.SHADOW);

        Future<Void> first = executor.submit(() -> invokeAfterGate(fixture.roundId(), workersReady, startGate));
        Future<Void> second = executor.submit(() -> invokeAfterGate(fixture.roundId(), workersReady, startGate));

        // when
        List<Throwable> failures;
        try {
            assertThat(workersReady.await(WAIT_TIMEOUT_SECONDS, TimeUnit.SECONDS)).isTrue();
            startGate.countDown();
            failures = Arrays.asList(awaitFailure(first), awaitFailure(second));
        } finally {
            startGate.countDown();
            executor.shutdownNow();
            assertThat(executor.awaitTermination(WAIT_TIMEOUT_SECONDS, TimeUnit.SECONDS)).isTrue();
        }

        // then
        assertThat(failures).containsOnlyNulls();
        entityManager.clear();

        ProjectMatchingRound finalizedRound = loadProjectMatchingRoundPort.getById(fixture.roundId());
        assertThat(finalizedRound.getAutoDecisionExecutedAt()).isNotNull();
        assertThat(finalizedRound.getAutoDecisionExecutedMemberId()).isNull();

        List<ProjectApplication> applications = projectApplicationJpaRepository
            .findAllByAppliedMatchingRound_Id(fixture.roundId());
        assertThat(applications)
            .singleElement()
            .satisfies(application -> {
                assertThat(application.getId()).isEqualTo(fixture.applicationId());
                assertThat(application.getStatus()).isEqualTo(ProjectApplicationStatus.APPROVED);
                assertThat(application.getStatusChangedMemberId()).isNull();
                assertThat(application.getStatusChangeReason()).isEqualTo("auto-decide");
                assertThat(application.getStatusChangedAt()).isNotNull();
            });

        assertThat(projectMemberJpaRepository.findByProjectIdAndStatusOrderByCreatedAtAscIdAsc(
            fixture.projectId(), ProjectMemberStatus.ACTIVE
        )).singleElement().satisfies(member -> {
            assertThat(member.getMemberId()).isEqualTo(fixture.applicantMemberId());
            assertThat(member.getApplication().getId()).isEqualTo(fixture.applicationId());
            assertThat(member.getDecidedMemberId()).isNull();
        });

        List<EventOutbox> auditOutboxes = eventOutboxJpaRepository.findAll().stream()
            .filter(outbox -> "audit.log.finalize".equals(outbox.getEventType()))
            .filter(outbox -> isMatchingRoundAudit(outbox, fixture.roundId()))
            .toList();
        assertThat(auditOutboxes).hasSize(1);
        assertThat(rolloutDecisionCount() - rolloutDecisionsBefore)
            .isEqualTo(CONCURRENT_INVOCATIONS);
    }

    @Test
    @DisplayName("권한 없는 MEMBER 자동 확정 호출은 영속 효과와 감사 outbox를 만들지 않는다")
    void 권한_없는_MEMBER_자동_확정은_모든_효과를_만들지_않는다() {
        // given
        FinalizationFixture fixture = finalizableRoundWithOneRequiredApproval();
        Member unauthorized = memberFixture.일반("unauthorized-finalization");

        // when & then
        assertThatThrownBy(() -> autoDecideUseCase.autoDecide(
            fixture.roundId(), new AutoDecisionActor.Member(unauthorized.getId())))
            .isInstanceOf(ProjectDomainException.class)
            .extracting("baseCode")
            .isEqualTo(ProjectErrorCode.PROJECT_MATCHING_ROUND_ACCESS_DENIED);
        entityManager.clear();

        ProjectMatchingRound unchangedRound = loadProjectMatchingRoundPort.getById(fixture.roundId());
        assertThat(unchangedRound.getAutoDecisionExecutedAt()).isNull();
        assertThat(unchangedRound.getAutoDecisionExecutedMemberId()).isNull();
        assertThat(projectApplicationJpaRepository.findAllByAppliedMatchingRound_Id(fixture.roundId()))
            .singleElement()
            .satisfies(application ->
                assertThat(application.getStatus()).isEqualTo(ProjectApplicationStatus.SUBMITTED));
        assertThat(projectMemberJpaRepository.findByProjectIdAndStatusOrderByCreatedAtAscIdAsc(
            fixture.projectId(), ProjectMemberStatus.ACTIVE
        )).isEmpty();
        assertThat(eventOutboxJpaRepository.findAll().stream()
            .filter(outbox -> "audit.log.finalize".equals(outbox.getEventType()))
            .filter(outbox -> isMatchingRoundAudit(outbox, fixture.roundId())))
            .isEmpty();
    }

    private Void invokeAfterGate(
        Long roundId,
        CountDownLatch workersReady,
        CountDownLatch startGate
    ) throws InterruptedException {
        workersReady.countDown();
        if (!startGate.await(WAIT_TIMEOUT_SECONDS, TimeUnit.SECONDS)) {
            throw new IllegalStateException("동시 실행 start gate가 열리지 않았습니다.");
        }
        autoDecideUseCase.autoDecide(roundId, AutoDecisionActor.matchingRoundScheduler());
        return null;
    }

    private Throwable awaitFailure(Future<Void> invocation) throws InterruptedException {
        try {
            invocation.get(WAIT_TIMEOUT_SECONDS, TimeUnit.SECONDS);
            return null;
        } catch (ExecutionException exception) {
            return exception.getCause();
        } catch (TimeoutException exception) {
            return exception;
        }
    }

    private boolean isMatchingRoundAudit(EventOutbox outbox, Long roundId) {
        try {
            JsonNode payload = objectMapper.readTree(outbox.getPayload());
            return "ProjectMatchingRound".equals(payload.path("targetType").asText())
                && roundId.toString().equals(payload.path("targetId").asText());
        } catch (Exception exception) {
            throw new AssertionError("감사 outbox payload를 읽을 수 없습니다.", exception);
        }
    }

    private double rolloutDecisionCount() {
        return meterRegistry.find("project.authorization.decision.total")
            .tag("action", ProjectPolicyAction.MATCHING_SYSTEM_AUTO_DECIDE.id())
            .tag("mode", ProjectAuthorizationRolloutMode.SHADOW.name())
            .counters()
            .stream()
            .mapToDouble(counter -> counter.count())
            .sum();
    }

    private FinalizationFixture finalizableRoundWithOneRequiredApproval() {
        Instant now = Instant.now();
        Gisu gisu = gisuFixture.비활성_기수(
            8_001L,
            now.minusSeconds(86_400L),
            now.plusSeconds(86_400L)
        );
        Chapter chapter = chapterFixture.지부(gisu, "concurrency-chapter");
        Member owner = memberFixture.일반("owner-concurrency");
        Member applicant = memberFixture.일반("applicant-concurrency");
        challengerFixture.웹(applicant.getId(), gisu.getId());

        Project project = saveProjectPort.save(Project.createDraft(
            gisu.getId(), chapter.getId(), owner.getId(), 1L, owner.getId()
        ));
        saveProjectPartQuotaPort.save(ProjectPartQuota.create(
            project, ChallengerPart.WEB, 1L, owner.getId()
        ));
        ProjectMatchingRound round = saveProjectMatchingRoundPort.save(ProjectMatchingRound.create(
            "동시성 검증 차수",
            "두 SYSTEM 호출은 정확히 한 번만 확정되어야 합니다.",
            MatchingType.PLAN_DEVELOPER,
            MatchingPhase.FIRST,
            gisu.getId(),
            chapter.getId(),
            now.minusSeconds(10_800L),
            now.minusSeconds(7_200L),
            now.minusSeconds(3_600L)
        ));
        ProjectApplicationForm form = saveProjectApplicationFormPort.save(
            ProjectApplicationForm.create(project, 10_001L)
        );
        ProjectApplication application = ProjectApplication.create(
            form, 20_001L, applicant.getId(), round
        );
        application.submit();
        application = projectApplicationJpaRepository.saveAndFlush(application);

        return new FinalizationFixture(
            round.getId(), project.getId(), application.getId(), applicant.getId()
        );
    }

    private record FinalizationFixture(
        Long roundId,
        Long projectId,
        Long applicationId,
        Long applicantMemberId
    ) {
    }
}
