package com.umc.product.recruiting.application.service.query;

import java.time.Clock;
import java.time.Instant;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.umc.product.authorization.application.port.in.CheckPermissionUseCase;
import com.umc.product.authorization.domain.PermissionType;
import com.umc.product.authorization.domain.ResourcePermission;
import com.umc.product.authorization.domain.ResourceType;
import com.umc.product.authorization.domain.SubjectAttributes;
import com.umc.product.organization.application.port.in.query.GetSchoolUseCase;
import com.umc.product.organization.application.port.in.query.dto.school.SchoolDetailInfo;
import com.umc.product.recruiting.application.port.in.query.CheckRecruitingRoundTitleUseCase;
import com.umc.product.recruiting.application.port.in.query.GetRecruitingSeasonConfigurationUseCase;
import com.umc.product.recruiting.application.port.in.query.SearchPublicRecruitingRoundUseCase;
import com.umc.product.recruiting.application.port.in.query.SearchRecruitingRoundGroupUseCase;
import com.umc.product.recruiting.application.port.in.query.SearchRecruitingRoundUseCase;
import com.umc.product.recruiting.application.port.in.query.SearchRecruitingSeasonUseCase;
import com.umc.product.recruiting.application.port.in.query.dto.RecruitingPublicRoundGroupInfo;
import com.umc.product.recruiting.application.port.in.query.dto.RecruitingPublicRoundInfo;
import com.umc.product.recruiting.application.port.in.query.dto.RecruitingPublicRoundSearchQuery;
import com.umc.product.recruiting.application.port.in.query.dto.RecruitingRoundConfigurationInfo;
import com.umc.product.recruiting.application.port.in.query.dto.RecruitingRoundGroupSearchQuery;
import com.umc.product.recruiting.application.port.in.query.dto.RecruitingRoundSearchQuery;
import com.umc.product.recruiting.application.port.in.query.dto.RecruitingRoundSummaryInfo;
import com.umc.product.recruiting.application.port.in.query.dto.RecruitingSeasonConfigurationInfo;
import com.umc.product.recruiting.application.port.in.query.dto.RecruitingSeasonSearchQuery;
import com.umc.product.recruiting.application.port.in.query.dto.RecruitingSeasonSummaryInfo;
import com.umc.product.recruiting.application.port.in.query.dto.RecruitingSeasonTrackQuotaInfo;
import com.umc.product.recruiting.application.port.out.LoadRecruitingApplicationFormPort;
import com.umc.product.recruiting.application.port.out.LoadRecruitingRoundPort;
import com.umc.product.recruiting.application.port.out.LoadRecruitingSeasonPort;
import com.umc.product.recruiting.application.port.out.LoadRecruitingSeasonTrackQuotaPort;
import com.umc.product.recruiting.domain.RecruitingApplicationForm;
import com.umc.product.recruiting.domain.RecruitingRound;
import com.umc.product.recruiting.domain.RecruitingSeason;
import com.umc.product.recruiting.domain.enums.RecruitingApplicationFormStatus;
import com.umc.product.recruiting.domain.enums.RecruitingRoundPhase;
import com.umc.product.recruiting.domain.enums.RecruitingRoundSort;
import com.umc.product.recruiting.domain.enums.RecruitingRoundStatus;
import com.umc.product.recruiting.domain.enums.RecruitingRoundType;

import lombok.RequiredArgsConstructor;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class RecruitingSeasonQueryService implements
    GetRecruitingSeasonConfigurationUseCase,
    SearchRecruitingSeasonUseCase,
    SearchRecruitingRoundUseCase,
    SearchRecruitingRoundGroupUseCase,
    SearchPublicRecruitingRoundUseCase,
    CheckRecruitingRoundTitleUseCase {

    private static final Comparator<RecruitingRound> ROUND_ORDER = Comparator
        .comparing((RecruitingRound round) -> round.getSeason().getSchoolId())
        .thenComparing(round -> round.getSeason().getId())
        .thenComparingInt(round -> round.getType() == RecruitingRoundType.REGULAR ? 0 : 1)
        .thenComparing(RecruitingRound::getRoundNo)
        .thenComparing(RecruitingRound::getId);

    private final LoadRecruitingSeasonPort loadSeasonPort;
    private final LoadRecruitingSeasonTrackQuotaPort loadQuotaPort;
    private final LoadRecruitingRoundPort loadRoundPort;
    private final LoadRecruitingApplicationFormPort loadApplicationFormPort;
    private final GetSchoolUseCase getSchoolUseCase;
    private final CheckPermissionUseCase checkPermissionUseCase;
    private final Clock clock;

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

    @Override
    public List<RecruitingSeasonSummaryInfo> searchRoundGroups(RecruitingRoundGroupSearchQuery query) {
        List<VisibleSeason> visibleSeasons = listVisibleSeasons(
            query.gisuId(),
            query.chapterId(),
            query.schoolId(),
            query.seasonId(),
            query.requesterMemberId()
        );
        List<RecruitingRound> rounds = sortRounds(filterRounds(listRounds(visibleSeasons), query.track()), query.effectiveSort());
        Map<Long, VisibleSeason> seasonById = visibleSeasons.stream()
            .collect(Collectors.toMap(visible -> visible.season().getId(), Function.identity()));
        Map<Long, List<RecruitingRoundConfigurationInfo>> roundsBySeason = rounds.stream()
            .collect(Collectors.groupingBy(
                round -> round.getSeason().getId(),
                LinkedHashMap::new,
                Collectors.mapping(RecruitingRoundConfigurationInfo::from, Collectors.toList())
            ));
        List<RecruitingSeasonSummaryInfo> populatedSeasons = roundsBySeason.entrySet().stream()
            .map(entry -> toSummary(seasonById.get(entry.getKey()), entry.getValue()))
            .toList();
        List<RecruitingSeasonSummaryInfo> emptySeasons = visibleSeasons.stream()
            .filter(visible -> !roundsBySeason.containsKey(visible.season().getId()))
            .map(visible -> toSummary(visible, List.of()))
            .toList();
        return Stream.concat(populatedSeasons.stream(), emptySeasons.stream()).toList();
    }

    private RecruitingSeasonSummaryInfo toSummary(
        VisibleSeason visible,
        List<RecruitingRoundConfigurationInfo> rounds
    ) {
        return RecruitingSeasonSummaryInfo.of(
            visible.season(),
            visible.school().chapterId(),
            visible.school().chapterName(),
            visible.school().schoolName(),
            rounds
        );
    }

    @Override
    public List<RecruitingPublicRoundGroupInfo> searchPublicRounds(RecruitingPublicRoundSearchQuery query) {
        List<VisibleSeason> seasons = listPublicSeasons(
            query.gisuId(),
            query.chapterId(),
            query.schoolIds(),
            query.schoolName(),
            query.seasonId()
        );
        List<RecruitingRound> candidateRounds = filterRounds(listRounds(seasons), query.track()).stream()
            .filter(round -> query.roundIds().isEmpty() || query.roundIds().contains(round.getId()))
            .filter(round -> round.getStatus() != RecruitingRoundStatus.DRAFT)
            .toList();
        Map<Long, RecruitingApplicationForm> formByRoundId = loadApplicationFormPort.listByRoundIds(
                candidateRounds.stream().map(RecruitingRound::getId).toList()
            ).stream()
            .filter(form -> form.getStatus() != RecruitingApplicationFormStatus.DRAFT)
            .collect(Collectors.toMap(form -> form.getRound().getId(), Function.identity()));
        Instant now = Instant.now(clock);
        List<RecruitingRound> rounds = sortRounds(candidateRounds.stream()
            .filter(round -> formByRoundId.containsKey(round.getId()))
            .filter(round -> matchesPhase(round, formByRoundId.get(round.getId()), query.effectivePhase(), now))
            .toList(), query.effectiveSort());
        Map<Long, VisibleSeason> seasonById = seasons.stream()
            .collect(Collectors.toMap(visible -> visible.season().getId(), Function.identity()));
        Map<Long, List<RecruitingPublicRoundInfo>> roundsBySeason = rounds.stream()
            .collect(Collectors.groupingBy(
                round -> round.getSeason().getId(),
                LinkedHashMap::new,
                Collectors.mapping(round -> {
                    RecruitingApplicationForm form = formByRoundId.get(round.getId());
                    return RecruitingPublicRoundInfo.of(
                        round,
                        form,
                        round.isLocalApplicationPeriodOpenAt(now, form.getStatus())
                    );
                }, Collectors.toList())
            ));
        return roundsBySeason.entrySet().stream()
            .map(entry -> {
                VisibleSeason visible = seasonById.get(entry.getKey());
                return new RecruitingPublicRoundGroupInfo(
                    visible.season().getId(),
                    visible.season().getGisuId(),
                    visible.school().chapterId(),
                    visible.school().chapterName(),
                    visible.season().getSchoolId(),
                    visible.school().schoolName(),
                    entry.getValue()
                );
            })
            .toList();
    }

    @Override
    public boolean isTitleAvailable(Long seasonId, String title, Long excludedRoundId) {
        String normalizedTitle = RecruitingRound.normalizeTitle(title);
        return excludedRoundId == null
            ? !loadRoundPort.existsBySeasonIdAndTitleIgnoreCase(seasonId, normalizedTitle)
            : !loadRoundPort.existsBySeasonIdAndTitleIgnoreCaseAndIdNot(seasonId, normalizedTitle, excludedRoundId);
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

    private List<VisibleSeason> listPublicSeasons(
        Long gisuId,
        Long chapterId,
        Set<Long> schoolIds,
        String schoolName,
        Long seasonId
    ) {
        String normalizedSchoolName = schoolName == null ? null : schoolName.toLowerCase(Locale.ROOT);
        Map<Long, SchoolDetailInfo> schoolsById = getSchoolUseCase.getSchoolListByGisuId(gisuId).stream()
            .filter(school -> chapterId == null || chapterId.equals(school.chapterId()))
            .filter(school -> schoolIds.isEmpty() || schoolIds.contains(school.schoolId()))
            .filter(school -> normalizedSchoolName == null
                || school.schoolName().toLowerCase(Locale.ROOT).contains(normalizedSchoolName))
            .collect(Collectors.toMap(SchoolDetailInfo::schoolId, Function.identity()));
        return loadSeasonPort.listByGisuId(gisuId).stream()
            .filter(season -> seasonId == null || seasonId.equals(season.getId()))
            .filter(season -> schoolsById.containsKey(season.getSchoolId()))
            .map(season -> new VisibleSeason(season, schoolsById.get(season.getSchoolId())))
            .toList();
    }

    private List<RecruitingRound> filterRounds(
        List<RecruitingRound> rounds,
        com.umc.product.common.domain.enums.ChallengerTrack track
    ) {
        return rounds.stream()
            .filter(round -> track == null || round.getRecruitableTracks().contains(track))
            .toList();
    }

    private List<RecruitingRound> sortRounds(List<RecruitingRound> rounds, RecruitingRoundSort sort) {
        Comparator<RecruitingRound> comparator = switch (sort) {
            case NEWEST -> Comparator.comparing(
                RecruitingRound::getCreatedAt,
                Comparator.nullsLast(Comparator.reverseOrder())
            );
            case REGISTERED -> Comparator.comparing(
                RecruitingRound::getCreatedAt,
                Comparator.nullsLast(Comparator.naturalOrder())
            );
            case RECRUITMENT -> Comparator.comparing(
                RecruitingRound::getDocumentStartAt,
                Comparator.nullsLast(Comparator.naturalOrder())
            );
        };
        return rounds.stream()
            .sorted(comparator.thenComparing(RecruitingRound::getId))
            .toList();
    }

    private boolean matchesPhase(
        RecruitingRound round,
        RecruitingApplicationForm form,
        RecruitingRoundPhase phase,
        Instant now
    ) {
        if (phase == RecruitingRoundPhase.OPEN) {
            return round.isLocalApplicationPeriodOpenAt(now, form.getStatus());
        }
        return round.getStatus() == RecruitingRoundStatus.CLOSED
            || (round.getDocumentEndAt() != null && !now.isBefore(round.getDocumentEndAt()));
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
