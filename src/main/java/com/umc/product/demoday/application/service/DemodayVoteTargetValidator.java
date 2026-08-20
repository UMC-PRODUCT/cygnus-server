package com.umc.product.demoday.application.service;

import static java.util.stream.Collectors.toUnmodifiableSet;

import java.util.List;
import java.util.Objects;
import java.util.Set;

import org.springframework.stereotype.Component;

import com.umc.product.demoday.application.port.in.query.participant.DemodayParticipant;
import com.umc.product.demoday.application.port.in.query.participant.DemodayParticipantType;
import com.umc.product.demoday.domain.DemodayBooth;
import com.umc.product.demoday.domain.exception.DemodayDomainException;
import com.umc.product.demoday.domain.exception.DemodayErrorCode;
import com.umc.product.project.application.port.in.query.ListProjectParticipationUseCase;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class DemodayVoteTargetValidator {

    private final ListProjectParticipationUseCase listProjectParticipationUseCase;

    public List<DemodayBooth> filterEligibleBooths(
        List<DemodayBooth> booths,
        DemodayParticipant participant
    ) {
        if (participant.participantType() == DemodayParticipantType.GUEST) {
            return List.copyOf(booths);
        }

        Set<Long> participantProjectIds = findParticipantProjectIds(booths, participant.participantId());

        return booths.stream()
            .filter(booth -> booth.getProjectId() == null
                || !participantProjectIds.contains(booth.getProjectId()))
            .toList();
    }

    public void validateEligibleBooth(DemodayBooth booth, DemodayParticipant participant) {
        if (participant.participantType() == DemodayParticipantType.GUEST || booth.getProjectId() == null) {
            return;
        }

        Set<Long> participatingProjectIds = listProjectParticipationUseCase.listParticipatingProjectIds(
            Set.of(booth.getProjectId()),
            participant.participantId());

        if (participatingProjectIds.contains(booth.getProjectId())) {
            throw new DemodayDomainException(DemodayErrorCode.DEMODAY_VOTE_OWN_BOOTH_FORBIDDEN);
        }
    }

    private Set<Long> findParticipantProjectIds(List<DemodayBooth> booths, Long memberId) {
        Set<Long> projectIds = booths.stream()
            .map(DemodayBooth::getProjectId)
            .filter(Objects::nonNull)
            .collect(toUnmodifiableSet());

        if (projectIds.isEmpty()) {
            return Set.of();
        }

        return listProjectParticipationUseCase.listParticipatingProjectIds(projectIds, memberId);
    }
}
