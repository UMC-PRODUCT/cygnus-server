package com.umc.product.test.application.service;

import java.time.Instant;
import java.time.YearMonth;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.umc.product.challenger.domain.enums.PointType;
import com.umc.product.common.domain.enums.ChallengerPart;
import com.umc.product.organization.application.port.in.query.GetChapterUseCase;
import com.umc.product.organization.application.port.in.query.GetGisuUseCase;
import com.umc.product.test.application.port.in.command.SeedBulkDataUseCase;
import com.umc.product.test.application.port.in.command.dto.SeedBulkDataCommand;
import com.umc.product.test.application.port.in.command.dto.SeedBulkDataResult;
import com.umc.product.test.application.port.out.BulkSeedPort;
import com.umc.product.test.application.port.out.dto.BulkSeedBaseIds;
import com.umc.product.test.application.port.out.dto.SeedChallengerPointRow;
import com.umc.product.test.application.port.out.dto.SeedChallengerRow;
import com.umc.product.test.application.port.out.dto.SeedMemberRow;
import com.umc.product.test.application.port.out.dto.SeedNoticeRow;
import com.umc.product.test.application.port.out.dto.SeedScheduleParticipantRow;
import com.umc.product.test.application.port.out.dto.SeedScheduleRow;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 대규모 부하 테스트 데이터 벌크 시더 (SEED_STRATEGY=bulk, seeder 프로파일 전용).
 * <p>
 * SeedController(api 시더)와 달리 HTTP·도메인 use case 를 거치지 않고 JDBC 배치로 직접 적재한다 —
 * 전 엔티티가 IDENTITY 전략이라 JPA 배치 인서트가 무력화되기 때문. 골격(활성 기수·학교)은
 * organization use case 로 조회해 재사용하고, 대량 행만 {@link BulkSeedPort}로 적재한다.
 * <ul>
 *   <li><b>결정성</b>: 고정 seed 의 {@link Random} 만 사용한다 — 같은 seed 면 같은 데이터·같은 seed.json.</li>
 *   <li><b>스큐</b>: 상위 {@value #TOP_SCHOOL_COUNT}개 학교에 멤버의 절반을 몰아준다.
 *       균등 분포는 인덱스 선택도·핫 페이지 문제를 가려 부하 결과를 낙관 왜곡하기 때문.</li>
 *   <li><b>id 전략</b>: 현재 max(id) 이후 구간을 명시적으로 부여해 FK 조립·seed.json 산출에
 *       별도 조회를 없앤다. 적재 후 {@link BulkSeedPort#finalizeBulkLoad()}가 시퀀스를 동기화한다.</li>
 * </ul>
 * 빈 DB(또는 골격만 있는 DB)를 전제한다 — 같은 seed 로 재실행하면 email unique 충돌로 실패한다(의도:
 * 이중 시딩으로 측정 조건이 오염되는 것을 막는다).
 */
@Slf4j
@Service
@Profile("seeder")
@RequiredArgsConstructor
public class BulkSeedService implements SeedBulkDataUseCase {

    private static final int CHUNK_SIZE = 1_000;
    private static final int TOP_SCHOOL_COUNT = 5;
    private static final double TOP_SCHOOL_SHARE = 0.5;
    // 스케줄당 참여자 상한. 실제 서비스 최대 초대 한도(총괄단 2,000)보다 보수적으로 잡는다.
    private static final int PARTICIPANTS_PER_SCHEDULE = 500;
    private static final List<ChallengerPart> PART_CYCLE = List.of(
        ChallengerPart.WEB, ChallengerPart.ANDROID, ChallengerPart.IOS, ChallengerPart.NODEJS,
        ChallengerPart.SPRINGBOOT, ChallengerPart.DESIGN, ChallengerPart.PLAN
    );
    // SEED-007(ChallengerPointSeedService)과 동일한 상점 3 + 벌점 2 순환 셋
    private static final List<PointType> POINT_CYCLE = List.of(
        PointType.BLOG_CHALLENGE,
        PointType.PEER_REVIEW_SUBMISSION,
        PointType.BEST_WORKBOOK_V2,
        PointType.STUDY_LATE,
        PointType.NO_WORKBOOK_MISSION
    );

    private final GetGisuUseCase getGisuUseCase;
    private final GetChapterUseCase getChapterUseCase;
    private final BulkSeedPort bulkSeedPort;

    @Override
    @Transactional
    public SeedBulkDataResult seed(SeedBulkDataCommand command) {
        Long gisuId = getGisuUseCase.getActiveGisuId();
        List<Long> schoolIds = getChapterUseCase.getChaptersWithSchoolsByGisuId(gisuId).stream()
            .flatMap(chapter -> chapter.schools().stream())
            .map(school -> school.schoolId())
            .sorted()
            .toList();
        if (schoolIds.isEmpty()) {
            throw new IllegalStateException(
                "활성 기수 %d 에 학교가 없습니다 — 골격(기수·학교) 데이터가 선행돼야 합니다".formatted(gisuId));
        }

        BulkSeedBaseIds base = bulkSeedPort.currentMaxIds();
        Random rng = new Random(command.randomSeed());
        int memberCount = command.memberCount();

        int scheduleCount = seedSchedules(base, command, memberCount);
        seedNotices(base, command, gisuId);
        seedMembersWithActivity(base, command, gisuId, schoolIds, rng, scheduleCount);
        bulkSeedPort.finalizeBulkLoad();

        List<Long> sampledMemberIds =
            sampleMemberIds(base.memberMaxId(), memberCount, command.sampleMemberIdCount());
        SeedBulkDataResult result = new SeedBulkDataResult(
            gisuId,
            memberCount,
            memberCount,
            (long) memberCount * command.pointsPerChallenger(),
            scheduleCount,
            (long) memberCount * command.schedulesPerMember(),
            command.noticeGlobalCount(),
            sampledMemberIds
        );
        log.info(
            "bulk seed completed: gisuId={}, members={}, points={}, schedules={}, participants={}, notices={}",
            gisuId, result.memberCount(), result.pointCount(),
            result.scheduleCount(), result.participantCount(), result.noticeCount()
        );
        return result;
    }

    /** 이번 달에 스케줄을 분산 생성한다. k6 홈 시나리오가 "이번 달" 범위로 조회하는 것과 짝. */
    private int seedSchedules(BulkSeedBaseIds base, SeedBulkDataCommand command, int memberCount) {
        int scheduleCount = Math.max(4, (int) Math.ceil(
            (double) memberCount * command.schedulesPerMember() / PARTICIPANTS_PER_SCHEDULE));
        YearMonth month = YearMonth.now(ZoneOffset.UTC);
        List<SeedScheduleRow> rows = new ArrayList<>(Math.min(scheduleCount, CHUNK_SIZE));
        long authorMemberId = base.memberMaxId() + 1; // 첫 벌크 멤버 (같은 트랜잭션에서 적재됨)
        for (int s = 0; s < scheduleCount; s++) {
            Instant startsAt = month.atDay(1 + (s % 28))
                .atTime(10, 0)
                .toInstant(ZoneOffset.UTC);
            rows.add(new SeedScheduleRow(
                base.scheduleMaxId() + 1 + s,
                "벌크 정기모임 " + (s + 1),
                authorMemberId,
                startsAt,
                startsAt.plusSeconds(2 * 3600)
            ));
            if (rows.size() == CHUNK_SIZE) {
                bulkSeedPort.insertSchedules(List.copyOf(rows));
                rows.clear();
            }
        }
        if (!rows.isEmpty()) {
            bulkSeedPort.insertSchedules(List.copyOf(rows));
        }
        return scheduleCount;
    }

    /** GLOBAL(CHALLENGER 탭) 공지. created_at 을 과거로 분산해 최신순 정렬이 실데이터처럼 보이게 한다. */
    private void seedNotices(BulkSeedBaseIds base, SeedBulkDataCommand command, Long gisuId) {
        Instant now = Instant.now();
        List<SeedNoticeRow> rows = new ArrayList<>(command.noticeGlobalCount());
        long authorMemberId = base.memberMaxId() + 1;
        for (int i = 0; i < command.noticeGlobalCount(); i++) {
            rows.add(new SeedNoticeRow(
                base.noticeMaxId() + 1 + i,
                "[전체] 벌크 공지 " + (i + 1),
                "부하 테스트 벌크 시딩 공지입니다. (seed=" + command.randomSeed() + ")",
                authorMemberId,
                now.minusSeconds((long) i * 6 * 3600)
            ));
        }
        if (!rows.isEmpty()) {
            bulkSeedPort.insertNotices(rows, gisuId);
        }
    }

    /** 멤버·챌린저·상벌점·스케줄 참여를 한 루프에서 청크 단위로 적재한다 (FK 순서: 챌린저 → 상벌점). */
    private void seedMembersWithActivity(
        BulkSeedBaseIds base,
        SeedBulkDataCommand command,
        Long gisuId,
        List<Long> schoolIds,
        Random rng,
        int scheduleCount
    ) {
        List<Long> topSchools = schoolIds.subList(0, Math.min(TOP_SCHOOL_COUNT, schoolIds.size()));
        List<Long> restSchools = schoolIds.size() > topSchools.size()
            ? schoolIds.subList(topSchools.size(), schoolIds.size())
            : topSchools;

        List<SeedMemberRow> members = new ArrayList<>(CHUNK_SIZE);
        List<SeedChallengerRow> challengers = new ArrayList<>(CHUNK_SIZE);
        List<SeedChallengerPointRow> points = new ArrayList<>(CHUNK_SIZE * command.pointsPerChallenger());
        List<SeedScheduleParticipantRow> participants =
            new ArrayList<>(CHUNK_SIZE * command.schedulesPerMember());

        for (int i = 0; i < command.memberCount(); i++) {
            long memberId = base.memberMaxId() + 1 + i;
            long challengerId = base.challengerMaxId() + 1 + i;
            Long schoolId = rng.nextDouble() < TOP_SCHOOL_SHARE
                ? topSchools.get(rng.nextInt(topSchools.size()))
                : restSchools.get(rng.nextInt(restSchools.size()));

            members.add(new SeedMemberRow(
                memberId,
                "벌크멤버" + i,
                "blt" + command.randomSeed() + "-" + i,
                "bulk-" + command.randomSeed() + "-" + i + "@loadtest.umc.local",
                schoolId
            ));
            challengers.add(new SeedChallengerRow(
                challengerId, memberId, PART_CYCLE.get(i % PART_CYCLE.size()), gisuId));
            for (int p = 0; p < command.pointsPerChallenger(); p++) {
                points.add(new SeedChallengerPointRow(
                    challengerId,
                    POINT_CYCLE.get((i * command.pointsPerChallenger() + p) % POINT_CYCLE.size()),
                    "벌크 시딩 상벌점"
                ));
            }
            // 멤버당 schedulesPerMember 개의 서로 다른 스케줄에 round-robin 배정 (scheduleCount >= 4 라 겹치지 않음)
            for (int s = 0; s < command.schedulesPerMember(); s++) {
                participants.add(new SeedScheduleParticipantRow(
                    base.scheduleMaxId() + 1 + ((i + s) % scheduleCount), memberId));
            }

            if (members.size() == CHUNK_SIZE) {
                flush(members, challengers, points, participants);
            }
        }
        if (!members.isEmpty()) {
            flush(members, challengers, points, participants);
        }
    }

    // 포트에는 불변 복사본을 넘긴다 — 버퍼 clear() 가 어댑터(또는 테스트 캡처)에 비치지 않게.
    private void flush(
        List<SeedMemberRow> members,
        List<SeedChallengerRow> challengers,
        List<SeedChallengerPointRow> points,
        List<SeedScheduleParticipantRow> participants
    ) {
        bulkSeedPort.insertMembers(List.copyOf(members));
        bulkSeedPort.insertChallengers(List.copyOf(challengers));
        bulkSeedPort.insertChallengerPoints(List.copyOf(points));
        bulkSeedPort.insertScheduleParticipants(List.copyOf(participants));
        members.clear();
        challengers.clear();
        points.clear();
        participants.clear();
    }

    /** k6 seed.json 용 멤버 id 샘플 — 균등 간격 추출이라 seed 와 무관하게 결정적이다. */
    private List<Long> sampleMemberIds(long memberBaseId, int memberCount, int sampleCount) {
        int step = Math.max(1, memberCount / Math.max(1, sampleCount));
        List<Long> sampled = new ArrayList<>();
        for (int i = 0; i < memberCount && sampled.size() < sampleCount; i += step) {
            sampled.add(memberBaseId + 1 + i);
        }
        return sampled;
    }
}
