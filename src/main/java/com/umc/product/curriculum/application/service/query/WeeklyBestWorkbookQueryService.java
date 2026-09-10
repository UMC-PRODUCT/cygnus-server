package com.umc.product.curriculum.application.service.query;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.umc.product.curriculum.application.port.in.query.GetWeeklyBestWorkbookUseCase;
import com.umc.product.curriculum.application.port.in.query.dto.ChallengerWorkbookInfo;
import com.umc.product.curriculum.application.port.in.query.dto.GetBestWorkbooksQuery;
import com.umc.product.curriculum.application.port.in.query.dto.WeeklyBestWorkbookInfo;
import com.umc.product.curriculum.application.port.in.query.dto.WeeklyBestWorkbookPageInfo;
import com.umc.product.curriculum.application.port.out.LoadChallengerWorkbookPort;
import com.umc.product.curriculum.application.port.out.LoadChallengerWorkbookPort.ChallengerWorkbookLookupKey;
import com.umc.product.curriculum.application.port.out.LoadMissionFeedbackPort;
import com.umc.product.curriculum.application.port.out.LoadMissionSubmissionPort;
import com.umc.product.curriculum.application.port.out.LoadOriginalWorkbookMissionPort;
import com.umc.product.curriculum.application.port.out.SearchWeeklyBestWorkbookPort;
import com.umc.product.curriculum.domain.ChallengerWorkbook;
import com.umc.product.curriculum.domain.MissionFeedback;
import com.umc.product.curriculum.domain.MissionSubmission;
import com.umc.product.curriculum.domain.WeeklyBestWorkbook;
import com.umc.product.curriculum.domain.WeeklyCurriculum;
import com.umc.product.member.application.port.in.query.GetMemberUseCase;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class WeeklyBestWorkbookQueryService implements GetWeeklyBestWorkbookUseCase {

    private final SearchWeeklyBestWorkbookPort searchWeeklyBestWorkbookPort;
    private final LoadChallengerWorkbookPort loadChallengerWorkbookPort;
    private final LoadMissionSubmissionPort loadMissionSubmissionPort;
    private final LoadMissionFeedbackPort loadMissionFeedbackPort;
    private final LoadOriginalWorkbookMissionPort loadOriginalWorkbookMissionPort;
    private final GetMemberUseCase getMemberUseCase;

    @Override
    public WeeklyBestWorkbookPageInfo searchBestWorkbooks(GetBestWorkbooksQuery query) {
        Set<Long> filteredMemberIds = resolveMemberIds(query.schoolIds());
        if (query.schoolIds() != null && !query.schoolIds().isEmpty() && filteredMemberIds.isEmpty()) {
            return WeeklyBestWorkbookPageInfo.empty(query.page(), query.size());
        }

        Page<WeeklyBestWorkbook> fetched =
            searchWeeklyBestWorkbookPort.searchBestWorkbooks(query.withMemberIds(filteredMemberIds));
        List<WeeklyBestWorkbook> pageItems = fetched.getContent();
        if (pageItems.isEmpty()) {
            return new WeeklyBestWorkbookPageInfo(
                List.of(),
                fetched.getNumber(),
                fetched.getSize(),
                fetched.getTotalElements(),
                fetched.getTotalPages(),
                fetched.hasNext(),
                fetched.hasPrevious()
            );
        }

        List<ChallengerWorkbookLookupKey> lookupKeys = pageItems.stream()
            .map(best -> new ChallengerWorkbookLookupKey(
                best.getMemberId(),
                best.getWeeklyCurriculum().getId(),
                best.getStudyGroupId()
            ))
            .distinct()
            .toList();
        List<ChallengerWorkbook> challengerWorkbooks = loadChallengerWorkbookPort.listByLookupKeys(lookupKeys);
        Map<BestWorkbookKey, List<ChallengerWorkbook>> workbooksByKey = challengerWorkbooks.stream()
            .collect(Collectors.groupingBy(BestWorkbookKey::from));

        Map<Long, List<MissionSubmission>> submissionsByWorkbookId = findSubmissions(challengerWorkbooks);
        Map<Long, List<MissionFeedback>> feedbacksBySubmissionId = findFeedbacks(submissionsByWorkbookId);
        Map<Long, Set<Long>> requiredMissionIdsByOriginalWorkbookId =
            findRequiredMissionIds(challengerWorkbooks);

        List<WeeklyBestWorkbookInfo> content = pageItems.stream()
            .map(best -> toInfo(
                best,
                workbooksByKey,
                submissionsByWorkbookId,
                feedbacksBySubmissionId,
                requiredMissionIdsByOriginalWorkbookId
            ))
            .toList();
        return new WeeklyBestWorkbookPageInfo(
            content,
            fetched.getNumber(),
            fetched.getSize(),
            fetched.getTotalElements(),
            fetched.getTotalPages(),
            fetched.hasNext(),
            fetched.hasPrevious()
        );
    }

    private Set<Long> resolveMemberIds(Set<Long> schoolIds) {
        if (schoolIds == null || schoolIds.isEmpty()) {
            return null;
        }
        return getMemberUseCase.listIdsBySchoolIds(schoolIds).values().stream()
            .flatMap(Set::stream)
            .collect(Collectors.toSet());
    }

    private Map<Long, List<MissionSubmission>> findSubmissions(List<ChallengerWorkbook> workbooks) {
        List<Long> workbookIds = workbooks.stream().map(ChallengerWorkbook::getId).toList();
        if (workbookIds.isEmpty()) {
            return Map.of();
        }
        return loadMissionSubmissionPort.listActiveByChallengerWorkbookIdIn(workbookIds).stream()
            .collect(Collectors.groupingBy(submission -> submission.getChallengerWorkbook().getId()));
    }

    private Map<Long, List<MissionFeedback>> findFeedbacks(
        Map<Long, List<MissionSubmission>> submissionsByWorkbookId
    ) {
        List<Long> submissionIds = submissionsByWorkbookId.values().stream()
            .flatMap(List::stream)
            .map(MissionSubmission::getId)
            .toList();
        if (submissionIds.isEmpty()) {
            return Map.of();
        }
        return loadMissionFeedbackPort.listByMissionSubmissionIdIn(submissionIds).stream()
            .collect(Collectors.groupingBy(feedback -> feedback.getMissionSubmission().getId()));
    }

    private Map<Long, Set<Long>> findRequiredMissionIds(List<ChallengerWorkbook> workbooks) {
        List<Long> originalWorkbookIds = workbooks.stream()
            .map(workbook -> workbook.getOriginalWorkbook().getId())
            .distinct()
            .toList();
        if (originalWorkbookIds.isEmpty()) {
            return Map.of();
        }
        return loadOriginalWorkbookMissionPort.findByOriginalWorkbookIdIn(originalWorkbookIds).stream()
            .filter(mission -> mission.isNecessary())
            .collect(Collectors.groupingBy(
                mission -> mission.getOriginalWorkbook().getId(),
                Collectors.mapping(mission -> mission.getId(), Collectors.toSet())
            ));
    }

    private WeeklyBestWorkbookInfo toInfo(
        WeeklyBestWorkbook bestWorkbook,
        Map<BestWorkbookKey, List<ChallengerWorkbook>> workbooksByKey,
        Map<Long, List<MissionSubmission>> submissionsByWorkbookId,
        Map<Long, List<MissionFeedback>> feedbacksBySubmissionId,
        Map<Long, Set<Long>> requiredMissionIdsByOriginalWorkbookId
    ) {
        WeeklyCurriculum weeklyCurriculum = bestWorkbook.getWeeklyCurriculum();
        BestWorkbookKey key = new BestWorkbookKey(
            bestWorkbook.getMemberId(), weeklyCurriculum.getId(), bestWorkbook.getStudyGroupId()
        );
        List<ChallengerWorkbookInfo> workbookInfos = workbooksByKey.getOrDefault(key, List.of()).stream()
            .map(workbook -> ChallengerWorkbookInfoAssembler.toInfo(
                workbook,
                submissionsByWorkbookId.getOrDefault(workbook.getId(), List.of()),
                feedbacksBySubmissionId,
                requiredMissionIdsByOriginalWorkbookId.getOrDefault(
                    workbook.getOriginalWorkbook().getId(),
                    Set.of()
                ),
                true
            ))
            .toList();

        return WeeklyBestWorkbookInfo.builder()
            .weeklyBestWorkbookEntityId(bestWorkbook.getId())
            .challengerId(bestWorkbook.getMemberId())
            .gisuId(weeklyCurriculum.getCurriculum().getGisuId())
            .part(weeklyCurriculum.getCurriculum().getPart())
            .track(weeklyCurriculum.getCurriculum().getTrack())
            .studyGroupId(bestWorkbook.getStudyGroupId())
            .decidedMemberId(bestWorkbook.getDecidedMemberId())
            .reason(bestWorkbook.getReason())
            .challengerWorkbooks(workbookInfos)
            .build();
    }

    private record BestWorkbookKey(Long memberId, Long weeklyCurriculumId, Long studyGroupId) {
        private static BestWorkbookKey from(ChallengerWorkbook workbook) {
            return new BestWorkbookKey(
                workbook.getMemberId(),
                workbook.getOriginalWorkbook().getWeeklyCurriculum().getId(),
                workbook.getStudyGroupId()
            );
        }
    }
}
