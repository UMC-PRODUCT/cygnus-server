package com.umc.product.curriculum.application.service.command;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.umc.product.audit.application.port.in.annotation.Audited;
import com.umc.product.audit.domain.AuditAction;
import com.umc.product.authorization.application.port.in.CheckPermissionUseCase;
import com.umc.product.authorization.domain.SubjectAttributes;
import com.umc.product.challenger.application.port.in.query.GetChallengerUseCase;
import com.umc.product.challenger.application.port.in.query.dto.ChallengerInfo;
import com.umc.product.common.domain.enums.ChallengerPart;
import com.umc.product.common.domain.enums.ChallengerStatus;
import com.umc.product.common.domain.enums.ChallengerTrack;
import com.umc.product.curriculum.application.port.in.command.ManageWeeklyBestWorkbookUseCase;
import com.umc.product.curriculum.application.port.in.command.dto.workbook.CreateWeeklyBestWorkbookCommand;
import com.umc.product.curriculum.application.port.in.command.dto.workbook.EditWeeklyBestWorkbookCommand;
import com.umc.product.curriculum.application.port.out.LoadChallengerWorkbookPort;
import com.umc.product.curriculum.application.port.out.LoadMissionFeedbackPort;
import com.umc.product.curriculum.application.port.out.LoadMissionSubmissionPort;
import com.umc.product.curriculum.application.port.out.LoadOriginalWorkbookMissionPort;
import com.umc.product.curriculum.application.port.out.LoadOriginalWorkbookPort;
import com.umc.product.curriculum.application.port.out.LoadWeeklyBestWorkbookPort;
import com.umc.product.curriculum.application.port.out.LoadWeeklyCurriculumPort;
import com.umc.product.curriculum.application.port.out.SaveWeeklyBestWorkbookPort;
import com.umc.product.curriculum.application.service.evaluator.CurriculumStudyGroupStaffPolicy;
import com.umc.product.curriculum.domain.ChallengerWorkbook;
import com.umc.product.curriculum.domain.MissionFeedback;
import com.umc.product.curriculum.domain.MissionSubmission;
import com.umc.product.curriculum.domain.OriginalWorkbook;
import com.umc.product.curriculum.domain.OriginalWorkbookMission;
import com.umc.product.curriculum.domain.WeeklyBestWorkbook;
import com.umc.product.curriculum.domain.WeeklyCurriculum;
import com.umc.product.curriculum.domain.enums.FeedbackResult;
import com.umc.product.curriculum.domain.exception.CurriculumDomainException;
import com.umc.product.curriculum.domain.exception.CurriculumErrorCode;
import com.umc.product.global.exception.constant.Domain;
import com.umc.product.organization.application.port.in.query.GetStudyGroupUseCase;
import com.umc.product.organization.application.port.in.query.dto.studygroup.StudyGroupInfo;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class WeeklyBestWorkbookCommandService implements ManageWeeklyBestWorkbookUseCase {

    private final LoadWeeklyCurriculumPort loadWeeklyCurriculumPort;
    private final LoadOriginalWorkbookPort loadOriginalWorkbookPort;
    private final LoadOriginalWorkbookMissionPort loadOriginalWorkbookMissionPort;
    private final LoadChallengerWorkbookPort loadChallengerWorkbookPort;
    private final LoadMissionSubmissionPort loadMissionSubmissionPort;
    private final LoadMissionFeedbackPort loadMissionFeedbackPort;
    private final LoadWeeklyBestWorkbookPort loadWeeklyBestWorkbookPort;
    private final SaveWeeklyBestWorkbookPort saveWeeklyBestWorkbookPort;
    private final GetChallengerUseCase getChallengerUseCase;
    private final GetStudyGroupUseCase getStudyGroupUseCase;
    private final CheckPermissionUseCase checkPermissionUseCase;
    private final CurriculumStudyGroupStaffPolicy staffPolicy;
    private final MissionMutationPolicy missionMutationPolicy;

    @Audited(
        domain = Domain.CURRICULUM,
        action = AuditAction.APPROVE,
        targetType = "WeeklyBestWorkbook",
        description = "'주간 베스트 워크북을 선정했습니다.'"
    )
    @Override
    public void selectBest(CreateWeeklyBestWorkbookCommand command) {
        WeeklyCurriculum weekly = loadWeeklyCurriculumPort.getById(command.weeklyCurriculumId());
        StudyGroupInfo group = getStudyGroupUseCase.getById(command.studyGroupId());

        validateGroupAndCandidate(command.bestMemberId(), weekly, group);
        validateDecisionAuthority(command, weekly, group);
        validateNotSelected(weekly.getId(), group.groupId());
        validateRequiredMissionsPassed(command.bestMemberId(), weekly, group.groupId());

        saveWeeklyBestWorkbookPort.save(WeeklyBestWorkbook.create(
            weekly, command.bestMemberId(), group.groupId(), command.reason(), command.decidedMemberId()
        ));
    }

    @Override
    public void editReason(EditWeeklyBestWorkbookCommand command) {
        WeeklyBestWorkbook best = loadWeeklyBestWorkbookPort.getById(command.weeklyBestWorkbookId());
        best.editReason(command.newReason());
        saveWeeklyBestWorkbookPort.save(best);
    }

    @Override
    public void withdraw(Long weeklyBestWorkbookId) {
        WeeklyBestWorkbook best = loadWeeklyBestWorkbookPort.getById(weeklyBestWorkbookId);
        missionMutationPolicy.validateWeeklyBestWithdraw(best);
        saveWeeklyBestWorkbookPort.delete(best);
    }

    private void validateGroupAndCandidate(Long memberId, WeeklyCurriculum weekly, StudyGroupInfo group) {
        var curriculum = weekly.getCurriculum();
        boolean groupMatches = curriculum.getGisuId().equals(group.gisuId())
            && curriculum.getPart() == group.part()
            && curriculum.getTrack() == group.track()
            && group.memberIds().contains(memberId);
        if (!groupMatches) {
            throw new CurriculumDomainException(CurriculumErrorCode.STUDY_GROUP_NOT_MATCHED);
        }

        boolean active = getChallengerUseCase.getAllByMemberId(memberId).stream()
            .anyMatch(challenger -> matches(
                challenger, curriculum.getGisuId(), curriculum.getPart(), curriculum.getTrack()));
        if (!active) {
            throw new CurriculumDomainException(CurriculumErrorCode.BEST_WORKBOOK_REQUIREMENTS_NOT_MET);
        }
    }

    private boolean matches(ChallengerInfo challenger, Long gisuId, ChallengerPart part, ChallengerTrack track) {
        return ChallengerStatus.ACTIVE == challenger.challengerStatus()
            && gisuId.equals(challenger.gisuId())
            && (track == null ? part == challenger.part()
            : challenger.tracks() != null && challenger.tracks().contains(track));
    }

    private void validateDecisionAuthority(
        CreateWeeklyBestWorkbookCommand command,
        WeeklyCurriculum weekly,
        StudyGroupInfo group
    ) {
        SubjectAttributes subject = checkPermissionUseCase.loadSubject(command.decidedMemberId());
        Long gisuId = weekly.getCurriculum().getGisuId();
        if (!staffPolicy.canManage(subject, group.groupId(), command.bestMemberId(), gisuId)) {
            throw new CurriculumDomainException(CurriculumErrorCode.WORKBOOK_ACCESS_DENIED);
        }
    }

    private void validateNotSelected(Long weeklyCurriculumId, Long studyGroupId) {
        if (loadWeeklyBestWorkbookPort.existsByWeeklyCurriculumIdAndStudyGroupId(
            weeklyCurriculumId, studyGroupId
        )) {
            throw new CurriculumDomainException(CurriculumErrorCode.WEEKLY_BEST_ALREADY_EXISTS);
        }
    }

    private void validateRequiredMissionsPassed(Long memberId, WeeklyCurriculum weekly, Long groupId) {
        List<OriginalWorkbook> originals = loadOriginalWorkbookPort.findReleasedByWeeklyCurriculumId(weekly.getId());
        if (originals.isEmpty()) {
            throw new CurriculumDomainException(CurriculumErrorCode.BEST_WORKBOOK_REQUIREMENTS_NOT_MET);
        }

        List<Long> originalIds = originals.stream().map(OriginalWorkbook::getId).toList();
        List<ChallengerWorkbook> workbooks = loadChallengerWorkbookPort
            .listByMemberIdAndOriginalWorkbookIdIn(memberId, originalIds);
        boolean invalidWorkbooks = workbooks.size() != originals.size()
            || workbooks.stream().anyMatch(workbook -> workbook.isExcused()
                || !groupId.equals(workbook.getStudyGroupId()));
        if (invalidWorkbooks) {
            throw new CurriculumDomainException(CurriculumErrorCode.BEST_WORKBOOK_REQUIREMENTS_NOT_MET);
        }

        Set<Long> requiredMissionIds = loadOriginalWorkbookMissionPort.findByOriginalWorkbookIdIn(originalIds)
            .stream()
            .filter(OriginalWorkbookMission::isNecessary)
            .map(OriginalWorkbookMission::getId)
            .collect(Collectors.toSet());
        if (requiredMissionIds.isEmpty()) {
            return;
        }

        List<Long> workbookIds = workbooks.stream().map(ChallengerWorkbook::getId).toList();
        List<MissionSubmission> submissions = loadMissionSubmissionPort
            .listActiveByChallengerWorkbookIdIn(workbookIds);
        Map<Long, MissionSubmission> submissionByMissionId = submissions.stream()
            .filter(submission -> requiredMissionIds.contains(submission.getOriginalWorkbookMission().getId()))
            .collect(Collectors.toMap(
                submission -> submission.getOriginalWorkbookMission().getId(),
                submission -> submission
            ));
        if (!submissionByMissionId.keySet().containsAll(requiredMissionIds)) {
            throw new CurriculumDomainException(CurriculumErrorCode.BEST_WORKBOOK_REQUIREMENTS_NOT_MET);
        }

        List<MissionFeedback> feedbacks = loadMissionFeedbackPort.listByMissionSubmissionIdIn(
                submissionByMissionId.values().stream().map(MissionSubmission::getId).toList()
            );
        Set<Long> passedSubmissionIds = feedbacks.stream()
            .filter(feedback -> feedback.getFeedbackResult() == FeedbackResult.PASS)
            .map(MissionFeedback::getMissionSubmission)
            .map(MissionSubmission::getId)
            .collect(Collectors.toSet());
        Set<Long> failedSubmissionIds = feedbacks.stream()
            .filter(feedback -> feedback.getFeedbackResult() == FeedbackResult.FAIL)
            .map(MissionFeedback::getMissionSubmission)
            .map(MissionSubmission::getId)
            .collect(Collectors.toSet());
        boolean allPassed = submissionByMissionId.values().stream()
            .map(MissionSubmission::getId)
            .allMatch(submissionId ->
                passedSubmissionIds.contains(submissionId) && !failedSubmissionIds.contains(submissionId)
            );
        if (!allPassed) {
            throw new CurriculumDomainException(CurriculumErrorCode.BEST_WORKBOOK_REQUIREMENTS_NOT_MET);
        }
    }
}
