package com.umc.product.curriculum.application.service.query;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import com.umc.product.curriculum.application.port.in.query.dto.ChallengerWorkbookInfo;
import com.umc.product.curriculum.application.port.out.LoadChallengerWorkbookPort;
import com.umc.product.curriculum.application.port.out.LoadMissionFeedbackPort;
import com.umc.product.curriculum.application.port.out.LoadMissionSubmissionPort;
import com.umc.product.curriculum.domain.ChallengerWorkbook;
import com.umc.product.curriculum.domain.MissionFeedback;
import com.umc.product.curriculum.domain.MissionSubmission;
import com.umc.product.curriculum.domain.OriginalWorkbook;
import com.umc.product.curriculum.domain.OriginalWorkbookMission;
import com.umc.product.curriculum.domain.enums.FeedbackResult;
import com.umc.product.curriculum.domain.enums.MissionType;

@ExtendWith(MockitoExtension.class)
class ChallengerWorkbookQueryServiceTest {

    @Mock
    LoadChallengerWorkbookPort loadChallengerWorkbookPort;

    @Mock
    LoadMissionSubmissionPort loadMissionSubmissionPort;

    @Mock
    LoadMissionFeedbackPort loadMissionFeedbackPort;

    @InjectMocks
    ChallengerWorkbookQueryService sut;

    @Test
    @DisplayName("챌린저 워크북 상세 조회 시 제출물과 피드백을 조립한다")
    void getChallengerWorkbookSuccess() {
        // given
        OriginalWorkbook originalWorkbook = Mockito.mock(OriginalWorkbook.class);
        given(originalWorkbook.getId()).willReturn(200L);

        ChallengerWorkbook challengerWorkbook = Mockito.mock(ChallengerWorkbook.class);
        given(challengerWorkbook.getId()).willReturn(10L);
        given(challengerWorkbook.getOriginalWorkbook()).willReturn(originalWorkbook);
        given(challengerWorkbook.getStudyGroupId()).willReturn(30L);
        given(challengerWorkbook.getMemberId()).willReturn(40L);
        given(challengerWorkbook.isExcused()).willReturn(false);
        given(challengerWorkbook.getExcusedReason()).willReturn(null);
        given(challengerWorkbook.getContent()).willReturn("챌린저 워크북 본문");

        OriginalWorkbookMission mission = Mockito.mock(OriginalWorkbookMission.class);
        given(mission.getId()).willReturn(300L);

        MissionSubmission submission = Mockito.mock(MissionSubmission.class);
        given(submission.getId()).willReturn(400L);
        given(submission.getOriginalWorkbookMission()).willReturn(mission);
        given(submission.getSubmittedAsType()).willReturn(MissionType.LINK);
        given(submission.getContent()).willReturn("https://github.com/umc/product");
        given(submission.getCreatedAt()).willReturn(Instant.parse("2026-06-02T00:00:00Z"));
        given(submission.getUpdatedAt()).willReturn(Instant.parse("2026-06-03T00:00:00Z"));

        MissionFeedback feedback = Mockito.mock(MissionFeedback.class);
        given(feedback.getId()).willReturn(500L);
        given(feedback.getMissionSubmission()).willReturn(submission);
        given(feedback.getReviewerMemberId()).willReturn(60L);
        given(feedback.getContent()).willReturn("좋습니다");
        given(feedback.getFeedbackResult()).willReturn(FeedbackResult.PASS);

        given(loadChallengerWorkbookPort.findById(10L)).willReturn(challengerWorkbook);
        given(loadMissionSubmissionPort.findByChallengerWorkbookId(10L)).willReturn(List.of(submission));
        given(loadMissionFeedbackPort.findByMissionSubmissionIdIn(List.of(400L))).willReturn(List.of(feedback));

        // when
        ChallengerWorkbookInfo result = sut.getById(10L);

        // then
        assertThat(result.challengerWorkbookId()).isEqualTo(10L);
        assertThat(result.originalWorkbookId()).isEqualTo(200L);
        assertThat(result.receivedStudyGroupId()).isEqualTo(30L);
        assertThat(result.challengerId()).isEqualTo(40L);
        assertThat(result.content()).isEqualTo("챌린저 워크북 본문");
        assertThat(result.isBestWorkbook()).isFalse();
        assertThat(result.submissions()).hasSize(1);
        assertThat(result.submissions().get(0).missionSubmissionId()).isEqualTo(400L);
        assertThat(result.submissions().get(0).hasFeedback()).isTrue();
        assertThat(result.submissions().get(0).feedbacks()).hasSize(1);
        assertThat(result.submissions().get(0).feedbacks().get(0).feedbackResult()).isEqualTo(FeedbackResult.PASS);
    }
}
