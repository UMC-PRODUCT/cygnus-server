package com.umc.product.curriculum.application.service.command;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.umc.product.curriculum.application.port.in.command.dto.workbook.mission.CreateMissionSubmissionCommand;
import com.umc.product.curriculum.application.port.in.command.dto.workbook.mission.WithdrawMissionSubmissionCommand;
import com.umc.product.curriculum.application.port.in.command.dto.workbook.mission.EditMissionSubmissionCommand;
import com.umc.product.curriculum.application.port.out.LoadChallengerWorkbookPort;
import com.umc.product.curriculum.application.port.out.LoadMissionSubmissionPort;
import com.umc.product.curriculum.application.port.out.LoadOriginalWorkbookMissionPort;
import com.umc.product.curriculum.application.port.out.SaveMissionFeedbackPort;
import com.umc.product.curriculum.application.port.out.SaveMissionSubmissionPort;
import com.umc.product.curriculum.domain.ChallengerWorkbook;
import com.umc.product.curriculum.domain.MissionSubmission;
import com.umc.product.curriculum.domain.OriginalWorkbook;
import com.umc.product.curriculum.domain.OriginalWorkbookMission;
import com.umc.product.curriculum.domain.enums.MissionType;
import com.umc.product.curriculum.domain.exception.CurriculumDomainException;
import com.umc.product.curriculum.domain.exception.CurriculumErrorCode;

@ExtendWith(MockitoExtension.class)
@DisplayName("미션 제출 Command Service")
class MissionSubmissionCommandServiceTest {

    private static final Long ORIGINAL_WORKBOOK_MISSION_ID = 10L;
    private static final Long CHALLENGER_WORKBOOK_ID = 20L;
    private static final Long MISSION_SUBMISSION_ID = 30L;
    private static final Long REQUESTER_MEMBER_ID = 40L;

    @Mock
    LoadChallengerWorkbookPort loadChallengerWorkbookPort;

    @Mock
    LoadOriginalWorkbookMissionPort loadOriginalWorkbookMissionPort;

    @Mock
    LoadMissionSubmissionPort loadMissionSubmissionPort;

    @Mock
    SaveMissionSubmissionPort saveMissionSubmissionPort;

    @Mock
    SaveMissionFeedbackPort saveMissionFeedbackPort;

    @InjectMocks
    MissionSubmissionCommandService sut;

    @Nested
    @DisplayName("미션 제출")
    class Create {

        @Test
        @DisplayName("챌린저 워크북 소유자는 미션을 제출할 수 있다")
        void createMissionSubmissionByOwnerSuccess() {
            // given
            OriginalWorkbook workbook = mock(OriginalWorkbook.class);
            OriginalWorkbookMission mission = mission(workbook, MissionType.LINK);
            ChallengerWorkbook challengerWorkbook = challengerWorkbook(REQUESTER_MEMBER_ID, workbook);

            given(loadOriginalWorkbookMissionPort.getById(ORIGINAL_WORKBOOK_MISSION_ID)).willReturn(mission);
            given(loadChallengerWorkbookPort.getById(CHALLENGER_WORKBOOK_ID)).willReturn(challengerWorkbook);
            given(loadMissionSubmissionPort.existsByOriginalWorkbookMissionIdAndChallengerWorkbookId(
                ORIGINAL_WORKBOOK_MISSION_ID,
                CHALLENGER_WORKBOOK_ID
            )).willReturn(false);
            given(saveMissionSubmissionPort.save(any(MissionSubmission.class))).willAnswer(invocation -> {
                MissionSubmission submission = invocation.getArgument(0);
                ReflectionTestUtils.setField(submission, "id", MISSION_SUBMISSION_ID);
                return submission;
            });

            CreateMissionSubmissionCommand command = createCommand("https://github.com/umc/product");

            // when
            Long result = sut.create(command);

            // then
            assertThat(result).isEqualTo(MISSION_SUBMISSION_ID);

            ArgumentCaptor<MissionSubmission> captor = ArgumentCaptor.forClass(MissionSubmission.class);
            then(saveMissionSubmissionPort).should().save(captor.capture());
            assertThat(captor.getValue().getOriginalWorkbookMission()).isSameAs(mission);
            assertThat(captor.getValue().getChallengerWorkbook()).isSameAs(challengerWorkbook);
            assertThat(captor.getValue().getContent()).isEqualTo("https://github.com/umc/product");
        }

        @Test
        @DisplayName("이미 제출한 미션은 다시 제출할 수 없다")
        void createMissionSubmissionDuplicateFails() {
            // given
            OriginalWorkbook workbook = mock(OriginalWorkbook.class);
            OriginalWorkbookMission mission = mission(workbook, MissionType.MEMO);
            ChallengerWorkbook challengerWorkbook = challengerWorkbook(REQUESTER_MEMBER_ID, workbook);

            given(loadOriginalWorkbookMissionPort.getById(ORIGINAL_WORKBOOK_MISSION_ID))
                .willReturn(mission);
            given(loadChallengerWorkbookPort.getById(CHALLENGER_WORKBOOK_ID))
                .willReturn(challengerWorkbook);
            given(loadMissionSubmissionPort.existsByOriginalWorkbookMissionIdAndChallengerWorkbookId(
                ORIGINAL_WORKBOOK_MISSION_ID,
                CHALLENGER_WORKBOOK_ID
            )).willReturn(true);

            // when & then
            assertThatThrownBy(() -> sut.create(createCommand("제출 내용")))
                .isInstanceOf(CurriculumDomainException.class)
                .extracting("baseCode")
                .isEqualTo(CurriculumErrorCode.WORKBOOK_SUBMISSION_ALREADY_EXISTS);

            then(saveMissionSubmissionPort).should(never()).save(any());
        }

        @Test
        @DisplayName("다른 멤버에게 배포된 챌린저 워크북에는 제출할 수 없다")
        void createMissionSubmissionByNonOwnerFails() {
            // given
            OriginalWorkbook workbook = mock(OriginalWorkbook.class);
            OriginalWorkbookMission mission = mission(workbook, MissionType.MEMO);
            ChallengerWorkbook challengerWorkbook = challengerWorkbook(999L, workbook);

            given(loadOriginalWorkbookMissionPort.getById(ORIGINAL_WORKBOOK_MISSION_ID))
                .willReturn(mission);
            given(loadChallengerWorkbookPort.getById(CHALLENGER_WORKBOOK_ID))
                .willReturn(challengerWorkbook);

            // when & then
            assertThatThrownBy(() -> sut.create(createCommand("제출 내용")))
                .isInstanceOf(CurriculumDomainException.class)
                .extracting("baseCode")
                .isEqualTo(CurriculumErrorCode.WORKBOOK_ACCESS_DENIED);

            then(saveMissionSubmissionPort).should(never()).save(any());
        }

        @Test
        @DisplayName("챌린저 워크북에 포함되지 않은 원본 미션은 제출할 수 없다")
        void createSubmissionForMismatchedOriginalMissionFails() {
            // given
            OriginalWorkbook missionWorkbook = mock(OriginalWorkbook.class);
            OriginalWorkbook challengerWorkbookOriginal = mock(OriginalWorkbook.class);
            given(missionWorkbook.getId()).willReturn(100L);
            given(challengerWorkbookOriginal.getId()).willReturn(200L);
            OriginalWorkbookMission mission = mission(missionWorkbook, MissionType.MEMO);
            ChallengerWorkbook challengerWorkbook = challengerWorkbook(REQUESTER_MEMBER_ID, challengerWorkbookOriginal);

            given(loadOriginalWorkbookMissionPort.getById(ORIGINAL_WORKBOOK_MISSION_ID))
                .willReturn(mission);
            given(loadChallengerWorkbookPort.getById(CHALLENGER_WORKBOOK_ID))
                .willReturn(challengerWorkbook);

            // when & then
            assertThatThrownBy(() -> sut.create(createCommand("제출 내용")))
                .isInstanceOf(CurriculumDomainException.class)
                .extracting("baseCode")
                .isEqualTo(CurriculumErrorCode.WORKBOOK_NOT_IN_CURRICULUM);

            then(saveMissionSubmissionPort).should(never()).save(any());
        }
    }

    @Nested
    @DisplayName("미션 제출 수정")
    class Edit {

        @Test
        @DisplayName("제출자 본인은 제출 내용을 수정할 수 있다")
        void editMissionSubmissionByOwnerSuccess() {
            // given
            MissionSubmission submission = submission(REQUESTER_MEMBER_ID, "기존 내용");
            given(loadMissionSubmissionPort.getById(MISSION_SUBMISSION_ID)).willReturn(submission);

            EditMissionSubmissionCommand command = EditMissionSubmissionCommand.builder()
                .missionSubmissionId(MISSION_SUBMISSION_ID)
                .requesterMemberId(REQUESTER_MEMBER_ID)
                .content("수정된 내용")
                .build();

            // when
            sut.edit(command);

            // then
            assertThat(submission.getContent()).isEqualTo("수정된 내용");
            then(saveMissionSubmissionPort).should().save(submission);
        }

        @Test
        @DisplayName("제출자가 아니면 제출 내용을 수정할 수 없다")
        void editMissionSubmissionByNonOwnerFails() {
            // given
            MissionSubmission submission = submission(999L, "기존 내용");
            given(loadMissionSubmissionPort.getById(MISSION_SUBMISSION_ID)).willReturn(submission);

            EditMissionSubmissionCommand command = EditMissionSubmissionCommand.builder()
                .missionSubmissionId(MISSION_SUBMISSION_ID)
                .requesterMemberId(REQUESTER_MEMBER_ID)
                .content("수정된 내용")
                .build();

            // when & then
            assertThatThrownBy(() -> sut.edit(command))
                .isInstanceOf(CurriculumDomainException.class)
                .extracting("baseCode")
                .isEqualTo(CurriculumErrorCode.WORKBOOK_ACCESS_DENIED);

            then(saveMissionSubmissionPort).should(never()).save(any());
        }
    }

    @Nested
    @DisplayName("미션 제출 철회")
    class Withdraw {

        @Test
        @DisplayName("제출자 본인은 제출을 철회할 수 있다")
        void withdrawMissionSubmissionByOwnerSuccess() {
            // given
            MissionSubmission submission = submission(REQUESTER_MEMBER_ID, "기존 내용");
            given(loadMissionSubmissionPort.getById(MISSION_SUBMISSION_ID)).willReturn(submission);

            WithdrawMissionSubmissionCommand command = WithdrawMissionSubmissionCommand.builder()
                .missionSubmissionId(MISSION_SUBMISSION_ID)
                .requesterMemberId(REQUESTER_MEMBER_ID)
                .build();

            // when
            sut.withdraw(command);

            // then
            then(saveMissionFeedbackPort).should().deleteByMissionSubmissionId(MISSION_SUBMISSION_ID);
            then(saveMissionSubmissionPort).should().delete(submission);
        }
    }

    private CreateMissionSubmissionCommand createCommand(String content) {
        return CreateMissionSubmissionCommand.builder()
            .originalWorkbookMissionId(ORIGINAL_WORKBOOK_MISSION_ID)
            .challengerWorkbookId(CHALLENGER_WORKBOOK_ID)
            .requesterMemberId(REQUESTER_MEMBER_ID)
            .content(content)
            .build();
    }

    private OriginalWorkbookMission mission(OriginalWorkbook workbook, MissionType missionType) {
        OriginalWorkbookMission mission = mock(OriginalWorkbookMission.class);
        lenient().when(mission.getOriginalWorkbook()).thenReturn(workbook);
        lenient().when(mission.getMissionType()).thenReturn(missionType);
        return mission;
    }

    private ChallengerWorkbook challengerWorkbook(Long memberId, OriginalWorkbook workbook) {
        ChallengerWorkbook challengerWorkbook = mock(ChallengerWorkbook.class);
        lenient().when(challengerWorkbook.getMemberId()).thenReturn(memberId);
        lenient().when(challengerWorkbook.getOriginalWorkbook()).thenReturn(workbook);
        return challengerWorkbook;
    }

    private MissionSubmission submission(Long memberId, String content) {
        MissionSubmission submission = MissionSubmission.create(
            mission(mock(OriginalWorkbook.class), MissionType.MEMO),
            challengerWorkbook(memberId, mock(OriginalWorkbook.class)),
            content
        );
        ReflectionTestUtils.setField(submission, "id", MISSION_SUBMISSION_ID);
        return submission;
    }
}
