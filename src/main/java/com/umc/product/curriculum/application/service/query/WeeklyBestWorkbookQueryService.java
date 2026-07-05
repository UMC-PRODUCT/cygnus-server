package com.umc.product.curriculum.application.service.query;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.umc.product.curriculum.application.port.in.query.GetWeeklyBestWorkbookUseCase;
import com.umc.product.curriculum.application.port.in.query.dto.ChallengerWorkbookInfo;
import com.umc.product.curriculum.application.port.in.query.dto.GetBestWorkbooksQuery;
import com.umc.product.curriculum.application.port.in.query.dto.WeeklyBestWorkbookInfo;
import com.umc.product.curriculum.application.port.in.query.dto.WeeklyBestWorkbookPageInfo;
import com.umc.product.curriculum.application.port.out.LoadChallengerWorkbookPort;
import com.umc.product.curriculum.application.port.out.LoadMissionFeedbackPort;
import com.umc.product.curriculum.application.port.out.LoadMissionSubmissionPort;
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
    private final GetMemberUseCase getMemberUseCase;

    @Override
    public WeeklyBestWorkbookPageInfo searchBestWorkbooks(GetBestWorkbooksQuery query) {
        Set<Long> filteredMemberIds = resolveMemberIds(query.schoolIds());
        if (query.schoolIds() != null && !query.schoolIds().isEmpty() && filteredMemberIds.isEmpty()) {
            return WeeklyBestWorkbookPageInfo.empty();
        }

        GetBestWorkbooksQuery searchQuery = query.withMemberIds(filteredMemberIds).withSize(query.size() + 1);
        List<WeeklyBestWorkbook> fetched =
            searchWeeklyBestWorkbookPort.searchBestWorkbooks(searchQuery);
        boolean hasNext = fetched.size() > query.size();
        List<WeeklyBestWorkbook> pageItems = hasNext ? fetched.subList(0, query.size()) : fetched;
        if (pageItems.isEmpty()) {
            return WeeklyBestWorkbookPageInfo.empty();
        }

        List<Long> memberIds = pageItems.stream()
            .map(WeeklyBestWorkbook::getMemberId)
            .distinct()
            .toList();
        List<Long> weeklyCurriculumIds = pageItems.stream()
            .map(bestWorkbook -> bestWorkbook.getWeeklyCurriculum().getId())
            .distinct()
            .toList();

        List<ChallengerWorkbook> challengerWorkbooks =
            loadChallengerWorkbookPort.findByMemberIdInAndWeeklyCurriculumIdIn(memberIds, weeklyCurriculumIds);
        Map<BestWorkbookKey, List<ChallengerWorkbook>> challengerWorkbooksByKey = challengerWorkbooks.stream()
            .collect(Collectors.groupingBy(workbook -> BestWorkbookKey.from(
                workbook.getMemberId(),
                workbook.getOriginalWorkbook().getWeeklyCurriculum().getId()
            )));

        Map<Long, List<MissionSubmission>> submissionsByChallengerWorkbookId =
            findSubmissionsByChallengerWorkbookId(challengerWorkbooks);
        Map<Long, List<MissionFeedback>> feedbacksBySubmissionId =
            findFeedbacksBySubmissionId(submissionsByChallengerWorkbookId);

        List<WeeklyBestWorkbookInfo> content = pageItems.stream()
            .map(bestWorkbook -> toInfo(
                bestWorkbook,
                challengerWorkbooksByKey,
                submissionsByChallengerWorkbookId,
                feedbacksBySubmissionId
            ))
            .toList();

        Long nextCursor = hasNext ? content.get(content.size() - 1).weeklyBestWorkbookEntityId() : null;
        return new WeeklyBestWorkbookPageInfo(content, nextCursor, hasNext);
    }

    private Set<Long> resolveMemberIds(Set<Long> schoolIds) {
        if (schoolIds == null || schoolIds.isEmpty()) {
            return null;
        }
        return getMemberUseCase.listIdsBySchoolIds(schoolIds).values().stream()
            .flatMap(Set::stream)
            .collect(Collectors.toSet());
    }

    private Map<Long, List<MissionSubmission>> findSubmissionsByChallengerWorkbookId(
        List<ChallengerWorkbook> challengerWorkbooks
    ) {
        List<Long> challengerWorkbookIds = challengerWorkbooks.stream()
            .map(ChallengerWorkbook::getId)
            .toList();
        if (challengerWorkbookIds.isEmpty()) {
            return Map.of();
        }
        return loadMissionSubmissionPort.findByChallengerWorkbookIdIn(challengerWorkbookIds).stream()
            .collect(Collectors.groupingBy(submission -> submission.getChallengerWorkbook().getId()));
    }

    private Map<Long, List<MissionFeedback>> findFeedbacksBySubmissionId(
        Map<Long, List<MissionSubmission>> submissionsByChallengerWorkbookId
    ) {
        List<Long> submissionIds = submissionsByChallengerWorkbookId.values().stream()
            .flatMap(List::stream)
            .map(MissionSubmission::getId)
            .toList();
        if (submissionIds.isEmpty()) {
            return Map.of();
        }
        return loadMissionFeedbackPort.findByMissionSubmissionIdIn(submissionIds).stream()
            .collect(Collectors.groupingBy(feedback -> feedback.getMissionSubmission().getId()));
    }

    private WeeklyBestWorkbookInfo toInfo(
        WeeklyBestWorkbook bestWorkbook,
        Map<BestWorkbookKey, List<ChallengerWorkbook>> challengerWorkbooksByKey,
        Map<Long, List<MissionSubmission>> submissionsByChallengerWorkbookId,
        Map<Long, List<MissionFeedback>> feedbacksBySubmissionId
    ) {
        WeeklyCurriculum weeklyCurriculum = bestWorkbook.getWeeklyCurriculum();
        List<ChallengerWorkbookInfo> challengerWorkbookInfos = challengerWorkbooksByKey
            .getOrDefault(BestWorkbookKey.from(bestWorkbook.getMemberId(), weeklyCurriculum.getId()), List.of())
            .stream()
            .map(workbook -> ChallengerWorkbookInfoAssembler.toInfo(
                workbook,
                submissionsByChallengerWorkbookId.getOrDefault(workbook.getId(), List.of()),
                feedbacksBySubmissionId,
                true
            ))
            .toList();

        return WeeklyBestWorkbookInfo.builder()
            .weeklyBestWorkbookEntityId(bestWorkbook.getId())
            .challengerId(bestWorkbook.getMemberId())
            .gisuId(weeklyCurriculum.getCurriculum().getGisuId())
            .part(weeklyCurriculum.getCurriculum().getPart())
            .studyGroupId(bestWorkbook.getStudyGroupId())
            .decidedMemberId(bestWorkbook.getDecidedMemberId())
            .reason(bestWorkbook.getReason())
            .challengerWorkbooks(challengerWorkbookInfos)
            .build();
    }

    private record BestWorkbookKey(Long memberId, Long weeklyCurriculumId) {

        private static BestWorkbookKey from(Long memberId, Long weeklyCurriculumId) {
            return new BestWorkbookKey(
                Objects.requireNonNull(memberId),
                Objects.requireNonNull(weeklyCurriculumId)
            );
        }
    }
}
