package com.umc.product.curriculum.application.service.query;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.umc.product.challenger.application.port.in.query.GetChallengerUseCase;
import com.umc.product.challenger.application.port.in.query.dto.ChallengerInfo;
import com.umc.product.common.domain.enums.ChallengerPart;
import com.umc.product.common.domain.enums.ChallengerTrack;
import com.umc.product.common.domain.enums.GisuLearningType;
import com.umc.product.curriculum.application.port.in.query.GetCurriculumUseCase;
import com.umc.product.curriculum.application.port.in.query.dto.CurriculumOverviewInfo;
import com.umc.product.curriculum.application.port.in.query.dto.CurriculumOverviewInfo.WeeklyCurriculumOverviewInfo;
import com.umc.product.curriculum.application.port.in.query.dto.CurriculumProjection;
import com.umc.product.curriculum.application.port.in.query.dto.MyCurriculumInfo;
import com.umc.product.curriculum.application.port.in.query.dto.MyCurriculumInfo.MissionFeedbackInfo;
import com.umc.product.curriculum.application.port.in.query.dto.MyCurriculumInfo.MissionSubmissionInfo;
import com.umc.product.curriculum.application.port.in.query.dto.MyCurriculumInfo.MyOriginalWorkbookInfo;
import com.umc.product.curriculum.application.port.in.query.dto.MyCurriculumInfo.MyOriginalWorkbookMissionInfo;
import com.umc.product.curriculum.application.port.in.query.dto.MyCurriculumInfo.MyWeeklyCurriculumInfo;
import com.umc.product.curriculum.application.port.out.LoadChallengerWorkbookPort;
import com.umc.product.curriculum.application.port.out.LoadCurriculumPort;
import com.umc.product.curriculum.application.port.out.LoadMissionFeedbackPort;
import com.umc.product.curriculum.application.port.out.LoadMissionSubmissionPort;
import com.umc.product.curriculum.application.port.out.LoadOriginalWorkbookMissionPort;
import com.umc.product.curriculum.application.port.out.LoadOriginalWorkbookPort;
import com.umc.product.curriculum.application.port.out.LoadWeeklyCurriculumPort;
import com.umc.product.curriculum.domain.ChallengerWorkbook;
import com.umc.product.curriculum.domain.ChallengerWorkbookStatusPolicy;
import com.umc.product.curriculum.domain.MissionFeedback;
import com.umc.product.curriculum.domain.MissionSubmission;
import com.umc.product.curriculum.domain.OriginalWorkbook;
import com.umc.product.curriculum.domain.OriginalWorkbookMission;
import com.umc.product.curriculum.domain.WeeklyCurriculum;
import com.umc.product.curriculum.domain.enums.SubmissionStatus;
import com.umc.product.curriculum.domain.exception.CurriculumDomainException;
import com.umc.product.curriculum.domain.exception.CurriculumErrorCode;
import com.umc.product.organization.application.port.in.query.GetGisuUseCase;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CurriculumQueryService implements GetCurriculumUseCase {

    private final GetChallengerUseCase getChallengerUseCase;
    private final GetGisuUseCase getGisuUseCase;
    private final LoadCurriculumPort loadCurriculumPort;
    private final LoadWeeklyCurriculumPort loadWeeklyCurriculumPort;
    private final LoadOriginalWorkbookPort loadOriginalWorkbookPort;
    private final LoadOriginalWorkbookMissionPort loadOriginalWorkbookMissionPort;
    private final LoadChallengerWorkbookPort loadChallengerWorkbookPort;
    private final LoadMissionSubmissionPort loadMissionSubmissionPort;
    private final LoadMissionFeedbackPort loadMissionFeedbackPort;

    @Override
    public CurriculumOverviewInfo getCurriculumOverview(Long gisuId, ChallengerPart part, Long weekNo) {
        return getCurriculumOverview(gisuId, part, null, weekNo);
    }

    @Override
    public CurriculumOverviewInfo getCurriculumOverview(
        Long gisuId, ChallengerPart part, ChallengerTrack track, Long weekNo
    ) {
        GisuLearningType learningType = getGisuUseCase.getById(gisuId).learningType();
        boolean valid = learningType == GisuLearningType.PART
            ? part != null && track == null
            : part == null && track != null;
        if (!valid) {
            throw new CurriculumDomainException(CurriculumErrorCode.INVALID_CURRICULUM_LEARNING_TYPE);
        }
        validateBasicTrack(track);
        CurriculumProjection projection = track == null
            ? loadCurriculumPort.getByGisuIdAndPart(gisuId, part)
            : loadCurriculumPort.getByGisuIdAndTrack(gisuId, track);
        List<WeeklyCurriculum> weeklyCurriculums = loadWeeklyCurriculumPort.findByCurriculumId(projection.id(), weekNo);

        List<WeeklyCurriculumOverviewInfo> weeks = weeklyCurriculums.stream()
            .map(WeeklyCurriculumOverviewInfo::of)
            .toList();

        return CurriculumOverviewInfo.of(projection, weeks);
    }

    @Override
    public MyCurriculumInfo getMyProgress(Long memberId, Long gisuId) {
        return getMyProgress(memberId, gisuId, null);
    }

    @Override
    public MyCurriculumInfo getMyProgress(Long memberId, Long gisuId, ChallengerTrack track) {
        GisuLearningType learningType = getGisuUseCase.getById(gisuId).learningType();
        ChallengerInfo challengerInfo = getChallengerUseCase.getByMemberIdAndGisuId(memberId, gisuId);
        CurriculumProjection projection;
        if (learningType == GisuLearningType.PART) {
            if (track != null) {
                throw new CurriculumDomainException(CurriculumErrorCode.INVALID_CURRICULUM_LEARNING_TYPE);
            }
            projection = loadCurriculumPort.getByGisuIdAndPart(gisuId, challengerInfo.part());
        } else {
            ChallengerTrack selected = resolveTrack(challengerInfo, track);
            projection = loadCurriculumPort.getByGisuIdAndTrack(gisuId, selected);
        }

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
            loadChallengerWorkbookPort.listByMemberIdAndOriginalWorkbookIdIn(memberId, originalWorkbookIds);

        List<Long> challengerWorkbookIds = allChallengerWorkbooks.stream().map(ChallengerWorkbook::getId).toList();
        List<MissionSubmission> allSubmissions =
            loadMissionSubmissionPort.listActiveByChallengerWorkbookIdIn(challengerWorkbookIds);

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

    private ChallengerTrack resolveTrack(ChallengerInfo challenger, ChallengerTrack requestedTrack) {
        validateBasicTrack(requestedTrack);
        List<ChallengerTrack> basicTracks = challenger.tracks() == null ? List.of()
            : challenger.tracks().stream().filter(ChallengerTrack::isBasic).distinct().toList();
        if (requestedTrack != null) {
            if (!basicTracks.contains(requestedTrack)) {
                throw new CurriculumDomainException(CurriculumErrorCode.WORKBOOK_ACCESS_DENIED);
            }
            return requestedTrack;
        }
        if (basicTracks.size() != 1) {
            throw new CurriculumDomainException(CurriculumErrorCode.CURRICULUM_TRACK_REQUIRED);
        }
        return basicTracks.getFirst();
    }

    private void validateBasicTrack(ChallengerTrack track) {
        if (track != null && !track.isBasic()) {
            throw new CurriculumDomainException(CurriculumErrorCode.UNSUPPORTED_CURRICULUM_TRACK);
        }
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
        return ChallengerWorkbookStatusPolicy.resolveSubmissionStatus(
            feedbacks.stream().map(feedback -> feedback.getFeedbackResult()).toList()
        );
    }
}
