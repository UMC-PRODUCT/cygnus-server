package com.umc.product.recruiting.application.service.query;

import java.time.Clock;
import java.util.Arrays;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.umc.product.authorization.application.port.in.query.CheckChallengerAuthorityUseCase;
import com.umc.product.common.domain.enums.ChallengerRoleType;
import com.umc.product.common.domain.enums.ChallengerTrack;
import com.umc.product.organization.application.port.in.query.GetSchoolUseCase;
import com.umc.product.organization.application.port.in.query.dto.school.SchoolChapterNameInfo;
import com.umc.product.recruiting.application.port.in.query.GetRecruitingEvaluationStatisticsUseCase;
import com.umc.product.recruiting.application.port.in.query.dto.RecruitingChapterEvaluationStatisticsInfo;
import com.umc.product.recruiting.application.port.in.query.dto.RecruitingEvaluationStatisticsInfo;
import com.umc.product.recruiting.application.port.in.query.dto.RecruitingEvaluationStatisticsQuery;
import com.umc.product.recruiting.application.port.in.query.dto.RecruitingSchoolEvaluationStatisticsInfo;
import com.umc.product.recruiting.application.port.in.query.dto.RecruitingTrackEvaluationCountInfo;
import com.umc.product.recruiting.application.port.out.LoadRecruitingEvaluationStatisticsPort;
import com.umc.product.recruiting.application.port.out.dto.RecruitingEvaluationStatisticsRow;
import com.umc.product.recruiting.domain.enums.RecruitingApplicationStatus;
import com.umc.product.recruiting.domain.exception.RecruitingDomainException;
import com.umc.product.recruiting.domain.exception.RecruitingErrorCode;

import lombok.RequiredArgsConstructor;

/**
 * 평가 현황 대시보드용 집계 Query 서비스.
 * <p>
 * 집계 기준:
 * <p>
 * - 지원자(분모): DRAFT, CANCELLED 를 제외한 모든 상태 (조회 쿼리에서 제외됨)
 * <p>
 * - 평가 완료(분자): DOCUMENT_FAILED, FINAL_PASSED, FINAL_FAILED (판정이 확정된 상태)
 * <p>
 * - 파트 귀속: 1지망(firstChoice) 기준
 */
@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class RecruitingEvaluationStatisticsQueryService implements GetRecruitingEvaluationStatisticsUseCase {

    private static final Set<RecruitingApplicationStatus> EVALUATED_STATUSES = EnumSet.of(
        RecruitingApplicationStatus.DOCUMENT_FAILED,
        RecruitingApplicationStatus.FINAL_PASSED,
        RecruitingApplicationStatus.FINAL_FAILED
    );

    private static final List<ChallengerTrack> RECRUITING_TRACKS = Arrays.stream(ChallengerTrack.values())
        .filter(track -> track != ChallengerTrack.INFRA_PLUS)
        .sorted(Comparator.comparing(ChallengerTrack::getSortOrder))
        .toList();

    private final LoadRecruitingEvaluationStatisticsPort loadStatisticsPort;
    private final GetSchoolUseCase getSchoolUseCase;
    private final CheckChallengerAuthorityUseCase checkChallengerAuthorityUseCase;
    private final Clock clock;

    @Override
    public RecruitingEvaluationStatisticsInfo getEvaluationStatistics(RecruitingEvaluationStatisticsQuery query) {
        validateReadAccess(query.requesterMemberId(), query.gisuId());

        List<SchoolChapterNameInfo> schools = getSchoolUseCase.getSchoolChapterNamesByGisuId(query.gisuId());
        Set<Long> knownSchoolIds = schools.stream()
            .map(SchoolDetailInfo::schoolId)
            .collect(Collectors.toSet());

        // 학교 목록에 없는 학교의 row는 제외해 전체 합계와 지부·학교 합계의 정합성을 보장한다.
        List<RecruitingEvaluationStatisticsRow> rows = loadStatisticsPort.listByGisuId(query.gisuId()).stream()
            .filter(row -> knownSchoolIds.contains(row.schoolId()))
            .toList();

        Map<Long, List<RecruitingEvaluationStatisticsRow>> rowsBySchool = rows.stream()
            .collect(Collectors.groupingBy(RecruitingEvaluationStatisticsRow::schoolId));

        return new RecruitingEvaluationStatisticsInfo(
            clock.instant(),
            sumCount(rows, row -> true),
            sumCount(rows, this::isEvaluated),
            countByTrack(rows),
            toChapterInfos(schools, rowsBySchool)
        );
    }

    private void validateReadAccess(Long requesterMemberId, Long gisuId) {
        if (checkChallengerAuthorityUseCase.isSuperAdmin(requesterMemberId)
            || checkChallengerAuthorityUseCase.hasAnyRoleTypeInGisu(
                requesterMemberId, gisuId, ChallengerRoleType.values())) {
            return;
        }
        throw new RecruitingDomainException(RecruitingErrorCode.RECRUITING_EVALUATION_STATISTICS_ACCESS_DENIED);
    }

    private List<RecruitingChapterEvaluationStatisticsInfo> toChapterInfos(
        List<SchoolChapterNameInfo> schools,
        Map<Long, List<RecruitingEvaluationStatisticsRow>> rowsBySchool
    ) {
        Map<Long, List<SchoolDetailInfo>> schoolsByChapter = schools.stream()
            .sorted(Comparator.comparing(SchoolDetailInfo::chapterName)
                .thenComparing(SchoolDetailInfo::schoolName))
            .collect(Collectors.groupingBy(SchoolDetailInfo::chapterId, LinkedHashMap::new, Collectors.toList()));

        return schoolsByChapter.values().stream()
            .map(chapterSchools -> toChapterInfo(chapterSchools, rowsBySchool))
            .toList();
    }

    private RecruitingChapterEvaluationStatisticsInfo toChapterInfo(
        List<SchoolChapterNameInfo> chapterSchools,
        Map<Long, List<RecruitingEvaluationStatisticsRow>> rowsBySchool
    ) {
        SchoolChapterNameInfo first = chapterSchools.get(0);
        List<RecruitingEvaluationStatisticsRow> chapterRows = chapterSchools.stream()
            .flatMap(school -> rowsBySchool.getOrDefault(school.schoolId(), List.of()).stream())
            .toList();

        return new RecruitingChapterEvaluationStatisticsInfo(
            first.chapterId(),
            first.chapterName(),
            sumCount(chapterRows, row -> true),
            sumCount(chapterRows, this::isEvaluated),
            countByTrack(chapterRows),
            chapterSchools.stream()
                .map(school -> toSchoolInfo(school, rowsBySchool.getOrDefault(school.schoolId(), List.of())))
                .toList()
        );
    }

    private RecruitingSchoolEvaluationStatisticsInfo toSchoolInfo(
        SchoolChapterNameInfo school,
        List<RecruitingEvaluationStatisticsRow> schoolRows
    ) {
        return new RecruitingSchoolEvaluationStatisticsInfo(
            school.schoolId(),
            school.schoolName(),
            sumCount(schoolRows, row -> true),
            sumCount(schoolRows, this::isEvaluated),
            countByTrack(schoolRows)
        );
    }

    private List<RecruitingTrackEvaluationCountInfo> countByTrack(List<RecruitingEvaluationStatisticsRow> rows) {
        return RECRUITING_TRACKS.stream()
            .map(track -> new RecruitingTrackEvaluationCountInfo(
                track,
                sumCount(rows, row -> row.track() == track),
                sumCount(rows, row -> row.track() == track && isEvaluated(row))
            ))
            .toList();
    }

    private boolean isEvaluated(RecruitingEvaluationStatisticsRow row) {
        return EVALUATED_STATUSES.contains(row.status());
    }

    private Long sumCount(
        List<RecruitingEvaluationStatisticsRow> rows,
        Predicate<RecruitingEvaluationStatisticsRow> filter
    ) {
        return rows.stream()
            .filter(filter)
            .mapToLong(RecruitingEvaluationStatisticsRow::count)
            .sum();
    }
}
