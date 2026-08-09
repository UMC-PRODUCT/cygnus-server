package com.umc.product.curriculum.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.umc.product.common.domain.enums.ChallengerPart;
import com.umc.product.curriculum.domain.enums.MissionType;
import com.umc.product.curriculum.domain.enums.OriginalWorkbookType;
import com.umc.product.curriculum.domain.exception.CurriculumDomainException;
import com.umc.product.curriculum.domain.exception.CurriculumErrorCode;

class MissionSubmissionTest {

    @Test
    @DisplayName("제출을 철회하면 행을 유지한 채 철회 시각을 기록한다")
    void withdraw_recordsTimestamp() {
        MissionSubmission submission = submission();
        Instant withdrawnAt = Instant.parse("2026-07-23T00:00:00Z");

        submission.withdraw(withdrawnAt);

        assertThat(submission.isWithdrawn()).isTrue();
        assertThat(submission.getWithdrawnAt()).isEqualTo(withdrawnAt);
    }

    @Test
    @DisplayName("철회한 제출은 다시 수정하거나 철회할 수 없다")
    void withdrawnSubmission_cannotMutateAgain() {
        MissionSubmission submission = submission();
        submission.withdraw(Instant.parse("2026-07-23T00:00:00Z"));

        assertThatThrownBy(() -> submission.edit("수정"))
            .isInstanceOf(CurriculumDomainException.class)
            .extracting("baseCode")
            .isEqualTo(CurriculumErrorCode.MISSION_SUBMISSION_ALREADY_WITHDRAWN);
        assertThatThrownBy(() -> submission.withdraw(Instant.parse("2026-07-24T00:00:00Z")))
            .isInstanceOf(CurriculumDomainException.class)
            .extracting("baseCode")
            .isEqualTo(CurriculumErrorCode.MISSION_SUBMISSION_ALREADY_WITHDRAWN);
    }

    private MissionSubmission submission() {
        Curriculum curriculum = Curriculum.create(9L, ChallengerPart.SPRINGBOOT, "커리큘럼");
        WeeklyCurriculum weekly = WeeklyCurriculum.create(
            curriculum, 1L, false, "1주차",
            Instant.parse("2026-07-01T00:00:00Z"), Instant.parse("2026-07-31T00:00:00Z")
        );
        OriginalWorkbook original = OriginalWorkbook.createAsDraft(
            weekly, "워크북", null, null, null, OriginalWorkbookType.MAIN
        );
        OriginalWorkbookMission mission = OriginalWorkbookMission.create(
            original, "미션", null, MissionType.MEMO, true
        );
        ChallengerWorkbook workbook = ChallengerWorkbook.create(original, 1L, 10L);
        return MissionSubmission.create(mission, workbook, "제출");
    }
}
