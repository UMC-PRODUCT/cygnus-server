package com.umc.product.project.application.service.query;

import java.time.Instant;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.umc.product.authorization.domain.policy.PolicyEffect;
import com.umc.product.project.application.authorization.ProjectPolicyAction;
import com.umc.product.project.application.authorization.ProjectPolicyAuthorizationService;
import com.umc.product.project.application.authorization.ProjectPolicyResourceContext;
import com.umc.product.project.application.port.in.query.GetProjectMatchingRoundUseCase;
import com.umc.product.project.application.port.in.query.dto.ProjectMatchingRoundInfo;
import com.umc.product.project.application.port.out.LoadProjectMatchingRoundPort;
import com.umc.product.project.domain.ProjectMatchingRound;
import com.umc.product.project.domain.exception.ProjectDomainException;
import com.umc.product.project.domain.exception.ProjectErrorCode;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProjectMatchingRoundQueryService implements GetProjectMatchingRoundUseCase {

    private final LoadProjectMatchingRoundPort loadProjectMatchingRoundPort;
    private final ProjectPolicyAuthorizationService projectPolicyAuthorizationService;

    @Override
    public List<ProjectMatchingRoundInfo> list(
        Long requesterMemberId,
        Long gisuId,
        Long chapterId,
        Instant time
    ) {
        if (projectPolicyAuthorizationService.evaluate(
            requesterMemberId,
            ProjectPolicyAction.MATCHING_LIST,
            ProjectPolicyResourceContext.builder().build()
        ).effect() != PolicyEffect.ALLOW) {
            throw new ProjectDomainException(ProjectErrorCode.PROJECT_MATCHING_ROUND_ACCESS_DENIED);
        }
        return loadProjectMatchingRoundPort.listByFilters(gisuId, chapterId, time).stream()
            .map(ProjectMatchingRoundInfo::from)
            .toList();
    }

    @Override
    public Map<Long, ProjectMatchingRoundInfo> findAllByIds(Collection<Long> ids) {
        if (ids.isEmpty()) {
            return Map.of();
        }
        List<ProjectMatchingRound> rounds = loadProjectMatchingRoundPort.listByIds(List.copyOf(ids));
        Map<Long, ProjectMatchingRoundInfo> result = new LinkedHashMap<>();
        for (ProjectMatchingRound round : rounds) {
            result.put(round.getId(), ProjectMatchingRoundInfo.from(round));
        }
        return result;
    }
}
