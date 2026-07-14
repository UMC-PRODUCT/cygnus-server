package com.umc.product.project.application.service.query;

import static com.umc.product.project.application.authorization.ProjectPolicyDecisionOutcomes.enumValue;
import static com.umc.product.project.application.authorization.ProjectPolicyDecisionOutcomes.longSetValue;

import java.time.Instant;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.umc.product.authorization.application.port.in.CheckPermissionUseCase;
import com.umc.product.authorization.domain.SubjectAttributes;
import com.umc.product.authorization.domain.exception.AuthorizationDomainException;
import com.umc.product.authorization.domain.exception.AuthorizationErrorCode;
import com.umc.product.authorization.domain.policy.PolicyDecision;
import com.umc.product.authorization.domain.policy.PolicyEffect;
import com.umc.product.challenger.application.port.in.query.GetChallengerUseCase;
import com.umc.product.challenger.application.port.in.query.dto.ChallengerInfo;
import com.umc.product.project.application.authorization.ProjectPolicyAction;
import com.umc.product.project.application.authorization.ProjectPolicyAuthorizationService;
import com.umc.product.project.application.authorization.ProjectPolicyOutcomes;
import com.umc.product.project.application.authorization.ProjectPolicyResourceContext;
import com.umc.product.project.application.authorization.ProjectPolicySubjectSnapshot;
import com.umc.product.project.application.port.in.query.GetProjectPermissionsUseCase;
import com.umc.product.project.application.port.in.query.dto.ProjectPermissionCapabilityInfo;
import com.umc.product.project.application.port.in.query.dto.ProjectPermissionInfo;
import com.umc.product.project.application.port.in.query.dto.ProjectPermissionInfo.ApplicationFormPermissions;
import com.umc.product.project.application.port.in.query.dto.ProjectPermissionInfo.ApplicationPermissions;
import com.umc.product.project.application.port.in.query.dto.ProjectPermissionInfo.MemberPermissions;
import com.umc.product.project.application.port.in.query.dto.ProjectPermissionInfo.PartQuotaPermissions;
import com.umc.product.project.application.port.in.query.dto.ProjectPermissionInfo.StatisticsPermissions;
import com.umc.product.project.application.port.in.query.dto.ProjectPermissionInfo.StatusPermissions;
import com.umc.product.project.application.port.in.query.dto.ProjectPermissionReason;
import com.umc.product.project.application.port.out.LoadProjectApplicationFormPort;
import com.umc.product.project.application.port.out.LoadProjectMatchingRoundPort;
import com.umc.product.project.application.port.out.LoadProjectMemberPort;
import com.umc.product.project.application.port.out.LoadProjectPartQuotaPort;
import com.umc.product.project.application.port.out.LoadProjectPort;
import com.umc.product.project.domain.Project;
import com.umc.product.project.domain.ProjectApplicationForm;
import com.umc.product.project.domain.ProjectMatchingRound;
import com.umc.product.project.domain.ProjectPartQuota;
import com.umc.product.project.domain.enums.MatchingType;
import com.umc.product.project.domain.enums.ProjectApplicationStatus;
import com.umc.product.project.domain.enums.ProjectStatus;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProjectPermissionQueryService implements GetProjectPermissionsUseCase {

    private static final ProjectPermissionCapabilityInfo NOT_IMPLEMENTED_FORM_PUBLISH =
        ProjectPermissionCapabilityInfo.denied(
            ProjectPermissionReason.NOT_IMPLEMENTED,
            "아직은 지원 폼 공개를 별도로 지원하지 않아요."
        );
    private static final ProjectPermissionCapabilityInfo NOT_IMPLEMENTED_FORM_DELETE =
        ProjectPermissionCapabilityInfo.denied(
            ProjectPermissionReason.NOT_IMPLEMENTED,
            "아직은 지원 폼 삭제를 별도로 지원하지 않아요."
        );
    private static final ProjectPermissionCapabilityInfo NOT_IMPLEMENTED_PROJECT_COMPLETE =
        ProjectPermissionCapabilityInfo.denied(
            ProjectPermissionReason.NOT_IMPLEMENTED,
            "아직 프로젝트 완료 처리를 지원하지 않아요."
        );

    private final CheckPermissionUseCase checkPermissionUseCase;
    private final LoadProjectPort loadProjectPort;
    private final LoadProjectApplicationFormPort loadProjectApplicationFormPort;
    private final LoadProjectPartQuotaPort loadProjectPartQuotaPort;
    private final LoadProjectMemberPort loadProjectMemberPort;
    private final LoadProjectMatchingRoundPort loadProjectMatchingRoundPort;
    private final GetChallengerUseCase getChallengerUseCase;
    private final ProjectPolicyAuthorizationService projectPolicyAuthorizationService;

    @Override
    public List<ProjectPermissionInfo> listByProjectIds(Long requesterMemberId, List<Long> projectIds) {
        List<Long> uniqueIds = deduplicate(projectIds);
        if (uniqueIds.isEmpty()) {
            return List.of();
        }

        SubjectAttributes subject = checkPermissionUseCase.loadSubject(requesterMemberId);
        PermissionCache permissionCache = new PermissionCache(subject);
        Map<Long, Project> projectsById = loadProjectPort.listByIds(uniqueIds).stream()
            .collect(Collectors.toMap(
                Project::getId,
                project -> project,
                (first, ignored) -> first,
                LinkedHashMap::new
            ));
        Map<Long, ProjectApplicationForm> formsByProjectId =
            loadProjectApplicationFormPort.findAllByProjectIds(uniqueIds);
        Map<Long, List<ProjectPartQuota>> quotasByProjectId =
            loadProjectPartQuotaPort.listByProjectIdsGroupedByProjectId(uniqueIds);
        Instant now = permissionCache.evaluatedAt();

        return uniqueIds.stream()
            .map(projectId -> {
                Project project = projectsById.get(projectId);
                if (project == null) {
                    return ProjectPermissionInfo.notFound(projectId);
                }
                ProjectCapabilityContext context = ProjectCapabilityContext.of(
                    project,
                    formsByProjectId.get(projectId),
                    quotasByProjectId.getOrDefault(projectId, List.of()),
                    permissionCache.openMatchingRounds(project.getChapterId(), now),
                    permissionCache
                );
                return buildInfo(requesterMemberId, context);
            })
            .toList();
    }

    private ProjectPermissionInfo buildInfo(
        Long requesterMemberId,
        ProjectCapabilityContext context
    ) {
        Project project = context.project();
        ProjectPermissionCapabilityInfo canEditInfo = requirePermission(
            context.policy(ProjectPolicyAction.PROJECT_UPDATE),
            ProjectPermissionCapabilityInfo::allow
        );
        ProjectPermissionCapabilityInfo canTransferOwnership = requirePermission(
            context.policy(ProjectPolicyAction.PROJECT_TRANSFER_OWNERSHIP),
            ProjectPermissionCapabilityInfo::allow
        );
        ProjectPermissionCapabilityInfo canDelete = canDeleteProject(context);

        return new ProjectPermissionInfo(
            project.getId(),
            true,
            canEditInfo,
            canTransferOwnership,
            canDelete,
            applicationFormPermissions(context),
            partQuotaPermissions(context),
            statusPermissions(context),
            applicationPermissions(requesterMemberId, context),
            memberPermissions(context),
            statisticsPermissions(context)
        );
    }

    private ApplicationFormPermissions applicationFormPermissions(ProjectCapabilityContext context) {
        return new ApplicationFormPermissions(
            canReadApplicationForm(context),
            canCreateApplicationForm(context),
            canEditApplicationForm(context),
            NOT_IMPLEMENTED_FORM_PUBLISH,
            NOT_IMPLEMENTED_FORM_DELETE
        );
    }

    private ProjectPermissionCapabilityInfo canReadApplicationForm(ProjectCapabilityContext context) {
        if (!context.hasForm()) {
            return denied(ProjectPermissionReason.APPLICATION_FORM_NOT_FOUND);
        }
        PolicyDecision decision = context.policyDecision(ProjectPolicyAction.FORM_READ);
        String view = enumValue(decision, ProjectPolicyOutcomes.FORM_VIEW, "NONE");
        if (decision.effect() != PolicyEffect.ALLOW || "NONE".equals(view)) {
            return denied(ProjectPermissionReason.PERMISSION_DENIED);
        }
        return ProjectPermissionCapabilityInfo.allow();
    }

    private ProjectPermissionCapabilityInfo canCreateApplicationForm(ProjectCapabilityContext context) {
        return requirePermission(context.policy(ProjectPolicyAction.FORM_UPDATE), () -> {
            if (context.hasForm()) {
                return ProjectPermissionCapabilityInfo.denied(
                    ProjectPermissionReason.NOT_IMPLEMENTED,
                    "아직은 프로젝트에 여러 개의 폼을 연결하는 것을 허용하지 않아요."
                );
            }
            return formEditable(context);
        });
    }

    private ProjectPermissionCapabilityInfo canEditApplicationForm(ProjectCapabilityContext context) {
        return requirePermission(context.policy(ProjectPolicyAction.FORM_UPDATE), () -> {
            if (!context.hasForm()) {
                return denied(ProjectPermissionReason.APPLICATION_FORM_NOT_FOUND);
            }
            return formEditable(context);
        });
    }

    private ProjectPermissionCapabilityInfo formEditable(ProjectCapabilityContext context) {
        ProjectStatus status = context.project().getStatus();
        if (status == ProjectStatus.DRAFT || status == ProjectStatus.PENDING_REVIEW) {
            return ProjectPermissionCapabilityInfo.allow();
        }
        if (status == ProjectStatus.IN_PROGRESS) {
            if (context.hasOpenMatchingRound()) {
                return denied(ProjectPermissionReason.ACTIVE_MATCHING_ROUND_EXISTS);
            }
            return ProjectPermissionCapabilityInfo.allow();
        }
        return denied(ProjectPermissionReason.INVALID_PROJECT_STATUS);
    }

    private PartQuotaPermissions partQuotaPermissions(ProjectCapabilityContext context) {
        return new PartQuotaPermissions(requirePermission(
            context.policy(ProjectPolicyAction.PROJECT_QUOTA_UPDATE),
            ProjectPermissionCapabilityInfo::allow
        ));
    }

    private StatusPermissions statusPermissions(ProjectCapabilityContext context) {
        return new StatusPermissions(
            canRequestReview(context),
            canPublishProject(context),
            NOT_IMPLEMENTED_PROJECT_COMPLETE,
            canAbortProject(context)
        );
    }

    private ProjectPermissionCapabilityInfo canRequestReview(ProjectCapabilityContext context) {
        return requirePermission(context.policy(ProjectPolicyAction.PROJECT_SUBMIT), () -> {
            Project project = context.project();
            if (project.getStatus() != ProjectStatus.DRAFT) {
                return denied(ProjectPermissionReason.INVALID_PROJECT_STATUS);
            }
            if (project.getName() == null || project.getName().isBlank()) {
                return denied(ProjectPermissionReason.PROJECT_INFO_REQUIRED);
            }
            if (!context.hasForm()) {
                return denied(ProjectPermissionReason.APPLICATION_FORM_NOT_FOUND);
            }
            return ProjectPermissionCapabilityInfo.allow();
        });
    }

    private ProjectPermissionCapabilityInfo canPublishProject(ProjectCapabilityContext context) {
        return requirePermission(context.policy(ProjectPolicyAction.PROJECT_PUBLISH), () -> {
            if (context.project().getStatus() != ProjectStatus.PENDING_REVIEW) {
                return denied(ProjectPermissionReason.INVALID_PROJECT_STATUS);
            }
            if (!context.hasForm()) {
                return denied(ProjectPermissionReason.APPLICATION_FORM_NOT_FOUND);
            }
            if (context.quotas().isEmpty()) {
                return denied(ProjectPermissionReason.PART_QUOTA_REQUIRED);
            }
            return ProjectPermissionCapabilityInfo.allow();
        });
    }

    private ProjectPermissionCapabilityInfo canAbortProject(ProjectCapabilityContext context) {
        return requirePermission(context.policy(ProjectPolicyAction.PROJECT_ABORT), () -> {
            if (context.project().getStatus() != ProjectStatus.IN_PROGRESS) {
                return ProjectPermissionCapabilityInfo.denied(
                    ProjectPermissionReason.INVALID_PROJECT_STATUS,
                    "현재 진행 중인 프로젝트만 중단 시킬 수 있어요."
                );
            }
            return ProjectPermissionCapabilityInfo.allow();
        });
    }

    private ApplicationPermissions applicationPermissions(Long requesterMemberId, ProjectCapabilityContext context) {
        return new ApplicationPermissions(
            canCreateApplication(requesterMemberId, context),
            canReadApplicationList(context),
            canDecideApplication(context)
        );
    }

    private ProjectPermissionCapabilityInfo canCreateApplication(
        Long requesterMemberId,
        ProjectCapabilityContext context
    ) {
        if (!context.policy(ProjectPolicyAction.APPLICATION_CREATE)) {
            return denied(ProjectPermissionReason.PERMISSION_DENIED);
        }
        Project project = context.project();
        if (project.getStatus() != ProjectStatus.IN_PROGRESS) {
            return denied(ProjectPermissionReason.INVALID_PROJECT_STATUS);
        }
        if (!context.hasForm()) {
            return denied(ProjectPermissionReason.APPLICATION_FORM_NOT_FOUND);
        }
        if (Objects.equals(project.getProductOwnerMemberId(), requesterMemberId)) {
            return denied(ProjectPermissionReason.PROJECT_APPLICATION_SELF_APPLY_NOT_ALLOWED);
        }

        Optional<ChallengerInfo> challenger =
            context.challengerInfo(requesterMemberId);
        if (challenger.isEmpty()) {
            return denied(ProjectPermissionReason.NOT_PROJECT_GISU_CHALLENGER);
        }
        Optional<MatchingType> matchingType = MatchingType.fromPart(challenger.get().part());
        if (matchingType.isEmpty()) {
            return denied(ProjectPermissionReason.NOT_PROJECT_GISU_CHALLENGER);
        }
        if (context.quotas().stream().noneMatch(quota -> quota.getPart() == challenger.get().part())) {
            return denied(ProjectPermissionReason.PROJECT_APPLICATION_PART_NOT_ALLOWED);
        }
        if (context.existsByGisuAndMember(requesterMemberId)) {
            return denied(ProjectPermissionReason.PROJECT_APPLICATION_MEMBER_ALREADY_IN_TEAM);
        }
        if (context.openMatchingRounds().stream().noneMatch(round -> round.getType() == matchingType.get())) {
            return denied(ProjectPermissionReason.MATCHING_ROUND_NOT_OPEN);
        }
        return ProjectPermissionCapabilityInfo.allow();
    }

    private ProjectPermissionCapabilityInfo canReadApplicationList(ProjectCapabilityContext context) {
        PolicyDecision decision = context.policyDecision(ProjectPolicyAction.APPLICATION_LIST_PROJECT);
        if (decision.effect() != PolicyEffect.ALLOW
            || !longSetValue(decision, ProjectPolicyOutcomes.APPLICATION_PROJECT_IDS)
                .contains(context.project().getId())) {
            return denied(ProjectPermissionReason.PERMISSION_DENIED);
        }
        return ProjectPermissionCapabilityInfo.allow();
    }

    private ProjectPermissionCapabilityInfo canDecideApplication(ProjectCapabilityContext context) {
        Project project = context.project();
        if (project.getStatus() != ProjectStatus.IN_PROGRESS) {
            return denied(ProjectPermissionReason.INVALID_PROJECT_STATUS);
        }
        if (!context.policy(ProjectPolicyAction.APPLICATION_DECIDE)) {
            return denied(ProjectPermissionReason.PERMISSION_DENIED);
        }
        return ProjectPermissionCapabilityInfo.allow();
    }

    private MemberPermissions memberPermissions(ProjectCapabilityContext context) {
        ProjectPermissionCapabilityInfo read = requirePermission(
            context.policy(ProjectPolicyAction.PROJECT_MEMBER_LIST),
            ProjectPermissionCapabilityInfo::allow
        );
        ProjectPermissionCapabilityInfo create = requirePermission(
            context.policy(ProjectPolicyAction.PROJECT_MEMBER_ADD),
            ProjectPermissionCapabilityInfo::allow
        );
        ProjectPermissionCapabilityInfo delete = requirePermission(
            context.policy(ProjectPolicyAction.PROJECT_MEMBER_REMOVE),
            ProjectPermissionCapabilityInfo::allow
        );
        return new MemberPermissions(read, create, delete);
    }

    private StatisticsPermissions statisticsPermissions(ProjectCapabilityContext context) {
        if (!context.policy(ProjectPolicyAction.STATISTICS_PROJECT)) {
            return new StatisticsPermissions(ProjectPermissionCapabilityInfo.denied(
                ProjectPermissionReason.PERMISSION_DENIED,
                "통계를 조회할 권한이 없어요."
            ));
        }
        return new StatisticsPermissions(ProjectPermissionCapabilityInfo.allow());
    }

    private ProjectPermissionCapabilityInfo canDeleteProject(ProjectCapabilityContext context) {
        return requirePermission(context.policy(ProjectPolicyAction.PROJECT_DELETE), () -> {
            ProjectStatus status = context.project().getStatus();
            if (status == ProjectStatus.IN_PROGRESS) {
                return ProjectPermissionCapabilityInfo.denied(
                    ProjectPermissionReason.INVALID_PROJECT_STATUS,
                    "진행 중인 프로젝트는 중단 기능을 이용해주세요."
                );
            }
            if (status != ProjectStatus.DRAFT && status != ProjectStatus.PENDING_REVIEW) {
                return ProjectPermissionCapabilityInfo.denied(
                    ProjectPermissionReason.INVALID_PROJECT_STATUS,
                    "현재 상태에서는 프로젝트를 삭제할 수 없습니다."
                );
            }
            return ProjectPermissionCapabilityInfo.allow();
        });
    }

    private ProjectPermissionCapabilityInfo requirePermission(
        boolean hasPermission,
        Supplier<ProjectPermissionCapabilityInfo> allowedSupplier
    ) {
        if (!hasPermission) {
            return denied(ProjectPermissionReason.PERMISSION_DENIED);
        }
        return allowedSupplier.get();
    }

    private ProjectPermissionCapabilityInfo denied(ProjectPermissionReason reason) {
        return ProjectPermissionCapabilityInfo.denied(reason);
    }

    private List<Long> deduplicate(List<Long> projectIds) {
        if (projectIds == null || projectIds.isEmpty()) {
            return List.of();
        }
        return projectIds.stream()
            .filter(Objects::nonNull)
            .collect(Collectors.collectingAndThen(
                Collectors.toCollection(LinkedHashSet::new),
                set -> List.copyOf(set)
            ));
    }

    private class PermissionCache {

        private final SubjectAttributes subject;
        private final ProjectPolicySubjectSnapshot snapshot;
        private final Map<Long, Map<ProjectPolicyAction, PolicyDecision>> policyDecisions = new LinkedHashMap<>();
        private final Map<Long, List<ProjectMatchingRound>> openMatchingRounds = new LinkedHashMap<>();
        private final Map<Long, Optional<ChallengerInfo>> challengerInfos = new LinkedHashMap<>();
        private final Map<Long, Boolean> projectMemberExists = new LinkedHashMap<>();
        private final Map<Long, Boolean> activePlanMembers = new LinkedHashMap<>();

        private PermissionCache(SubjectAttributes subject) {
            this.subject = subject;
            this.snapshot = projectPolicyAuthorizationService.snapshot(subject);
            PolicyDecision capabilityDecision = projectPolicyAuthorizationService.evaluate(
                snapshot,
                ProjectPolicyAction.CAPABILITY_LIST,
                ProjectPolicyResourceContext.builder().build()
            );
            if (capabilityDecision.effect() != PolicyEffect.ALLOW) {
                throw new AuthorizationDomainException(AuthorizationErrorCode.RESOURCE_ACCESS_DENIED);
            }
        }

        private boolean policy(Project project, ProjectPolicyAction action) {
            return policyDecision(project, action).effect() == PolicyEffect.ALLOW;
        }

        private PolicyDecision policyDecision(Project project, ProjectPolicyAction action) {
            return policyDecisions
                .computeIfAbsent(project.getId(), ignored -> new EnumMap<>(ProjectPolicyAction.class))
                .computeIfAbsent(action, ignored -> projectPolicyAuthorizationService.evaluate(
                    snapshot, action, policyContext(project, action)));
        }

        private ProjectPolicyResourceContext policyContext(Project project, ProjectPolicyAction action) {
            ProjectPolicyResourceContext.Builder builder = ProjectPolicyResourceContext.builder()
                .project(project.getId(), project.getGisuId(), project.getChapterId(), project.getStatus())
                .creatorMemberId(project.getCreatorMemberId())
                .productOwnerMemberId(project.getProductOwnerMemberId())
                .activePlanMember(activePlanMember(project.getId()));
            if (action == ProjectPolicyAction.APPLICATION_DECIDE) {
                builder.application(Long.MIN_VALUE, ProjectApplicationStatus.SUBMITTED, Long.MIN_VALUE);
            }
            return builder.build();
        }

        private List<ProjectMatchingRound> openMatchingRounds(Long chapterId, Instant now) {
            return openMatchingRounds.computeIfAbsent(chapterId, id ->
                List.copyOf(loadProjectMatchingRoundPort.listOpenAt(id, now))
            );
        }

        private Optional<ChallengerInfo> challengerInfo(Long memberId, Long gisuId) {
            return challengerInfos.computeIfAbsent(gisuId, id ->
                getChallengerUseCase.findByMemberIdAndGisuId(memberId, id)
            );
        }

        private boolean existsByGisuAndMember(Long gisuId, Long memberId) {
            return projectMemberExists.computeIfAbsent(gisuId, id ->
                loadProjectMemberPort.existsByGisuAndMember(id, memberId)
            );
        }

        private boolean activePlanMember(Long projectId) {
            return activePlanMembers.computeIfAbsent(projectId, id ->
                loadProjectMemberPort.isActivePlanMember(id, subject.memberId()));
        }

        private Instant evaluatedAt() {
            return snapshot.evaluatedAt();
        }
    }

    private record ProjectCapabilityContext(
        Project project,
        ProjectApplicationForm form,
        List<ProjectPartQuota> quotas,
        List<ProjectMatchingRound> openMatchingRounds,
        PermissionCache permissionCache
    ) {

        private static ProjectCapabilityContext of(
            Project project,
            ProjectApplicationForm form,
            List<ProjectPartQuota> quotas,
            List<ProjectMatchingRound> openMatchingRounds,
            PermissionCache permissionCache
        ) {
            return new ProjectCapabilityContext(
                project,
                form,
                List.copyOf(quotas),
                List.copyOf(openMatchingRounds),
                permissionCache
            );
        }

        private boolean hasForm() {
            return form != null;
        }

        private boolean hasOpenMatchingRound() {
            return !openMatchingRounds.isEmpty();
        }

        private boolean policy(ProjectPolicyAction action) {
            return permissionCache.policy(project, action);
        }

        private PolicyDecision policyDecision(ProjectPolicyAction action) {
            return permissionCache.policyDecision(project, action);
        }

        private Optional<ChallengerInfo> challengerInfo(Long memberId) {
            return permissionCache.challengerInfo(memberId, project.getGisuId());
        }

        private boolean existsByGisuAndMember(Long memberId) {
            return permissionCache.existsByGisuAndMember(project.getGisuId(), memberId);
        }
    }
}
