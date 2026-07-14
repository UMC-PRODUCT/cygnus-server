package com.umc.product.project.application.service.command;

import java.util.List;
import java.util.Objects;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.umc.product.audit.application.port.in.annotation.Audited;
import com.umc.product.audit.domain.AuditAction;
import com.umc.product.authorization.domain.policy.PolicyEffect;
import com.umc.product.challenger.application.port.in.query.GetChallengerUseCase;
import com.umc.product.challenger.application.port.in.query.dto.ChallengerInfo;
import com.umc.product.common.domain.enums.ChallengerPart;
import com.umc.product.form.application.port.in.command.ManageFormUseCase;
import com.umc.product.form.application.port.in.command.dto.DeleteFormCommand;
import com.umc.product.form.application.port.in.command.dto.PublishFormCommand;
import com.umc.product.global.exception.constant.Domain;
import com.umc.product.member.application.port.in.query.GetMemberUseCase;
import com.umc.product.member.application.port.in.query.dto.MemberInfo;
import com.umc.product.organization.application.port.in.query.GetChapterUseCase;
import com.umc.product.organization.application.port.in.query.GetGisuUseCase;
import com.umc.product.organization.application.port.in.query.dto.chapter.ChapterInfo;
import com.umc.product.project.application.authorization.ProjectPolicyAction;
import com.umc.product.project.application.authorization.ProjectPolicyAuthorizationService;
import com.umc.product.project.application.authorization.ProjectPolicyResourceContext;
import com.umc.product.project.application.authorization.rollout.ProjectAuthorizationResourceSnapshot;
import com.umc.product.project.application.port.in.command.AbortProjectUseCase;
import com.umc.product.project.application.port.in.command.CreateDraftProjectUseCase;
import com.umc.product.project.application.port.in.command.DeleteProjectUseCase;
import com.umc.product.project.application.port.in.command.PublishProjectUseCase;
import com.umc.product.project.application.port.in.command.SubmitProjectUseCase;
import com.umc.product.project.application.port.in.command.TransferProjectOwnershipUseCase;
import com.umc.product.project.application.port.in.command.UpdateProjectUseCase;
import com.umc.product.project.application.port.in.command.dto.AbortProjectCommand;
import com.umc.product.project.application.port.in.command.dto.CreateDraftProjectCommand;
import com.umc.product.project.application.port.in.command.dto.DeleteProjectCommand;
import com.umc.product.project.application.port.in.command.dto.PublishProjectCommand;
import com.umc.product.project.application.port.in.command.dto.SubmitProjectCommand;
import com.umc.product.project.application.port.in.command.dto.TransferProjectOwnershipCommand;
import com.umc.product.project.application.port.in.command.dto.UpdateProjectCommand;
import com.umc.product.project.application.port.out.LoadProjectApplicationFormPort;
import com.umc.product.project.application.port.out.LoadProjectApplicationPort;
import com.umc.product.project.application.port.out.LoadProjectMemberPort;
import com.umc.product.project.application.port.out.LoadProjectPartQuotaPort;
import com.umc.product.project.application.port.out.LoadProjectPort;
import com.umc.product.project.application.port.out.SaveProjectApplicationFormPolicyPort;
import com.umc.product.project.application.port.out.SaveProjectApplicationFormPort;
import com.umc.product.project.application.port.out.SaveProjectMemberPort;
import com.umc.product.project.application.port.out.SaveProjectPartQuotaPort;
import com.umc.product.project.application.port.out.SaveProjectPort;
import com.umc.product.project.domain.Project;
import com.umc.product.project.domain.ProjectApplication;
import com.umc.product.project.domain.ProjectApplicationForm;
import com.umc.product.project.domain.ProjectMember;
import com.umc.product.project.domain.ProjectPartQuota;
import com.umc.product.project.domain.enums.ProjectStatus;
import com.umc.product.project.domain.exception.ProjectDomainException;
import com.umc.product.project.domain.exception.ProjectErrorCode;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class ProjectCommandService implements
    CreateDraftProjectUseCase,
    UpdateProjectUseCase,
    SubmitProjectUseCase,
    TransferProjectOwnershipUseCase,
    PublishProjectUseCase,
    DeleteProjectUseCase,
    AbortProjectUseCase {

    private final LoadProjectPort loadProjectPort;
    private final SaveProjectPort saveProjectPort;
    private final LoadProjectApplicationFormPort loadProjectApplicationFormPort;
    private final LoadProjectPartQuotaPort loadProjectPartQuotaPort;
    private final LoadProjectMemberPort loadProjectMemberPort;
    private final LoadProjectApplicationPort loadProjectApplicationPort;
    private final SaveProjectMemberPort saveProjectMemberPort;
    private final SaveProjectPartQuotaPort saveProjectPartQuotaPort;
    private final SaveProjectApplicationFormPort saveProjectApplicationFormPort;
    private final SaveProjectApplicationFormPolicyPort saveProjectApplicationFormPolicyPort;

    // Cross-domain UseCases
    private final GetMemberUseCase getMemberUseCase;
    private final GetChallengerUseCase getChallengerUseCase;
    private final GetGisuUseCase getGisuUseCase;
    private final GetChapterUseCase getChapterUseCase;
    private final ManageFormUseCase manageFormUseCase;
    private final ProjectPolicyAuthorizationService projectPolicyAuthorizationService;

    @Audited(
        domain = Domain.PROJECT,
        action = AuditAction.CREATE,
        targetType = "Project",
        targetId = "#result",
        description = "'프로젝트 초안을 생성했습니다.'"
    )
    @Override
    public Long create(CreateDraftProjectCommand command) {
        getGisuUseCase.getById(command.gisuId());

        ChallengerInfo challenger = getChallengerUseCase.getByMemberIdAndGisuId(
            command.productOwnerMemberId(), command.gisuId()
        );
        if (challenger.part() != ChallengerPart.PLAN) {
            throw new ProjectDomainException(ProjectErrorCode.PROJECT_OWNER_NOT_PLAN_CHALLENGER);
        }

        if (loadProjectPort.existsDraftByCreatorAndGisu(command.requesterMemberId(), command.gisuId())) {
            throw new ProjectDomainException(ProjectErrorCode.PROJECT_DRAFT_ALREADY_IN_PROGRESS);
        }

        if (loadProjectPort.existsDraftByOwnerAndGisu(command.productOwnerMemberId(), command.gisuId())) {
            throw new ProjectDomainException(ProjectErrorCode.PROJECT_DRAFT_ALREADY_IN_PROGRESS);
        }

        MemberInfo member = getMemberUseCase.getById(command.productOwnerMemberId());
        ChapterInfo chapter = getChapterUseCase.byGisuAndSchool(command.gisuId(), member.schoolId());

        ProjectPolicyResourceContext authorizationTarget = ProjectPolicyResourceContext.builder()
            .projectTarget(command.gisuId(), chapter.id())
            .creatorMemberId(command.productOwnerMemberId())
            .build();
        if (projectPolicyAuthorizationService.evaluateTrustedResource(
            command.requesterMemberId(),
            ProjectPolicyAction.PROJECT_CREATE,
            ProjectAuthorizationResourceSnapshot.withTargetMemberSchool(
                authorizationTarget, member.schoolId())
        ).effect() != PolicyEffect.ALLOW) {
            throw new ProjectDomainException(ProjectErrorCode.PROJECT_ACCESS_DENIED);
        }

        Project project = Project.createDraft(
            command.gisuId(),
            chapter.id(),
            command.productOwnerMemberId(),
            member.schoolId(),
            command.requesterMemberId()
        );
        return saveProjectPort.save(project).getId();
    }

    @Override
    public ProjectStatus update(UpdateProjectCommand command) {
        Project project = loadProjectPort.getById(command.projectId());
        project.updateBasicInfo(
            command.name(),
            command.description(),
            command.externalLink(),
            command.thumbnailFileId(),
            command.logoFileId()
        );
        return project.getStatus();
    }

    @Override
    public void submit(SubmitProjectCommand command) {
        Project project = loadProjectPort.getById(command.projectId());

        if (!loadProjectApplicationFormPort.existsByProjectId(project.getId())) {
            throw new ProjectDomainException(ProjectErrorCode.PROJECT_SUBMIT_VALIDATION_FAILED);
        }

        project.submit();
    }

    @Override
    public ProjectStatus transfer(TransferProjectOwnershipCommand command) {
        Project project = loadProjectPort.getById(command.projectId());

        ChallengerInfo newOwner = getChallengerUseCase.getByMemberIdAndGisuId(
            command.newOwnerMemberId(), project.getGisuId()
        );
        if (newOwner.part() != ChallengerPart.PLAN) {
            throw new ProjectDomainException(ProjectErrorCode.PROJECT_OWNER_NOT_PLAN_CHALLENGER);
        }

        // 도메인 가드 fail-fast — COMPLETED/ABORTED 시 cross-domain 호출 회피
        project.validateMutable();

        MemberInfo newOwnerMember = getMemberUseCase.getById(command.newOwnerMemberId());
        ChapterInfo newChapter = getChapterUseCase.byGisuAndSchool(
            project.getGisuId(), newOwnerMember.schoolId());
        boolean scopeChanged = !Objects.equals(project.getProductOwnerSchoolId(), newOwnerMember.schoolId())
            || !Objects.equals(project.getChapterId(), newChapter.id());
        if (scopeChanged && loadProjectApplicationPort.existsByProjectId(project.getId())) {
            throw new ProjectDomainException(
                ProjectErrorCode.PROJECT_TRANSFER_OWNERSHIP_APPLICATION_SCOPE_CONFLICT);
        }
        project.transferOwnership(
            command.newOwnerMemberId(), newOwnerMember.schoolId(), newChapter.id());
        return project.getStatus();
    }

    /**
     * Admin 검토 완료 후 공개. PENDING_REVIEW → IN_PROGRESS 전이 + 지원 폼 동반 publish.
     * <ul>
     *   <li>파트별 정원이 1개 이상 등록되어 있어야 함 (PROJECT-105 선행)</li>
     *   <li>지원 폼이 등록되어 있어야 함 (PROJECT-106 선행)</li>
     *   <li>같은 트랜잭션에서 Form 도 PUBLISHED 로 전이</li>
     * </ul>
     */
    @Audited(
        domain = Domain.PROJECT,
        action = AuditAction.PUBLISH,
        targetType = "Project",
        targetId = "#command.projectId()",
        description = "'프로젝트를 게시했습니다.'"
    )
    @Override
    public ProjectStatus publish(PublishProjectCommand command) {
        Project project = loadProjectPort.getById(command.projectId());

        List<ProjectPartQuota> quotas = loadProjectPartQuotaPort.listByProjectId(project.getId());
        if (quotas.isEmpty()) {
            throw new ProjectDomainException(ProjectErrorCode.PROJECT_PART_QUOTA_REQUIRED);
        }

        ProjectApplicationForm form = loadProjectApplicationFormPort.findByProjectId(project.getId())
            .orElseThrow(() -> new ProjectDomainException(ProjectErrorCode.APPLICATION_FORM_NOT_FOUND));

        project.publish();

        manageFormUseCase.publishForm(PublishFormCommand.builder()
            .formId(form.getFormId())
            .requesterMemberId(command.requesterMemberId())
            .build());

        return project.getStatus();
    }

    /**
     * 프로젝트 hard delete. DRAFT/PENDING_REVIEW 상태에서만 호출 가능하며 자식 row 들을 순서대로 정리한다.
     * <ol>
     *   <li>ProjectApplicationForm 이 등록되어 있으면 Policy → ApplicationForm row → form Form 순으로 정리.
     *       (Form 삭제는 form 도메인의 cascade 가 보장)</li>
     *   <li>ProjectPartQuota 일괄 삭제</li>
     *   <li>ProjectMember 일괄 삭제</li>
     *   <li>Project 삭제</li>
     * </ol>
     * 권한 검증은 Controller 단의 {@code @CheckAccess(DELETE)} + {@link com.umc.product.project.application.service.evaluator.ProjectPermissionEvaluator}
     * 가 담당. 본 Service 는 도메인 상태 invariant 만 책임진다.
     */
    @Audited(
        domain = Domain.PROJECT,
        action = AuditAction.DELETE,
        targetType = "Project",
        targetId = "#command.projectId()",
        description = "'프로젝트를 삭제했습니다.'"
    )
    @Override
    public void delete(DeleteProjectCommand command) {
        Project project = loadProjectPort.getById(command.projectId());
        project.validateDeletable();

        loadProjectApplicationFormPort.findByProjectId(project.getId())
            .ifPresent(form -> {
                saveProjectApplicationFormPolicyPort.deleteAllByApplicationFormId(form.getId());
                saveProjectApplicationFormPort.deleteAllByProjectId(project.getId());
                manageFormUseCase.deleteForm(DeleteFormCommand.builder()
                    .formId(form.getFormId())
                    .requesterMemberId(command.requesterMemberId())
                    .build());
            });

        saveProjectPartQuotaPort.deleteAllByProjectId(project.getId());
        saveProjectMemberPort.deleteAllByProjectId(project.getId());
        saveProjectPort.delete(project);
    }

    /**
     * 프로젝트 중단(abort). IN_PROGRESS → ABORTED 상태 전이 + 자식 도메인 일괄 동기화.
     * <ul>
     *   <li>{@link Project#abort} 로 상태 전이 (COMPLETED/ABORTED 는 도메인 가드가 거부)</li>
     *   <li>ACTIVE 인 ProjectMember 는 모두 WITHDRAWN, statusChangeReason 에 중단 사유 기록</li>
     *   <li>진행 중(DRAFT/SUBMITTED) ProjectApplication 은 모두 CANCELLED</li>
     * </ul>
     * 권한 검증은 Controller 단의 {@code @CheckAccess(MANAGE)} 가 담당.
     */
    @Override
    public void abort(AbortProjectCommand command) {
        Project project = loadProjectPort.getById(command.projectId());
        project.abort(command.reason(), command.requesterMemberId());

        List<ProjectMember> activeMembers = loadProjectMemberPort.listByProjectId(project.getId());
        for (ProjectMember member : activeMembers) {
            member.withdraw(command.reason(), command.requesterMemberId());
        }

        List<ProjectApplication> inProgressApplications =
            loadProjectApplicationPort.listInProgressByProjectId(project.getId());
        for (ProjectApplication application : inProgressApplications) {
            application.cancel(command.requesterMemberId(), command.reason());
        }
    }
}
