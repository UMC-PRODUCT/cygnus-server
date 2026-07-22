package com.umc.product.curriculum.adapter.in.web.v2;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;

import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import com.umc.product.common.domain.enums.ChallengerPart;
import com.umc.product.curriculum.adapter.in.web.v2.dto.request.ChangeOriginalWorkbookStatusRequest;
import com.umc.product.curriculum.adapter.in.web.v2.dto.request.CreateBestWorkbookRequest;
import com.umc.product.curriculum.adapter.in.web.v2.dto.request.CreateCurriculumRequest;
import com.umc.product.curriculum.adapter.in.web.v2.dto.request.CreateMissionFeedbackRequest;
import com.umc.product.curriculum.adapter.in.web.v2.dto.request.CreateMissionSubmissionRequest;
import com.umc.product.curriculum.adapter.in.web.v2.dto.request.CreateOriginalWorkbookMissionRequest;
import com.umc.product.curriculum.adapter.in.web.v2.dto.request.CreateOriginalWorkbookRequest;
import com.umc.product.curriculum.adapter.in.web.v2.dto.request.CreateWeeklyCurriculumRequest;
import com.umc.product.curriculum.adapter.in.web.v2.dto.request.EditCurriculumRequest;
import com.umc.product.curriculum.adapter.in.web.v2.dto.request.EditOriginalWorkbookMissionRequest;
import com.umc.product.curriculum.adapter.in.web.v2.dto.request.EditOriginalWorkbookRequest;
import com.umc.product.curriculum.adapter.in.web.v2.dto.request.EditWeeklyCurriculumRequest;
import com.umc.product.curriculum.application.port.in.command.ManageCurriculumUseCase;
import com.umc.product.curriculum.application.port.in.command.ManageOriginalWorkbookMissionUseCase;
import com.umc.product.curriculum.application.port.in.command.ManageOriginalWorkbookUseCase;
import com.umc.product.curriculum.application.port.in.command.ManageWeeklyCurriculumUseCase;
import com.umc.product.curriculum.application.port.in.command.dto.workbook.ChangeOriginalWorkbookStatusCommand;
import com.umc.product.curriculum.application.port.in.query.GetCurriculumUseCase;
import com.umc.product.curriculum.application.port.in.query.dto.CurriculumOverviewInfo;
import com.umc.product.curriculum.application.port.in.query.dto.MyCurriculumInfo;
import com.umc.product.curriculum.domain.enums.FeedbackResult;
import com.umc.product.curriculum.domain.enums.MissionType;
import com.umc.product.curriculum.domain.enums.OriginalWorkbookStatus;
import com.umc.product.curriculum.domain.enums.OriginalWorkbookType;
import com.umc.product.global.exception.NotImplementedException;
import com.umc.product.global.security.MemberPrincipal;

@DisplayName("Curriculum v2 controller 단위 계약")
class CurriculumControllerUnitTest {

    private static final Instant START = Instant.parse("2030-01-01T00:00:00Z");
    private static final Instant END = Instant.parse("2030-01-07T00:00:00Z");

    @Test
    @DisplayName("커리큘럼 controller는 path와 request를 command로 변환해 CUD를 위임한다")
    void delegates_curriculum_and_weekly_commands() {
        ManageCurriculumUseCase curriculumUseCase = mock(ManageCurriculumUseCase.class);
        ManageWeeklyCurriculumUseCase weeklyUseCase = mock(ManageWeeklyCurriculumUseCase.class);
        CurriculumCommandV2Controller sut = new CurriculumCommandV2Controller(curriculumUseCase, weeklyUseCase);
        given(curriculumUseCase.create(any())).willReturn(1L);
        given(weeklyUseCase.create(any())).willReturn(2L);

        assertThat(sut.createCurriculum(
            new CreateCurriculumRequest(9L, ChallengerPart.SPRINGBOOT, "커리큘럼"))).isEqualTo(1L);
        sut.editCurriculum(new EditCurriculumRequest("수정"), 1L);
        sut.deleteCurriculum(1L);
        assertThat(sut.createWeeklyCurriculum(
            new CreateWeeklyCurriculumRequest(1L, 1L, false, "1주차", START, END))).isEqualTo(2L);
        sut.editWeeklyCurriculum(2L, new EditWeeklyCurriculumRequest(2L, true, "부록", START, END));
        sut.deleteWeeklyCurriculum(2L);

        then(curriculumUseCase).should().edit(org.mockito.ArgumentMatchers.argThat(
            command -> command.curriculumId().equals(1L) && command.title().equals("수정")));
        then(curriculumUseCase).should().delete(1L);
        then(weeklyUseCase).should().edit(org.mockito.ArgumentMatchers.argThat(
            command -> command.weeklyCurriculumId().equals(2L) && Boolean.TRUE.equals(command.isExtra())));
        then(weeklyUseCase).should().delete(2L);
    }

    @Test
    @DisplayName("원본 워크북 controller는 READY·DRAFT 생성과 수정·삭제·상태 batch를 구분한다")
    void delegates_original_workbook_commands() {
        ManageOriginalWorkbookUseCase useCase = mock(ManageOriginalWorkbookUseCase.class);
        OriginalWorkbookCommandV2Controller sut = new OriginalWorkbookCommandV2Controller(useCase);
        var request = new CreateOriginalWorkbookRequest(
            1L, "워크북", null, null, null, OriginalWorkbookType.MAIN
        );
        given(useCase.create(any())).willReturn(10L, 11L);

        assertThat(sut.createOriginalWorkbook(request)).isEqualTo(10L);
        assertThat(sut.createOriginalWorkbookAsDraft(request)).isEqualTo(11L);
        sut.editOriginalWorkbook(10L, new EditOriginalWorkbookRequest("수정", null, null, null));
        sut.deleteOriginalWorkbook(10L);
        sut.changeOriginalWorkbookStatus(List.of(
            ChangeOriginalWorkbookStatusRequest.builder()
                .originalWorkbookId(10L).status(OriginalWorkbookStatus.RELEASED).build()
        ), new MemberPrincipal(99L));

        var createCaptor = org.mockito.ArgumentCaptor.forClass(
            com.umc.product.curriculum.application.port.in.command.dto.workbook.CreateOriginalWorkbookCommand.class);
        then(useCase).should(org.mockito.Mockito.times(2)).create(createCaptor.capture());
        assertThat(createCaptor.getAllValues()).extracting(value -> value.initialStatus())
            .containsExactly(OriginalWorkbookStatus.READY, OriginalWorkbookStatus.DRAFT);
        ArgumentCaptor<List<ChangeOriginalWorkbookStatusCommand>> statusCaptor = ArgumentCaptor.forClass(List.class);
        then(useCase).should().changeStatusForRelease(statusCaptor.capture());
        assertThat(statusCaptor.getValue()).singleElement().satisfies(command -> {
            assertThat(command.originalWorkbookId()).isEqualTo(10L);
            assertThat(command.requestedMemberId()).isEqualTo(99L);
        });
        then(useCase).should().delete(10L);
    }

    @Test
    @DisplayName("빈 상태 batch도 controller에서 빈 목록으로 안전하게 위임한다")
    void delegates_empty_status_batch() {
        ManageOriginalWorkbookUseCase useCase = mock(ManageOriginalWorkbookUseCase.class);
        new OriginalWorkbookCommandV2Controller(useCase)
            .changeOriginalWorkbookStatus(List.of(), new MemberPrincipal(99L));

        then(useCase).should().changeStatusForRelease(List.of());
    }

    @Test
    @DisplayName("원본 미션 controller는 생성 응답과 path 결합 수정·삭제를 위임한다")
    void delegates_original_mission_commands() {
        ManageOriginalWorkbookMissionUseCase useCase = mock(ManageOriginalWorkbookMissionUseCase.class);
        OriginalWorkbookMissionCommandV2Controller sut = new OriginalWorkbookMissionCommandV2Controller(useCase);
        given(useCase.create(any())).willReturn(30L);

        var response = sut.createOriginalWorkbookMission(new CreateOriginalWorkbookMissionRequest(
            10L, "미션", null, MissionType.LINK, true
        ));
        sut.editOriginalMission(30L, new EditOriginalWorkbookMissionRequest(
            "수정", null, MissionType.MEMO, false
        ));
        sut.deleteOriginalMission(30L);

        assertThat(response.originalWorkbookMissionId()).isEqualTo(30L);
        then(useCase).should().edit(org.mockito.ArgumentMatchers.argThat(
            command -> command.originalWorkbookMissionId().equals(30L)
                && command.missionType() == MissionType.MEMO));
        then(useCase).should().delete(30L);
    }

    @Test
    @DisplayName("query controller는 공개 필터와 현재 member ID를 use case에 전달하고 응답으로 변환한다")
    void delegates_curriculum_queries() {
        GetCurriculumUseCase useCase = mock(GetCurriculumUseCase.class);
        CurriculumQueryV2Controller sut = new CurriculumQueryV2Controller(useCase);
        given(useCase.getCurriculumOverview(9L, ChallengerPart.SPRINGBOOT, 1L)).willReturn(
            CurriculumOverviewInfo.builder().curriculumId(1L).title("커리큘럼").weeks(List.of()).build());
        given(useCase.getMyProgress(99L, 9L)).willReturn(
            MyCurriculumInfo.builder().curriculumId(1L).title("커리큘럼").weeks(List.of()).build());

        assertThat(sut.getCurriculum(9L, ChallengerPart.SPRINGBOOT, 1L).curriculumId()).isEqualTo(1L);
        assertThat(sut.getMyProgress(9L, new MemberPrincipal(99L)).curriculumId()).isEqualTo(1L);
    }

    @Test
    @DisplayName("구현 전 challenger workbook controller의 모든 endpoint는 명시적인 예외를 반환한다")
    void challenger_workbook_controller_is_explicit() {
        ChallengerWorkbookCommandV2Controller sut = new ChallengerWorkbookCommandV2Controller();
        MemberPrincipal principal = new MemberPrincipal(1L);

        assertNotImplemented(() -> sut.requestSingleChallengerWorkbookDeploy(principal, List.of(1L)));
        assertNotImplemented(() -> sut.editChallengerWorkbook(1L, "내용", principal));
        assertNotImplemented(() -> sut.deleteChallengerWorkbook(1L, "사유"));
        assertNotImplemented(() -> sut.excuseChallengerWorkbook(1L, "사유", principal));
        assertNotImplemented(() -> sut.createWeeklyBestWorkbook(
            new CreateBestWorkbookRequest(1L, 2L, 3L, "사유"), principal));
        assertNotImplemented(() -> sut.editWeeklyBestWorkbookReason(1L, "수정", principal));
        assertNotImplemented(() -> sut.deleteWeeklyBestWorkbook(1L, principal));
    }

    @Test
    @DisplayName("구현 전 mission과 workbook query endpoint도 모두 명시적인 예외를 반환한다")
    void remaining_controllers_are_explicit() {
        ChallengerWorkbookMissionCommandV2Controller mission =
            new ChallengerWorkbookMissionCommandV2Controller();
        assertNotImplemented(() -> mission.createOriginalWorkbookMission(
            new CreateMissionSubmissionRequest(1L, 2L, "제출")));
        assertNotImplemented(() -> mission.editOriginalMission(1L, "수정"));
        assertNotImplemented(() -> mission.deleteOriginalMission(1L));
        assertNotImplemented(() -> mission.createMissionFeedback(
            new CreateMissionFeedbackRequest(1L, "피드백", FeedbackResult.PASS)));
        assertNotImplemented(() -> mission.editMissionFeedback(1L, "수정"));
        assertNotImplemented(() -> mission.deleteMissionFeedback(1L));

        WorkbookQueryV2Controller query = new WorkbookQueryV2Controller();
        assertNotImplemented(() -> query.getOriginalWorkbook(1L));
        assertNotImplemented(() -> query.getChallengerWorkbook(1L));
        assertNotImplemented(() -> query.getBestWorkbooks(null));
    }

    private void assertNotImplemented(Runnable action) {
        assertThatThrownBy(action::run).isInstanceOf(NotImplementedException.class);
    }
}
