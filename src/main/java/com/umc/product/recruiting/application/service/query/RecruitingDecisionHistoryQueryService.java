package com.umc.product.recruiting.application.service.query;

import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.umc.product.authorization.application.port.in.query.CheckChallengerAuthorityUseCase;
import com.umc.product.global.util.EmailMasker;
import com.umc.product.member.application.port.in.query.GetMemberUseCase;
import com.umc.product.member.application.port.in.query.dto.MemberInfo;
import com.umc.product.organization.application.port.in.query.GetSchoolUseCase;
import com.umc.product.organization.application.port.in.query.dto.school.SchoolDetailInfo;
import com.umc.product.recruiting.application.port.in.query.ExportRecruitingDecisionHistoryCsvUseCase;
import com.umc.product.recruiting.application.port.in.query.SearchRecruitingDecisionHistoryUseCase;
import com.umc.product.recruiting.application.port.in.query.dto.RecruitingDecisionHistoryInfo;
import com.umc.product.recruiting.application.port.in.query.dto.RecruitingDecisionHistoryPageInfo;
import com.umc.product.recruiting.application.port.in.query.dto.RecruitingDecisionHistorySearchQuery;
import com.umc.product.recruiting.application.port.out.LoadRecruitingApplicationPort;
import com.umc.product.recruiting.application.port.out.LoadRecruitingDecisionHistoryPort;
import com.umc.product.recruiting.application.port.out.dto.RecruitingApplicationSummaryRow;
import com.umc.product.recruiting.application.port.out.dto.RecruitingDecisionHistoryRow;
import com.umc.product.recruiting.application.port.out.dto.RecruitingDecisionHistorySearchCondition;
import com.umc.product.recruiting.domain.enums.RecruitingApplicationStatus;
import com.umc.product.recruiting.domain.enums.RecruitingDecisionResult;
import com.umc.product.recruiting.domain.enums.RecruitingEvaluationProgressStatus;
import com.umc.product.recruiting.domain.exception.RecruitingDomainException;
import com.umc.product.recruiting.domain.exception.RecruitingErrorCode;

import lombok.RequiredArgsConstructor;

/**
 * 평가 이력 감사 화면용 Query 서비스입니다.
 * <p>
 * - 접근: SUPER_ADMIN 또는 기수 내 중앙운영사무국 구성원(총괄단·운영국·교육국)만 허용합니다.
 * <p>
 * - 상태 뱃지: 구조 조건(기수·지부·학교) 범위에서 판정 대상(DRAFT, CANCELLED 제외) 전원의 판정 완료 여부로 판단합니다.
 * <p>
 * - 담당자 이름·닉네임은 member 도메인에서 batch로 결합하고, 담당자 소속 학교는 직위가 학교 단위일 때
 * 지원서의 학교로 유도합니다(회장단은 자기 학교 지원서만 판정할 수 있음).
 */
@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class RecruitingDecisionHistoryQueryService implements
    SearchRecruitingDecisionHistoryUseCase,
    ExportRecruitingDecisionHistoryCsvUseCase {

    private static final Set<RecruitingApplicationStatus> DECISION_TARGET_STATUSES = EnumSet.of(
        RecruitingApplicationStatus.SUBMITTED,
        RecruitingApplicationStatus.DOCUMENT_FAILED,
        RecruitingApplicationStatus.INTERVIEW_ASSIGNED,
        RecruitingApplicationStatus.INTERVIEW_SKIPPED,
        RecruitingApplicationStatus.FINAL_PASSED,
        RecruitingApplicationStatus.FINAL_FAILED
    );

    private static final Set<RecruitingApplicationStatus> DECIDED_STATUSES = EnumSet.of(
        RecruitingApplicationStatus.DOCUMENT_FAILED,
        RecruitingApplicationStatus.FINAL_PASSED,
        RecruitingApplicationStatus.FINAL_FAILED
    );

    private static final String CSV_HEADER = String.join(",",
        "decidedAt",
        "decisionStatus",
        "result",
        "gisuId",
        "chapterId",
        "schoolId",
        "applicationId",
        "maskedEmail",
        "firstChoiceTrack",
        "secondChoiceTrack",
        "acceptedTrack",
        "deciderMemberId",
        "deciderRoleType",
        "deciderSchoolId",
        "deciderNickname"
    );

    private final LoadRecruitingDecisionHistoryPort loadDecisionHistoryPort;
    private final LoadRecruitingApplicationPort loadApplicationPort;
    private final GetSchoolUseCase getSchoolUseCase;
    private final GetMemberUseCase getMemberUseCase;
    private final CheckChallengerAuthorityUseCase checkChallengerAuthorityUseCase;
    private final Clock clock;

    @Override
    public RecruitingDecisionHistoryPageInfo search(RecruitingDecisionHistorySearchQuery query) {
        validateReadAccess(query.requesterMemberId(), query.gisuId());
        List<SchoolDetailInfo> scopedSchools = listScopedSchools(query);
        Page<RecruitingDecisionHistoryRow> rowPage = searchRowPage(query, scopedSchools, query.pageable());

        Map<Long, SchoolDetailInfo> schoolById = scopedSchools.stream()
            .collect(Collectors.toMap(SchoolDetailInfo::schoolId, school -> school));
        Map<Long, MemberInfo> deciderById = findDeciders(rowPage.getContent());
        return new RecruitingDecisionHistoryPageInfo(
            clock.instant(),
            resolveProgressStatus(query.gisuId(), schoolById.keySet()),
            rowPage.map(row -> toInfo(row, schoolById, deciderById))
        );
    }

    @Override
    public byte[] exportCsv(RecruitingDecisionHistorySearchQuery query) {
        validateReadAccess(query.requesterMemberId(), query.gisuId());
        List<SchoolDetailInfo> scopedSchools = listScopedSchools(query);
        List<RecruitingDecisionHistoryRow> rows =
            searchRowPage(query, scopedSchools, Pageable.unpaged()).getContent();

        Map<Long, SchoolDetailInfo> schoolById = scopedSchools.stream()
            .collect(Collectors.toMap(SchoolDetailInfo::schoolId, school -> school));
        Map<Long, MemberInfo> deciderById = findDeciders(rows);
        StringBuilder builder = new StringBuilder(CSV_HEADER).append('\n');
        for (RecruitingDecisionHistoryRow row : rows) {
            builder.append(toCsvLine(query.gisuId(), row, schoolById, deciderById)).append('\n');
        }
        return builder.toString().getBytes(StandardCharsets.UTF_8);
    }

    private Page<RecruitingDecisionHistoryRow> searchRowPage(
        RecruitingDecisionHistorySearchQuery query,
        List<SchoolDetailInfo> scopedSchools,
        Pageable pageable
    ) {
        Set<Long> scopedSchoolIds = scopedSchools.stream()
            .map(SchoolDetailInfo::schoolId)
            .collect(Collectors.toSet());
        if (scopedSchoolIds.isEmpty()) {
            return Page.empty(pageable);
        }
        return loadDecisionHistoryPort.searchRows(
            RecruitingDecisionHistorySearchCondition.builder()
                .gisuId(query.gisuId())
                .schoolIds(scopedSchoolIds)
                .tracks(query.tracks())
                .decisionStatuses(query.results().stream()
                    .flatMap(result -> result.getStatuses().stream())
                    .collect(Collectors.toSet()))
                .searchName(query.searchName())
                .matchedDeciderMemberIds(matchDeciderMemberIds(query, scopedSchoolIds))
                .latestFirst(query.effectiveSortOrder().isLatestFirst())
                .groupByDecider(query.groupByDecider())
                .build(),
            pageable
        );
    }

    /**
     * 조건에 포함된 학교 목록입니다. 기수의 학교 목록을 벗어난 이력 행은 결과에서 제외해 이름 결합 정합성을 보장합니다.
     */
    private List<SchoolDetailInfo> listScopedSchools(RecruitingDecisionHistorySearchQuery query) {
        return getSchoolUseCase.getSchoolListByGisuId(query.gisuId()).stream()
            .filter(school -> query.chapterId() == null || query.chapterId().equals(school.chapterId()))
            .filter(school -> query.schoolId() == null || query.schoolId().equals(school.schoolId()))
            .toList();
    }

    /**
     * 담당자 이름 검색은 이력에 이름이 저장되지 않으므로, 범위 내 담당자를 member 도메인에서 batch 조회해
     * 이름·닉네임이 부분일치하는 member ID 집합으로 변환합니다.
     */
    private Set<Long> matchDeciderMemberIds(RecruitingDecisionHistorySearchQuery query, Set<Long> scopedSchoolIds) {
        if (query.searchName() == null) {
            return Set.of();
        }
        List<Long> deciderMemberIds = loadDecisionHistoryPort.listDeciderMemberIds(query.gisuId(), scopedSchoolIds);
        if (deciderMemberIds.isEmpty()) {
            return Set.of();
        }
        String keyword = query.searchName().toLowerCase(Locale.ROOT);
        return getMemberUseCase.findAllByIds(Set.copyOf(deciderMemberIds)).entrySet().stream()
            .filter(entry -> containsIgnoreCase(entry.getValue().name(), keyword)
                || containsIgnoreCase(entry.getValue().nickname(), keyword))
            .map(Map.Entry::getKey)
            .collect(Collectors.toSet());
    }

    private Map<Long, MemberInfo> findDeciders(List<RecruitingDecisionHistoryRow> rows) {
        Set<Long> deciderMemberIds = rows.stream()
            .map(RecruitingDecisionHistoryRow::decidedByMemberId)
            .collect(Collectors.toSet());
        if (deciderMemberIds.isEmpty()) {
            return Map.of();
        }
        return getMemberUseCase.findAllByIds(deciderMemberIds);
    }

    private RecruitingEvaluationProgressStatus resolveProgressStatus(Long gisuId, Set<Long> scopedSchoolIds) {
        if (scopedSchoolIds.isEmpty()) {
            return RecruitingEvaluationProgressStatus.BEFORE_EVALUATION;
        }
        List<RecruitingApplicationSummaryRow> targets = loadApplicationPort.searchSummaryRows(
            gisuId,
            scopedSchoolIds,
            null,
            DECISION_TARGET_STATUSES
        );
        long decidedCount = targets.stream()
            .filter(target -> DECIDED_STATUSES.contains(target.applicationStatus()))
            .count();
        if (targets.isEmpty() || decidedCount == 0) {
            return RecruitingEvaluationProgressStatus.BEFORE_EVALUATION;
        }
        if (decidedCount < targets.size()) {
            return RecruitingEvaluationProgressStatus.IN_PROGRESS;
        }
        return RecruitingEvaluationProgressStatus.COMPLETED;
    }

    private RecruitingDecisionHistoryInfo toInfo(
        RecruitingDecisionHistoryRow row,
        Map<Long, SchoolDetailInfo> schoolById,
        Map<Long, MemberInfo> deciderById
    ) {
        SchoolDetailInfo applicantSchool = schoolById.get(row.schoolId());
        SchoolDetailInfo deciderSchool = resolveDeciderSchool(row, applicantSchool);
        MemberInfo decider = deciderById.get(row.decidedByMemberId());
        return RecruitingDecisionHistoryInfo.builder()
            .decisionHistoryId(row.decisionHistoryId())
            .applicationId(row.applicationId())
            .decidedAt(row.decidedAt())
            .decisionStatus(row.decisionStatus())
            .result(RecruitingDecisionResult.from(row.decisionStatus()))
            .applicant(RecruitingDecisionHistoryInfo.ApplicantInfo.builder()
                .chapterId(applicantSchool.chapterId())
                .chapterName(applicantSchool.chapterName())
                .schoolId(applicantSchool.schoolId())
                .schoolName(applicantSchool.schoolName())
                .name(row.applicantName())
                .firstChoice(row.firstChoice())
                .secondChoice(row.secondChoice())
                .acceptedTrack(row.acceptedTrack())
                .build())
            .decider(RecruitingDecisionHistoryInfo.DeciderInfo.builder()
                .memberId(row.decidedByMemberId())
                .chapterId(deciderSchool == null ? null : deciderSchool.chapterId())
                .chapterName(deciderSchool == null ? null : deciderSchool.chapterName())
                .schoolId(deciderSchool == null ? null : deciderSchool.schoolId())
                .schoolName(deciderSchool == null ? null : deciderSchool.schoolName())
                .roleType(row.deciderRoleType())
                .name(decider == null ? null : decider.name())
                .nickname(decider == null ? null : decider.nickname())
                .build())
            .build();
    }

    /**
     * 회장단은 자기 학교 지원서만 판정할 수 있으므로, 직위가 학교 단위면 담당자 소속을 지원서의 학교로 유도합니다.
     * 중앙 직위나 SUPER_ADMIN은 학교 소속 없이(null) 노출합니다.
     */
    private SchoolDetailInfo resolveDeciderSchool(RecruitingDecisionHistoryRow row, SchoolDetailInfo applicantSchool) {
        if (row.deciderRoleType() == null || !row.deciderRoleType().isAtLeastSchoolCore()) {
            return null;
        }
        return applicantSchool;
    }

    private String toCsvLine(
        Long gisuId,
        RecruitingDecisionHistoryRow row,
        Map<Long, SchoolDetailInfo> schoolById,
        Map<Long, MemberInfo> deciderById
    ) {
        SchoolDetailInfo applicantSchool = schoolById.get(row.schoolId());
        SchoolDetailInfo deciderSchool = resolveDeciderSchool(row, applicantSchool);
        MemberInfo decider = deciderById.get(row.decidedByMemberId());
        return String.join(",",
            RecruitingCsvCellEncoder.encode(row.decidedAt()),
            RecruitingCsvCellEncoder.encode(row.decisionStatus()),
            RecruitingCsvCellEncoder.encode(RecruitingDecisionResult.from(row.decisionStatus())),
            RecruitingCsvCellEncoder.encode(gisuId),
            RecruitingCsvCellEncoder.encode(applicantSchool.chapterId()),
            RecruitingCsvCellEncoder.encode(row.schoolId()),
            RecruitingCsvCellEncoder.encode(row.applicationId()),
            RecruitingCsvCellEncoder.encode(EmailMasker.mask(row.applicantEmail())),
            RecruitingCsvCellEncoder.encode(row.firstChoice()),
            RecruitingCsvCellEncoder.encode(row.secondChoice()),
            RecruitingCsvCellEncoder.encode(row.acceptedTrack()),
            RecruitingCsvCellEncoder.encode(row.decidedByMemberId()),
            RecruitingCsvCellEncoder.encode(row.deciderRoleType()),
            RecruitingCsvCellEncoder.encode(deciderSchool == null ? null : deciderSchool.schoolId()),
            RecruitingCsvCellEncoder.encode(decider == null ? null : decider.nickname())
        );
    }

    private boolean containsIgnoreCase(String value, String lowerCaseKeyword) {
        return value != null && value.toLowerCase(Locale.ROOT).contains(lowerCaseKeyword);
    }

    private void validateReadAccess(Long requesterMemberId, Long gisuId) {
        if (checkChallengerAuthorityUseCase.isSuperAdmin(requesterMemberId)
            || checkChallengerAuthorityUseCase.isCentralMemberInGisu(requesterMemberId, gisuId)) {
            return;
        }
        throw new RecruitingDomainException(RecruitingErrorCode.RECRUITING_DECISION_HISTORY_ACCESS_DENIED);
    }
}
