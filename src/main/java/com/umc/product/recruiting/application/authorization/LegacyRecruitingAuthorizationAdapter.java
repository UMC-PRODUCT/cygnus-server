package com.umc.product.recruiting.application.authorization;

import org.springframework.stereotype.Component;

import com.umc.product.authorization.application.port.in.query.GetChallengerRoleUseCase;
import com.umc.product.authorization.application.service.policy.rollout.PolicyRolloutEvaluator;
import com.umc.product.authorization.domain.SubjectAttributes;
import com.umc.product.authorization.domain.policy.rollout.PolicyRolloutEvaluation;
import com.umc.product.common.domain.enums.ChallengerRoleType;

@Component
public class LegacyRecruitingAuthorizationAdapter
    implements PolicyRolloutEvaluator<RecruitingAuthorizationContext, Boolean> {

    private final GetChallengerRoleUseCase challengerRoleUseCase;

    public LegacyRecruitingAuthorizationAdapter(
        GetChallengerRoleUseCase challengerRoleUseCase
    ) {
        this.challengerRoleUseCase = challengerRoleUseCase;
    }

    @Override
    public PolicyRolloutEvaluation<Boolean> evaluate(RecruitingAuthorizationContext context) {
        boolean allowed = context.legacySubject()
            .map(subject -> evaluateSubject(context, subject))
            .orElseGet(() -> evaluateMember(context));
        return new PolicyRolloutEvaluation.Success<>(allowed);
    }

    private boolean evaluateSubject(
        RecruitingAuthorizationContext context,
        SubjectAttributes subject
    ) {
        var authority = subject.toAuthoritySnapshot();
        return switch (context.action()) {
            case OPERATE_SCHOOL -> context.resourceSpecified()
                ? authority.isCentralCoreInGisu(context.targetGisuId())
                    || authority.isSchoolCoreInGisu(
                        context.targetGisuId(),
                        context.targetSchoolId())
                : authority.isCentralCoreInAnyGisu()
                    || subject.roleAttributes().stream()
                        .map(role -> role.roleType())
                        .anyMatch(this::isSchoolCore);
            case MANAGE_ALL -> context.resourceSpecified()
                ? authority.isCentralCoreInGisu(context.targetGisuId())
                : authority.isCentralCoreInAnyGisu();
            case SEASON_CREATE, APPLICATION_DECIDE ->
                authority.isCentralCoreInGisu(context.targetGisuId())
                    || authority.isSchoolCoreInGisu(
                        context.targetGisuId(),
                        context.targetSchoolId());
            case REGISTRATION_MANAGE, SUMMARY_READ, CSV_EXPORT ->
                authority.isCentralCoreInGisu(context.targetGisuId());
        };
    }

    private boolean evaluateMember(RecruitingAuthorizationContext context) {
        return switch (context.action()) {
            case OPERATE_SCHOOL -> context.resourceSpecified()
                ? centralCoreInTarget(context) || schoolCoreForTarget(context)
                : anyCentralCore(context) || anySchoolCore(context);
            case MANAGE_ALL -> context.resourceSpecified()
                ? centralCoreInTarget(context)
                : anyCentralCore(context);
            case SEASON_CREATE, APPLICATION_DECIDE ->
                centralCoreInTarget(context) || schoolCoreForTarget(context);
            case REGISTRATION_MANAGE, SUMMARY_READ, CSV_EXPORT ->
                centralCoreInTarget(context);
        };
    }

    private boolean anyCentralCore(RecruitingAuthorizationContext context) {
        return challengerRoleUseCase.isCentralCoreInAnyGisu(context.memberId());
    }

    private boolean anySchoolCore(RecruitingAuthorizationContext context) {
        return challengerRoleUseCase.findAllByMemberId(context.memberId()).stream()
            .map(role -> role.roleType())
            .anyMatch(this::isSchoolCore);
    }

    private boolean centralCoreInTarget(RecruitingAuthorizationContext context) {
        return challengerRoleUseCase.isCentralCoreInGisu(
            context.memberId(),
            context.targetGisuId());
    }

    private boolean schoolCoreForTarget(RecruitingAuthorizationContext context) {
        return challengerRoleUseCase.isSchoolCoreInGisu(
            context.memberId(),
            context.targetGisuId(),
            context.targetSchoolId());
    }

    private boolean isSchoolCore(ChallengerRoleType roleType) {
        return roleType == ChallengerRoleType.SCHOOL_PRESIDENT
            || roleType == ChallengerRoleType.SCHOOL_VICE_PRESIDENT;
    }
}
