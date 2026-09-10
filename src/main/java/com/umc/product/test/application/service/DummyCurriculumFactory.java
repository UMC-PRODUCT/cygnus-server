package com.umc.product.test.application.service;

import java.time.Duration;
import java.time.Instant;
import java.util.Locale;
import java.util.concurrent.ThreadLocalRandom;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import net.datafaker.Faker;

import com.umc.product.common.domain.enums.ChallengerPart;
import com.umc.product.common.domain.enums.ChallengerTrack;
import com.umc.product.curriculum.application.port.in.command.dto.curriculum.CreateCurriculumCommand;
import com.umc.product.curriculum.application.port.in.command.dto.curriculum.CreateWeeklyCurriculumCommand;
import com.umc.product.curriculum.application.port.in.command.dto.workbook.CreateOriginalWorkbookCommand;
import com.umc.product.curriculum.application.port.in.command.dto.workbook.mission.CreateOriginalWorkbookMissionCommand;
import com.umc.product.curriculum.domain.enums.MissionType;
import com.umc.product.curriculum.domain.enums.OriginalWorkbookStatus;
import com.umc.product.curriculum.domain.enums.OriginalWorkbookType;

/**
 * datafaker 를 사용해 test 도메인 시딩용 더미 Curriculum Command 를 생성한다. ADR-017 참조.
 * <p>
 * WeeklyCurriculum 의 시작·종료 일시는 시딩 시점을 기준으로 주차당 7 일 간격으로 분포시킨다.
 * 1 주차는 (now - 1d) ~ (now + 6d), 2 주차는 그 다음 주 ... 로 설정해 첫 주차는 진행 중,
 * 이후 주차는 미래로 설정된다. 이렇게 하면 도메인의 "주차 시작 전 또는 진행 중에만 워크북 추가
 * 가능" 검증을 모두 통과한다.
 */
@Component
@Profile("!prod")
@ConditionalOnProperty(prefix = "app.seed", name = "enabled", havingValue = "true")
public class DummyCurriculumFactory {

    private static final Duration WEEK_DURATION = Duration.ofDays(7);
    private static final Duration FIRST_WEEK_BACKDATE = Duration.ofDays(1);

    private final Faker faker = new Faker(Locale.KOREAN);

    /**
     * 기수의 학습 방식에 맞는 파트 또는 트랙 커리큘럼 Command 를 생성한다.
     */
    public CreateCurriculumCommand nextCurriculumCommand(Long gisuId, ChallengerPart part, ChallengerTrack track) {
        return CreateCurriculumCommand.builder()
            .gisuId(gisuId)
            .part(part)
            .track(track)
            .title("%d기 %s 커리큘럼".formatted(gisuId, track != null ? track.name() : part.name()))
            .build();
    }

    /**
     * 주차별 커리큘럼 Command 생성. isExtra = false (정규), week 번호는 1 부터 시작한다.
     * 시작 일시는 시딩 시점에서 (week-1)주 - 1일, 종료 일시는 시작 + 7일.
     */
    public CreateWeeklyCurriculumCommand nextWeeklyCurriculumCommand(Long curriculumId, long weekNo) {
        Instant now = Instant.now();
        Instant startsAt = now.plus(WEEK_DURATION.multipliedBy(weekNo - 1)).minus(FIRST_WEEK_BACKDATE);
        Instant endsAt = startsAt.plus(WEEK_DURATION);
        return CreateWeeklyCurriculumCommand.builder()
            .curriculumId(curriculumId)
            .weekNo(weekNo)
            .isExtra(false)
            .title("%d주차".formatted(weekNo))
            .startsAt(startsAt)
            .endsAt(endsAt)
            .build();
    }

    /**
     * 원본 워크북 Command 생성. type = MAIN, initialStatus = RELEASED 로 즉시 챌린저가 배포받을
     * 수 있도록 한다.
     */
    public CreateOriginalWorkbookCommand nextOriginalWorkbookCommand(Long weeklyCurriculumId, long weekNo) {
        return CreateOriginalWorkbookCommand.builder()
            .weeklyCurriculumId(weeklyCurriculumId)
            .title("%d주차 워크북".formatted(weekNo))
            .description(faker.lorem().sentence())
            .url("https://alpha.umc.test/workbook/" + faker.internet().uuid())
            .content(String.join("\n\n", faker.lorem().paragraphs(2)))
            .type(OriginalWorkbookType.MAIN)
            .initialStatus(OriginalWorkbookStatus.READY)
            .build();
    }

    /**
     * 원본 워크북 미션 Command 생성. missionType 은 LINK/MEMO/PLAIN 중 무작위.
     * 첫 미션은 필수, 나머지는 선택으로 설정한다.
     */
    public CreateOriginalWorkbookMissionCommand nextOriginalWorkbookMissionCommand(
        Long originalWorkbookId, int missionIndex
    ) {
        MissionType[] types = MissionType.values();
        MissionType missionType = types[ThreadLocalRandom.current().nextInt(types.length)];
        return CreateOriginalWorkbookMissionCommand.builder()
            .originalWorkbookId(originalWorkbookId)
            .title("미션 #%d".formatted(missionIndex + 1))
            .description(faker.lorem().sentence())
            .missionType(missionType)
            .isNecessary(missionIndex == 0)
            .build();
    }
}
