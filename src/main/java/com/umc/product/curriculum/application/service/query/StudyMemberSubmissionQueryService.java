package com.umc.product.curriculum.application.service.query;

import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.umc.product.common.domain.enums.ChallengerPart;
import com.umc.product.common.domain.enums.ChallengerTrack;
import com.umc.product.curriculum.application.port.in.query.GetStudyMemberSubmissionUseCase;
import com.umc.product.curriculum.application.port.in.query.dto.CurriculumProjection;
import com.umc.product.curriculum.application.port.in.query.dto.StudyMemberSubmissionInfo;
import com.umc.product.curriculum.application.port.in.query.dto.StudyMemberSubmissionInfo.WeeklySubmissionInfo;
import com.umc.product.curriculum.application.port.in.query.dto.StudyMemberSubmissionQuery;
import com.umc.product.curriculum.application.port.out.LoadChallengerWorkbookPort;
import com.umc.product.curriculum.application.port.out.LoadChallengerWorkbookPort.ChallengerWorkbookLookupKey;
import com.umc.product.curriculum.application.port.out.LoadCurriculumPort;
import com.umc.product.curriculum.application.port.out.LoadMissionFeedbackPort;
import com.umc.product.curriculum.application.port.out.LoadMissionSubmissionPort;
import com.umc.product.curriculum.application.port.out.LoadOriginalWorkbookMissionPort;
import com.umc.product.curriculum.application.port.out.LoadOriginalWorkbookPort;
import com.umc.product.curriculum.application.port.out.LoadWeeklyBestWorkbookPort;
import com.umc.product.curriculum.application.port.out.LoadWeeklyBestWorkbookPort.BestWorkbookHolder;
import com.umc.product.curriculum.application.port.out.LoadWeeklyCurriculumPort;
import com.umc.product.curriculum.domain.ChallengerWorkbook;
import com.umc.product.curriculum.domain.ChallengerWorkbookStatusPolicy;
import com.umc.product.curriculum.domain.MissionFeedback;
import com.umc.product.curriculum.domain.MissionSubmission;
import com.umc.product.curriculum.domain.OriginalWorkbook;
import com.umc.product.curriculum.domain.OriginalWorkbookMission;
import com.umc.product.curriculum.domain.WeeklyCurriculum;
import com.umc.product.curriculum.domain.enums.ChallengerWorkbookStatus;
import com.umc.product.curriculum.domain.enums.OriginalWorkbookType;
import com.umc.product.curriculum.domain.enums.SubmissionStatus;
import com.umc.product.curriculum.domain.exception.CurriculumDomainException;
import com.umc.product.curriculum.domain.exception.CurriculumErrorCode;
import com.umc.product.member.application.port.in.query.GetMemberUseCase;
import com.umc.product.member.application.port.in.query.dto.MemberInfo;
import com.umc.product.organization.application.port.in.query.GetGisuUseCase;
import com.umc.product.organization.application.port.in.query.GetStudyGroupUseCase;
import com.umc.product.organization.application.port.in.query.dto.studygroup.StudyGroupMemberPageInfo;

import lombok.RequiredArgsConstructor;

/**
 * 스터디원 제출 현황 조회 (활동 관리 &gt; 스터디 관리 &gt; 제출 현황).
 *
 * <h3>조회 축</h3>
 * 모수는 제출물이 아니라 {@code study_group_member} 다. 제출물을 모수로 잡으면 미제출자가 결과에서 빠지는데, 이 화면은 "아직 안 낸 사람"을 보려고 여는 화면이다.
 *
 * <h3>쿼리 개수</h3>
 * 페이지 크기·주차 수와 무관하게 고정이다. 스터디원 페이지 1회 → 파트별 커리큘럼 조회 → 주차/원본 워크북/챌린저 워크북/제출물/피드백/필수 미션/베스트/멤버 정보를 각각 batch 로
 * 조회한다. 그룹·멤버 루프 안에서 쿼리를 돌리지 않는다.
 *
 * <h3>cross-domain 경계</h3>
 * 스터디원 명단과 권한 Scope 는 Organization, 이름·학교·프로필은 Member 의 UseCase 로 받아서 합성한다. 여기서 그 테이블들을 직접 JOIN 하지 않는다.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class StudyMemberSubmissionQueryService implements GetStudyMemberSubmissionUseCase {

    private final GetStudyGroupUseCase getStudyGroupUseCase;
    private final GetGisuUseCase getGisuUseCase;
    private final GetMemberUseCase getMemberUseCase;
    private final LoadCurriculumPort loadCurriculumPort;
    private final LoadWeeklyCurriculumPort loadWeeklyCurriculumPort;
    private final LoadOriginalWorkbookPort loadOriginalWorkbookPort;
    private final LoadOriginalWorkbookMissionPort loadOriginalWorkbookMissionPort;
    private final LoadChallengerWorkbookPort loadChallengerWorkbookPort;
    private final LoadMissionSubmissionPort loadMissionSubmissionPort;
    private final LoadMissionFeedbackPort loadMissionFeedbackPort;
    private final LoadWeeklyBestWorkbookPort loadWeeklyBestWorkbookPort;

    @Override
    public List<StudyMemberSubmissionInfo> getStudyMemberSubmissions(StudyMemberSubmissionQuery query) {
        List<StudyGroupMemberPageInfo> page = query.gisuId() == null
            ? getStudyGroupUseCase.getVisibleStudyGroupMembers(
                query.requesterMemberId(), query.studyGroupId(), query.cursor(), query.fetchSize())
            : getStudyGroupUseCase.getVisibleStudyGroupMembers(
                query.requesterMemberId(), query.studyGroupId(), query.cursor(), query.fetchSize(), query.gisuId());
        if (page.isEmpty()) {
            return List.of();
        }

        Long gisuId = query.gisuId() == null ? getGisuUseCase.getActiveGisuId() : query.gisuId();
        Map<LearningKey, List<WeeklyCurriculum>> weeksByLearning =
            resolveWeeksByLearning(page, gisuId, query.weekNos());
        WorkbookSnapshot snapshot = loadWorkbookSnapshot(page, weeksByLearning);
        Map<Long, MemberInfo> memberMap = getMemberUseCase.findAllByIds(
            page.stream().map(StudyGroupMemberPageInfo::memberId).collect(Collectors.toSet())
        );

        return page.stream()
            .map(row -> toInfo(row,
                weeksByLearning.getOrDefault(new LearningKey(row.part(), row.track()), List.of()), snapshot, memberMap))
            .toList();
    }

    /**
     * 제출 현황 필터용 조회 가능 주차 목록.
     * <p>
     * 주차 기준은 {@link #getStudyMemberSubmissions} 행의 weeks 와 동일 — 커리큘럼에 정의된 주차 전부 (배포 여부 무관).
     * 주차 정보는 공개 데이터(CURRICULUM-101 {@code @Public})라 역할 Scope 를 태우지 않고, 그룹 지정 시에만 그 그룹 파트로 좁힌다.
     * 커리큘럼 없는 파트는 건너뛴다.
     */
    @Override
    public List<Long> getAvailableWeekNos(Long studyGroupId) {
        return getAvailableWeekNos(studyGroupId, null);
    }

    @Override
    public List<Long> getAvailableWeekNos(Long studyGroupId, Long requestedGisuId) {
        Long gisuId = requestedGisuId == null ? getGisuUseCase.getActiveGisuId()
            : getGisuUseCase.getById(requestedGisuId).gisuId();
        List<LearningKey> learningKeys;
        if (studyGroupId != null) {
            var group = getStudyGroupUseCase.getById(studyGroupId);
            if (requestedGisuId != null && !group.gisuId().equals(gisuId)) {
                throw new CurriculumDomainException(CurriculumErrorCode.STUDY_GROUP_NOT_MATCHED);
            }
            learningKeys = List.of(new LearningKey(group.part(), group.track()));
        } else {
            learningKeys = new java.util.ArrayList<>();
            for (ChallengerPart part : ChallengerPart.values()) {
                learningKeys.add(new LearningKey(part, null));
            }
            for (ChallengerTrack track : ChallengerTrack.values()) {
                if (track.isBasic()) {
                    learningKeys.add(new LearningKey(null, track));
                }
            }
        }

        return learningKeys.stream()
            .map(key -> findCurriculum(gisuId, key))
            .flatMap(Optional::stream)
            .flatMap(curriculum -> loadWeeklyCurriculumPort.findByCurriculumId(curriculum.id(), null).stream())
            .map(WeeklyCurriculum::getWeekNo)
            .distinct()
            .sorted()
            .toList();
    }

    /**
     * 페이지에 등장한 파트별로 해당 기수 커리큘럼의 주차 목록을 조회한다.
     * <p>
     * 주차 번호는 파트마다 별도 커리큘럼에 속하므로, 회장단처럼 여러 파트의 그룹을 한 번에 보는 경우 같은 "3주차"라도 파트별로 다른
     * {@code weekly_curriculum} 행이다. 파트 수만큼만 조회하며 페이지 크기와는 무관하다.
     */
    private Map<LearningKey, List<WeeklyCurriculum>> resolveWeeksByLearning(
        List<StudyGroupMemberPageInfo> page, Long gisuId, List<Long> weekNos
    ) {
        Map<LearningKey, List<WeeklyCurriculum>> weeksByLearning = new LinkedHashMap<>();

        for (LearningKey key : page.stream()
            .map(row -> new LearningKey(row.part(), row.track())).collect(Collectors.toSet())) {
            Optional<CurriculumProjection> curriculum = findCurriculum(gisuId, key);
            if (curriculum.isEmpty()) {
                continue;
            }
            weeksByLearning.put(key, loadWeeks(curriculum.get().id(), weekNos));
        }

        return weeksByLearning;
    }

    private Optional<CurriculumProjection> findCurriculum(Long gisuId, LearningKey key) {
        return key.track() == null
            ? loadCurriculumPort.findByGisuIdAndPart(gisuId, key.part())
            : loadCurriculumPort.findByGisuIdAndTrack(gisuId, key.track());
    }

    private record LearningKey(ChallengerPart part, ChallengerTrack track) {
    }

    private List<WeeklyCurriculum> loadWeeks(Long curriculumId, List<Long> weekNos) {
        if (weekNos.isEmpty()) {
            return loadWeeklyCurriculumPort.findByCurriculumId(curriculumId, null);
        }
        return weekNos.stream()
            .flatMap(weekNo -> loadWeeklyCurriculumPort.findByCurriculumId(curriculumId, weekNo).stream())
            .toList();
    }

    /**
     * 화면 한 페이지에 필요한 워크북 관련 데이터를 한꺼번에 적재한다.
     */
    private WorkbookSnapshot loadWorkbookSnapshot(
        List<StudyGroupMemberPageInfo> page,
        Map<LearningKey, List<WeeklyCurriculum>> weeksByLearning
    ) {
        List<Long> weeklyCurriculumIds = weeksByLearning.values().stream()
            .flatMap(List::stream)
            .map(week -> week.getId())
            .distinct()
            .toList();
        if (weeklyCurriculumIds.isEmpty()) {
            return WorkbookSnapshot.empty();
        }

        // 한 주차에 MAIN/EXTRA 원본 워크북이 함께 있을 수 있다. 제출 현황의 주차별 상태는 정규 과제인 MAIN 기준이다.
        Set<Long> mainWorkbookIds = loadOriginalWorkbookPort
            .findReleasedByWeeklyCurriculumIdIn(weeklyCurriculumIds).stream()
            .filter(workbook -> workbook.getType() == OriginalWorkbookType.MAIN)
            .map(OriginalWorkbook::getId)
            .collect(Collectors.toSet());

        List<ChallengerWorkbook> workbooks = loadChallengerWorkbookPort
            .listByLookupKeys(buildLookupKeys(page, weeksByLearning)).stream()
            .filter(workbook -> mainWorkbookIds.contains(workbook.getOriginalWorkbook().getId()))
            .toList();

        List<Long> workbookIds = workbooks.stream().map(ChallengerWorkbook::getId).toList();
        List<MissionSubmission> submissions =
            loadMissionSubmissionPort.listActiveByChallengerWorkbookIdIn(workbookIds);
        List<MissionFeedback> feedbacks = loadMissionFeedbackPort.listByMissionSubmissionIdIn(
            submissions.stream().map(MissionSubmission::getId).toList()
        );
        List<OriginalWorkbookMission> missions = loadOriginalWorkbookMissionPort.findByOriginalWorkbookIdIn(
            workbooks.stream().map(workbook -> workbook.getOriginalWorkbook().getId()).distinct().toList()
        );
        List<BestWorkbookHolder> bestHolders = loadWeeklyBestWorkbookPort.findHolders(
            page.stream().map(StudyGroupMemberPageInfo::studyGroupId).collect(Collectors.toSet()),
            weeklyCurriculumIds
        );

        return WorkbookSnapshot.of(workbooks, submissions, feedbacks, missions, bestHolders);
    }

    /**
     * 페이지의 (스터디원, 그룹) × 그 파트의 주차 조합으로 조회 키를 만든다.
     * <p>
     * 키에 스터디 그룹이 포함되므로, 제출 당시 그룹이 기록되지 않은 과거 워크북({@code study_group_id IS NULL})은 매칭되지 않고 미제출로 표시된다. 워크북 배포가 다시
     * 실행되면 {@code assignStudyGroupIfAbsent} 로 채워지며 정상 표시된다.
     */
    private List<ChallengerWorkbookLookupKey> buildLookupKeys(
        List<StudyGroupMemberPageInfo> page,
        Map<LearningKey, List<WeeklyCurriculum>> weeksByLearning
    ) {
        return page.stream()
            .flatMap(row -> weeksByLearning
                .getOrDefault(new LearningKey(row.part(), row.track()), List.<WeeklyCurriculum>of()).stream()
                .map(week -> new ChallengerWorkbookLookupKey(row.memberId(), week.getId(), row.studyGroupId())))
            .toList();
    }

    private StudyMemberSubmissionInfo toInfo(
        StudyGroupMemberPageInfo row,
        List<WeeklyCurriculum> weeks,
        WorkbookSnapshot snapshot,
        Map<Long, MemberInfo> memberMap
    ) {
        MemberInfo member = memberMap.get(row.memberId());

        return StudyMemberSubmissionInfo.builder()
            .studyGroupMemberId(row.studyGroupMemberId())
            .memberId(row.memberId())
            .memberName(member == null ? null : member.name())
            .nickname(member == null ? null : member.nickname())
            .schoolName(member == null ? null : member.schoolName())
            .profileImageUrl(member == null ? null : member.profileImageLink())
            .studyGroupId(row.studyGroupId())
            .studyGroupName(row.studyGroupName())
            .part(row.part())
            .track(row.track())
            .weeks(weeks.stream()
                .sorted((left, right) -> Long.compare(left.getWeekNo(), right.getWeekNo()))
                .map(week -> toWeeklyInfo(row, week, snapshot))
                .toList())
            .build();
    }

    private WeeklySubmissionInfo toWeeklyInfo(
        StudyGroupMemberPageInfo row, WeeklyCurriculum week, WorkbookSnapshot snapshot
    ) {
        ChallengerWorkbook workbook = snapshot.findWorkbook(row.memberId(), week.getId(), row.studyGroupId());
        boolean isBest = snapshot.isBest(row.studyGroupId(), week.getId(), row.memberId());

        if (workbook == null) {
            return WeeklySubmissionInfo.builder()
                .weekNo(week.getWeekNo())
                .weeklyCurriculumId(week.getId())
                .weeklyCurriculumTitle(week.getTitle())
                .challengerWorkbookId(null)
                .status(ChallengerWorkbookStatus.NOT_SUBMITTED)
                .isBest(isBest)
                .build();
        }

        return WeeklySubmissionInfo.builder()
            .weekNo(week.getWeekNo())
            .weeklyCurriculumId(week.getId())
            .weeklyCurriculumTitle(week.getTitle())
            .challengerWorkbookId(workbook.getId())
            .status(ChallengerWorkbookStatusPolicy.resolveWorkbookStatus(
                workbook.isExcused(),
                snapshot.submissionStatusByMission(workbook.getId()),
                snapshot.requiredMissionIds(workbook.getOriginalWorkbook().getId())
            ))
            .isBest(isBest)
            .build();
    }

    /**
     * 한 페이지 분량의 워크북 관련 데이터를 조합 키로 찾을 수 있게 색인해 둔 것.
     */
    private record WorkbookSnapshot(
        Map<WorkbookKey, ChallengerWorkbook> workbookByKey,
        Map<Long, Map<Long, SubmissionStatus>> submissionStatusByWorkbook,
        Map<Long, Set<Long>> requiredMissionIdsByOriginalWorkbook,
        Set<BestKey> bestKeys
    ) {

        private static WorkbookSnapshot empty() {
            return new WorkbookSnapshot(Map.of(), Map.of(), Map.of(), Set.of());
        }

        private static WorkbookSnapshot of(
            List<ChallengerWorkbook> workbooks,
            List<MissionSubmission> submissions,
            List<MissionFeedback> feedbacks,
            List<OriginalWorkbookMission> missions,
            List<BestWorkbookHolder> bestHolders
        ) {
            Map<Long, List<MissionFeedback>> feedbacksBySubmission = feedbacks.stream()
                .collect(Collectors.groupingBy(feedback -> feedback.getMissionSubmission().getId()));

            Map<Long, Map<Long, SubmissionStatus>> statusByWorkbook = submissions.stream()
                .collect(Collectors.groupingBy(
                    submission -> submission.getChallengerWorkbook().getId(),
                    Collectors.toMap(
                        submission -> submission.getOriginalWorkbookMission().getId(),
                        submission -> ChallengerWorkbookStatusPolicy.resolveSubmissionStatus(
                            feedbacksBySubmission.getOrDefault(submission.getId(), List.of()).stream()
                                .map(feedback -> feedback.getFeedbackResult())
                                .toList()
                        )
                    )
                ));

            Map<Long, Set<Long>> requiredByWorkbook = missions.stream()
                .filter(mission -> mission.isNecessary())
                .collect(Collectors.groupingBy(
                    mission -> mission.getOriginalWorkbook().getId(),
                    Collectors.mapping(mission -> mission.getId(), Collectors.toSet())
                ));

            return new WorkbookSnapshot(
                workbooks.stream().collect(Collectors.toMap(
                    workbook -> new WorkbookKey(
                        workbook.getMemberId(),
                        workbook.getOriginalWorkbook().getWeeklyCurriculum().getId(),
                        workbook.getStudyGroupId()
                    ),
                    workbook -> workbook,
                    (left, right) -> left
                )),
                statusByWorkbook,
                requiredByWorkbook,
                bestHolders.stream()
                    .map(holder -> new BestKey(
                        holder.studyGroupId(), holder.weeklyCurriculumId(), holder.memberId()))
                    .collect(Collectors.toCollection(HashSet::new))
            );
        }

        private ChallengerWorkbook findWorkbook(Long memberId, Long weeklyCurriculumId, Long studyGroupId) {
            return workbookByKey.get(new WorkbookKey(memberId, weeklyCurriculumId, studyGroupId));
        }

        private Map<Long, SubmissionStatus> submissionStatusByMission(Long challengerWorkbookId) {
            return submissionStatusByWorkbook.getOrDefault(challengerWorkbookId, Map.of());
        }

        private Set<Long> requiredMissionIds(Long originalWorkbookId) {
            return requiredMissionIdsByOriginalWorkbook.getOrDefault(originalWorkbookId, Set.of());
        }

        private boolean isBest(Long studyGroupId, Long weeklyCurriculumId, Long memberId) {
            return bestKeys.contains(new BestKey(studyGroupId, weeklyCurriculumId, memberId));
        }

        private record WorkbookKey(Long memberId, Long weeklyCurriculumId, Long studyGroupId) {
        }

        private record BestKey(Long studyGroupId, Long weeklyCurriculumId, Long memberId) {
        }
    }
}
