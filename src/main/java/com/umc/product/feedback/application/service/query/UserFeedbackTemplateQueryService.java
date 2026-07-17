package com.umc.product.feedback.application.service.query;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.umc.product.authorization.application.port.in.query.GetChallengerRoleUseCase;
import com.umc.product.challenger.application.port.in.query.GetChallengerUseCase;
import com.umc.product.challenger.application.port.in.query.dto.ChallengerInfo;
import com.umc.product.common.domain.enums.ChallengerPart;
import com.umc.product.feedback.application.policy.FeedbackTemplateOwnerReferenceFactory;
import com.umc.product.feedback.application.port.in.query.GetUserFeedbackTemplateUseCase;
import com.umc.product.feedback.application.port.in.query.dto.UserFeedbackTemplateInfo;
import com.umc.product.feedback.application.port.out.LoadUserFeedbackTemplatePort;
import com.umc.product.feedback.domain.enums.UserFeedbackContext;
import com.umc.product.feedback.domain.enums.UserFeedbackTargetType;
import com.umc.product.form.application.port.in.FormActorContext;
import com.umc.product.form.application.port.in.query.GetFormUseCase;
import com.umc.product.organization.application.port.in.query.GetGisuUseCase;

import lombok.RequiredArgsConstructor;

/**
 * 사용자 피드백 템플릿 조회 서비스.
 * <p>
 * 요청자의 챌린저 이력과 운영진 역할을 기반으로 UserFeedbackTargetType을 판별하고,
 * 해당 context + targetType 조합의 활성 UserFeedbackTemplate을 조회하여 Form 폼 구조와 함께 반환합니다.
 * <p>
 * <b>이번 기수 특수 케이스:</b> 신규 PM(기획 파트)은 이번 기수에서 기획-디자인 매칭을 경험했으므로 챌린저 이력이 이번 기수 하나뿐이더라도 EXPERIENCED_CHALLENGER로 분류합니다.
 * 이 판별 기준은 코드에 하드코딩되어 있어, 기수별 기준 변경 시 배포가 필요합니다.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserFeedbackTemplateQueryService
    implements GetUserFeedbackTemplateUseCase {

    private final LoadUserFeedbackTemplatePort loadUserFeedbackTemplatePort;

    // Cross-domain
    private final GetGisuUseCase getGisuUseCase;
    private final GetChallengerUseCase getChallengerUseCase;
    private final GetChallengerRoleUseCase getChallengerRoleUseCase;
    private final GetFormUseCase getFormUseCase;
    private final FeedbackTemplateOwnerReferenceFactory ownerReferenceFactory;

    @Override
    public Optional<UserFeedbackTemplateInfo> findTemplate(
        FormActorContext actorContext,
        UserFeedbackContext context
    ) {
        if (actorContext == null || context == null) {
            return Optional.empty();
        }
        Optional<Long> memberId = actorContext.authenticatedMemberId();
        if (memberId.isEmpty()) {
            return Optional.empty();
        }

        return getGisuUseCase.findActiveGisu()
            .flatMap(gisu -> resolveTargetType(memberId.get(), gisu.gisuId(), gisu.generation())
                .flatMap(targetType -> loadUserFeedbackTemplatePort.findByContextAndTargetType(context, targetType)
                    .map(template -> UserFeedbackTemplateInfo.builder()
                        .templateId(template.getId())
                        .context(template.getContext())
                        .targetType(template.getTargetType())
                        .form(getFormUseCase.getFormWithStructure(
                            ownerReferenceFactory.create(template.getId(), template.getFormId()),
                            actorContext
                        ))
                        .build())));
    }

    /**
     * 요청자의 UserFeedbackTargetType을 판별합니다.
     * <p>
     * 판별 순서:
     * <ol>
     *   <li>현재 기수 중앙 운영진(isCentralMemberInGisu) -> ADMIN</li>
     *   <li>현재 기수 활성 챌린저가 아닌 경우 -> Optional.empty()</li>
     *   <li>이번 기수 외 챌린저 이력 보유 -> EXPERIENCED_CHALLENGER</li>
     *   <li>10기 한정: PM(기획) 파트 -> EXPERIENCED_CHALLENGER (기획-디자인 매칭 경험 특수 케이스)</li>
     *   <li>그 외 -> NEW_CHALLENGER</li>
     * </ol>
     */
    private Optional<UserFeedbackTargetType> resolveTargetType(Long memberId, Long activeGisuId, Long generation) {
        if (getChallengerRoleUseCase.isCentralMemberInGisu(memberId, activeGisuId)) {
            return Optional.of(UserFeedbackTargetType.ADMIN);
        }

        List<ChallengerInfo> allHistory = getChallengerUseCase.getAllByMemberId(memberId);

        boolean isActiveChallenger = allHistory.stream()
            .anyMatch(c -> c.gisuId().equals(activeGisuId));
        if (!isActiveChallenger) {
            return Optional.empty();
        }

        boolean hasPreviousGisu = allHistory.stream()
            .anyMatch(c -> !c.gisuId().equals(activeGisuId));
        if (hasPreviousGisu) {
            return Optional.of(UserFeedbackTargetType.EXPERIENCED_CHALLENGER);
        }

        // 10기 한정: 기획-디자인 매칭을 경험한 PM은 챌린저 이력이 이번 기수 하나뿐이더라도 EXPERIENCED_CHALLENGER
        if (Long.valueOf(10).equals(generation)) {
            boolean isPm = allHistory.stream()
                .filter(c -> c.gisuId().equals(activeGisuId))
                .anyMatch(c -> c.part() == ChallengerPart.PLAN);
            if (isPm) {
                return Optional.of(UserFeedbackTargetType.EXPERIENCED_CHALLENGER);
            }
        }

        return Optional.of(UserFeedbackTargetType.NEW_CHALLENGER);
    }
}
