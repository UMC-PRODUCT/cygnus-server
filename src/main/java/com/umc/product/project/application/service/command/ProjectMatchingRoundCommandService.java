package com.umc.product.project.application.service.command;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Objects;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import com.umc.product.authorization.application.port.in.query.GetChallengerRoleUseCase;
import com.umc.product.authorization.application.port.in.query.dto.ChallengerRoleInfo;
import com.umc.product.common.domain.enums.ChallengerRoleType;
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

    private final GetChallengerRoleUseCase getChallengerRoleUseCase;
    private final ProjectMatchingRoundProperties projectMatchingRoundProperties;

    @Override
    public Long create(CreateProjectMatchingRoundCommand command) {
        // 지부장 이상부터 매칭 차수에 대한 수정이 가능합니다.
        validateManageAccess(command.requesterMemberId(), command.chapterId());
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

        validateManageAccess(command.requesterMemberId(), matchingRound.getChapterId());
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
        validateManageAccess(requesterMemberId, matchingRound.getChapterId());

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

    private void validateManageAccess(Long memberId, Long chapterId) {
        if (getChallengerRoleUseCase.isSuperAdmin(memberId)) {
            return;
        }
        List<ChallengerRoleInfo> roles = getChallengerRoleUseCase.findAllByMemberId(memberId);
        boolean allowed = roles.stream()
            .anyMatch(role -> role.roleType().isAtLeastCentralCore()
                || (role.roleType() == ChallengerRoleType.CHAPTER_PRESIDENT
                && Objects.equals(role.organizationId(), chapterId)));

        if (!allowed) {
            throw new ProjectDomainException(ProjectErrorCode.PROJECT_MATCHING_ROUND_ACCESS_DENIED);
        }
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
