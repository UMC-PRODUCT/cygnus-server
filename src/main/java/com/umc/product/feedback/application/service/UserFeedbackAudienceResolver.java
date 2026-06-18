package com.umc.product.feedback.application.service;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.umc.product.authorization.application.port.in.query.GetChallengerRoleUseCase;
import com.umc.product.challenger.application.port.in.query.GetChallengerUseCase;
import com.umc.product.challenger.application.port.in.query.dto.ChallengerInfo;
import com.umc.product.common.domain.enums.ChallengerPart;
import com.umc.product.feedback.domain.enums.UserFeedbackTargetType;
import com.umc.product.organization.application.port.in.query.GetGisuUseCase;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserFeedbackAudienceResolver {

    private static final long PLAN_EXPERIENCED_GENERATION = 10L;

    private final GetGisuUseCase getGisuUseCase;
    private final GetChallengerUseCase getChallengerUseCase;
    private final GetChallengerRoleUseCase getChallengerRoleUseCase;

    public Optional<UserFeedbackTargetType> resolve(Long memberId) {
        return getGisuUseCase.findActiveGisu()
            .flatMap(gisu -> resolve(memberId, gisu.gisuId(), gisu.generation()));
    }

    private Optional<UserFeedbackTargetType> resolve(Long memberId, Long activeGisuId, Long generation) {
        if (getChallengerRoleUseCase.isCentralMemberInGisu(memberId, activeGisuId)) {
            return Optional.of(UserFeedbackTargetType.ADMIN);
        }

        List<ChallengerInfo> allHistory = getChallengerUseCase.getAllByMemberId(memberId);
        boolean isActiveChallenger = allHistory.stream()
            .anyMatch(challenger -> challenger.gisuId().equals(activeGisuId));
        if (!isActiveChallenger) {
            return Optional.empty();
        }

        boolean hasPreviousGisu = allHistory.stream()
            .anyMatch(challenger -> !challenger.gisuId().equals(activeGisuId));
        if (hasPreviousGisu) {
            return Optional.of(UserFeedbackTargetType.EXPERIENCED_CHALLENGER);
        }

        if (Long.valueOf(PLAN_EXPERIENCED_GENERATION).equals(generation) && isPlanMember(allHistory, activeGisuId)) {
            return Optional.of(UserFeedbackTargetType.EXPERIENCED_CHALLENGER);
        }

        return Optional.of(UserFeedbackTargetType.NEW_CHALLENGER);
    }

    private boolean isPlanMember(List<ChallengerInfo> allHistory, Long activeGisuId) {
        return allHistory.stream()
            .filter(challenger -> challenger.gisuId().equals(activeGisuId))
            .anyMatch(challenger -> challenger.part() == ChallengerPart.PLAN);
    }
}
