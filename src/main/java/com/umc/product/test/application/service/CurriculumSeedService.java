package com.umc.product.test.application.service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.umc.product.common.domain.enums.ChallengerPart;
import com.umc.product.common.domain.enums.ChallengerTrack;
import com.umc.product.common.domain.enums.GisuLearningType;
import com.umc.product.curriculum.application.port.in.command.ManageCurriculumUseCase;
import com.umc.product.curriculum.application.port.in.command.ManageOriginalWorkbookMissionUseCase;
import com.umc.product.curriculum.application.port.in.command.ManageOriginalWorkbookUseCase;
import com.umc.product.curriculum.application.port.in.command.ManageWeeklyCurriculumUseCase;
import com.umc.product.curriculum.application.port.in.command.dto.curriculum.CreateCurriculumCommand;
import com.umc.product.curriculum.application.port.in.command.dto.curriculum.CreateWeeklyCurriculumCommand;
import com.umc.product.curriculum.application.port.in.command.dto.workbook.ChangeOriginalWorkbookStatusCommand;
import com.umc.product.curriculum.application.port.in.command.dto.workbook.CreateOriginalWorkbookCommand;
import com.umc.product.curriculum.domain.enums.OriginalWorkbookStatus;
import com.umc.product.curriculum.domain.exception.CurriculumDomainException;
import com.umc.product.curriculum.domain.exception.CurriculumErrorCode;
import com.umc.product.organization.application.port.in.query.GetGisuUseCase;
import com.umc.product.test.application.port.in.command.SeedCurriculumUseCase;
import com.umc.product.test.application.port.in.command.dto.SeedCurriculumCommand;
import com.umc.product.test.application.port.in.command.dto.SeedCurriculumResult;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Curriculum 시딩 서비스. ADR-017 참조.
 * <p>
 * 활성 기수(또는 지정 기수)의 학습 방식에 맞는 파트 또는 기본 트랙별로 다음 골격을 생성한다.
 * <pre>
 *  Curriculum (gisu, part 또는 track)
 *    └─ WeeklyCurriculum (week 1 ~ N)
 *         └─ OriginalWorkbook (MAIN, READY)
 *              └─ OriginalWorkbookMission (M개)
 * </pre>
 * 선택적으로 {@code releaseRequesterMemberId} 가 지정되면 모든 워크북을 READY → RELEASED 로
 * 전환해 챌린저가 즉시 배포받을 수 있도록 한다.
 * <p>
 * Hexagonal 원칙을 따라 다른 도메인의 UseCase 만 호출하며, 각 Create 호출은 자체 트랜잭션
 * (Curriculum/Workbook/Mission CommandService 가 모두 {@code @Transactional}) 으로 격리된다.
 * 외부 트랜잭션이 묶이지 않도록 본 서비스의 {@code seed()} 는 {@link Propagation#NOT_SUPPORTED}.
 */
@Slf4j
@Service
@Profile("!prod")
@ConditionalOnProperty(prefix = "app.seed", name = "enabled", havingValue = "true")
@RequiredArgsConstructor
public class CurriculumSeedService implements SeedCurriculumUseCase {

    private static final List<ChallengerPart> DEFAULT_PARTS = Arrays.stream(ChallengerPart.values())
        .filter(p -> p != ChallengerPart.ADMIN)
        .toList();
    private static final List<ChallengerTrack> DEFAULT_TRACKS = Arrays.stream(ChallengerTrack.values())
        .filter(ChallengerTrack::isBasic)
        .toList();

    private final DummyCurriculumFactory dummyCurriculumFactory;
    private final GetGisuUseCase getGisuUseCase;
    private final ManageCurriculumUseCase manageCurriculumUseCase;
    private final ManageWeeklyCurriculumUseCase manageWeeklyCurriculumUseCase;
    private final ManageOriginalWorkbookUseCase manageOriginalWorkbookUseCase;
    private final ManageOriginalWorkbookMissionUseCase manageOriginalWorkbookMissionUseCase;

    @Override
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public SeedCurriculumResult seed(SeedCurriculumCommand command) {
        Long gisuId = command.gisuId() != null ? command.gisuId() : getGisuUseCase.getActiveGisuId();
        List<CreateCurriculumCommand> curricula = resolveCurricula(gisuId, command);
        int weeks = command.weeksPerCurriculum();
        int missionsPerWorkbook = command.missionsPerWorkbook();

        long startedAt = System.currentTimeMillis();
        log.info(
            "curriculum seed start: gisuId={}, curricula={}, weeks={}, missionsPerWorkbook={}, release={}",
            gisuId, curricula.size(), weeks, missionsPerWorkbook, command.releaseRequesterMemberId() != null
        );

        Counters counters = new Counters();

        for (CreateCurriculumCommand curriculum : curricula) {
            Long curriculumId = tryCreateCurriculum(curriculum, counters);
            if (curriculumId == null) {
                continue;
            }
            counters.curriculumIds.add(curriculumId);

            List<Long> weeklyIds = tryCreateWeeklyCurriculaBulk(curriculumId, weeks, counters);
            if (weeklyIds.isEmpty()) {
                continue;
            }
            counters.weeklyIds.addAll(weeklyIds);

            List<Long> workbookIds = tryCreateOriginalWorkbooksBulk(weeklyIds, counters);
            counters.workbookIds.addAll(workbookIds);

            for (Long workbookId : workbookIds) {
                tryCreateMissions(workbookId, missionsPerWorkbook, counters);
            }
        }

        boolean released = false;
        if (command.releaseRequesterMemberId() != null && !counters.workbookIds.isEmpty()) {
            released = tryReleaseAll(counters.workbookIds, command.releaseRequesterMemberId(), counters);
        }

        long elapsedMs = System.currentTimeMillis() - startedAt;
        log.info(
            "curriculum seed completed in {}ms: curriculum={} (failed={}), weekly={} (failed={}), "
                + "workbook={} (failed={}), mission={} (failed={}), released={} (failed={})",
            elapsedMs,
            counters.curriculumIds.size(), counters.curriculumFailed,
            counters.weeklyIds.size(), counters.weeklyFailed,
            counters.workbookIds.size(), counters.workbookFailed,
            counters.missionIds.size(), counters.missionFailed,
            released, counters.releaseFailed
        );

        return new SeedCurriculumResult(
            gisuId,
            counters.curriculumIds,
            counters.weeklyIds,
            counters.workbookIds,
            counters.missionIds,
            released,
            counters.curriculumFailed,
            counters.weeklyFailed,
            counters.workbookFailed,
            counters.missionFailed,
            counters.releaseFailed
        );
    }

    private List<CreateCurriculumCommand> resolveCurricula(Long gisuId, SeedCurriculumCommand command) {
        GisuLearningType learningType = getGisuUseCase.getById(gisuId).learningType();
        List<ChallengerPart> parts = command.parts() == null ? List.of() : command.parts();
        List<ChallengerTrack> tracks = command.tracks() == null ? List.of() : command.tracks();
        if (parts.stream().anyMatch(Objects::isNull) || tracks.stream().anyMatch(Objects::isNull)) {
            throw new CurriculumDomainException(CurriculumErrorCode.INVALID_CURRICULUM_LEARNING_TYPE);
        }
        if (learningType == GisuLearningType.TRACK) {
            if (!parts.isEmpty()) {
                throw new CurriculumDomainException(CurriculumErrorCode.INVALID_CURRICULUM_LEARNING_TYPE);
            }
            if (tracks.stream().anyMatch(track -> !track.isBasic())) {
                throw new CurriculumDomainException(CurriculumErrorCode.UNSUPPORTED_CURRICULUM_TRACK);
            }
            return (tracks.isEmpty() ? DEFAULT_TRACKS : tracks).stream()
                .distinct()
                .map(track -> dummyCurriculumFactory.nextCurriculumCommand(gisuId, null, track))
                .toList();
        }
        if (!tracks.isEmpty()) {
            throw new CurriculumDomainException(CurriculumErrorCode.INVALID_CURRICULUM_LEARNING_TYPE);
        }
        return (parts.isEmpty() ? DEFAULT_PARTS : parts).stream()
            .filter(part -> part != ChallengerPart.ADMIN)
            .map(part -> dummyCurriculumFactory.nextCurriculumCommand(gisuId, part, null))
            .toList();
    }

    private Long tryCreateCurriculum(CreateCurriculumCommand command, Counters counters) {
        try {
            return manageCurriculumUseCase.create(command);
        } catch (Exception e) {
            counters.curriculumFailed++;
            log.error("curriculum seed: create curriculum failed (gisuId={}, part={}, track={}): {}",
                command.gisuId(), command.part(), command.track(), e.toString());
            return null;
        }
    }

    private List<Long> tryCreateWeeklyCurriculaBulk(Long curriculumId, int weeks, Counters counters) {
        if (weeks <= 0) {
            return List.of();
        }
        List<CreateWeeklyCurriculumCommand> commands = new ArrayList<>(weeks);
        for (long week = 1; week <= weeks; week++) {
            commands.add(dummyCurriculumFactory.nextWeeklyCurriculumCommand(curriculumId, week));
        }
        try {
            List<Long> ids = manageWeeklyCurriculumUseCase.createBulk(commands);
            counters.weeklyFailed += Math.max(0, weeks - ids.size());
            return ids;
        } catch (Exception e) {
            counters.weeklyFailed += weeks;
            log.error(
                "curriculum seed: create weekly bulk failed (curriculumId={}, weeks={}): {}",
                curriculumId, weeks, e.toString()
            );
            return List.of();
        }
    }

    private List<Long> tryCreateOriginalWorkbooksBulk(List<Long> weeklyIds, Counters counters) {
        if (weeklyIds.isEmpty()) {
            return List.of();
        }
        List<CreateOriginalWorkbookCommand> commands = new ArrayList<>(weeklyIds.size());
        for (int i = 0; i < weeklyIds.size(); i++) {
            commands.add(dummyCurriculumFactory.nextOriginalWorkbookCommand(weeklyIds.get(i), i + 1L));
        }
        try {
            List<Long> ids = manageOriginalWorkbookUseCase.createBulk(commands);
            counters.workbookFailed += Math.max(0, weeklyIds.size() - ids.size());
            return ids;
        } catch (Exception e) {
            counters.workbookFailed += weeklyIds.size();
            log.error(
                "curriculum seed: create workbook bulk failed (weeklyCount={}): {}",
                weeklyIds.size(), e.toString()
            );
            return List.of();
        }
    }

    private void tryCreateMissions(Long workbookId, int count, Counters counters) {
        for (int i = 0; i < count; i++) {
            try {
                Long missionId = manageOriginalWorkbookMissionUseCase.create(
                    dummyCurriculumFactory.nextOriginalWorkbookMissionCommand(workbookId, i)
                );
                counters.missionIds.add(missionId);
            } catch (Exception e) {
                counters.missionFailed++;
                log.error(
                    "curriculum seed: create mission failed (workbookId={}, index={}): {}",
                    workbookId, i, e.toString()
                );
            }
        }
    }

    private boolean tryReleaseAll(List<Long> workbookIds, Long requesterMemberId, Counters counters) {
        List<ChangeOriginalWorkbookStatusCommand> commands = workbookIds.stream()
            .map(id -> ChangeOriginalWorkbookStatusCommand.builder()
                .originalWorkbookId(id)
                .status(OriginalWorkbookStatus.RELEASED)
                .requestedMemberId(requesterMemberId)
                .build())
            .toList();
        try {
            manageOriginalWorkbookUseCase.changeStatusForRelease(commands);
            return true;
        } catch (Exception e) {
            counters.releaseFailed = workbookIds.size();
            log.error(
                "curriculum seed: bulk release failed (workbookCount={}, requesterMemberId={}): {}",
                workbookIds.size(), requesterMemberId, e.toString()
            );
            return false;
        }
    }

    private static class Counters {
        final List<Long> curriculumIds = new ArrayList<>();
        final List<Long> weeklyIds = new ArrayList<>();
        final List<Long> workbookIds = new ArrayList<>();
        final List<Long> missionIds = new ArrayList<>();
        int curriculumFailed = 0;
        int weeklyFailed = 0;
        int workbookFailed = 0;
        int missionFailed = 0;
        int releaseFailed = 0;
    }
}
