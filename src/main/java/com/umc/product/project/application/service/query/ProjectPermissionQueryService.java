package com.umc.product.project.application.service.query;

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
import com.umc.product.authorization.domain.PermissionType;
import com.umc.product.authorization.domain.ResourcePermission;
import com.umc.product.authorization.domain.ResourceType;
import com.umc.product.authorization.domain.SubjectAttributes;
import com.umc.product.challenger.application.port.in.query.GetChallengerUseCase;
import com.umc.product.challenger.application.port.in.query.dto.ChallengerInfo;
import com.umc.product.common.domain.enums.ChallengerRoleType;
import com.umc.product.project.application.access.ProjectApplicationAccessScope;
import com.umc.product.project.application.access.ProjectApplicationAccessScopeResolver;
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
import com.umc.product.project.application.service.policy.ProjectStatisticsAccessPolicy;
import com.umc.product.project.domain.Project;
import com.umc.product.project.domain.ProjectApplicationForm;
import com.umc.product.project.domain.ProjectMatchingRound;
import com.umc.product.project.domain.ProjectPartQuota;
import com.umc.product.project.domain.enums.MatchingType;
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

    private final CheckPermissionUseCase checkPermissionUseCase;
    private final LoadProjectPort loadProjectPort;
    private final LoadProjectApplicationFormPort loadProjectApplicationFormPort;
    private final LoadProjectPartQuotaPort loadProjectPartQuotaPort;
    private final LoadProjectMemberPort loadProjectMemberPort;
    private final LoadProjectMatchingRoundPort loadProjectMatchingRoundPort;
    private final GetChallengerUseCase getChallengerUseCase;
    private final ProjectApplicationAccessScopeResolver projectApplicationAccessScopeResolver;
    private final ProjectStatisticsAccessPolicy projectStatisticsAccessPolicy;

    @Override
    public List<ProjectPermissionInfo> listByProjectIds(Long requesterMemberId, List<Long> projectIds) {
        List<Long> uniqueIds = deduplicate(projectIds);
        if (uniqueIds.isEmpty()) {
            return List.of();
        }

        SubjectAttributes subject = checkPermissionUseCase.loadSubject(requesterMemberId);
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
        PermissionCache permissionCache = new PermissionCache(subject);
        Instant now = Instant.now();

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
                return buildInfo(requesterMemberId, subject, context);
            })
            .toList();
    }

    private ProjectPermissionInfo buildInfo(
        Long requesterMemberId,
        SubjectAttributes subject,
        ProjectCapabilityContext context
    ) {
        Project project = context.project();
        ProjectPermissionCapabilityInfo canEditInfo = requirePermission(
            context.projectPermission(PermissionType.EDIT),
            ProjectPermissionCapabilityInfo::allow
        );
        ProjectPermissionCapabilityInfo canTransferOwnership = requirePermission(
            context.projectPermission(PermissionType.EDIT),
            ProjectPermissionCapabilityInfo::allow
        );
        ProjectPermissionCapabilityInfo canDelete = canDeleteProject(context);

        return new ProjectPermissionInfo(
            project.getId(),
            true,
            canEditInfo,
            canTransferOwnership,
            canDelete,
            applicationFormPermissions(requesterMemberId, subject, context),
            partQuotaPermissions(context),
            statusPermissions(context),
            applicationPermissions(requesterMemberId, context),
            memberPermissions(context),
            statisticsPermissions(requesterMemberId, project)
        );
    }

    private ApplicationFormPermissions applicationFormPermissions(
        Long requesterMemberId,
        SubjectAttributes subject,
        ProjectCapabilityContext context
    ) {
        return new ApplicationFormPermissions(
            canReadApplicationForm(requesterMemberId, subject, context),
            canCreateApplicationForm(context),
            canEditApplicationForm(context),
            NOT_IMPLEMENTED_FORM_PUBLISH,
            NOT_IMPLEMENTED_FORM_DELETE
        );
    }

    private ProjectPermissionCapabilityInfo canReadApplicationForm(
        Long requesterMemberId,
        SubjectAttributes subject,
        ProjectCapabilityContext context
    ) {
        if (!context.projectPermission(PermissionType.READ)) {
            return denied(ProjectPermissionReason.PERMISSION_DENIED);
        }
        if (!context.hasForm()) {
            return denied(ProjectPermissionReason.APPLICATION_FORM_NOT_FOUND);
        }
        if (!canReadApplicationFormPolicy(requesterMemberId, subject, context)) {
            return denied(ProjectPermissionReason.PERMISSION_DENIED);
        }
        return ProjectPermissionCapabilityInfo.allow();
    }

    private ProjectPermissionCapabilityInfo canCreateApplicationForm(ProjectCapabilityContext context) {
        return requirePermission(context.projectPermission(PermissionType.EDIT), () -> {
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
        return requirePermission(context.projectPermission(PermissionType.EDIT), () -> {
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
            context.projectPermission(PermissionType.MANAGE),
            ProjectPermissionCapabilityInfo::allow
        ));
    }

    private StatusPermissions statusPermissions(ProjectCapabilityContext context) {
        return new StatusPermissions(
            canRequestReview(context),
            canPublishProject(context),
            canCompleteProject(context),
            canAbortProject(context)
        );
    }

    private ProjectPermissionCapabilityInfo canRequestReview(ProjectCapabilityContext context) {
        return requirePermission(context.projectPermission(PermissionType.EDIT), () -> {
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
        return requirePermission(context.projectPermission(PermissionType.MANAGE), () -> {
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
        return requirePermission(context.projectPermission(PermissionType.MANAGE), () -> {
            if (context.project().getStatus() != ProjectStatus.IN_PROGRESS) {
                return ProjectPermissionCapabilityInfo.denied(
                    ProjectPermissionReason.INVALID_PROJECT_STATUS,
                    "현재 진행 중인 프로젝트만 중단 시킬 수 있어요."
                );
            }
            return ProjectPermissionCapabilityInfo.allow();
        });
    }

    private ProjectPermissionCapabilityInfo canCompleteProject(ProjectCapabilityContext context) {
        return requirePermission(context.projectPermission(PermissionType.MANAGE), () -> {
            if (context.project().getStatus() != ProjectStatus.IN_PROGRESS) {
                return ProjectPermissionCapabilityInfo.denied(
                    ProjectPermissionReason.INVALID_PROJECT_STATUS,
                    "현재 진행 중인 프로젝트만 완료 처리할 수 있어요."
                );
            }
            return ProjectPermissionCapabilityInfo.allow();
        });
    }

    private ApplicationPermissions applicationPermissions(Long requesterMemberId, ProjectCapabilityContext context) {
        return new ApplicationPermissions(
            canCreateApplication(requesterMemberId, context),
            canReadApplicationList(requesterMemberId, context.project()),
            canDecideApplication(requesterMemberId, context.project())
        );
    }

    private ProjectPermissionCapabilityInfo canCreateApplication(
        Long requesterMemberId,
        ProjectCapabilityContext context
    ) {
        if (!context.applicationWritePermission()) {
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

    private ProjectPermissionCapabilityInfo canReadApplicationList(Long requesterMemberId, Project project) {
        ProjectApplicationAccessScope scope =
            projectApplicationAccessScopeResolver.resolveForProjectApplicantList(requesterMemberId, project);
        if (scope instanceof ProjectApplicationAccessScope.None) {
            return denied(ProjectPermissionReason.PERMISSION_DENIED);
        }
        return ProjectPermissionCapabilityInfo.allow();
    }

    private ProjectPermissionCapabilityInfo canDecideApplication(Long requesterMemberId, Project project) {
        if (project.getStatus() != ProjectStatus.IN_PROGRESS) {
            return denied(ProjectPermissionReason.INVALID_PROJECT_STATUS);
        }
        if (!Objects.equals(project.getProductOwnerMemberId(), requesterMemberId)) {
            return denied(ProjectPermissionReason.PERMISSION_DENIED);
        }
        return ProjectPermissionCapabilityInfo.allow();
    }

    private MemberPermissions memberPermissions(ProjectCapabilityContext context) {
        ProjectPermissionCapabilityInfo read = requirePermission(
            context.projectPermission(PermissionType.READ),
            ProjectPermissionCapabilityInfo::allow
        );
        ProjectPermissionCapabilityInfo edit = requirePermission(
            context.projectPermission(PermissionType.EDIT),
            ProjectPermissionCapabilityInfo::allow
        );
        return new MemberPermissions(read, edit, edit);
    }

    private StatisticsPermissions statisticsPermissions(Long requesterMemberId, Project project) {
        if (!projectStatisticsAccessPolicy.canReadProjectStatistics(requesterMemberId, project)) {
            return new StatisticsPermissions(ProjectPermissionCapabilityInfo.denied(
                ProjectPermissionReason.PERMISSION_DENIED,
                "통계를 조회할 권한이 없어요."
            ));
        }
        return new StatisticsPermissions(ProjectPermissionCapabilityInfo.allow());
    }

    private ProjectPermissionCapabilityInfo canDeleteProject(ProjectCapabilityContext context) {
        return requirePermission(context.projectPermission(PermissionType.DELETE), () -> {
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

    private boolean canReadApplicationFormPolicy(
        Long requesterMemberId,
        SubjectAttributes subject,
        ProjectCapabilityContext context
    ) {
        Project project = context.project();
        if (Objects.equals(requesterMemberId, project.getProductOwnerMemberId())) {
            return true;
        }
        if (isCentralCoreInGisu(subject, project.getGisuId())) {
            return true;
        }
        if (isChapterPresidentOf(subject, project.getChapterId(), project.getGisuId())) {
            return true;
        }
        return subject.gisuChallengerInfos().stream()
            .anyMatch(info -> Objects.equals(info.gisuId(), project.getGisuId()))
            || context.challengerInfo(requesterMemberId).isPresent();
    }

    private boolean isCentralCoreInGisu(SubjectAttributes subject, Long gisuId) {
        return subject.roleAttributes().stream()
            .anyMatch(role -> role.roleType().isSuperAdmin()
                || (role.roleType().isAtLeastCentralCore() && Objects.equals(role.gisuId(), gisuId)));
    }

    private boolean isChapterPresidentOf(SubjectAttributes subject, Long chapterId, Long gisuId) {
        return subject.roleAttributes().stream()
            .anyMatch(role -> role.roleType() == ChallengerRoleType.CHAPTER_PRESIDENT
                && Objects.equals(role.gisuId(), gisuId)
                && Objects.equals(role.organizationId(), chapterId));
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
        private final Map<Long, Map<PermissionType, Boolean>> projectPermissions = new LinkedHashMap<>();
        private final Map<Long, Boolean> applicationWritePermissions = new LinkedHashMap<>();
        private final Map<Long, List<ProjectMatchingRound>> openMatchingRounds = new LinkedHashMap<>();
        private final Map<Long, Optional<ChallengerInfo>> challengerInfos = new LinkedHashMap<>();
        private final Map<Long, Boolean> projectMemberExists = new LinkedHashMap<>();

        private PermissionCache(SubjectAttributes subject) {
            this.subject = subject;
        }

        private boolean project(Long projectId, PermissionType permissionType) {
            return projectPermissions
                .computeIfAbsent(projectId, ignored -> new EnumMap<>(PermissionType.class))
                .computeIfAbsent(permissionType, type -> checkPermissionUseCase.check(
                    subject,
                    ResourcePermission.of(ResourceType.PROJECT, projectId, type)
                ));
        }

        private boolean applicationWrite(Long projectId) {
            return applicationWritePermissions.computeIfAbsent(projectId, id -> checkPermissionUseCase.check(
                subject,
                ResourcePermission.of(ResourceType.PROJECT_APPLICATION, id, PermissionType.WRITE)
            ));
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

        private boolean projectPermission(PermissionType permissionType) {
            return permissionCache.project(project.getId(), permissionType);
        }

        private boolean applicationWritePermission() {
            return permissionCache.applicationWrite(project.getId());
        }

        private Optional<ChallengerInfo> challengerInfo(Long memberId) {
            return permissionCache.challengerInfo(memberId, project.getGisuId());
        }

        private boolean existsByGisuAndMember(Long memberId) {
            return permissionCache.existsByGisuAndMember(project.getGisuId(), memberId);
        }
    }
}
