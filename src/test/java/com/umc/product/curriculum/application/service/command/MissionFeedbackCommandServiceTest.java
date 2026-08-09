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
import com.umc.product.curriculum.application.port.in.command.dto.workbook.mission.CreateMissionFeedbackCommand;
import com.umc.product.curriculum.application.port.out.LoadMissionFeedbackPort;
import com.umc.product.curriculum.application.port.out.LoadMissionSubmissionPort;
import com.umc.product.curriculum.application.port.out.SaveMissionFeedbackPort;
import com.umc.product.curriculum.domain.ChallengerWorkbook;
import com.umc.product.curriculum.domain.Curriculum;
import com.umc.product.curriculum.domain.MissionFeedback;
import com.umc.product.curriculum.domain.MissionSubmission;
import com.umc.product.curriculum.domain.OriginalWorkbook;
import com.umc.product.curriculum.domain.OriginalWorkbookMission;
import com.umc.product.curriculum.domain.WeeklyCurriculum;
import com.umc.product.curriculum.domain.enums.FeedbackResult;
import com.umc.product.curriculum.domain.enums.MissionType;
import com.umc.product.curriculum.domain.enums.OriginalWorkbookType;

@ExtendWith(MockitoExtension.class)
class MissionFeedbackCommandServiceTest {

    @Mock private LoadMissionSubmissionPort loadMissionSubmissionPort;
    @Mock private LoadMissionFeedbackPort loadMissionFeedbackPort;
    @Mock private SaveMissionFeedbackPort saveMissionFeedbackPort;
    @Mock private MissionMutationPolicy missionMutationPolicy;
    @InjectMocks private MissionFeedbackCommandService service;

    @Test
    @DisplayName("피드백 생성은 제출 철회와 직렬화하기 위해 제출물을 쓰기 잠금으로 조회한다")
    void create_loadsSubmissionForUpdate() {
        MissionSubmission submission = submission();
        given(loadMissionSubmissionPort.getByIdForUpdate(400L)).willReturn(submission);
        given(saveMissionFeedbackPort.save(org.mockito.ArgumentMatchers.any(MissionFeedback.class)))
            .willAnswer(invocation -> {
                MissionFeedback feedback = invocation.getArgument(0);
                ReflectionTestUtils.setField(feedback, "id", 500L);
                return feedback;
            });

        Long feedbackId = service.create(CreateMissionFeedbackCommand.builder()
            .missionSubmissionId(400L)
            .reviewerMemberId(50L)
            .content("통과")
            .result(FeedbackResult.PASS)
            .build());

        assertThat(feedbackId).isEqualTo(500L);
        verify(loadMissionSubmissionPort).getByIdForUpdate(400L);
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
