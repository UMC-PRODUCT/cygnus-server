package com.umc.product.curriculum.adapter.in.web.v2.dto.response;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.umc.product.curriculum.application.port.in.query.dto.CurriculumOverviewInfo;
import com.umc.product.curriculum.application.port.in.query.dto.MyCurriculumInfo;
import com.umc.product.curriculum.domain.enums.FeedbackResult;
import com.umc.product.curriculum.domain.enums.MissionType;
import com.umc.product.curriculum.domain.enums.OriginalWorkbookStatus;
import com.umc.product.curriculum.domain.enums.OriginalWorkbookType;
import com.umc.product.curriculum.domain.enums.SubmissionStatus;

@DisplayName("Curriculum v2 response 변환 계약")
class CurriculumResponseContractTest {

    private static final Instant START = Instant.parse("2030-01-01T00:00:00Z");
    private static final Instant END = Instant.parse("2030-01-07T00:00:00Z");

    @Test
    @DisplayName("개요 info의 주차 순서와 필드를 응답으로 보존한다")
    void converts_overview() {
        var week = CurriculumOverviewInfo.WeeklyCurriculumOverviewInfo.builder()
            .weeklyCurriculumId(10L).weekNo(1L).title("1주차").isExtra(false)
            .startsAt(START).endsAt(END).build();
        var info = CurriculumOverviewInfo.builder()
            .curriculumId(1L).title("커리큘럼").weeks(List.of(week)).build();

        CurriculumOverviewResponse response = CurriculumOverviewResponse.from(info);

        assertThat(response.curriculumId()).isEqualTo(1L);
        assertThat(response.weeks()).singleElement().satisfies(value -> {
            assertThat(value.weeklyCurriculumId()).isEqualTo(10L);
            assertThat(value.endsAt()).isEqualTo(END);
        });
    }

    @Test
    @DisplayName("배포·제출·피드백이 있는 전체 중첩 info를 누락 없이 응답으로 변환한다")
    void converts_nested_progress_with_submission_and_feedback() {
        var feedback = MyCurriculumInfo.MissionFeedbackInfo.builder()
            .missionFeedbackId(50L).reviewerMemberId(7L).content("통과")
            .feedbackResult(FeedbackResult.PASS).build();
        var submission = MyCurriculumInfo.MissionSubmissionInfo.builder()
            .missionSubmissionId(40L).submittedAsType(MissionType.LINK)
            .submittedContent("https://example.com").submittedAt(START).lastEditedAt(END)
            .status(SubmissionStatus.PASS).hasFeedback(true).feedbacks(List.of(feedback)).build();
        var mission = MyCurriculumInfo.MyOriginalWorkbookMissionInfo.builder()
            .originalWorkbookMissionId(30L).title("미션").description("설명")
            .missionType(MissionType.LINK).isNecessary(true).hasSubmission(true)
            .submission(Optional.of(submission)).build();
        var workbook = MyCurriculumInfo.MyOriginalWorkbookInfo.builder()
            .originalWorkbookId(20L).title("워크북").description("설명").url("url")
            .type(OriginalWorkbookType.MAIN).missions(List.of(mission))
            .challengerWorkbookId(Optional.of(99L)).build();
        var week = MyCurriculumInfo.MyWeeklyCurriculumInfo.builder()
            .weeklyCurriculumId(10L).weekNo(1L).title("1주차").startsAt(START).endsAt(END)
            .releasedOriginalWorkbooks(List.of(workbook)).build();

        MyCurriculumResponse response = MyCurriculumResponse.from(
            MyCurriculumInfo.builder().curriculumId(1L).title("커리큘럼").weeks(List.of(week)).build()
        );

        var resultWeek = response.weeks().get(0);
        assertThat(resultWeek.status()).isEqualTo(WeeklyCurriculumStatus.IN_PROGRESS);
        var resultWorkbook = resultWeek.originalWorkbooks().get(0);
        assertThat(resultWorkbook.isDeployedToMember()).isTrue();
        assertThat(resultWorkbook.challengerWorkbookId()).isEqualTo(99L);
        var resultSubmission = resultWorkbook.missions().get(0).submission();
        assertThat(resultSubmission.originalWorkbookMissionId()).isEqualTo(30L);
        assertThat(resultSubmission.status()).isEqualTo(SubmissionStatus.PASS);
        assertThat(resultSubmission.feedbacks()).singleElement()
            .extracting(MissionFeedbackResponse::feedbackResult).isEqualTo(FeedbackResult.PASS);
    }

    @Test
    @DisplayName("미배포·미제출 상태는 NOT_STARTED와 null 중첩 값으로 변환한다")
    void converts_absent_nested_data() {
        var mission = MyCurriculumInfo.MyOriginalWorkbookMissionInfo.builder()
            .originalWorkbookMissionId(30L).title("미션").missionType(MissionType.PLAIN)
            .hasSubmission(false).submission(Optional.empty()).build();
        var workbook = MyCurriculumInfo.MyOriginalWorkbookInfo.builder()
            .originalWorkbookId(20L).title("워크북").type(OriginalWorkbookType.MAIN)
            .missions(List.of(mission)).challengerWorkbookId(Optional.empty()).build();
        var week = MyCurriculumInfo.MyWeeklyCurriculumInfo.builder()
            .weeklyCurriculumId(10L).weekNo(1L).title("1주차")
            .releasedOriginalWorkbooks(List.of(workbook)).build();

        MyCurriculumResponse response = MyCurriculumResponse.from(
            MyCurriculumInfo.builder().curriculumId(1L).title("커리큘럼").weeks(List.of(week)).build()
        );

        assertThat(response.weeks().get(0).status()).isEqualTo(WeeklyCurriculumStatus.NOT_STARTED);
        var result = response.weeks().get(0).originalWorkbooks().get(0);
        assertThat(result.isDeployedToMember()).isFalse();
        assertThat(result.challengerWorkbookId()).isNull();
        assertThat(result.missions().get(0).submission()).isNull();
    }

    @Test
    @DisplayName("정적 변환이 없는 응답 DTO와 enum도 생성·직렬화 계약을 유지한다")
    void plain_response_contracts() {
        var submission = MissionSubmissionResponse.builder().missionSubmissionId(1L).feedbacks(List.of()).build();
        var challenger = ChallengerWorkbookResponse.builder()
            .challengerWorkbookId(2L).status(ChallengerWorkbookStatusResponse.IN_PROGRESS)
            .submission(submission).build();
        var best = BestWorkbookResponse.builder()
            .weeklyBestWorkbookEntityId(3L).challengerWorkbooks(List.of(challenger)).build();
        var mission = new OriginalWorkbookMissionResponse(4L, 5L, "미션", null, MissionType.PLAIN, false);
        var original = new OriginalWorkbookResponse(
            4L, "워크북", null, null, null, OriginalWorkbookType.MAIN,
            OriginalWorkbookStatus.DRAFT, null, null, mission
        );

        assertThat(best.challengerWorkbooks()).containsExactly(challenger);
        assertThat(original.missions()).isSameAs(mission);
        assertThat(CreateOriginalWorkbookMissionResponse.from(5L).originalWorkbookMissionId()).isEqualTo(5L);
        assertThat(ChallengerWorkbookStatusResponse.values()).hasSize(3);
        assertThat(WeeklyCurriculumStatus.values()).contains(
            WeeklyCurriculumStatus.AWAITING_RESULT,
            WeeklyCurriculumStatus.BEST,
            WeeklyCurriculumStatus.PASS,
            WeeklyCurriculumStatus.FAIL
        );
    }
}
