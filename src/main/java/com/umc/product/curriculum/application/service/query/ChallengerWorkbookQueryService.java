package com.umc.product.curriculum.application.service.query;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.umc.product.challenger.application.port.in.query.GetChallengerUseCase;
import com.umc.product.curriculum.application.port.in.query.GetChallengerWorkbookUseCase;
import com.umc.product.curriculum.application.port.in.query.dto.ChallengerWorkbookInfo;
import com.umc.product.curriculum.application.port.out.LoadChallengerWorkbookPort;
import com.umc.product.curriculum.application.port.out.LoadMissionFeedbackPort;
import com.umc.product.curriculum.application.port.out.LoadMissionSubmissionPort;
import com.umc.product.curriculum.application.port.out.LoadOriginalWorkbookMissionPort;
import com.umc.product.curriculum.application.port.out.LoadWeeklyBestWorkbookPort;
import com.umc.product.curriculum.domain.ChallengerWorkbook;
import com.umc.product.curriculum.domain.MissionFeedback;
import com.umc.product.curriculum.domain.MissionSubmission;
import com.umc.product.curriculum.domain.exception.CurriculumDomainException;
import com.umc.product.curriculum.domain.exception.CurriculumErrorCode;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ChallengerWorkbookQueryService implements GetChallengerWorkbookUseCase {

    private final LoadChallengerWorkbookPort loadChallengerWorkbookPort;
    private final LoadMissionSubmissionPort loadMissionSubmissionPort;
    private final LoadMissionFeedbackPort loadMissionFeedbackPort;
    private final GetChallengerUseCase getChallengerUseCase;
    private final LoadOriginalWorkbookMissionPort loadOriginalWorkbookMissionPort;
    private final LoadWeeklyBestWorkbookPort loadWeeklyBestWorkbookPort;

    @Override
    public ChallengerWorkbookInfo getById(Long challengerWorkbookId, Long requesterMemberId) {
        ChallengerWorkbook workbook = loadChallengerWorkbookPort.getById(challengerWorkbookId);
        Long gisuId = workbook.getOriginalWorkbook().getWeeklyCurriculum().getCurriculum().getGisuId();
        if (getChallengerUseCase.findByMemberIdAndGisuId(requesterMemberId, gisuId).isEmpty()) {
            throw new CurriculumDomainException(CurriculumErrorCode.WORKBOOK_ACCESS_DENIED);
        }

        List<MissionSubmission> submissions =
            loadMissionSubmissionPort.listActiveByChallengerWorkbookId(challengerWorkbookId);
        List<Long> submissionIds = submissions.stream().map(MissionSubmission::getId).toList();
        Map<Long, List<MissionFeedback>> feedbacksBySubmissionId = submissionIds.isEmpty()
            ? Map.of()
            : loadMissionFeedbackPort.listByMissionSubmissionIdIn(submissionIds).stream()
                .collect(Collectors.groupingBy(feedback -> feedback.getMissionSubmission().getId()));

        Set<Long> requiredMissionIds = loadOriginalWorkbookMissionPort.findByOriginalWorkbookId(
            workbook.getOriginalWorkbook().getId()
        ).stream()
            .filter(mission -> mission.isNecessary())
            .map(mission -> mission.getId())
            .collect(Collectors.toSet());
        boolean isBestWorkbook = workbook.getStudyGroupId() != null
            && loadWeeklyBestWorkbookPort.existsByMemberIdAndWeeklyCurriculumIdAndStudyGroupId(
                workbook.getMemberId(),
                workbook.getOriginalWorkbook().getWeeklyCurriculum().getId(),
                workbook.getStudyGroupId()
            );

        return ChallengerWorkbookInfoAssembler.toInfo(
            workbook,
            submissions,
            feedbacksBySubmissionId,
            requiredMissionIds,
            isBestWorkbook
        );
    }
}
