package com.umc.product.project.application.service.policy;

import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import com.umc.product.authorization.application.port.in.query.GetChallengerRoleUseCase;
import com.umc.product.authorization.application.port.in.query.dto.ChallengerRoleInfo;
import com.umc.product.common.domain.enums.ChallengerRoleType;
import com.umc.product.organization.application.port.in.query.GetChapterUseCase;
import com.umc.product.project.application.port.out.LoadProjectMemberPort;
import com.umc.product.project.domain.Project;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class ProjectStatisticsAccessPolicy {

    private final LoadProjectMemberPort loadProjectMemberPort;
    private final GetChallengerRoleUseCase getChallengerRoleUseCase;
    private final GetChapterUseCase getChapterUseCase;

    public boolean canReadProjectStatistics(Long memberId, Project project) {
        if (Objects.equals(project.getProductOwnerMemberId(), memberId)) {
            return true;
        }
        if (loadProjectMemberPort.isActivePlanMember(project.getId(), memberId)) {
            return true;
        }
        return canReadChapterStatistics(memberId, project.getChapterId());
    }

    public boolean canReadChapterStatistics(Long memberId, Long chapterId) {
        if (getChallengerRoleUseCase.isSuperAdmin(memberId)) {
            return true;
        }

        List<ChallengerRoleInfo> roles = getChallengerRoleUseCase.findAllByMemberId(memberId);
        return roles.stream().anyMatch(role -> role.roleType().isAtLeastCentralCore()
                || (role.roleType() == ChallengerRoleType.CHAPTER_PRESIDENT
                    && Objects.equals(role.organizationId(), chapterId)))
            || isSchoolCoreOfChapter(roles, chapterId);
    }

    private boolean isSchoolCoreOfChapter(List<ChallengerRoleInfo> roles, Long chapterId) {
        Set<Long> schoolIds = roles.stream()
            .filter(role -> role.roleType() == ChallengerRoleType.SCHOOL_PRESIDENT
                || role.roleType() == ChallengerRoleType.SCHOOL_VICE_PRESIDENT)
            .map(ChallengerRoleInfo::organizationId)
            .filter(Objects::nonNull)
            .collect(Collectors.toSet());
        if (schoolIds.isEmpty()) {
            return false;
        }
        return getChapterUseCase.getChaptersBySchoolIds(schoolIds).stream()
            .anyMatch(chapter -> Objects.equals(chapter.id(), chapterId));
    }
}
