package com.umc.product.curriculum.application.port.in;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

import java.time.Instant;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.umc.product.common.domain.enums.ChallengerPart;
import com.umc.product.curriculum.application.port.in.command.dto.ReviewWorkbookCommand;
import com.umc.product.curriculum.application.port.in.command.dto.SelectBestWorkbookCommand;
import com.umc.product.curriculum.application.port.in.command.dto.SubmitChallengerWorkbookCommand;
import com.umc.product.curriculum.application.port.in.command.dto.SubmitMissionCommand;
import com.umc.product.curriculum.application.port.in.command.dto.SubmitWorkbookCommand;
import com.umc.product.curriculum.application.port.in.command.dto.curriculum.CurriculumCommand;
import com.umc.product.curriculum.application.port.in.query.dto.ChallengerWorkbookInfo;
import com.umc.product.curriculum.application.port.in.query.dto.CurriculumInfo;
import com.umc.product.curriculum.application.port.in.query.dto.CurriculumProgressInfo;
import com.umc.product.curriculum.application.port.in.query.dto.CurriculumProjection;
import com.umc.product.curriculum.application.port.in.query.dto.CurriculumWeekInfo;
import com.umc.product.curriculum.application.port.in.query.dto.GetBestWorkbooksQuery;
import com.umc.product.curriculum.application.port.in.query.dto.GetWorkbookSubmissionsQuery;
import com.umc.product.curriculum.application.port.in.query.dto.OriginalWorkbookInfo;
import com.umc.product.curriculum.application.port.in.query.dto.StudyGroupFilterInfo;
import com.umc.product.curriculum.application.port.in.query.dto.WeeklyBestWorkbookInfo;
import com.umc.product.curriculum.application.port.in.query.dto.WorkbookProgressProjection;
import com.umc.product.curriculum.application.port.in.query.dto.WorkbookSubmissionDetailInfo;
import com.umc.product.curriculum.application.port.in.query.dto.WorkbookSubmissionInfo;
import com.umc.product.curriculum.domain.ChallengerWorkbook;
import com.umc.product.curriculum.domain.enums.FeedbackResult;
import com.umc.product.curriculum.domain.enums.MissionType;
import com.umc.product.curriculum.domain.enums.OriginalWorkbookStatus;
import com.umc.product.curriculum.domain.enums.OriginalWorkbookType;
import com.umc.product.curriculum.domain.enums.WorkbookStatus;
import com.umc.product.global.exception.NotImplementedException;

@SuppressWarnings("removal")
@DisplayName("Curriculum application DTO 계약")
class CurriculumDtoContractTest {

    private static final Instant DATE = Instant.parse("2030-01-01T00:00:00Z");

    @Test
    @DisplayName("legacy command와 미션 command는 필수 식별자 및 nullable 제출을 보존한다")
    void command_records_and_required_identifiers() {
        assertThat(new ReviewWorkbookCommand(1L, WorkbookStatus.PASS, "통과").status())
            .isEqualTo(WorkbookStatus.PASS);
        assertThat(new SelectBestWorkbookCommand(1L, "우수").bestReason()).isEqualTo("우수");
        assertThat(new SubmitChallengerWorkbookCommand(1L, 2L, null).submission()).isNull();
        assertThat(new SubmitWorkbookCommand(1L, 2L, "제출").challengerId()).isEqualTo(2L);
        assertThat(new SubmitMissionCommand(1L, 2L, null).submission()).isNull();

        assertThatThrownBy(() -> new SubmitMissionCommand(null, 2L, null))
            .isInstanceOf(NullPointerException.class).hasMessageContaining("missionId");
        assertThatThrownBy(() -> new SubmitMissionCommand(1L, null, null))
            .isInstanceOf(NullPointerException.class).hasMessageContaining("challengerWorkbookId");
    }

    @Test
    @DisplayName("legacy workbook command는 실제 값과 UI 호환 기본값을 모두 해석한다")
    void workbook_command_defaults_and_values() {
        var defaults = new CurriculumCommand.WorkbookCommand(
            null, 1, "제목", null, null, null, null, null
        );
        var values = new CurriculumCommand.WorkbookCommand(
            2L, 2, "제목2", "설명", "url", DATE, DATE.plusSeconds(1), MissionType.MEMO
        );
        var command = new CurriculumCommand(ChallengerPart.SPRINGBOOT, "커리큘럼", List.of(defaults, values));

        assertThat(command.workbooks()).hasSize(2);
        assertThat(defaults.hasId()).isFalse();
        assertThat(defaults.resolveStartDate()).isEqualTo(Instant.parse("2099-12-31T00:00:00Z"));
        assertThat(defaults.resolveEndDate()).isEqualTo(Instant.parse("2099-12-31T00:00:00Z"));
        assertThat(defaults.resolveMissionType()).isEqualTo(MissionType.LINK);
        assertThat(values.hasId()).isTrue();
        assertThat(values.resolveStartDate()).isEqualTo(DATE);
        assertThat(values.resolveEndDate()).isEqualTo(DATE.plusSeconds(1));
        assertThat(values.resolveMissionType()).isEqualTo(MissionType.MEMO);
    }

    @Test
    @DisplayName("검색 query는 0·음수 크기 기본값과 양수 fetch 경계를 적용한다")
    void query_size_defaults_and_fetch_boundary() {
        var bestDefault = new GetBestWorkbooksQuery(9L, null, null, null, null, null, -1);
        var bestSized = new GetBestWorkbooksQuery(
            9L, Set.of(1L), Set.of(ChallengerPart.SPRINGBOOT), List.of(1L), List.of(2L), 3L, 7
        );
        var submissionsDefault = new GetWorkbookSubmissionsQuery(null, 1, null, null, null, 0);
        var submissionsSized = new GetWorkbookSubmissionsQuery(1L, 2, 3L, ChallengerPart.SPRINGBOOT, 4L, 5);

        assertThat(bestDefault.size()).isEqualTo(20);
        assertThat(bestSized.size()).isEqualTo(7);
        assertThat(submissionsDefault.size()).isEqualTo(20);
        assertThat(submissionsDefault.fetchSize()).isEqualTo(21);
        assertThat(submissionsSized.fetchSize()).isEqualTo(6);
    }

    @Test
    @DisplayName("projection과 legacy info record의 생성 계약을 보존한다")
    void projection_and_legacy_info_records() {
        var curriculumWorkbook = new CurriculumInfo.WorkbookInfo(
            1L, 1, "워크북", "설명", "url", DATE, DATE, MissionType.LINK, DATE, true
        );
        var curriculum = new CurriculumInfo(
            1L, ChallengerPart.SPRINGBOOT, "커리큘럼", List.of(curriculumWorkbook)
        );
        var progressWorkbook = new CurriculumProgressInfo.WorkbookProgressInfo(
            1L, 2L, 1, "워크북", "설명", MissionType.LINK, WorkbookStatus.PASS, true, false
        );
        var progress = new CurriculumProgressInfo(1L, "커리큘럼", "SPRINGBOOT", 1, 1, List.of(progressWorkbook));
        var projection = new WorkbookProgressProjection(
            1L, 1, "워크북", "설명", MissionType.LINK, DATE, DATE, DATE, 2L, WorkbookStatus.PASS
        );
        var submission = new WorkbookSubmissionInfo(
            1L, 2L, "회원", "챌린저", "image", "학교", "SPRINGBOOT", "워크북", WorkbookStatus.PASS
        );

        assertThat(curriculum.workbooks()).containsExactly(curriculumWorkbook);
        assertThat(progress.completedCount()).isEqualTo(1);
        assertThat(projection.challengerWorkbookId()).isEqualTo(2L);
        assertThat(submission.schoolName()).isEqualTo("학교");
        assertThat(new CurriculumProjection(1L, ChallengerPart.SPRINGBOOT, "제목").id()).isEqualTo(1L);
        assertThat(new CurriculumWeekInfo(1, "1주차").weekNo()).isEqualTo(1);
        assertThat(new StudyGroupFilterInfo(1L, "그룹").name()).isEqualTo("그룹");
    }

    @Test
    @DisplayName("신규 workbook info의 모든 중첩 record를 생성할 수 있다")
    void new_workbook_info_records() {
        var feedback = ChallengerWorkbookInfo.MissionFeedbackInfo.builder()
            .missionFeedbackId(1L).reviewerMemberId(2L).content("통과")
            .feedbackResult(FeedbackResult.PASS).build();
        var submission = ChallengerWorkbookInfo.MissionSubmissionInfo.builder()
            .missionSubmissionId(3L).originalWorkbookMissionId(4L).submittedAsType(MissionType.LINK)
            .submittedContent("url").submittedAt(DATE).lastEditedAt(DATE)
            .hasFeedback(true).feedbacks(List.of(feedback)).build();
        var challenger = ChallengerWorkbookInfo.builder()
            .challengerWorkbookId(5L).originalWorkbookId(6L).submissions(List.of(submission)).build();
        var mission = OriginalWorkbookInfo.OriginalWorkbookMissionInfo.builder()
            .originalWorkbookMissionId(4L).title("미션").missionType(MissionType.LINK).build();
        var original = OriginalWorkbookInfo.builder()
            .originalWorkbookId(6L).title("워크북").type(OriginalWorkbookType.MAIN)
            .status(OriginalWorkbookStatus.READY).missions(List.of(mission)).build();
        var best = WeeklyBestWorkbookInfo.builder()
            .weeklyBestWorkbookEntityId(7L).challengerWorkbooks(List.of(challenger)).build();

        assertThat(best.challengerWorkbooks()).containsExactly(challenger);
        assertThat(original.missions()).containsExactly(mission);
        assertThat(submission.feedbacks()).containsExactly(feedback);
        assertThatThrownBy(() -> WorkbookSubmissionDetailInfo.from(mock(ChallengerWorkbook.class)))
            .isInstanceOf(NotImplementedException.class);
        assertThat(new WorkbookSubmissionDetailInfo(1L, "제출").submission()).isEqualTo("제출");
    }
}
