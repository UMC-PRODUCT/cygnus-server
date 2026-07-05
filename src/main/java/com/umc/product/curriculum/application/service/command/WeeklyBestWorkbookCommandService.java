package com.umc.product.curriculum.application.service.command;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.umc.product.audit.application.port.in.annotation.Audited;
import com.umc.product.audit.domain.AuditAction;
import com.umc.product.challenger.application.port.in.query.GetChallengerUseCase;
import com.umc.product.challenger.application.port.in.query.dto.ChallengerInfo;
import com.umc.product.common.domain.enums.ChallengerStatus;
import com.umc.product.curriculum.application.port.in.command.ManageWeeklyBestWorkbookUseCase;
import com.umc.product.curriculum.application.port.in.command.dto.workbook.CreateWeeklyBestWorkbookCommand;
import com.umc.product.curriculum.application.port.in.command.dto.workbook.EditWeeklyBestWorkbookCommand;
import com.umc.product.curriculum.application.port.out.LoadChallengerWorkbookPort;
import com.umc.product.curriculum.application.port.out.LoadOriginalWorkbookPort;
import com.umc.product.curriculum.application.port.out.LoadWeeklyBestWorkbookPort;
import com.umc.product.curriculum.application.port.out.LoadWeeklyCurriculumPort;
import com.umc.product.curriculum.application.port.out.SaveWeeklyBestWorkbookPort;
import com.umc.product.curriculum.domain.ChallengerWorkbook;
import com.umc.product.curriculum.domain.Curriculum;
import com.umc.product.curriculum.domain.OriginalWorkbook;
import com.umc.product.curriculum.domain.WeeklyBestWorkbook;
import com.umc.product.curriculum.domain.WeeklyCurriculum;
import com.umc.product.curriculum.domain.exception.CurriculumDomainException;
import com.umc.product.curriculum.domain.exception.CurriculumErrorCode;
import com.umc.product.global.exception.constant.Domain;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class WeeklyBestWorkbookCommandService implements ManageWeeklyBestWorkbookUseCase {

    private static final int WITHDRAW_DEADLINE_DAYS = 7;

    private final LoadWeeklyCurriculumPort loadWeeklyCurriculumPort;
    private final LoadOriginalWorkbookPort loadOriginalWorkbookPort;
    private final LoadChallengerWorkbookPort loadChallengerWorkbookPort;
    private final LoadWeeklyBestWorkbookPort loadWeeklyBestWorkbookPort;
    private final SaveWeeklyBestWorkbookPort saveWeeklyBestWorkbookPort;
    private final GetChallengerUseCase getChallengerUseCase;

    @Audited(
        domain = Domain.CURRICULUM,
        action = AuditAction.APPROVE,
        targetType = "WeeklyBestWorkbook",
        description = "'주간 베스트 워크북을 선정했습니다.'"
    )
    @Override
    public void selectBest(CreateWeeklyBestWorkbookCommand command) {
        WeeklyCurriculum weeklyCurriculum = loadWeeklyCurriculumPort.getById(command.weeklyCurriculumId());

        validateCandidate(command.bestMemberId(), weeklyCurriculum);
        validateNotSelected(command.weeklyCurriculumId(), command.studyGroupId());
        validateNoExcusedWorkbook(command.bestMemberId(), weeklyCurriculum);

        WeeklyBestWorkbook weeklyBestWorkbook = WeeklyBestWorkbook.create(
            weeklyCurriculum,
            command.bestMemberId(),
            command.studyGroupId(),
            command.reason(),
            command.decidedMemberId()
        );
        saveWeeklyBestWorkbookPort.save(weeklyBestWorkbook);
    }

    @Override
    public void editReason(EditWeeklyBestWorkbookCommand command) {
        WeeklyBestWorkbook weeklyBestWorkbook = loadWeeklyBestWorkbookPort.getById(command.weeklyBestWorkbookId());
        weeklyBestWorkbook.editReason(command.newReason());
        saveWeeklyBestWorkbookPort.save(weeklyBestWorkbook);
    }

    @Override
    public void withdraw(Long weeklyBestWorkbookId) {
        WeeklyBestWorkbook weeklyBestWorkbook = loadWeeklyBestWorkbookPort.getById(weeklyBestWorkbookId);
        validateWithdrawDeadline(weeklyBestWorkbook);
        saveWeeklyBestWorkbookPort.delete(weeklyBestWorkbook);
    }

    private void validateCandidate(Long bestMemberId, WeeklyCurriculum weeklyCurriculum) {
        Curriculum curriculum = weeklyCurriculum.getCurriculum();
        boolean isActiveChallenger = getChallengerUseCase.getAllByMemberId(bestMemberId)
            .stream()
            .anyMatch(challenger -> matchesCurriculum(challenger, curriculum));

        if (!isActiveChallenger) {
            throw new CurriculumDomainException(
                CurriculumErrorCode.WORKBOOK_ACCESS_DENIED,
                "해당 주차 커리큘럼에 참여 중인 활성 챌린저만 베스트 워크북으로 선정할 수 있어요."
            );
        }
    }

    private boolean matchesCurriculum(ChallengerInfo challenger, Curriculum curriculum) {
        return ChallengerStatus.ACTIVE == challenger.challengerStatus()
            && curriculum.getGisuId().equals(challenger.gisuId())
            && curriculum.getPart() == challenger.part();
    }

    private void validateNotSelected(Long weeklyCurriculumId, Long studyGroupId) {
        if (loadWeeklyBestWorkbookPort.existsByWeeklyCurriculumIdAndStudyGroupId(weeklyCurriculumId, studyGroupId)) {
            throw new CurriculumDomainException(
                CurriculumErrorCode.WORKBOOK_SUBMISSION_ALREADY_EXISTS,
                "이미 해당 주차와 스터디 그룹에 선정된 베스트 워크북이 있어요."
            );
        }
    }

    private void validateNoExcusedWorkbook(Long bestMemberId, WeeklyCurriculum weeklyCurriculum) {
        List<Long> releasedOriginalWorkbookIds = loadOriginalWorkbookPort
            .findReleasedByWeeklyCurriculumId(weeklyCurriculum.getId())
            .stream()
            .map(OriginalWorkbook::getId)
            .toList();

        if (releasedOriginalWorkbookIds.isEmpty()) {
            throw new CurriculumDomainException(
                CurriculumErrorCode.INVALID_WORKBOOK_STATUS,
                "배포된 원본 워크북이 없는 주차에서는 베스트 워크북을 선정할 수 없어요."
            );
        }

        boolean hasExcusedWorkbook = loadChallengerWorkbookPort.listByMemberIdAndOriginalWorkbookIdIn(
                bestMemberId,
                releasedOriginalWorkbookIds
            )
            .stream()
            .anyMatch(ChallengerWorkbook::isExcused);

        if (hasExcusedWorkbook) {
            throw new CurriculumDomainException(
                CurriculumErrorCode.WORKBOOK_ACCESS_DENIED,
                "인정 처리된 워크북이 있는 챌린저는 베스트 워크북으로 선정할 수 없어요."
            );
        }
    }

    private void validateWithdrawDeadline(WeeklyBestWorkbook weeklyBestWorkbook) {
        Instant withdrawDeadline = weeklyBestWorkbook.getWeeklyCurriculum()
            .getEndsAt()
            .plus(WITHDRAW_DEADLINE_DAYS, ChronoUnit.DAYS);
        if (Instant.now().isAfter(withdrawDeadline)) {
            throw new CurriculumDomainException(
                CurriculumErrorCode.INVALID_WORKBOOK_STATUS,
                "베스트 워크북 선정 철회는 해당 주차가 종료된 후 1주일 이내에만 가능합니다."
            );
        }
    }
}
