package com.umc.product.recruiting.application.service.query;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.umc.product.authorization.application.port.in.CheckPermissionUseCase;
import com.umc.product.authorization.domain.PermissionType;
import com.umc.product.authorization.domain.ResourcePermission;
import com.umc.product.authorization.domain.ResourceType;
import com.umc.product.authorization.domain.SubjectAttributes;
import com.umc.product.organization.application.port.in.query.GetSchoolUseCase;
import com.umc.product.organization.application.port.in.query.dto.school.SchoolDetailInfo;
import com.umc.product.recruiting.application.port.in.query.GetRecruitingSeasonConfigurationUseCase;
import com.umc.product.recruiting.application.port.in.query.SearchRecruitingRoundUseCase;
import com.umc.product.recruiting.application.port.in.query.SearchRecruitingSeasonUseCase;
import com.umc.product.recruiting.application.port.in.query.dto.RecruitingRoundConfigurationInfo;
import com.umc.product.recruiting.application.port.in.query.dto.RecruitingRoundSearchQuery;
import com.umc.product.recruiting.application.port.in.query.dto.RecruitingRoundSummaryInfo;
import com.umc.product.recruiting.application.port.in.query.dto.RecruitingSeasonConfigurationInfo;
import com.umc.product.recruiting.application.port.in.query.dto.RecruitingSeasonSearchQuery;
import com.umc.product.recruiting.application.port.in.query.dto.RecruitingSeasonSummaryInfo;
import com.umc.product.recruiting.application.port.in.query.dto.RecruitingSeasonTrackQuotaInfo;
import com.umc.product.recruiting.application.port.out.LoadRecruitingRoundPort;
import com.umc.product.recruiting.application.port.out.LoadRecruitingSeasonPort;
import com.umc.product.recruiting.application.port.out.LoadRecruitingSeasonTrackQuotaPort;
import com.umc.product.recruiting.domain.RecruitingRound;
import com.umc.product.recruiting.domain.RecruitingSeason;
import com.umc.product.recruiting.domain.enums.RecruitingRoundType;

import lombok.RequiredArgsConstructor;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class RecruitingSeasonQueryService implements
    GetRecruitingSeasonConfigurationUseCase,
    SearchRecruitingSeasonUseCase,
    SearchRecruitingRoundUseCase {

    private static final Comparator<RecruitingRound> ROUND_ORDER = Comparator
        .comparing((RecruitingRound round) -> round.getSeason().getSchoolId())
        .thenComparing(round -> round.getSeason().getId())
        .thenComparingInt(round -> round.getType() == RecruitingRoundType.REGULAR ? 0 : 1)
        .thenComparing(RecruitingRound::getRoundNo)
        .thenComparing(RecruitingRound::getId);

    private final LoadRecruitingSeasonPort loadSeasonPort;
    private final LoadRecruitingSeasonTrackQuotaPort loadQuotaPort;
    private final LoadRecruitingRoundPort loadRoundPort;
    private final GetSchoolUseCase getSchoolUseCase;
    private final CheckPermissionUseCase checkPermissionUseCase;

    @Override
    public RecruitingSeasonConfigurationInfo getBySeasonId(Long seasonId) {
        RecruitingSeason season = loadSeasonPort.getById(seasonId);
        return RecruitingSeasonConfigurationInfo.of(
            season,
            loadQuotaPort.listBySeasonId(seasonId).stream()
                .map(RecruitingSeasonTrackQuotaInfo::from)
                .toList(),
            loadRoundPort.listBySeasonId(seasonId).stream()
                .map(RecruitingRoundConfigurationInfo::from)
                .toList()
        );
    }

    @Override
    public List<RecruitingSeasonSummaryInfo> searchSeasons(RecruitingSeasonSearchQuery query) {
        List<VisibleSeason> visibleSeasons = listVisibleSeasons(
            query.gisuId(),
            query.chapterId(),
            query.schoolId(),
            null,
            query.requesterMemberId()
        );
        Map<Long, List<RecruitingRoundConfigurationInfo>> roundsBySeasonId = listRounds(visibleSeasons).stream()
            .collect(Collectors.groupingBy(
                round -> round.getSeason().getId(),
                Collectors.mapping(RecruitingRoundConfigurationInfo::from, Collectors.toList())
            ));
        return visibleSeasons.stream()
            .map(visible -> RecruitingSeasonSummaryInfo.of(
                visible.season(),
                visible.school().chapterId(),
                visible.school().chapterName(),
                visible.school().schoolName(),
                roundsBySeasonId.getOrDefault(visible.season().getId(), List.of())
            ))
            .toList();
    }

    @Override
    public List<RecruitingRoundSummaryInfo> searchRounds(RecruitingRoundSearchQuery query) {
        List<VisibleSeason> visibleSeasons = listVisibleSeasons(
            query.gisuId(),
            query.chapterId(),
            query.schoolId(),
            query.seasonId(),
            query.requesterMemberId()
        );
        Map<Long, SchoolDetailInfo> schoolBySeasonId = visibleSeasons.stream()
            .collect(Collectors.toMap(visible -> visible.season().getId(), VisibleSeason::school));
        return listRounds(visibleSeasons).stream()
            .map(round -> {
                SchoolDetailInfo school = schoolBySeasonId.get(round.getSeason().getId());
                return RecruitingRoundSummaryInfo.of(
                    round,
                    school.chapterId(),
                    school.chapterName(),
                    school.schoolName()
                );
            })
            .toList();
    }

    private List<VisibleSeason> listVisibleSeasons(
        Long gisuId,
        Long chapterId,
        Long schoolId,
        Long seasonId,
        Long requesterMemberId
    ) {
        Map<Long, SchoolDetailInfo> schoolsById = getSchoolUseCase.getSchoolListByGisuId(gisuId).stream()
            .filter(school -> chapterId == null || chapterId.equals(school.chapterId()))
            .filter(school -> schoolId == null || schoolId.equals(school.schoolId()))
            .collect(Collectors.toMap(SchoolDetailInfo::schoolId, Function.identity()));
        List<RecruitingSeason> candidates = loadSeasonPort.listByGisuId(gisuId).stream()
            .filter(season -> seasonId == null || seasonId.equals(season.getId()))
            .filter(season -> schoolsById.containsKey(season.getSchoolId()))
            .toList();
        if (candidates.isEmpty()) {
            return List.of();
        }
        SubjectAttributes subject = checkPermissionUseCase.loadSubject(requesterMemberId);
        return candidates.stream()
            .filter(season -> checkPermissionUseCase.check(subject, ResourcePermission.of(
                ResourceType.RECRUITMENT,
                season.getId(),
                PermissionType.READ
            )))
            .map(season -> new VisibleSeason(season, schoolsById.get(season.getSchoolId())))
            .toList();
    }

    private List<RecruitingRound> listRounds(List<VisibleSeason> visibleSeasons) {
        return loadRoundPort.listBySeasonIds(visibleSeasons.stream()
                .map(visible -> visible.season().getId())
                .toList())
            .stream()
            .sorted(ROUND_ORDER)
            .toList();
    }

    private record VisibleSeason(RecruitingSeason season, SchoolDetailInfo school) {
    }
}
