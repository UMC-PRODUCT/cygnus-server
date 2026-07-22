package com.umc.product.curriculum.adapter.in.web.v2.dto.request;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.umc.product.common.domain.enums.ChallengerPart;
import com.umc.product.curriculum.domain.enums.FeedbackResult;
import com.umc.product.curriculum.domain.enums.MissionType;
import com.umc.product.curriculum.domain.enums.OriginalWorkbookStatus;
import com.umc.product.curriculum.domain.enums.OriginalWorkbookType;

@DisplayName("Curriculum v2 request 변환 계약")
class CurriculumRequestContractTest {

    private static final Instant START = Instant.parse("2030-01-01T00:00:00Z");
    private static final Instant END = Instant.parse("2030-01-07T00:00:00Z");

    @Test
    @DisplayName("커리큘럼과 주차 생성·수정 request는 모든 필드를 command로 보존한다")
    void converts_curriculum_requests() {
        var create = new CreateCurriculumRequest(9L, ChallengerPart.SPRINGBOOT, "제목").toCommand();
        var createWeekly = new CreateWeeklyCurriculumRequest(
            1L, 2L, true, "부록", START, END
        ).toCommand();
        var editWeekly = new EditWeeklyCurriculumRequest(
            3L, false, "수정", START.plusSeconds(1), END.plusSeconds(1)
        ).toCommand(10L);

        assertThat(create.gisuId()).isEqualTo(9L);
        assertThat(create.part()).isEqualTo(ChallengerPart.SPRINGBOOT);
        assertThat(createWeekly.isExtra()).isTrue();
        assertThat(createWeekly.endsAt()).isEqualTo(END);
        assertThat(editWeekly.weeklyCurriculumId()).isEqualTo(10L);
        assertThat(editWeekly.weekNo()).isEqualTo(3L);
        assertThat(new EditCurriculumRequest("수정").title()).isEqualTo("수정");
    }

    @Test
    @DisplayName("원본 워크북과 미션 request는 path·requester·상태 값을 command에 결합한다")
    void converts_workbook_and_mission_requests() {
        var createWorkbook = new CreateOriginalWorkbookRequest(
            10L, "워크북", "설명", "url", "내용", OriginalWorkbookType.EXTRA
        ).toCommand(OriginalWorkbookStatus.READY);
        var editWorkbook = new EditOriginalWorkbookRequest("수정", "설명2", "url2", "내용2")
            .toCommand(20L);
        var status = ChangeOriginalWorkbookStatusRequest.builder()
            .originalWorkbookId(20L).status(OriginalWorkbookStatus.RELEASED).build()
            .toCommand(99L);
        var createMission = new CreateOriginalWorkbookMissionRequest(
            20L, "미션", "설명", MissionType.MEMO, true
        ).toCommand();
        var editMission = new EditOriginalWorkbookMissionRequest(
            "수정 미션", "설명2", MissionType.LINK, false
        ).toCommand(30L);

        assertThat(createWorkbook.initialStatus()).isEqualTo(OriginalWorkbookStatus.READY);
        assertThat(createWorkbook.type()).isEqualTo(OriginalWorkbookType.EXTRA);
        assertThat(editWorkbook.originalWorkbookId()).isEqualTo(20L);
        assertThat(status.requestedMemberId()).isEqualTo(99L);
        assertThat(createMission.isNecessary()).isTrue();
        assertThat(editMission.originalWorkbookMissionId()).isEqualTo(30L);
        assertThat(editMission.isNecessary()).isFalse();
    }

    @Test
    @DisplayName("현재 command 변환이 없는 request도 직렬화 계약 필드를 보존한다")
    void preserves_plain_request_records() {
        var best = new CreateBestWorkbookRequest(1L, 2L, 3L, "우수");
        var feedback = new CreateMissionFeedbackRequest(4L, "피드백", FeedbackResult.PASS);
        var submission = new CreateMissionSubmissionRequest(5L, 6L, "제출");
        var filters = new GetBestWorkbooksRequest(
            9L, Set.of(1L), Set.of(ChallengerPart.SPRINGBOOT), List.of(1L), List.of(2L)
        );

        assertThat(best.studyGroupId()).isEqualTo(3L);
        assertThat(feedback.result()).isEqualTo(FeedbackResult.PASS);
        assertThat(submission.challengerMissionId()).isEqualTo(6L);
        assertThat(filters.schoolIds()).containsExactly(1L);
    }
}
