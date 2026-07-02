package com.umc.product.challenger.adapter.in.web.assembler;

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
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Component;

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

        Map<Long, SchoolDetailInfo> schoolMap = getSchoolUseCase.getSchoolListByGisuIds(gisuIds).values().stream()
            .flatMap(List::stream)
            .collect(Collectors.toMap(SchoolDetailInfo::schoolId, Function.identity(), (a, b) -> a));

        return PageResponse.of(page,
            info -> toSummary(info, gisuMap.get(info.gisuId()), schoolMap.get(info.schoolId())));
    }

    /**
     * 기수×학교별 미사용 챌린저 기록 코드 개수 통계를 조회합니다 (CHALLENGER-RECORD-104).
     * <p>
     * 기수 세대/학교명 보강은 결과에 등장하는 gisuId 집합을 일괄 조회({@code getByIds},
     * {@code getSchoolListByGisuIds})하여 N+1 없이 매핑합니다. 전체 합계는 각 행의 합으로 계산합니다.
     */
    public UnusedChallengerRecordStatisticsResponse unusedStatistics() {
        List<UnusedChallengerRecordCountInfo> counts =
            getUnusedChallengerRecordStatisticsUseCase.getUnusedCountByGisuAndSchool();

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

        Map<Long, SchoolDetailInfo> schoolMap = getSchoolUseCase.getSchoolListByGisuIds(gisuIds).values().stream()
            .flatMap(List::stream)
            .collect(Collectors.toMap(SchoolDetailInfo::schoolId, Function.identity(), (a, b) -> a));

        List<UnusedChallengerRecordStatisticsResponse.Row> rows = counts.stream()
            .map(count -> toStatisticsRow(count, gisuMap.get(count.gisuId()), schoolMap.get(count.schoolId())))
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
