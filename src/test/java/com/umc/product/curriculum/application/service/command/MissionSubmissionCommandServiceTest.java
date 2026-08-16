package com.umc.product.curriculum.application.service.command;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

import java.time.Instant;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.umc.product.common.domain.enums.ChallengerPart;
import com.umc.product.curriculum.application.port.in.command.dto.workbook.mission.DeleteMissionSubmissionCommand;
import com.umc.product.curriculum.application.port.in.command.dto.workbook.mission.EditMissionSubmissionCommand;
import com.umc.product.curriculum.application.port.out.LoadChallengerWorkbookPort;
import com.umc.product.curriculum.application.port.out.LoadMissionSubmissionPort;
import com.umc.product.curriculum.application.port.out.LoadOriginalWorkbookMissionPort;
import com.umc.product.curriculum.application.port.out.SaveMissionFeedbackPort;
import com.umc.product.curriculum.application.port.out.SaveMissionSubmissionPort;
import com.umc.product.curriculum.domain.ChallengerWorkbook;
import com.umc.product.curriculum.domain.Curriculum;
import com.umc.product.curriculum.domain.MissionSubmission;
import com.umc.product.curriculum.domain.OriginalWorkbook;
import com.umc.product.curriculum.domain.OriginalWorkbookMission;
import com.umc.product.curriculum.domain.WeeklyCurriculum;
import com.umc.product.curriculum.domain.enums.MissionType;
import com.umc.product.curriculum.domain.enums.OriginalWorkbookType;

@ExtendWith(MockitoExtension.class)
class MissionSubmissionCommandServiceTest {

    @Mock private LoadChallengerWorkbookPort loadChallengerWorkbookPort;
    @Mock private LoadOriginalWorkbookMissionPort loadOriginalWorkbookMissionPort;
    @Mock private LoadMissionSubmissionPort loadMissionSubmissionPort;
    @Mock private SaveMissionSubmissionPort saveMissionSubmissionPort;
    @Mock private SaveMissionFeedbackPort saveMissionFeedbackPort;
    @Mock private MissionMutationPolicy missionMutationPolicy;
    @InjectMocks private MissionSubmissionCommandService service;

    @Test
    @DisplayName("미션 철회는 제출 행을 삭제하지 않고 철회 시각을 저장한다")
    void withdraw_marksSubmissionAndDeletesFeedback() {
        MissionSubmission submission = submission();
        Instant now = Instant.parse("2026-07-23T00:00:00Z");
        given(loadMissionSubmissionPort.getByIdForUpdate(400L)).willReturn(submission);
        given(missionMutationPolicy.now()).willReturn(now);

        service.withdraw(DeleteMissionSubmissionCommand.builder()
            .missionSubmissionId(400L).requesterMemberId(30L).build());

        assertThat(submission.getWithdrawnAt()).isEqualTo(now);
        verify(loadMissionSubmissionPort).getByIdForUpdate(400L);
        verify(saveMissionFeedbackPort).deleteByMissionSubmissionId(400L);
        verify(saveMissionSubmissionPort).save(submission);
    }

    @Test
    @DisplayName("미션 제출물 수정은 철회와 직렬화하기 위해 쓰기 잠금으로 조회한다")
    void edit_loadsSubmissionForUpdate() {
        MissionSubmission submission = submission();
        given(loadMissionSubmissionPort.getByIdForUpdate(400L)).willReturn(submission);

        service.edit(EditMissionSubmissionCommand.builder()
            .missionSubmissionId(400L)
            .requesterMemberId(30L)
            .content("수정")
            .build());

        verify(loadMissionSubmissionPort).getByIdForUpdate(400L);
        verify(saveMissionSubmissionPort).save(submission);
    }

    private MissionSubmission submission() {
        WeeklyCurriculum weekly = WeeklyCurriculum.create(
            Curriculum.create(9L, ChallengerPart.SPRINGBOOT, "커리큘럼"), 1L, false, "1주차",
            Instant.EPOCH, Instant.parse("2026-08-01T00:00:00Z")
        );
        OriginalWorkbook original = OriginalWorkbook.createAsDraft(
            weekly, "워크북", null, null, null, OriginalWorkbookType.MAIN
        );
        OriginalWorkbookMission mission = OriginalWorkbookMission.create(
            original, "미션", null, MissionType.MEMO, true
        );
        MissionSubmission submission = MissionSubmission.create(
            mission, ChallengerWorkbook.create(original, 30L, 10L), "제출"
        );
        ReflectionTestUtils.setField(submission, "id", 400L);
        return submission;
    }
}
