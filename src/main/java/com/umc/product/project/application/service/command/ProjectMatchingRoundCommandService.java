package com.umc.product.project.application.service.command;

import java.time.Duration;
import java.time.Instant;
import java.util.Objects;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import com.umc.product.authorization.domain.policy.PolicyEffect;
import com.umc.product.organization.application.port.in.query.GetChapterUseCase;
import com.umc.product.organization.application.port.in.query.GetGisuUseCase;
import com.umc.product.organization.application.port.in.query.dto.gisu.GisuInfo;
import com.umc.product.project.application.authorization.ProjectPolicyAction;
import com.umc.product.project.application.authorization.ProjectPolicyAuthorizationService;
import com.umc.product.project.application.authorization.ProjectPolicyResourceContext;
import com.umc.product.project.application.port.in.command.CreateProjectMatchingRoundUseCase;
import com.umc.product.project.application.port.in.command.DeleteProjectMatchingRoundUseCase;
import com.umc.product.project.application.port.in.command.UpdateProjectMatchingRoundUseCase;
import com.umc.product.project.application.port.in.command.dto.CreateProjectMatchingRoundCommand;
import com.umc.product.project.application.port.in.command.dto.UpdateProjectMatchingRoundCommand;
import com.umc.product.project.application.port.out.LoadProjectApplicationPort;
import com.umc.product.project.application.port.out.LoadProjectMatchingRoundPort;
import com.umc.product.project.application.port.out.SaveProjectMatchingRoundPort;
import com.umc.product.project.application.port.out.ScheduleMatchingRoundDeadlinePort;
import com.umc.product.project.domain.ProjectMatchingRound;
import com.umc.product.project.domain.enums.MatchingPhase;
import com.umc.product.project.domain.enums.MatchingType;
import com.umc.product.project.domain.exception.ProjectDomainException;
import com.umc.product.project.domain.exception.ProjectErrorCode;

import lombok.RequiredArgsConstructor;

/**
 * 매칭 차수의 CRUD lifecycle 만 담당한다.
 * <p>
 * 차수 종료 시점의 자동 선발(autoDecide) 처리는
 * {@link ProjectMatchingRoundFinalizationCommandService} 가 담당한다.
 * 두 책임을 분리하지 않으면 lifecycle 호출 흐름과 트리거 흐름이 한 클래스에 모여 순환 의존성이 발생한다.
 */
@Service
@RequiredArgsConstructor
@Transactional
public class ProjectMatchingRoundCommandService implements
    CreateProjectMatchingRoundUseCase,
    UpdateProjectMatchingRoundUseCase,
    DeleteProjectMatchingRoundUseCase {

    private final LoadProjectMatchingRoundPort loadProjectMatchingRoundPort;
    private final SaveProjectMatchingRoundPort saveProjectMatchingRoundPort;
    private final LoadProjectApplicationPort loadProjectApplicationPort;
    private final ScheduleMatchingRoundDeadlinePort scheduleMatchingRoundDeadlinePort;

    private final GetGisuUseCase getGisuUseCase;
    private final GetChapterUseCase getChapterUseCase;
    private final ProjectMatchingRoundProperties projectMatchingRoundProperties;
    private final ProjectPolicyAuthorizationService projectPolicyAuthorizationService;

    @Override
    public Long create(CreateProjectMatchingRoundCommand command) {
        validateGisuAndChapter(command.gisuId(), command.chapterId());
        validateWithinGisuPeriod(
            command.gisuId(), command.startsAt(), command.endsAt(), command.decisionDeadline());
        // 지부장 이상부터 매칭 차수에 대한 수정이 가능합니다.
        validateManageAccess(
            command.requesterMemberId(),
            ProjectPolicyAction.MATCHING_CREATE,
            ProjectPolicyResourceContext.builder()
                .matchingRound(null, command.gisuId(), command.chapterId())
                .build()
        );
        // 매칭 차수는 중복될 수 없습니다.
        validateNoOverlap(
            command.chapterId(), command.startsAt(), command.endsAt(), command.decisionDeadline());
        validatePhaseSequence(
            null,
            command.chapterId(),
            command.type(),
            command.phase(),
            command.startsAt(),
            command.decisionDeadline()
        );

        ProjectMatchingRound matchingRound = ProjectMatchingRound.create(
            command.name(),
            command.description(),
            command.type(),
            command.phase(),
            command.gisuId(),
            command.chapterId(),
            command.startsAt(),
            command.endsAt(),
            command.decisionDeadline()
        );

        ProjectMatchingRound saved = saveProjectMatchingRoundPort.save(matchingRound);
        scheduleAfterCommit(saved);
        return saved.getId();
    }

    @Override
    public void update(UpdateProjectMatchingRoundCommand command) {
        ProjectMatchingRound matchingRound = loadProjectMatchingRoundPort.getById(command.matchingRoundId());

        String name = command.name() != null ? command.name() : matchingRound.getName();
        String description = command.description() != null ? command.description() : matchingRound.getDescription();
        MatchingType type = command.type() != null ? command.type() : matchingRound.getType();
        MatchingPhase phase = command.phase() != null ? command.phase() : matchingRound.getPhase();
        Instant startsAt = command.startsAt() != null ? command.startsAt() : matchingRound.getStartsAt();
        Instant endsAt = command.endsAt() != null ? command.endsAt() : matchingRound.getEndsAt();
        Instant decisionDeadline = command.decisionDeadline() != null
            ? command.decisionDeadline()
            : matchingRound.getDecisionDeadline();

        validateGisuAndChapter(matchingRound.getGisuId(), matchingRound.getChapterId());
        validateWithinGisuPeriod(matchingRound.getGisuId(), startsAt, endsAt, decisionDeadline);
        validateManageAccess(
            command.requesterMemberId(),
            ProjectPolicyAction.MATCHING_UPDATE,
            matchingRoundContext(matchingRound)
        );
        validateNoOverlapExceptId(
            matchingRound.getId(),
            matchingRound.getChapterId(),
            startsAt,
            endsAt,
            decisionDeadline
        );
        validatePhaseSequence(
            matchingRound.getId(),
            matchingRound.getChapterId(),
            type,
            phase,
            startsAt,
            decisionDeadline
        );

        matchingRound.update(
            name,
            description,
            type,
            phase,
            startsAt,
            endsAt,
            decisionDeadline
        );
        scheduleAfterCommit(matchingRound);
    }

    @Override
    public void delete(Long matchingRoundId, Long requesterMemberId) {
        ProjectMatchingRound matchingRound = loadProjectMatchingRoundPort.getById(matchingRoundId);
        validateManageAccess(
            requesterMemberId,
            ProjectPolicyAction.MATCHING_DELETE,
            matchingRoundContext(matchingRound)
        );

        if (loadProjectApplicationPort.existsByAppliedMatchingRoundId(matchingRoundId)) {
            throw new ProjectDomainException(ProjectErrorCode.PROJECT_MATCHING_ROUND_DELETE_CONFLICT);
        }

        saveProjectMatchingRoundPort.delete(matchingRound);
        cancelAfterCommit(matchingRoundId);
    }

    private void validateNoOverlap(
        Long chapterId, Instant startsAt, Instant endsAt, Instant decisionDeadline
    ) {
        ProjectMatchingRound.validateDates(startsAt, endsAt, decisionDeadline);
        if (!loadProjectMatchingRoundPort.listOverlapping(chapterId, startsAt, decisionDeadline).isEmpty()) {
            throw new ProjectDomainException(ProjectErrorCode.PROJECT_MATCHING_ROUND_PERIOD_OVERLAPPED);
        }
    }

    private void validateGisuAndChapter(Long gisuId, Long chapterId) {
        if (!getChapterUseCase.belongsToGisu(chapterId, gisuId)) {
            throw new ProjectDomainException(ProjectErrorCode.PROJECT_MATCHING_ROUND_GISU_CHAPTER_MISMATCH);
        }
    }

    private void validateWithinGisuPeriod(
        Long gisuId, Instant startsAt, Instant endsAt, Instant decisionDeadline
    ) {
        ProjectMatchingRound.validateDates(startsAt, endsAt, decisionDeadline);
        GisuInfo gisu = getGisuUseCase.getById(gisuId);
        if (startsAt.isBefore(gisu.startAt()) || !decisionDeadline.isBefore(gisu.endAt())) {
            throw new ProjectDomainException(ProjectErrorCode.PROJECT_MATCHING_ROUND_OUTSIDE_GISU_PERIOD);
        }
    }

    private void validateNoOverlapExceptId(
        Long id, Long chapterId, Instant startsAt, Instant endsAt, Instant decisionDeadline
    ) {
        ProjectMatchingRound.validateDates(startsAt, endsAt, decisionDeadline);
        if (!loadProjectMatchingRoundPort.listOverlappingExceptId(
            id, chapterId, startsAt, decisionDeadline).isEmpty()) {
            throw new ProjectDomainException(ProjectErrorCode.PROJECT_MATCHING_ROUND_PERIOD_OVERLAPPED);
        }
    }

    /**
     * 트랜잭션 커밋 이후에 스케줄을 등록한다. 롤백 시 in-memory task 만 남는 사고를 방지한다.
     */
    private void scheduleAfterCommit(ProjectMatchingRound round) {
        runAfterCommit(() -> scheduleMatchingRoundDeadlinePort.schedule(round));
    }

    /**
     * 트랜잭션 커밋 이후에 스케줄을 취소한다. 롤백 시 잘못된 task 가 남거나 의도치 않게 취소되는 사고를 방지한다.
     */
    private void cancelAfterCommit(Long roundId) {
        runAfterCommit(() -> scheduleMatchingRoundDeadlinePort.cancel(roundId));
    }

    /**
     * 트랜잭션 동기화가 활성화돼 있으면 commit 이후로 미루고, 그렇지 않으면 즉시 실행한다.
     * <p>
     * 단위 테스트처럼 트랜잭션 컨텍스트 없이 호출되는 경우에도 동작하도록 fallback 을 둔다.
     */
    private static void runAfterCommit(Runnable action) {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(
                new TransactionSynchronization() {
                    @Override
                    public void afterCommit() {
                        action.run();
                    }
                });
            return;
        }
        action.run();
    }

    private void validateManageAccess(
        Long memberId,
        ProjectPolicyAction action,
        ProjectPolicyResourceContext resource
    ) {
        if (projectPolicyAuthorizationService.evaluate(memberId, action, resource).effect() != PolicyEffect.ALLOW) {
            throw new ProjectDomainException(ProjectErrorCode.PROJECT_MATCHING_ROUND_ACCESS_DENIED);
        }
    }

    private ProjectPolicyResourceContext matchingRoundContext(ProjectMatchingRound matchingRound) {
        return ProjectPolicyResourceContext.builder()
            .matchingRound(
                matchingRound.getId(),
                matchingRound.getGisuId(),
                matchingRound.getChapterId()
            )
            .build();
    }

    private void validatePhaseSequence(
        Long currentRoundId,
        Long chapterId,
        MatchingType type,
        MatchingPhase phase,
        Instant startsAt,
        Instant decisionDeadline
    ) {
        loadProjectMatchingRoundPort.listByChapterId(chapterId).stream()
            .filter(round -> !Objects.equals(round.getId(), currentRoundId))
            .filter(round -> round.getType() == type)
            .forEach(round -> validatePhaseSequence(round, phase, startsAt, decisionDeadline));
    }

    private void validatePhaseSequence(
        ProjectMatchingRound existingRound,
        MatchingPhase phase,
        Instant startsAt,
        Instant decisionDeadline
    ) {
        int phaseOrder = existingRound.getPhase().compareTo(phase);
        Duration minPhaseInterval = projectMatchingRoundProperties.minPhaseInterval();

        if (phaseOrder == 0) {
            throw new ProjectDomainException(ProjectErrorCode.PROJECT_MATCHING_ROUND_PHASE_SEQUENCE_INVALID);
        }

        if (phaseOrder < 0 && startsAt.isBefore(existingRound.getDecisionDeadline().plus(minPhaseInterval))) {
            throw new ProjectDomainException(ProjectErrorCode.PROJECT_MATCHING_ROUND_PHASE_SEQUENCE_INVALID);
        }

        if (phaseOrder > 0 && existingRound.getStartsAt().isBefore(decisionDeadline.plus(minPhaseInterval))) {
            throw new ProjectDomainException(ProjectErrorCode.PROJECT_MATCHING_ROUND_PHASE_SEQUENCE_INVALID);
        }
    }
}
