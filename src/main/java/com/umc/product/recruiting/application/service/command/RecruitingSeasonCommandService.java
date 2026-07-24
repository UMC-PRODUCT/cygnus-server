package com.umc.product.recruiting.application.service.command;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.umc.product.common.domain.enums.ChallengerTrack;
import com.umc.product.recruiting.application.authorization.RecruitingPolicyAction;
import com.umc.product.recruiting.application.authorization.RecruitingPolicyAuthorizationService;
import com.umc.product.recruiting.application.port.in.command.CreateRecruitingSeasonUseCase;
import com.umc.product.recruiting.application.port.in.command.ReplaceRecruitingSeasonTrackQuotasUseCase;
import com.umc.product.recruiting.application.port.in.command.UpdateRecruitingSeasonUseCase;
import com.umc.product.recruiting.application.port.in.command.dto.CreateRecruitingSeasonCommand;
import com.umc.product.recruiting.application.port.in.command.dto.RecruitingSeasonTrackQuotaCommand;
import com.umc.product.recruiting.application.port.in.command.dto.ReplaceRecruitingSeasonTrackQuotasCommand;
import com.umc.product.recruiting.application.port.in.command.dto.UpdateRecruitingSeasonCommand;
import com.umc.product.recruiting.application.port.out.LoadRecruitingApplicationPort;
import com.umc.product.recruiting.application.port.out.LoadRecruitingRoundPort;
import com.umc.product.recruiting.application.port.out.LoadRecruitingSeasonPort;
import com.umc.product.recruiting.application.port.out.LoadRecruitingSeasonTrackQuotaPort;
import com.umc.product.recruiting.application.port.out.SaveRecruitingSeasonPort;
import com.umc.product.recruiting.application.port.out.SaveRecruitingSeasonTrackQuotaPort;
import com.umc.product.recruiting.domain.RecruitingRound;
import com.umc.product.recruiting.domain.RecruitingSeason;
import com.umc.product.recruiting.domain.RecruitingSeasonTrackQuota;
import com.umc.product.recruiting.domain.exception.RecruitingDomainException;
import com.umc.product.recruiting.domain.exception.RecruitingErrorCode;

import lombok.RequiredArgsConstructor;

@Service
@Transactional
@RequiredArgsConstructor
public class RecruitingSeasonCommandService implements
    CreateRecruitingSeasonUseCase,
    UpdateRecruitingSeasonUseCase,
    ReplaceRecruitingSeasonTrackQuotasUseCase {

    private final LoadRecruitingSeasonPort loadSeasonPort;
    private final SaveRecruitingSeasonPort saveSeasonPort;
    private final LoadRecruitingRoundPort loadRoundPort;
    private final LoadRecruitingApplicationPort loadApplicationPort;
    private final LoadRecruitingSeasonTrackQuotaPort loadQuotaPort;
    private final SaveRecruitingSeasonTrackQuotaPort saveQuotaPort;
    private final RecruitingPolicyAuthorizationService policyAuthorizationService;

    @Override
    public Long createSeason(CreateRecruitingSeasonCommand command) {
        validateSeasonCreationPermission(command);
        if (loadSeasonPort.existsByGisuIdAndSchoolId(command.gisuId(), command.schoolId())) {
            throw new RecruitingDomainException(RecruitingErrorCode.RECRUITING_SEASON_ALREADY_EXISTS);
        }
        validateUniqueTracks(command.quotas());
        RecruitingSeason saved = saveSeasonPort.save(RecruitingSeason.create(command.gisuId(), command.schoolId()));
        saveQuotaPort.saveAll(toQuotas(saved, command.quotas()));
        return saved.getId();
    }

    private void validateSeasonCreationPermission(CreateRecruitingSeasonCommand command) {
        if (policyAuthorizationService.evaluateMember(
            RecruitingPolicyAction.SEASON_CREATE,
            command.requesterMemberId(),
            command.gisuId(),
            command.schoolId()
        )) {
            return;
        }
        throw new RecruitingDomainException(RecruitingErrorCode.RECRUITING_SEASON_CREATION_FORBIDDEN);
    }

    @Override
    public void updateSeason(UpdateRecruitingSeasonCommand command) {
        RecruitingSeason season = loadSeasonPort.getById(command.seasonId());
        season.updateMemo(command.memo());
        saveSeasonPort.save(season);
    }

    @Override
    public void replaceQuotas(ReplaceRecruitingSeasonTrackQuotasCommand command) {
        RecruitingSeason season = loadSeasonPort.getById(command.seasonId());
        validateUniqueTracks(command.quotas());
        List<RecruitingSeasonTrackQuota> currentQuotas = loadQuotaPort.listBySeasonIdForUpdate(command.seasonId());
        validateQuotaTracksCoverRounds(command.seasonId(), command.quotas());
        validateTargetsNotBelowUsage(command.seasonId(), currentQuotas, command.quotas());
        replaceLockedQuotas(season, currentQuotas, command.quotas());
    }

    private void validateUniqueTracks(List<RecruitingSeasonTrackQuotaCommand> quotas) {
        Set<ChallengerTrack> tracks = new HashSet<>();
        for (RecruitingSeasonTrackQuotaCommand quota : quotas) {
            if (quota == null || !tracks.add(quota.track())) {
                throw new RecruitingDomainException(RecruitingErrorCode.RECRUITING_QUOTA_DUPLICATE_TRACK);
            }
        }
    }

    private List<RecruitingSeasonTrackQuota> toQuotas(
        RecruitingSeason season,
        List<RecruitingSeasonTrackQuotaCommand> quotaCommands
    ) {
        return quotaCommands.stream()
            .map(quota -> RecruitingSeasonTrackQuota.create(season, quota.track(), quota.targetCount()))
            .toList();
    }

    private void validateQuotaTracksCoverRounds(Long seasonId, List<RecruitingSeasonTrackQuotaCommand> quotas) {
        Set<ChallengerTrack> quotaTracks = quotas.stream()
            .filter(quota -> quota.targetCount() > 0)
            .map(RecruitingSeasonTrackQuotaCommand::track)
            .collect(java.util.stream.Collectors.toSet());
        boolean unsupportedRoundExists = loadRoundPort.listBySeasonId(seasonId).stream()
            .map(RecruitingRound::getRecruitableTracks)
            .anyMatch(tracks -> !quotaTracks.containsAll(tracks));
        if (unsupportedRoundExists) {
            throw new RecruitingDomainException(RecruitingErrorCode.RECRUITING_ROUND_TRACK_NOT_IN_SEASON);
        }
    }

    private void validateTargetsNotBelowUsage(
        Long seasonId,
        List<RecruitingSeasonTrackQuota> currentQuotas,
        List<RecruitingSeasonTrackQuotaCommand> quotaCommands
    ) {
        Map<ChallengerTrack, Integer> targetByTrack = quotaCommands.stream()
            .collect(java.util.stream.Collectors.toMap(
                RecruitingSeasonTrackQuotaCommand::track,
                RecruitingSeasonTrackQuotaCommand::targetCount
            ));
        for (RecruitingSeasonTrackQuota currentQuota : currentQuotas) {
            long usedCount = loadApplicationPort.countReservedOrRegisteredBySeasonIdAndTrack(
                seasonId,
                currentQuota.getTrack()
            );
            int targetCount = targetByTrack.getOrDefault(currentQuota.getTrack(), 0);
            if (targetCount < usedCount) {
                throw new RecruitingDomainException(RecruitingErrorCode.RECRUITING_QUOTA_BELOW_RESERVED);
            }
        }
    }

    private void replaceLockedQuotas(
        RecruitingSeason season,
        List<RecruitingSeasonTrackQuota> currentQuotas,
        List<RecruitingSeasonTrackQuotaCommand> quotaCommands
    ) {
        Map<ChallengerTrack, Integer> remainingTargets = new LinkedHashMap<>();
        quotaCommands.forEach(command -> remainingTargets.put(command.track(), command.targetCount()));
        List<RecruitingSeasonTrackQuota> toSave = new ArrayList<>();
        List<RecruitingSeasonTrackQuota> toDelete = new ArrayList<>();
        for (RecruitingSeasonTrackQuota currentQuota : currentQuotas) {
            Integer targetCount = remainingTargets.remove(currentQuota.getTrack());
            if (targetCount == null) {
                toDelete.add(currentQuota);
                continue;
            }
            currentQuota.updateTargetCount(targetCount);
            toSave.add(currentQuota);
        }
        remainingTargets.forEach((track, targetCount) -> toSave.add(
            RecruitingSeasonTrackQuota.create(season, track, targetCount)
        ));
        saveQuotaPort.deleteAll(toDelete);
        saveQuotaPort.saveAll(toSave);
    }
}
