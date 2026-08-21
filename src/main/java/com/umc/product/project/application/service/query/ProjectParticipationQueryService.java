package com.umc.product.project.application.service.query;

import static java.util.stream.Collectors.toCollection;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.umc.product.project.application.port.in.query.ListProjectParticipationUseCase;
import com.umc.product.project.application.port.out.LoadProjectMemberPort;
import com.umc.product.project.application.port.out.LoadProjectPort;
import com.umc.product.project.domain.Project;
import com.umc.product.project.domain.ProjectMember;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProjectParticipationQueryService implements ListProjectParticipationUseCase {

    private final LoadProjectPort loadProjectPort;
    private final LoadProjectMemberPort loadProjectMemberPort;

    @Override
    public Set<Long> listParticipatingProjectIds(Collection<Long> projectIds, Long memberId) {
        Set<Long> uniqueProjectIds = projectIds.stream()
            .filter(Objects::nonNull)
            .collect(toCollection(LinkedHashSet::new));

        if (uniqueProjectIds.isEmpty()) {
            return Set.of();
        }

        Set<Long> participatingProjectIds = loadProjectPort.listByIds(uniqueProjectIds).stream()
            .filter(project -> Objects.equals(project.getProductOwnerMemberId(), memberId))
            .map(Project::getId)
            .collect(toCollection(LinkedHashSet::new));

        Map<Long, List<ProjectMember>> membersByProjectId =
            loadProjectMemberPort.listByProjectIds(uniqueProjectIds);
        membersByProjectId.forEach((projectId, members) -> {
            boolean participatesAsMember = members.stream()
                .map(ProjectMember::getMemberId)
                .anyMatch(projectMemberId -> Objects.equals(projectMemberId, memberId));
            if (participatesAsMember) {
                participatingProjectIds.add(projectId);
            }
        });

        return Collections.unmodifiableSet(participatingProjectIds);
    }
}
