package com.umc.product.project.application.service.policy;

import org.springframework.stereotype.Component;

import com.umc.product.authorization.domain.policy.PolicyEffect;
import com.umc.product.organization.application.port.in.query.dto.chapter.ChapterScopeInfo;
import com.umc.product.project.application.authorization.ProjectPolicyAction;
import com.umc.product.project.application.authorization.ProjectPolicyAuthorizationService;
import com.umc.product.project.application.authorization.ProjectPolicyResourceContext;
import com.umc.product.project.application.authorization.ProjectPolicySubjectSnapshot;
import com.umc.product.project.domain.Project;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class ProjectStatisticsAccessPolicy {

    private final ProjectPolicyAuthorizationService policyAuthorizationService;

    public ProjectPolicySubjectSnapshot snapshot(long memberId) {
        return policyAuthorizationService.snapshot(memberId);
    }

    public boolean canReadProjectStatistics(
        ProjectPolicySubjectSnapshot snapshot,
        Project project,
        boolean activePlanMember
    ) {
        return policyAuthorizationService.evaluate(
            snapshot,
            ProjectPolicyAction.STATISTICS_PROJECT,
            ProjectPolicyResourceContext.builder()
                .project(project.getId(), project.getGisuId(), project.getChapterId(), project.getStatus())
                .productOwnerMemberId(project.getProductOwnerMemberId())
                .activePlanMember(activePlanMember)
                .build()
        ).effect() == PolicyEffect.ALLOW;
    }

    public boolean canReadChapterStatistics(
        ProjectPolicySubjectSnapshot snapshot,
        ChapterScopeInfo chapter
    ) {
        return policyAuthorizationService.evaluate(
            snapshot,
            ProjectPolicyAction.STATISTICS_CHAPTER,
            ProjectPolicyResourceContext.builder()
                .chapterScope(chapter.gisuId(), chapter.chapterId())
                .build()
        ).effect() == PolicyEffect.ALLOW;
    }

    public boolean canReadPublicMatchingStatistics(
        ProjectPolicySubjectSnapshot snapshot,
        ChapterScopeInfo chapter
    ) {
        return policyAuthorizationService.evaluate(
            snapshot,
            ProjectPolicyAction.STATISTICS_PUBLIC_MATCHING,
            ProjectPolicyResourceContext.builder()
                .chapterScope(chapter.gisuId(), chapter.chapterId())
                .build()
        ).effect() == PolicyEffect.ALLOW;
    }
}
