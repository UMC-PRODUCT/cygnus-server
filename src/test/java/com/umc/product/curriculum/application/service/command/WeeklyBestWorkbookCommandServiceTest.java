package com.umc.product.curriculum.application.service.command;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.umc.product.challenger.application.port.in.query.GetChallengerUseCase;
import com.umc.product.challenger.application.port.in.query.dto.ChallengerInfo;
import com.umc.product.common.domain.enums.ChallengerPart;
import com.umc.product.common.domain.enums.ChallengerStatus;
import com.umc.product.curriculum.application.port.in.command.dto.workbook.CreateWeeklyBestWorkbookCommand;
import com.umc.product.curriculum.application.port.in.command.dto.workbook.EditWeeklyBestWorkbookCommand;
import com.umc.product.curriculum.application.port.out.LoadChallengerWorkbookPort;
import com.umc.product.curriculum.application.port.out.LoadOriginalWorkbookPort;
import com.umc.product.curriculum.application.port.out.LoadWeeklyBestWorkbookPort;
import com.umc.product.curriculum.application.port.out.LoadWeeklyCurriculumPort;
import com.umc.product.curriculum.application.port.out.SaveWeeklyBestWorkbookPort;
import com.umc.product.curriculum.domain.ChallengerWorkbook;
import com.umc.product.curriculum.domain.Curriculum;
import com.umc.product.curriculum.domain.OriginalWorkbook;
import com.umc.product.curriculum.domain.WeeklyBestWorkbook;
import com.umc.product.curriculum.domain.WeeklyCurriculum;
import com.umc.product.curriculum.domain.enums.OriginalWorkbookStatus;
import com.umc.product.curriculum.domain.enums.OriginalWorkbookType;
import com.umc.product.curriculum.domain.exception.CurriculumDomainException;
import com.umc.product.curriculum.domain.exception.CurriculumErrorCode;

@ExtendWith(MockitoExtension.class)
@DisplayName("주간 베스트 워크북 Command Service")
class WeeklyBestWorkbookCommandServiceTest {

    private static final Long MEMBER_ID = 1L;
    private static final Long DECIDED_MEMBER_ID = 2L;
    private static final Long WEEKLY_CURRICULUM_ID = 20L;
    private static final Long ORIGINAL_WORKBOOK_ID = 30L;
    private static final Long STUDY_GROUP_ID = 40L;
    private static final Long WEEKLY_BEST_WORKBOOK_ID = 50L;

    @Mock
    LoadWeeklyCurriculumPort loadWeeklyCurriculumPort;

    @Mock
    LoadOriginalWorkbookPort loadOriginalWorkbookPort;

    @Mock
    LoadChallengerWorkbookPort loadChallengerWorkbookPort;

    @Mock
    LoadWeeklyBestWorkbookPort loadWeeklyBestWorkbookPort;

    @Mock
    SaveWeeklyBestWorkbookPort saveWeeklyBestWorkbookPort;

    @Mock
    GetChallengerUseCase getChallengerUseCase;

    @InjectMocks
    WeeklyBestWorkbookCommandService sut;

    @Nested
    @DisplayName("베스트 워크북 선정")
    class SelectBest {

        @Test
        @DisplayName("활성 챌린저의 워크북을 베스트 워크북으로 선정할 수 있다")
        void 활성_챌린저의_워크북을_베스트_워크북으로_선정할_수_있다() {
            // given
            WeeklyCurriculum weeklyCurriculum = weeklyCurriculum();
            OriginalWorkbook originalWorkbook = releasedWorkbook(weeklyCurriculum);
            ChallengerWorkbook challengerWorkbook = ChallengerWorkbook.create(
                originalWorkbook,
                MEMBER_ID,
                STUDY_GROUP_ID
            );

            given(loadWeeklyCurriculumPort.getById(WEEKLY_CURRICULUM_ID)).willReturn(weeklyCurriculum);
            given(getChallengerUseCase.getAllByMemberId(MEMBER_ID)).willReturn(List.of(activeChallenger()));
            given(loadWeeklyBestWorkbookPort.existsByWeeklyCurriculumIdAndStudyGroupId(
                WEEKLY_CURRICULUM_ID,
                STUDY_GROUP_ID
            )).willReturn(false);
            given(loadOriginalWorkbookPort.findReleasedByWeeklyCurriculumId(WEEKLY_CURRICULUM_ID))
                .willReturn(List.of(originalWorkbook));
            given(loadChallengerWorkbookPort.listByMemberIdAndOriginalWorkbookIdIn(
                MEMBER_ID,
                List.of(ORIGINAL_WORKBOOK_ID)
            )).willReturn(List.of(challengerWorkbook));

            var command = createCommand();

            // when
            sut.selectBest(command);

            // then
            ArgumentCaptor<WeeklyBestWorkbook> captor = ArgumentCaptor.forClass(WeeklyBestWorkbook.class);
            then(saveWeeklyBestWorkbookPort).should().save(captor.capture());
            assertThat(captor.getValue().getMemberId()).isEqualTo(MEMBER_ID);
            assertThat(captor.getValue().getStudyGroupId()).isEqualTo(STUDY_GROUP_ID);
            assertThat(captor.getValue().getReason()).isEqualTo("선정 사유");
        }

        @Test
        @DisplayName("이미 선정된 스터디 그룹에는 베스트 워크북을 추가 선정할 수 없다")
        void 이미_선정된_스터디_그룹에는_베스트_워크북을_추가_선정할_수_없다() {
            // given
            WeeklyCurriculum weeklyCurriculum = weeklyCurriculum();
            given(loadWeeklyCurriculumPort.getById(WEEKLY_CURRICULUM_ID)).willReturn(weeklyCurriculum);
            given(getChallengerUseCase.getAllByMemberId(MEMBER_ID)).willReturn(List.of(activeChallenger()));
            given(loadWeeklyBestWorkbookPort.existsByWeeklyCurriculumIdAndStudyGroupId(
                WEEKLY_CURRICULUM_ID,
                STUDY_GROUP_ID
            )).willReturn(true);

            // when & then
            assertThatThrownBy(() -> sut.selectBest(createCommand()))
                .isInstanceOf(CurriculumDomainException.class)
                .extracting("baseCode")
                .isEqualTo(CurriculumErrorCode.WORKBOOK_SUBMISSION_ALREADY_EXISTS);

            then(saveWeeklyBestWorkbookPort).should(never()).save(any());
        }

        @Test
        @DisplayName("인정 처리된 워크북이 있으면 베스트 워크북으로 선정할 수 없다")
        void 인정_처리된_워크북이_있으면_베스트_워크북으로_선정할_수_없다() {
            // given
            WeeklyCurriculum weeklyCurriculum = weeklyCurriculum();
            OriginalWorkbook originalWorkbook = releasedWorkbook(weeklyCurriculum);
            ChallengerWorkbook challengerWorkbook = ChallengerWorkbook.create(
                originalWorkbook,
                MEMBER_ID,
                STUDY_GROUP_ID
            );
            challengerWorkbook.excuse("공결", DECIDED_MEMBER_ID);

            given(loadWeeklyCurriculumPort.getById(WEEKLY_CURRICULUM_ID)).willReturn(weeklyCurriculum);
            given(getChallengerUseCase.getAllByMemberId(MEMBER_ID)).willReturn(List.of(activeChallenger()));
            given(loadWeeklyBestWorkbookPort.existsByWeeklyCurriculumIdAndStudyGroupId(
                WEEKLY_CURRICULUM_ID,
                STUDY_GROUP_ID
            )).willReturn(false);
            given(loadOriginalWorkbookPort.findReleasedByWeeklyCurriculumId(WEEKLY_CURRICULUM_ID))
                .willReturn(List.of(originalWorkbook));
            given(loadChallengerWorkbookPort.listByMemberIdAndOriginalWorkbookIdIn(
                MEMBER_ID,
                List.of(ORIGINAL_WORKBOOK_ID)
            )).willReturn(List.of(challengerWorkbook));

            // when & then
            assertThatThrownBy(() -> sut.selectBest(createCommand()))
                .isInstanceOf(CurriculumDomainException.class)
                .extracting("baseCode")
                .isEqualTo(CurriculumErrorCode.WORKBOOK_ACCESS_DENIED);

            then(saveWeeklyBestWorkbookPort).should(never()).save(any());
        }
    }

    @Nested
    @DisplayName("베스트 워크북 변경")
    class ChangeBestWorkbook {

        @Test
        @DisplayName("선정 사유를 수정할 수 있다")
        void 선정_사유를_수정할_수_있다() {
            // given
            WeeklyBestWorkbook weeklyBestWorkbook = weeklyBestWorkbook();
            given(loadWeeklyBestWorkbookPort.getById(WEEKLY_BEST_WORKBOOK_ID)).willReturn(weeklyBestWorkbook);

            var command = EditWeeklyBestWorkbookCommand.builder()
                .weeklyBestWorkbookId(WEEKLY_BEST_WORKBOOK_ID)
                .requestedMemberId(DECIDED_MEMBER_ID)
                .newReason("수정된 사유")
                .build();

            // when
            sut.editReason(command);

            // then
            assertThat(weeklyBestWorkbook.getReason()).isEqualTo("수정된 사유");
            then(saveWeeklyBestWorkbookPort).should().save(weeklyBestWorkbook);
        }

        @Test
        @DisplayName("베스트 워크북 선정을 철회할 수 있다")
        void 베스트_워크북_선정을_철회할_수_있다() {
            // given
            WeeklyBestWorkbook weeklyBestWorkbook = weeklyBestWorkbook();
            given(loadWeeklyBestWorkbookPort.getById(WEEKLY_BEST_WORKBOOK_ID)).willReturn(weeklyBestWorkbook);

            // when
            sut.withdraw(WEEKLY_BEST_WORKBOOK_ID);

            // then
            then(saveWeeklyBestWorkbookPort).should().delete(weeklyBestWorkbook);
        }

        @Test
        @DisplayName("주차 종료 후 7일이 지나면 베스트 워크북 선정을 철회할 수 없다")
        void 주차_종료_후_7일이_지나면_베스트_워크북_선정을_철회할_수_없다() {
            // given
            WeeklyBestWorkbook weeklyBestWorkbook = weeklyBestWorkbook(expiredWeeklyCurriculum());
            given(loadWeeklyBestWorkbookPort.getById(WEEKLY_BEST_WORKBOOK_ID)).willReturn(weeklyBestWorkbook);

            // when & then
            assertThatThrownBy(() -> sut.withdraw(WEEKLY_BEST_WORKBOOK_ID))
                .isInstanceOf(CurriculumDomainException.class)
                .extracting("baseCode")
                .isEqualTo(CurriculumErrorCode.INVALID_WORKBOOK_STATUS);

            then(saveWeeklyBestWorkbookPort).should(never()).delete(any());
        }
    }

    private CreateWeeklyBestWorkbookCommand createCommand() {
        return CreateWeeklyBestWorkbookCommand.builder()
            .decidedMemberId(DECIDED_MEMBER_ID)
            .bestMemberId(MEMBER_ID)
            .weeklyCurriculumId(WEEKLY_CURRICULUM_ID)
            .studyGroupId(STUDY_GROUP_ID)
            .reason("선정 사유")
            .build();
    }

    private ChallengerInfo activeChallenger() {
        return ChallengerInfo.builder()
            .challengerId(1L)
            .memberId(MEMBER_ID)
            .gisuId(9L)
            .part(ChallengerPart.SPRINGBOOT)
            .challengerStatus(ChallengerStatus.ACTIVE)
            .build();
    }

    private WeeklyBestWorkbook weeklyBestWorkbook() {
        return weeklyBestWorkbook(weeklyCurriculum());
    }

    private WeeklyBestWorkbook weeklyBestWorkbook(WeeklyCurriculum weeklyCurriculum) {
        WeeklyBestWorkbook weeklyBestWorkbook = WeeklyBestWorkbook.create(
            weeklyCurriculum,
            MEMBER_ID,
            STUDY_GROUP_ID,
            "선정 사유",
            DECIDED_MEMBER_ID
        );
        ReflectionTestUtils.setField(weeklyBestWorkbook, "id", WEEKLY_BEST_WORKBOOK_ID);
        return weeklyBestWorkbook;
    }

    private OriginalWorkbook releasedWorkbook(WeeklyCurriculum weeklyCurriculum) {
        OriginalWorkbook workbook = OriginalWorkbook.createAsReady(
            weeklyCurriculum,
            "원본 워크북",
            "설명",
            null,
            null,
            OriginalWorkbookType.MAIN
        );
        ReflectionTestUtils.setField(workbook, "id", ORIGINAL_WORKBOOK_ID);
        workbook.changeStatus(OriginalWorkbookStatus.RELEASED, DECIDED_MEMBER_ID);
        return workbook;
    }

    private WeeklyCurriculum weeklyCurriculum() {
        WeeklyCurriculum weeklyCurriculum = WeeklyCurriculum.create(
            Curriculum.create(9L, ChallengerPart.SPRINGBOOT, "9기 스프링부트"),
            1L,
            false,
            "1주차",
            Instant.parse("2099-03-01T00:00:00Z"),
            Instant.parse("2099-03-07T23:59:59Z")
        );
        ReflectionTestUtils.setField(weeklyCurriculum, "id", WEEKLY_CURRICULUM_ID);
        return weeklyCurriculum;
    }

    private WeeklyCurriculum expiredWeeklyCurriculum() {
        WeeklyCurriculum weeklyCurriculum = WeeklyCurriculum.create(
            Curriculum.create(9L, ChallengerPart.SPRINGBOOT, "9기 스프링부트"),
            1L,
            false,
            "1주차",
            Instant.parse("2020-03-01T00:00:00Z"),
            Instant.parse("2020-03-07T23:59:59Z")
        );
        ReflectionTestUtils.setField(weeklyCurriculum, "id", WEEKLY_CURRICULUM_ID);
        return weeklyCurriculum;
    }
}
