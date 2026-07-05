package com.umc.product.curriculum.application.service.query;

import com.umc.product.challenger.application.port.in.query.GetChallengerUseCase;
import com.umc.product.challenger.application.port.in.query.dto.ChallengerInfo;
import com.umc.product.common.domain.enums.ChallengerPart;
import com.umc.product.curriculum.application.port.in.query.GetCurriculumUseCase;
import com.umc.product.curriculum.application.port.in.query.dto.CurriculumOverviewInfo;
import com.umc.product.curriculum.application.port.in.query.dto.CurriculumOverviewInfo.WeeklyCurriculumOverviewInfo;
import com.umc.product.curriculum.application.port.in.query.dto.CurriculumProjection;
import com.umc.product.curriculum.application.port.in.query.dto.MyCurriculumInfo;
import com.umc.product.curriculum.application.port.in.query.dto.MyCurriculumInfo.*;
import com.umc.product.curriculum.application.port.out.*;
import com.umc.product.curriculum.domain.*;
import com.umc.product.curriculum.domain.enums.FeedbackResult;
import com.umc.product.curriculum.domain.enums.SubmissionStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CurriculumQueryService implements GetCurriculumUseCase {

    private final GetChallengerUseCase getChallengerUseCase;
    private final LoadCurriculumPort loadCurriculumPort;
    private final LoadWeeklyCurriculumPort loadWeeklyCurriculumPort;
    private final LoadOriginalWorkbookPort loadOriginalWorkbookPort;
    private final LoadOriginalWorkbookMissionPort loadOriginalWorkbookMissionPort;
    private final LoadChallengerWorkbookPort loadChallengerWorkbookPort;
    private final LoadMissionSubmissionPort loadMissionSubmissionPort;
    private final LoadMissionFeedbackPort loadMissionFeedbackPort;

    @Override
    public CurriculumOverviewInfo getCurriculumOverview(Long gisuId, ChallengerPart part, Long weekNo) {
        CurriculumProjection projection = loadCurriculumPort.getByGisuIdAndPart(gisuId, part);
        List<WeeklyCurriculum> weeklyCurriculums = loadWeeklyCurriculumPort.findByCurriculumId(projection.id(), weekNo);

        List<WeeklyCurriculumOverviewInfo> weeks = weeklyCurriculums.stream()
            .map(WeeklyCurriculumOverviewInfo::of)
            .toList();

        return CurriculumOverviewInfo.of(projection, weeks);
    }

    @Override
    public MyCurriculumInfo getMyProgress(Long memberId, Long gisuId) {
        ChallengerInfo challengerInfo = getChallengerUseCase.getByMemberIdAndGisuId(memberId, gisuId);
        CurriculumProjection projection = loadCurriculumPort.getByGisuIdAndPart(gisuId, challengerInfo.part());

        List<WeeklyCurriculum> weeklyCurriculums = loadWeeklyCurriculumPort.findByCurriculumId(projection.id(), null);
        if (weeklyCurriculums.isEmpty()) {
            return MyCurriculumInfo.of(projection, List.of());
        }

        // 배치 조회 — 주차 수에 무관하게 6번의 고정 쿼리
        List<Long> weeklyCurriculumIds = weeklyCurriculums.stream().map(WeeklyCurriculum::getId).toList();
        List<OriginalWorkbook> allWorkbooks =
            loadOriginalWorkbookPort.findReleasedByWeeklyCurriculumIdIn(weeklyCurriculumIds);

        List<Long> originalWorkbookIds = allWorkbooks.stream().map(OriginalWorkbook::getId).toList();
        List<OriginalWorkbookMission> allMissions =
            loadOriginalWorkbookMissionPort.findByOriginalWorkbookIdIn(originalWorkbookIds);
        List<ChallengerWorkbook> allChallengerWorkbooks =
            loadChallengerWorkbookPort.findByMemberIdAndOriginalWorkbookIdIn(memberId, originalWorkbookIds);

        List<Long> challengerWorkbookIds = allChallengerWorkbooks.stream().map(ChallengerWorkbook::getId).toList();
        List<MissionSubmission> allSubmissions =
            loadMissionSubmissionPort.findByChallengerWorkbookIdIn(challengerWorkbookIds);

        List<Long> submissionIds = allSubmissions.stream().map(MissionSubmission::getId).toList();
        List<MissionFeedback> allFeedbacks =
            loadMissionFeedbackPort.listByMissionSubmissionIdIn(submissionIds);

        // 인메모리 조립용 Map
        Map<Long, List<OriginalWorkbook>> workbooksByWcId = allWorkbooks.stream()
            .collect(Collectors.groupingBy(wb -> wb.getWeeklyCurriculum().getId()));
        Map<Long, List<OriginalWorkbookMission>> missionsByWbId = allMissions.stream()
            .collect(Collectors.groupingBy(m -> m.getOriginalWorkbook().getId()));
        Map<Long, ChallengerWorkbook> cwByWbId = allChallengerWorkbooks.stream()
            .collect(Collectors.toMap(cw -> cw.getOriginalWorkbook().getId(), cw -> cw));
        Map<Long, MissionSubmissionInfo> submissionInfoByMissionId =
            buildSubmissionInfoMap(allSubmissions, allFeedbacks);

        List<MyWeeklyCurriculumInfo> weeks = weeklyCurriculums.stream()
            .map(wc -> {
                List<OriginalWorkbook> workbooks = workbooksByWcId.getOrDefault(wc.getId(), List.of());
                List<MyOriginalWorkbookInfo> workbookInfos = workbooks.stream()
                    .map(wb -> buildMyOriginalWorkbookInfo(wb, missionsByWbId, cwByWbId, submissionInfoByMissionId))
                    .toList();
                return MyWeeklyCurriculumInfo.of(wc, workbookInfos);
            })
            .toList();

        return MyCurriculumInfo.of(projection, weeks);
    }

    private MyOriginalWorkbookInfo buildMyOriginalWorkbookInfo(
        OriginalWorkbook wb,
        Map<Long, List<OriginalWorkbookMission>> missionsByWbId,
        Map<Long, ChallengerWorkbook> cwByWbId,
        Map<Long, MissionSubmissionInfo> submissionInfoByMissionId
    ) {
        List<OriginalWorkbookMission> missions = missionsByWbId.getOrDefault(wb.getId(), List.of());
        ChallengerWorkbook cw = cwByWbId.get(wb.getId());

        List<MyOriginalWorkbookMissionInfo> missionInfos = missions.stream()
            .map(m -> MyOriginalWorkbookMissionInfo.of(m, submissionInfoByMissionId.get(m.getId())))
            .toList();

        return MyOriginalWorkbookInfo.of(
            wb,
            missionInfos,
            cw != null ? java.util.Optional.of(cw.getId()) : java.util.Optional.empty()
        );
    }

    private Map<Long, MissionSubmissionInfo> buildSubmissionInfoMap(
        List<MissionSubmission> submissions,
        List<MissionFeedback> feedbacks
    ) {
        if (submissions.isEmpty()) {
            return Map.of();
        }

        Map<Long, List<MissionFeedback>> feedbacksBySubmissionId = feedbacks.stream()
            .collect(Collectors.groupingBy(f -> f.getMissionSubmission().getId()));

        return submissions.stream().collect(Collectors.toMap(
            s -> s.getOriginalWorkbookMission().getId(),
            s -> toSubmissionInfo(s, feedbacksBySubmissionId.getOrDefault(s.getId(), List.of()))
        ));
    }

    private MissionSubmissionInfo toSubmissionInfo(MissionSubmission s, List<MissionFeedback> feedbacks) {
        List<MissionFeedbackInfo> feedbackInfos = feedbacks.stream()
            .map(MissionFeedbackInfo::of)
            .toList();

        return MissionSubmissionInfo.of(s, resolveSubmissionStatus(feedbacks), feedbackInfos);
    }

    private SubmissionStatus resolveSubmissionStatus(List<MissionFeedback> feedbacks) {
        if (feedbacks.isEmpty()) {
            return SubmissionStatus.PENDING;
        }
        return feedbacks.stream()
            .map(MissionFeedback::getFeedbackResult)
            .anyMatch(r -> r == FeedbackResult.PASS)
            ? SubmissionStatus.PASS
            : SubmissionStatus.FAIL;
    }
}
