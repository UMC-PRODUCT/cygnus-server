package com.umc.product.test.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.umc.product.common.domain.enums.ChallengerPart;
import com.umc.product.curriculum.domain.enums.OriginalWorkbookStatus;
import com.umc.product.curriculum.domain.enums.OriginalWorkbookType;
import com.umc.product.notice.application.port.in.command.dto.CreateNoticeCommand;
import com.umc.product.term.application.port.in.query.GetTermUseCase;

@ExtendWith(MockitoExtension.class)
class SeedFactoryTest {

    @Mock GetTermUseCase getTermUseCase;

    @Test
    @DisplayName("더미 멤버는 필수 약관과 지정 학교를 보존하고 식별 가능한 고유 값을 만든다")
    void 더미_멤버_Command_생성() {
        given(getTermUseCase.getRequiredTermIds()).willReturn(Set.of(1L, 2L));
        DummyMemberFactory factory = new DummyMemberFactory(
            new SeedProperties(true, 100, "alpha.umc.test", "password"), getTermUseCase
        );
        var consents = factory.snapshotMandatoryConsents();

        var specified = factory.nextEmailCommandWithSchool(7, 33L, consents);
        var randomSchool = factory.nextEmailCommand(8, consents);

        assertThat(consents).hasSize(2).allMatch(consent -> consent.isAgreed());
        assertThat(specified.email()).isEqualTo("alpha_user_0007@alpha.umc.test");
        assertThat(specified.rawPassword()).isEqualTo("password");
        assertThat(specified.schoolId()).isEqualTo(33L);
        assertThat(specified.termConsents()).isSameAs(consents);
        assertThat(randomSchool.schoolId()).isBetween(1L, 38L);
        assertThat(factory.safeNickname("짧음", 1)).isEqualTo("짧음1");
        assertThat(factory.safeNickname("아주긴닉네임아주긴닉네임아주긴닉네임", 1234))
            .hasSize(20);
    }

    @Test
    @DisplayName("더미 공지는 전체·지부·학교·파트 대상과 무알림 정책을 정확히 표현한다")
    void 더미_공지_Command_생성() {
        DummyNoticeFactory factory = new DummyNoticeFactory();

        CreateNoticeCommand global = factory.nextGlobalNoticeCommand(9L, 1L, 1);
        CreateNoticeCommand chapter = factory.nextChapterNoticeCommand(9L, 1L, 2L, "서울", 2);
        CreateNoticeCommand school = factory.nextSchoolNoticeCommand(9L, 1L, 3L, "한국대", 3);
        CreateNoticeCommand part = factory.nextPartNoticeCommand(9L, 1L, ChallengerPart.WEB, 4);

        assertThat(List.of(global, chapter, school, part))
            .allMatch(command -> Boolean.FALSE.equals(command.shouldNotify()))
            .allMatch(command -> command.title().startsWith("["))
            .allMatch(command -> command.content().contains("대상:"));
        assertThat(global.targetInfo().targetGisuId()).isEqualTo(9L);
        assertThat(chapter.targetInfo().targetChapterId()).isEqualTo(2L);
        assertThat(school.targetInfo().targetSchoolId()).isEqualTo(3L);
        assertThat(part.targetInfo().targetParts()).containsExactly(ChallengerPart.WEB);
    }

    @Test
    @DisplayName("더미 커리큘럼은 유효한 주차·워크북·필수 미션 Command를 만든다")
    void 더미_커리큘럼_Command_생성() {
        DummyCurriculumFactory factory = new DummyCurriculumFactory();
        Instant before = Instant.now();

        var curriculum = factory.nextCurriculumCommand(9L, ChallengerPart.SPRINGBOOT);
        var week = factory.nextWeeklyCurriculumCommand(10L, 2);
        var workbook = factory.nextOriginalWorkbookCommand(11L, 2);
        var requiredMission = factory.nextOriginalWorkbookMissionCommand(12L, 0);
        var optionalMission = factory.nextOriginalWorkbookMissionCommand(12L, 1);

        assertThat(curriculum.title()).isEqualTo("9기 SPRINGBOOT 커리큘럼");
        assertThat(week.weekNo()).isEqualTo(2L);
        assertThat(Duration.between(week.startsAt(), week.endsAt())).isEqualTo(Duration.ofDays(7));
        assertThat(week.startsAt()).isAfter(before.plus(Duration.ofDays(5)));
        assertThat(workbook.type()).isEqualTo(OriginalWorkbookType.MAIN);
        assertThat(workbook.initialStatus()).isEqualTo(OriginalWorkbookStatus.READY);
        assertThat(workbook.url()).startsWith("https://alpha.umc.test/workbook/");
        assertThat(requiredMission.isNecessary()).isTrue();
        assertThat(optionalMission.isNecessary()).isFalse();
        assertThat(requiredMission.missionType()).isNotNull();
    }

    @Test
    @DisplayName("프로젝트 더미 문자열은 식별 접두사와 DB 길이 제한을 지킨다")
    void 더미_프로젝트_문자열_생성() {
        DummyProjectFactory factory = new DummyProjectFactory();

        assertThat(factory.nextName(1)).startsWith("[SEED] ").hasSizeLessThanOrEqualTo(100);
        assertThat(factory.nextDescription(1)).contains("#0001").hasSizeLessThanOrEqualTo(200);
        assertThat(factory.clip("short", 10)).isEqualTo("short");
        assertThat(factory.clip("1234567890", 4)).isEqualTo("1234");
    }

    @Test
    @DisplayName("시나리오 quota는 DESIGN·FE·BE 순서와 허용 범위를 지킨다")
    void 시나리오_quota_정책() {
        ScenarioPartQuotaPolicy policy = new ScenarioPartQuotaPolicy();

        for (int i = 0; i < 50; i++) {
            var quotas = policy.pickQuotas();
            assertThat(quotas).hasSize(3);
            assertThat(quotas.get(0).part()).isEqualTo(ChallengerPart.DESIGN);
            assertThat(quotas.get(0).quota()).isBetween(1L, 2L);
            assertThat(quotas.get(1).part())
                .isIn(ChallengerPart.WEB, ChallengerPart.ANDROID, ChallengerPart.IOS);
            assertThat(quotas.get(1).quota()).isBetween(3L, 4L);
            assertThat(quotas.get(2).part())
                .isIn(ChallengerPart.NODEJS, ChallengerPart.SPRINGBOOT);
            assertThat(quotas.get(2).quota()).isBetween(3L, 4L);
        }
        assertThat(policy.randomInclusive(ThreadLocalRandom.current(), 3, 3)).isEqualTo(3L);
    }
}
