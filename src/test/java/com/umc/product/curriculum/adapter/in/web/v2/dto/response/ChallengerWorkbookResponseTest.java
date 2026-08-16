package com.umc.product.curriculum.adapter.in.web.v2.dto.response;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;

import com.umc.product.curriculum.application.port.in.query.dto.ChallengerWorkbookInfo;
import com.umc.product.curriculum.application.port.in.query.dto.ChallengerWorkbookInfo.MissionFeedbackInfo;
import com.umc.product.curriculum.application.port.in.query.dto.ChallengerWorkbookInfo.MissionSubmissionInfo;
import com.umc.product.curriculum.domain.enums.FeedbackResult;
import com.umc.product.curriculum.domain.enums.MissionType;
import com.umc.product.curriculum.domain.enums.SubmissionStatus;

class ChallengerWorkbookResponseTest {

    @Test
    void 필수_미션_일부만_PASS면_워크북은_진행중이다() {
        ChallengerWorkbookInfo info = workbookInfo(
            Set.of(11L, 12L),
            List.of(submission(101L, 11L, FeedbackResult.PASS))
        );

        ChallengerWorkbookResponse response = ChallengerWorkbookResponse.from(info);

        assertThat(response.status()).isEqualTo(ChallengerWorkbookStatusResponse.IN_PROGRESS);
    }

    @Test
    void 동일_제출에_PASS와_FAIL이_함께_있으면_제출과_워크북은_FAIL이다() {
        MissionSubmissionInfo submission = MissionSubmissionInfo.builder()
            .missionSubmissionId(101L)
            .originalWorkbookMissionId(11L)
            .submittedAsType(MissionType.MEMO)
            .hasFeedback(true)
            .feedbacks(List.of(feedback(FeedbackResult.PASS), feedback(FeedbackResult.FAIL)))
            .build();

        ChallengerWorkbookResponse response = ChallengerWorkbookResponse.from(
            workbookInfo(Set.of(11L), List.of(submission))
        );

        assertThat(response.submissions().get(0).status()).isEqualTo(SubmissionStatus.FAIL);
        assertThat(response.status()).isEqualTo(ChallengerWorkbookStatusResponse.FAIL);
    }

    @Test
    void 모든_필수_미션이_PASS면_워크북도_PASS다() {
        ChallengerWorkbookInfo info = workbookInfo(
            Set.of(11L, 12L),
            List.of(
                submission(101L, 11L, FeedbackResult.PASS),
                submission(102L, 12L, FeedbackResult.PASS)
            )
        );

        ChallengerWorkbookResponse response = ChallengerWorkbookResponse.from(info);

        assertThat(response.status()).isEqualTo(ChallengerWorkbookStatusResponse.PASS);
    }

    private ChallengerWorkbookInfo workbookInfo(
        Set<Long> requiredMissionIds,
        List<MissionSubmissionInfo> submissions
    ) {
        return ChallengerWorkbookInfo.builder()
            .challengerWorkbookId(1L)
            .originalWorkbookId(2L)
            .receivedStudyGroupId(3L)
            .challengerId(4L)
            .requiredMissionIds(requiredMissionIds)
            .submissions(submissions)
            .build();
    }

    private MissionSubmissionInfo submission(
        Long submissionId,
        Long missionId,
        FeedbackResult result
    ) {
        return MissionSubmissionInfo.builder()
            .missionSubmissionId(submissionId)
            .originalWorkbookMissionId(missionId)
            .submittedAsType(MissionType.MEMO)
            .hasFeedback(true)
            .feedbacks(List.of(feedback(result)))
            .build();
    }

    private MissionFeedbackInfo feedback(FeedbackResult result) {
        return MissionFeedbackInfo.builder()
            .missionFeedbackId(201L)
            .reviewerMemberId(5L)
            .content("피드백")
            .feedbackResult(result)
            .build();
    }
}
