package com.umc.product.challenger.adapter.in.web.assembler;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.stereotype.Component;

import com.umc.product.challenger.adapter.in.web.dto.response.ChallengerRecordResponse;
import com.umc.product.challenger.adapter.in.web.dto.response.ChallengerRecordSummaryResponse;
import com.umc.product.challenger.adapter.in.web.dto.response.UnusedChallengerRecordStatisticsResponse;
import com.umc.product.challenger.application.port.in.query.GetChallengerRecordUseCase;
import com.umc.product.challenger.application.port.in.query.GetUnusedChallengerRecordStatisticsUseCase;
import com.umc.product.challenger.application.port.in.query.dto.ChallengerRecordInfo;
import com.umc.product.challenger.application.port.in.query.dto.ListChallengerRecordsQuery;
import com.umc.product.challenger.application.port.in.query.dto.UnusedChallengerRecordCountInfo;
import com.umc.product.global.response.PageResponse;
import com.umc.product.organization.application.port.in.query.GetGisuUseCase;
import com.umc.product.organization.application.port.in.query.GetSchoolUseCase;
import com.umc.product.organization.application.port.in.query.dto.gisu.GisuInfo;
import com.umc.product.organization.application.port.in.query.dto.school.SchoolDetailInfo;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Component
public class ChallengerRecordResponseAssembler {

    private final GetGisuUseCase getGisuUseCase;
    private final GetSchoolUseCase getSchoolUseCase;
    private final GetChallengerRecordUseCase getChallengerRecordUseCase;
    private final GetUnusedChallengerRecordStatisticsUseCase getUnusedChallengerRecordStatisticsUseCase;

    public ChallengerRecordResponse from(String code) {

        ChallengerRecordInfo recordInfo = getChallengerRecordUseCase.getByCode(code);

        return infoToResponse(recordInfo);
    }

    public ChallengerRecordResponse from(Long id) {

        ChallengerRecordInfo recordInfo = getChallengerRecordUseCase.getById(id);

        return infoToResponse(recordInfo);
    }

    /**
     * 조건별 챌린저 기록 코드 목록을 페이지 조회합니다 (CHALLENGER-RECORD-103).
     * <p>
     * 기수/학교 이름 보강은 페이지 내 항목의 gisuId를 일괄 조회({@code getByIds},
     * {@code getSchoolListByGisuIds})하여 N+1 없이 매핑합니다.
     * schoolMap은 기수 차원을 유지하는 중첩 맵 {@code Map<gisuId, Map<schoolId, SchoolDetailInfo>>}으로
     * 구성하여, 같은 학교가 기수별로 다른 지부(chapter)에 속하더라도 올바른 chapter 정보를 반환합니다.
     */
    public PageResponse<ChallengerRecordSummaryResponse> search(ListChallengerRecordsQuery query) {
        Page<ChallengerRecordInfo> page = getChallengerRecordUseCase.search(query);

        if (page.isEmpty()) {
            return PageResponse.of(page, info -> toSummary(info, null, null));
        }

        Set<Long> gisuIds = page.getContent().stream()
            .map(ChallengerRecordInfo::gisuId)
            .collect(Collectors.toSet());

        Map<Long, GisuInfo> gisuMap = getGisuUseCase.getByIds(gisuIds).stream()
            .collect(Collectors.toMap(GisuInfo::gisuId, Function.identity()));

        // 기수 차원 유지: Map<gisuId, Map<schoolId, SchoolDetailInfo>>
        // 같은 schoolId라도 기수별로 다른 chapterId/chapterName을 가질 수 있으므로
        // (gisuId, schoolId) 복합 키 조회를 위해 중첩 맵 구조를 사용한다.
        Map<Long, Map<Long, SchoolDetailInfo>> schoolMap = getSchoolUseCase.getSchoolListByGisuIds(gisuIds)
            .entrySet().stream()
            .collect(Collectors.toMap(
                Map.Entry::getKey,
                entry -> entry.getValue().stream()
                    .collect(Collectors.toMap(SchoolDetailInfo::schoolId, Function.identity()))
            ));

        return PageResponse.of(page,
            info -> toSummary(info, gisuMap.get(info.gisuId()),
                schoolMap.getOrDefault(info.gisuId(), Map.of()).get(info.schoolId())));
    }

    /**
     * 기수×학교별 미사용 챌린저 기록 코드 개수 통계를 조회합니다 (CHALLENGER-RECORD-104).
     * <p>
     * 기수 세대/학교명 보강은 결과에 등장하는 gisuId 집합을 일괄 조회({@code getByIds},
     * {@code getSchoolListByGisuIds})하여 N+1 없이 매핑합니다. 전체 합계는 각 행의 합으로 계산합니다.
     * schoolMap은 기수 차원을 유지하는 중첩 맵 {@code Map<gisuId, Map<schoolId, SchoolDetailInfo>>}으로
     * 구성하여, 같은 학교가 기수별로 다른 지부(chapter)에 속하더라도 올바른 schoolName을 반환합니다.
     */
    public UnusedChallengerRecordStatisticsResponse unusedStatistics() {
        List<UnusedChallengerRecordCountInfo> counts =
            getUnusedChallengerRecordStatisticsUseCase.listUnusedCountByGisuAndSchool();

        long totalUnusedCount = counts.stream()
            .mapToLong(UnusedChallengerRecordCountInfo::unusedCount)
            .sum();

        if (counts.isEmpty()) {
            return UnusedChallengerRecordStatisticsResponse.of(0L, List.of());
        }

        Set<Long> gisuIds = counts.stream()
            .map(UnusedChallengerRecordCountInfo::gisuId)
            .collect(Collectors.toSet());

        Map<Long, GisuInfo> gisuMap = getGisuUseCase.getByIds(gisuIds).stream()
            .collect(Collectors.toMap(GisuInfo::gisuId, Function.identity()));

        // 기수 차원 유지: Map<gisuId, Map<schoolId, SchoolDetailInfo>>
        // 같은 schoolId라도 기수별로 다른 chapterId/chapterName을 가질 수 있으므로
        // (gisuId, schoolId) 복합 키 조회를 위해 중첩 맵 구조를 사용한다.
        Map<Long, Map<Long, SchoolDetailInfo>> schoolMap = getSchoolUseCase.getSchoolListByGisuIds(gisuIds)
            .entrySet().stream()
            .collect(Collectors.toMap(
                Map.Entry::getKey,
                entry -> entry.getValue().stream()
                    .collect(Collectors.toMap(SchoolDetailInfo::schoolId, Function.identity()))
            ));

        List<UnusedChallengerRecordStatisticsResponse.Row> rows = counts.stream()
            .map(count -> toStatisticsRow(count, gisuMap.get(count.gisuId()),
                schoolMap.getOrDefault(count.gisuId(), Map.of()).get(count.schoolId())))
            .toList();

        return UnusedChallengerRecordStatisticsResponse.of(totalUnusedCount, rows);
    }

    private UnusedChallengerRecordStatisticsResponse.Row toStatisticsRow(
        UnusedChallengerRecordCountInfo count, GisuInfo gisuInfo, SchoolDetailInfo schoolInfo
    ) {
        return new UnusedChallengerRecordStatisticsResponse.Row(
            count.gisuId(),
            gisuInfo != null ? gisuInfo.generation() : null,
            count.schoolId(),
            schoolInfo != null ? schoolInfo.schoolName() : null,
            count.unusedCount()
        );
    }

    private ChallengerRecordResponse infoToResponse(ChallengerRecordInfo recordInfo) {
        GisuInfo gisuInfo = getGisuUseCase.getById(recordInfo.gisuId());
        SchoolDetailInfo schoolInfo = getSchoolUseCase.getSchoolDetail(recordInfo.schoolId());

        return ChallengerRecordResponse.builder()
            .code(recordInfo.code())
            .part(recordInfo.part())
            .gisuId(gisuInfo.gisuId())
            .gisu(gisuInfo.generation())
            .schoolId(schoolInfo.schoolId())
            .schoolName(schoolInfo.schoolName())
            .chapterId(schoolInfo.chapterId())
            .chapterName(schoolInfo.chapterName())
            .memberName(recordInfo.memberName())
            .challengerRoleType(recordInfo.challengerRoleType())
            .organizationId(recordInfo.organizationId())
            .build();
    }

    private ChallengerRecordSummaryResponse toSummary(
        ChallengerRecordInfo info, GisuInfo gisuInfo, SchoolDetailInfo schoolInfo
    ) {
        return ChallengerRecordSummaryResponse.builder()
            .id(info.id())
            .code(info.code())
            .part(info.part())
            .gisuId(info.gisuId())
            .gisu(gisuInfo != null ? gisuInfo.generation() : null)
            .schoolId(info.schoolId())
            .schoolName(schoolInfo != null ? schoolInfo.schoolName() : null)
            .chapterId(schoolInfo != null ? schoolInfo.chapterId() : info.chapterId())
            .chapterName(schoolInfo != null ? schoolInfo.chapterName() : null)
            .memberName(info.memberName())
            .challengerRoleType(info.challengerRoleType())
            .organizationId(info.organizationId())
            .isUsed(info.isUsed())
            .usedMemberId(info.usedMemberId())
            .usedAt(info.usedAt())
            .build();
    }
}
