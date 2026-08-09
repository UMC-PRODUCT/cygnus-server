package com.umc.product.curriculum.domain;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.umc.product.common.domain.enums.ChallengerPart;
import com.umc.product.curriculum.domain.enums.FeedbackResult;
import com.umc.product.curriculum.domain.enums.MissionType;
import com.umc.product.curriculum.domain.enums.OriginalWorkbookType;
import com.umc.product.curriculum.domain.exception.CurriculumDomainException;
import com.umc.product.curriculum.domain.exception.CurriculumErrorCode;

class MissionFeedbackTest {

    @Test
    @DisplayName("철회된 제출에는 피드백을 생성할 수 없다")
    void withdrawnSubmissionRejectsFeedback() {
        MissionSubmission submission = submission();
        submission.withdraw(Instant.parse("2026-07-23T00:00:00Z"));

        assertThatThrownBy(() -> MissionFeedback.create(
            submission, 2L, "피드백", FeedbackResult.PASS
        ))
            .isInstanceOf(CurriculumDomainException.class)
            .extracting("baseCode")
            .isEqualTo(CurriculumErrorCode.MISSION_SUBMISSION_ALREADY_WITHDRAWN);
    }

    private MissionSubmission submission() {
        Curriculum curriculum = Curriculum.create(9L, ChallengerPart.SPRINGBOOT, "커리큘럼");
        WeeklyCurriculum weekly = WeeklyCurriculum.create(
            curriculum,
            1L,
            false,
            "1주차",
            Instant.parse("2026-07-01T00:00:00Z"),
            Instant.parse("2026-07-31T00:00:00Z")
        );
        OriginalWorkbook original = OriginalWorkbook.createAsDraft(
            weekly, "워크북", null, null, null, OriginalWorkbookType.MAIN
        );
        OriginalWorkbookMission mission = OriginalWorkbookMission.create(
            original, "미션", null, MissionType.MEMO, true
        );
        return MissionSubmission.create(
            mission, ChallengerWorkbook.create(original, 1L, 10L), "제출"
        );
    }
}
