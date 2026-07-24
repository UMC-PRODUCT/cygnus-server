package com.umc.product.notice.application.authorization;

import java.util.Objects;

import org.springframework.stereotype.Component;

import com.umc.product.authorization.application.port.in.query.GetChallengerRoleUseCase;
import com.umc.product.authorization.application.service.policy.rollout.PolicyRolloutEvaluator;
import com.umc.product.authorization.domain.AuthoritySnapshot;
import com.umc.product.authorization.domain.RoleAttribute;
import com.umc.product.authorization.domain.SubjectAttributes;
import com.umc.product.authorization.domain.policy.rollout.PolicyRolloutEvaluation;
import com.umc.product.common.domain.enums.ChallengerPart;
import com.umc.product.notice.domain.NoticeTargetInfo;
import com.umc.product.notice.domain.enums.NoticeTab;

@Component
public class LegacyNoticeAuthorizationAdapter
    implements PolicyRolloutEvaluator<NoticeAuthorizationContext, Boolean> {

    private final GetChallengerRoleUseCase challengerRoleUseCase;

    public LegacyNoticeAuthorizationAdapter(GetChallengerRoleUseCase challengerRoleUseCase) {
        this.challengerRoleUseCase = challengerRoleUseCase;
    }

    @Override
    public PolicyRolloutEvaluation<Boolean> evaluate(NoticeAuthorizationContext context) {
        boolean allowed = switch (context.action()) {
            case CREATE -> context.targetPattern()
                .validatePermission(context.target(), context.memberId(), challengerRoleUseCase)
                || challengerRoleUseCase.isSuperAdmin(context.memberId());
            case READ -> canRead(context.legacySubject().orElseThrow(), context.target());
            case UPDATE, DELETE -> context.legacySubject().orElseThrow()
                .toAuthoritySnapshot().isSuperAdmin() || context.author();
            case READ_RECIPIENTS, CHECK_RECIPIENTS ->
                canManage(context.legacySubject().orElseThrow(), context.target());
        };
        return new PolicyRolloutEvaluation.Success<>(allowed);
    }

    private boolean canRead(SubjectAttributes subject, NoticeTargetInfo target) {
        if (subject.toAuthoritySnapshot().isCentralCoreInAnyGisu()) {
            return true;
        }
        if (target.isStaffNotice()) {
            return canReadStaffNotice(subject, target);
        }
        boolean targetChallenger = subject.gisuChallengerInfos().stream()
            .anyMatch(challenger -> target.isTarget(
                challenger.gisuId(),
                challenger.chapterId(),
                subject.schoolId(),
                challenger.part()));
        return targetChallenger
            || subject.roleAttributes().stream().anyMatch(role -> canReadByRole(role, target, subject));
    }

    private boolean canReadStaffNotice(SubjectAttributes subject, NoticeTargetInfo target) {
        return subject.roleAttributes().stream().anyMatch(role -> {
            NoticeTab viewerRole = NoticeTab.findFrom(role.roleType()).orElse(null);
            if (viewerRole == null || !target.targetNoticeTab().includes(viewerRole)) {
                return false;
            }
            if (target.targetGisuId() != null && !target.targetGisuId().equals(role.gisuId())) {
                return false;
            }
            if (target.targetSchoolId() != null
                && !target.targetSchoolId().equals(role.organizationId())) {
                return false;
            }
            if (viewerRole == NoticeTab.SCHOOL_PART_LEADER
                && target.targetParts() != null
                && !target.targetParts().isEmpty()) {
                boolean partFree = role.responsiblePart() == null
                    || role.responsiblePart() == ChallengerPart.ADMIN;
                return partFree || target.targetParts().contains(role.responsiblePart());
            }
            return true;
        });
    }

    private boolean canReadByRole(
        RoleAttribute role,
        NoticeTargetInfo target,
        SubjectAttributes subject
    ) {
        return switch (role.roleType()) {
            case CENTRAL_OPERATING_TEAM_MEMBER, CENTRAL_EDUCATION_TEAM_MEMBER ->
                target.targetGisuId() == null || target.targetGisuId().equals(role.gisuId());
            case CHAPTER_PRESIDENT -> chapterPresidentCanRead(role, target, subject);
            case SCHOOL_PRESIDENT, SCHOOL_VICE_PRESIDENT ->
                schoolCoreCanRead(role, target, subject);
            case SCHOOL_PART_LEADER -> schoolPartLeaderCanRead(role, target, subject);
            default -> false;
        };
    }

    private boolean chapterPresidentCanRead(
        RoleAttribute role,
        NoticeTargetInfo target,
        SubjectAttributes subject
    ) {
        Long chapterId = role.organizationId();
        if (chapterId == null) {
            return false;
        }
        if (target.targetChapterId() != null
            && !chapterId.equals(target.targetChapterId())) {
            return false;
        }
        if (target.targetSchoolId() != null
            && !Objects.equals(subject.schoolId(), target.targetSchoolId())) {
            return false;
        }
        if (target.targetChapterId() == null && target.targetSchoolId() == null) {
            return false;
        }
        return subject.gisuChallengerInfos().stream()
            .filter(info -> chapterId.equals(info.chapterId()))
            .filter(info -> role.gisuId().equals(info.gisuId()))
            .anyMatch(info -> target.targetGisuId() == null
                || target.targetGisuId().equals(info.gisuId()));
    }

    private boolean schoolCoreCanRead(
        RoleAttribute role,
        NoticeTargetInfo target,
        SubjectAttributes subject
    ) {
        if (role.organizationId() == null) {
            return false;
        }
        if (target.targetSchoolId() != null
            && !target.targetSchoolId().equals(role.organizationId())) {
            return false;
        }
        return isInGisuAndChapter(role, target, subject);
    }

    private boolean schoolPartLeaderCanRead(
        RoleAttribute role,
        NoticeTargetInfo target,
        SubjectAttributes subject
    ) {
        if (role.responsiblePart() == null || role.organizationId() == null) {
            return false;
        }
        if (target.targetSchoolId() != null
            && !target.targetSchoolId().equals(role.organizationId())) {
            return false;
        }
        if (target.targetParts() == null
            || target.targetParts().isEmpty()
            || !target.targetParts().contains(role.responsiblePart())) {
            return false;
        }
        return isInGisuAndChapter(role, target, subject);
    }

    private boolean isInGisuAndChapter(
        RoleAttribute role,
        NoticeTargetInfo target,
        SubjectAttributes subject
    ) {
        return subject.gisuChallengerInfos().stream()
            .filter(info -> role.gisuId().equals(info.gisuId()))
            .anyMatch(info ->
                (target.targetGisuId() == null || target.targetGisuId().equals(info.gisuId()))
                    && (target.targetChapterId() == null
                        || target.targetChapterId().equals(info.chapterId())));
    }

    private boolean canManage(SubjectAttributes subject, NoticeTargetInfo target) {
        AuthoritySnapshot snapshot = subject.toAuthoritySnapshot();
        if (snapshot.isCentralCoreInAnyGisu()) {
            return true;
        }
        if (target.targetSchoolId() != null) {
            return snapshot.isSchoolAdminInAnyGisu(target.targetSchoolId());
        }
        if (target.targetChapterId() != null) {
            return snapshot.isChapterPresidentInAnyGisu(target.targetChapterId());
        }
        return snapshot.isCentralMemberInAnyGisu();
    }
}
