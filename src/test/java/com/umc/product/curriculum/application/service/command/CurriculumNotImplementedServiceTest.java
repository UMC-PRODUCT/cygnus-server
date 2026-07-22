package com.umc.product.curriculum.application.service.command;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.umc.product.challenger.application.port.in.query.GetChallengerUseCase;
import com.umc.product.curriculum.application.port.in.command.dto.workbook.CreateWeeklyBestWorkbookCommand;
import com.umc.product.curriculum.application.port.in.command.dto.workbook.DeleteChallengerWorkbookCommand;
import com.umc.product.curriculum.application.port.in.command.dto.workbook.DeployChallengerWorkbookCommand;
import com.umc.product.curriculum.application.port.in.command.dto.workbook.EditChallengerWorkbookCommand;
import com.umc.product.curriculum.application.port.in.command.dto.workbook.EditWeeklyBestWorkbookCommand;
import com.umc.product.curriculum.application.port.in.command.dto.workbook.ExcuseChallengerWorkbookCommand;
import com.umc.product.curriculum.application.port.in.command.dto.workbook.mission.CreateMissionFeedbackCommand;
import com.umc.product.curriculum.application.port.in.command.dto.workbook.mission.CreateMissionSubmissionCommand;
import com.umc.product.curriculum.application.port.in.command.dto.workbook.mission.DeleteMissionFeedbackCommand;
import com.umc.product.curriculum.application.port.in.command.dto.workbook.mission.DeleteMissionSubmissionCommand;
import com.umc.product.curriculum.application.port.in.command.dto.workbook.mission.EditMissionFeedbackCommand;
import com.umc.product.curriculum.application.port.in.command.dto.workbook.mission.EditMissionSubmissionCommand;
import com.umc.product.curriculum.application.port.out.LoadChallengerWorkbookPort;
import com.umc.product.curriculum.application.port.out.LoadOriginalWorkbookPort;
import com.umc.product.curriculum.application.port.out.SaveChallengerWorkbookPort;
import com.umc.product.global.exception.NotImplementedException;

@DisplayName("Curriculum 명시적 미구현 service")
class CurriculumNotImplementedServiceTest {

    @Test
    @DisplayName("챌린저 워크북의 네 command는 silent no-op이 아니라 미구현 예외를 반환한다")
    void challenger_workbook_operations_are_explicit() {
        ChallengerWorkbookCommandService sut = new ChallengerWorkbookCommandService(
            mock(LoadChallengerWorkbookPort.class), mock(LoadOriginalWorkbookPort.class),
            mock(SaveChallengerWorkbookPort.class), mock(GetChallengerUseCase.class)
        );

        assertNotImplemented(() -> sut.batchDeploy(DeployChallengerWorkbookCommand.builder().build()));
        assertNotImplemented(() -> sut.edit(EditChallengerWorkbookCommand.builder().build()));
        assertNotImplemented(() -> sut.delete(DeleteChallengerWorkbookCommand.builder().build()));
        assertNotImplemented(() -> sut.excuse(ExcuseChallengerWorkbookCommand.builder().build()));
    }

    @Test
    @DisplayName("주간 베스트 워크북의 선정·수정·철회는 미구현 예외를 반환한다")
    void weekly_best_operations_are_explicit() {
        WeeklyBestWorkbookCommandService sut = new WeeklyBestWorkbookCommandService();

        assertNotImplemented(() -> sut.selectBest(CreateWeeklyBestWorkbookCommand.builder().build()));
        assertNotImplemented(() -> sut.editReason(EditWeeklyBestWorkbookCommand.builder().build()));
        assertNotImplemented(() -> sut.withdraw(1L));
    }

    @Test
    @DisplayName("미션 제출물의 생성·수정·철회는 미구현 예외를 반환한다")
    void submission_operations_are_explicit() {
        MissionSubmissionCommandService sut = new MissionSubmissionCommandService();

        assertNotImplemented(() -> sut.create(CreateMissionSubmissionCommand.builder().build()));
        assertNotImplemented(() -> sut.edit(EditMissionSubmissionCommand.builder().build()));
        assertNotImplemented(() -> sut.withdraw(DeleteMissionSubmissionCommand.builder().build()));
    }

    @Test
    @DisplayName("미션 피드백의 생성·수정·삭제는 미구현 예외를 반환한다")
    void feedback_operations_are_explicit() {
        MissionFeedbackCommandService sut = new MissionFeedbackCommandService();

        assertNotImplemented(() -> sut.create(CreateMissionFeedbackCommand.builder().build()));
        assertNotImplemented(() -> sut.edit(EditMissionFeedbackCommand.builder().build()));
        assertNotImplemented(() -> sut.delete(DeleteMissionFeedbackCommand.builder().build()));
    }

    private void assertNotImplemented(Runnable action) {
        assertThatThrownBy(action::run).isInstanceOf(NotImplementedException.class);
    }
}
