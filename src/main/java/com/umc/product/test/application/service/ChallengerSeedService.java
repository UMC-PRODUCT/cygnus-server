package com.umc.product.test.application.service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.umc.product.challenger.application.port.in.command.ManageChallengerUseCase;
import com.umc.product.challenger.application.port.in.command.dto.CreateChallengerCommand;
import com.umc.product.common.domain.enums.ChallengerPart;
import com.umc.product.common.domain.enums.ChallengerTrack;
import com.umc.product.common.domain.enums.GisuLearningType;
import com.umc.product.common.domain.exception.CommonException;
import com.umc.product.global.exception.constant.CommonErrorCode;
import com.umc.product.member.application.port.in.command.RegisterEmailMemberUseCase;
import com.umc.product.member.application.port.in.command.dto.TermConsents;
import com.umc.product.member.application.port.in.query.GetMemberUseCase;
import com.umc.product.organization.application.port.in.query.GetChapterUseCase;
import com.umc.product.organization.application.port.in.query.GetGisuUseCase;
import com.umc.product.organization.application.port.in.query.dto.chapter.ChapterWithSchoolsInfo;
import com.umc.product.test.application.port.in.command.CreateSeedChallengerUseCase;
import com.umc.product.test.application.port.in.command.SeedChallengersUseCase;
import com.umc.product.test.application.port.in.command.dto.CreateSeedChallengerCommand;
import com.umc.product.test.application.port.in.command.dto.CreateSeedChallengerResult;
import com.umc.product.test.application.port.in.command.dto.SeedChallengersCommand;
import com.umc.product.test.application.port.in.command.dto.SeedChallengersResult;
import com.umc.product.test.application.port.in.command.dto.SeedChallengersResult.PerCellSummary;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 챌린저 분포 시딩 서비스. ADR-017 참조.
 * <p>
 * (Chapter, School, Part 또는 Track) 셀별로 더미 회원과 챌린저를 함께 생성한다. 셀 단위로 try-catch 를 두어
 * 한 셀의 실패가 다른 셀 시딩을 막지 않는다. Hexagonal 원칙을 따라 다른 도메인의 UseCase 만 호출한다.
 */
@Slf4j
@Service
@Profile("!prod")
@ConditionalOnProperty(prefix = "app.seed", name = "enabled", havingValue = "true")
@RequiredArgsConstructor
public class ChallengerSeedService implements SeedChallengersUseCase, CreateSeedChallengerUseCase {

    private static final List<ChallengerPart> DEFAULT_PARTS = Arrays.stream(ChallengerPart.values())
        .filter(p -> p != ChallengerPart.ADMIN)
        .toList();
    private static final List<ChallengerTrack> DEFAULT_TRACKS = Arrays.stream(ChallengerTrack.values())
        .filter(ChallengerTrack::isBasic)
        .toList();

    private final DummyMemberFactory dummyMemberFactory;
    private final GetMemberUseCase getMemberUseCase;
    private final RegisterEmailMemberUseCase registerEmailMemberUseCase;
    private final GetGisuUseCase getGisuUseCase;
    private final GetChapterUseCase getChapterUseCase;
    private final ManageChallengerUseCase manageChallengerUseCase;

    @Override
    public SeedChallengersResult seed(SeedChallengersCommand command) {
        Long gisuId = resolveGisuId(command.gisuId());
        GisuLearningType learningType = getGisuUseCase.getById(gisuId).learningType();
        List<SeedTarget> targets = resolveTargets(command, learningType);
        Integer countPerCell = learningType == GisuLearningType.TRACK
            ? command.countPerTrackPerSchool() : command.countPerPartPerSchool();
        if (countPerCell == null || countPerCell <= 0) {
            throw new CommonException(CommonErrorCode.BAD_REQUEST, "기수의 학습 유형에 맞는 학교별 생성 수를 입력해주세요.");
        }
        List<ChapterWithSchoolsInfo> chapters = resolveChapters(gisuId, command.chapterIds());

        long startedAt = System.currentTimeMillis();
        log.info(
            "challenger seed start: gisuId={}, chapters={}, targets={}, countPerCell={}",
            gisuId, chapters.size(), targets.size(), countPerCell
        );

        AtomicLong sequence = new AtomicLong(getMemberUseCase.countAll() + 1);
        List<TermConsents> consents = dummyMemberFactory.snapshotMandatoryConsents();
        List<PerCellSummary> summaries = new ArrayList<>();
        int totalCreated = 0;
        int totalFailed = 0;

        for (ChapterWithSchoolsInfo chapter : chapters) {
            for (ChapterWithSchoolsInfo.SchoolInfo school : chapter.schools()) {
                for (SeedTarget target : targets) {
                    PerCellSummary summary = seedCell(
                        chapter.chapterId(),
                        school.schoolId(),
                        target,
                        gisuId,
                        countPerCell,
                        sequence,
                        consents
                    );
                    summaries.add(summary);
                    totalCreated += summary.created();
                    totalFailed += summary.totalFailed();
                }
            }
        }

        long elapsedMs = System.currentTimeMillis() - startedAt;
        log.info(
            "challenger seed completed in {}ms: gisuId={}, totalCreated={}, totalFailed={}",
            elapsedMs, gisuId, totalCreated, totalFailed
        );

        return new SeedChallengersResult(gisuId, totalCreated, totalFailed, summaries);
    }

    @Override
    @Transactional
    public CreateSeedChallengerResult create(CreateSeedChallengerCommand command) {
        GisuLearningType learningType = getGisuUseCase.getById(command.gisuId()).learningType();
        boolean valid = learningType == GisuLearningType.TRACK
            ? command.part() == null
                && command.tracks().stream().allMatch(track -> track != null && track.isBasic())
            : command.part() != null && command.tracks().isEmpty();
        if (!valid) {
            throw new CommonException(CommonErrorCode.BAD_REQUEST, "PART 기수는 part, TRACK 기수는 기본 tracks를 입력해주세요.");
        }
        Long challengerId = manageChallengerUseCase.createChallenger(CreateChallengerCommand.builder()
            .memberId(command.memberId())
            .gisuId(command.gisuId())
            .part(command.part())
            .tracks(command.tracks())
            .build());

        return CreateSeedChallengerResult.of(
            challengerId,
            command.memberId(),
            command.gisuId(),
            command.part(),
            command.tracks()
        );
    }

    private Long resolveGisuId(Long gisuId) {
        if (gisuId != null) {
            return gisuId;
        }
        return getGisuUseCase.getActiveGisuId();
    }

    private List<SeedTarget> resolveTargets(SeedChallengersCommand command, GisuLearningType learningType) {
        if (learningType == GisuLearningType.TRACK) {
            if ((command.parts() != null && !command.parts().isEmpty()) || command.countPerPartPerSchool() != null) {
                throw new CommonException(CommonErrorCode.BAD_REQUEST,
                    "TRACK 기수는 tracks와 countPerTrackPerSchool을 입력해주세요.");
            }
            List<ChallengerTrack> tracks = command.tracks() == null || command.tracks().isEmpty()
                ? DEFAULT_TRACKS : command.tracks();
            if (tracks.stream().anyMatch(track -> track == null || !track.isBasic())) {
                throw new CommonException(CommonErrorCode.BAD_REQUEST, "기본 트랙만 시딩할 수 있어요.");
            }
            return tracks.stream().distinct().map(track -> new SeedTarget(null, track)).toList();
        }
        if ((command.tracks() != null && !command.tracks().isEmpty()) || command.countPerTrackPerSchool() != null) {
            throw new CommonException(CommonErrorCode.BAD_REQUEST, "PART 기수는 parts와 countPerPartPerSchool을 입력해주세요.");
        }
        List<ChallengerPart> parts = command.parts() == null || command.parts().isEmpty()
            ? DEFAULT_PARTS : command.parts();
        if (parts.stream().anyMatch(part -> part == null)) {
            throw new CommonException(CommonErrorCode.BAD_REQUEST, "시딩할 파트에 null을 포함할 수 없어요.");
        }
        return parts.stream().distinct().map(part -> new SeedTarget(part, null)).toList();
    }

    private List<ChapterWithSchoolsInfo> resolveChapters(Long gisuId, List<Long> chapterIds) {
        List<ChapterWithSchoolsInfo> all = getChapterUseCase.getChaptersWithSchoolsByGisuId(gisuId);
        if (chapterIds == null || chapterIds.isEmpty()) {
            return all;
        }
        Set<Long> filter = Set.copyOf(chapterIds);
        return all.stream().filter(c -> filter.contains(c.chapterId())).toList();
    }

    /**
     * 한 (Chapter, School, Part 또는 Track) 셀을 시딩한다. 셀 단위 try-catch 로 한 셀의 실패가 다른 셀로
     * 전파되지 않게 한다. 멤버 생성은 per-call 트랜잭션, 챌린저 생성은 bulk 단일 트랜잭션이다.
     * 실패 단계(멤버 / 챌린저)를 분리해서 보고하므로 호출자는 응답만 보고도 실패 지점을 추적할 수 있다.
     */
    private PerCellSummary seedCell(
        Long chapterId,
        Long schoolId,
        SeedTarget target,
        Long gisuId,
        int countPerCell,
        AtomicLong sequence,
        List<TermConsents> consents
    ) {
        List<com.umc.product.member.application.port.in.command.dto.EmailRegisterMemberCommand> memberCommands =
            new ArrayList<>(countPerCell);
        for (int i = 0; i < countPerCell; i++) {
            long seq = sequence.getAndIncrement();
            memberCommands.add(dummyMemberFactory.nextEmailCommandWithSchool(seq, schoolId, consents));
        }

        List<Long> createdMemberIds;
        int memberFailed;
        try {
            createdMemberIds = registerEmailMemberUseCase.batchRegister(memberCommands);
            memberFailed = countPerCell - createdMemberIds.size();
        } catch (Exception e) {
            log.error(
                "challenger seed member batchRegister failed (chapterId={}, schoolId={}, target={}, count={}): {}",
                chapterId, schoolId, target, countPerCell, e.toString()
            );
            return new PerCellSummary(chapterId, schoolId, target.part(), 0, countPerCell, 0, target.track());
        }

        if (createdMemberIds.isEmpty()) {
            return new PerCellSummary(chapterId, schoolId, target.part(), 0, memberFailed, 0, target.track());
        }

        List<CreateChallengerCommand> commands = createdMemberIds.stream()
            .map(memberId -> CreateChallengerCommand.builder()
                .memberId(memberId)
                .part(target.part())
                .tracks(target.track() == null ? List.of() : List.of(target.track()))
                .gisuId(gisuId)
                .build())
            .toList();
        try {
            List<Long> challengerIds = manageChallengerUseCase.createChallengerBulk(commands);
            return new PerCellSummary(
                chapterId, schoolId, target.part(), challengerIds.size(), memberFailed, 0, target.track());
        } catch (Exception e) {
            log.error(
                "challenger seed challenger bulk failed (chapterId={}, schoolId={}, target={}, members={}): {}",
                chapterId, schoolId, target, createdMemberIds.size(), e.toString()
            );
            return new PerCellSummary(
                chapterId, schoolId, target.part(), 0, memberFailed, createdMemberIds.size(), target.track());
        }
    }

    private record SeedTarget(ChallengerPart part, ChallengerTrack track) {
    }
}
