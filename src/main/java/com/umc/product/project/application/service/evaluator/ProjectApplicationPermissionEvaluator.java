package com.umc.product.project.application.service.evaluator;

import java.util.Objects;

import org.springframework.stereotype.Component;

import com.umc.product.authorization.application.port.out.ResourcePermissionEvaluator;
import com.umc.product.authorization.domain.ResourcePermission;
import com.umc.product.authorization.domain.ResourceType;
import com.umc.product.authorization.domain.SubjectAttributes;
import com.umc.product.common.domain.enums.ChallengerRoleType;
import com.umc.product.project.application.port.out.LoadProjectApplicationPort;
import com.umc.product.project.application.port.out.LoadProjectMemberPort;
import com.umc.product.project.application.port.out.LoadProjectPort;
import com.umc.product.project.domain.Project;
import com.umc.product.project.domain.ProjectApplication;
import com.umc.product.project.domain.enums.ProjectApplicationStatus;
import com.umc.product.project.domain.exception.ProjectDomainException;
import com.umc.product.project.domain.exception.ProjectErrorCode;

import lombok.RequiredArgsConstructor;

/**
 * ProjectApplication 도메인 단건 액션의 권한 판정 (L2). subject × resource 속성 매칭만 다룬다.
 * <p>
 * 도메인 규칙(부모 프로젝트 status, 매칭 라운드 OPEN, 폼 정책 파트, 기수 ACTIVE 멤버, 동일 라운드 중복)은
 * {@code Project.validateApplicable()} / {@code ProjectApplicationCommandService} 가 책임진다.
 * <p>
 * resourceId 의미는 권한별로 다르다:
 * <ul>
 *   <li>{@code WRITE} — 부모 프로젝트의 ID (지원서 인스턴스가 아직 없으므로). 컨벤션상 약한 비대칭이지만 기수 매칭/자기지원을 L1 에서 처리하기 위함.</li>
 *   <li>{@code READ}/{@code EDIT}/{@code DELETE}/{@code APPROVE} — 지원서 인스턴스의 ID.</li>
 * </ul>
 */
@Component
@RequiredArgsConstructor
public class ProjectApplicationPermissionEvaluator implements ResourcePermissionEvaluator {

    private final LoadProjectPort loadProjectPort;
    private final LoadProjectApplicationPort loadProjectApplicationPort;
    private final LoadProjectMemberPort loadProjectMemberPort;
    private final SuperAdminProperties superAdminProperties;

    @Override
    public ResourceType supportedResourceType() {
        return ResourceType.PROJECT_APPLICATION;
    }

    @Override
    public boolean evaluate(SubjectAttributes subject, ResourcePermission permission) {
        return switch (permission.permission()) {
            case WRITE -> canWrite(subject, permission);
            case READ -> canRead(subject, permission);
            case EDIT -> canEdit(subject, permission);
            case DELETE -> canDelete(subject, permission);
            case APPROVE -> canApprove(subject, permission);
            default -> false;
        };
    }

    /**
     * 지원서 생성 진입 권한. subject 가 부모 프로젝트의 기수에 챌린저 레코드를 가져야 한다.
     * <p>
     * IN_PROGRESS 검사 / 자기지원 차단 / 매칭 라운드 OPEN / 폼 정책 파트 / 기수 ACTIVE 멤버 / 중복 등은
     * 도메인 규칙으로 분류되어 {@code Project.validateApplicable()} 와 {@code ProjectApplicationCommandService} 가 검증한다.
     */
    private boolean canWrite(SubjectAttributes subject, ResourcePermission permission) {
        Project project = loadProject(permission);
        return subject.gisuChallengerInfos().stream()
            .anyMatch(info -> Objects.equals(info.gisuId(), project.getGisuId()));
    }

    /**
     * 단건 READ.
     * <p>
     * resourceId 가 없으면 목록 컨텍스트로 보고 통과(스코프 리졸버가 거른다).
     * <p>
     * 단건은 다음 중 하나에 해당해야 한다:
     * <ul>
     *   <li>지원자 본인</li>
     *   <li>부모 프로젝트의 PO 또는 보조 PM (Sub-PM, ACTIVE PLAN 멤버)</li>
     *   <li>(SUBMITTED 이상) SUPER_ADMIN, 해당 기수의 총괄/부총괄 또는 해당 기수+해당 지부의 지부장</li>
     * </ul>
     * DRAFT 는 본인만 노출 — 임시저장은 외부에 보이지 않는다. 단, {@code app.super-admin.allow-draft-read} 가 켜진 동안은
     * SUPER_ADMIN 도 DRAFT 단건을 조회할 수 있다 (초기 배포 모니터링용).
     */
    private boolean canRead(SubjectAttributes subject, ResourcePermission permission) {
        if (permission.resourceId() == null) {
            return true;
        }
        ProjectApplication application = loadApplication(permission);
        Project project = application.getApplicationForm().getProject();

        if (isApplicant(subject, application)) {
            return true;
        }
        if (application.getStatus() == ProjectApplicationStatus.DRAFT) {
            return superAdminProperties.allowDraftRead() && isSuperAdmin(subject);
        }
        if (isOwner(subject, project) || isSubPm(subject, project)) {
            return true;
        }
        return isCentralCoreInGisu(subject, project.getGisuId())
            || isChapterPresidentOf(subject, project.getChapterId(), project.getGisuId());
    }

    /** 임시저장(DRAFT) 상태에서만 본인이 수정 가능. */
    private boolean canEdit(SubjectAttributes subject, ResourcePermission permission) {
        ProjectApplication application = loadApplication(permission);
        return isApplicant(subject, application)
            && application.getStatus() == ProjectApplicationStatus.DRAFT;
    }

    /** 본인이 DRAFT/SUBMITTED 단계에서 철회 가능. 종결 상태(APPROVED/REJECTED)와 이미 철회된 CANCELLED 는 차단. */
    private boolean canDelete(SubjectAttributes subject, ResourcePermission permission) {
        ProjectApplication application = loadApplication(permission);
        if (!isApplicant(subject, application)) {
            return false;
        }
        return switch (application.getStatus()) {
            case DRAFT, SUBMITTED -> true;
            case APPROVED, REJECTED, CANCELLED -> false;
        };
    }

    /**
     * 합불 결정. 부모 프로젝트의 PO 가 SUBMITTED / APPROVED / REJECTED 상태에서 자유롭게 토글한다.
     * SUPER_ADMIN 은 기수/프로젝트 소유 여부와 관계없이 토글할 수 있다.
     * 차수 진행 중 잠금 해제는 도메인 메서드({@code ProjectMatchingRound.validateIsMutableAt}) 가 책임진다.
     * (자동 매칭 스케줄러는 권한 모델 외)
     */
    private boolean canApprove(SubjectAttributes subject, ResourcePermission permission) {
        ProjectApplication application = loadApplication(permission);
        if (!isDecidableStatus(application.getStatus())) {
            return false;
        }
        Project project = application.getApplicationForm().getProject();
        return isOwner(subject, project) || isSuperAdmin(subject);
    }

    private boolean isDecidableStatus(ProjectApplicationStatus status) {
        return status == ProjectApplicationStatus.SUBMITTED
            || status == ProjectApplicationStatus.APPROVED
            || status == ProjectApplicationStatus.REJECTED;
    }

    private boolean isApplicant(SubjectAttributes subject, ProjectApplication application) {
        return Objects.equals(subject.memberId(), application.getApplicantMemberId());
    }

    private boolean isOwner(SubjectAttributes subject, Project project) {
        return Objects.equals(subject.memberId(), project.getProductOwnerMemberId());
    }

    private boolean isSubPm(SubjectAttributes subject, Project project) {
        return loadProjectMemberPort.isActivePlanMember(project.getId(), subject.memberId());
    }

    private boolean isSuperAdmin(SubjectAttributes subject) {
        return subject.toAuthoritySnapshot().isSuperAdmin();
    }

    private boolean isCentralCoreInGisu(SubjectAttributes subject, Long gisuId) {
        return subject.toAuthoritySnapshot().isCentralCoreInGisu(gisuId);
    }

    private boolean isChapterPresidentOf(SubjectAttributes subject, Long chapterId, Long gisuId) {
        return subject.roleAttributes().stream()
            .anyMatch(role -> role.roleType() == ChallengerRoleType.CHAPTER_PRESIDENT
                && Objects.equals(role.gisuId(), gisuId)
                && Objects.equals(role.organizationId(), chapterId));
    }

    private Project loadProject(ResourcePermission permission) {
        Long projectId = permission.getResourceIdAsLong();
        return loadProjectPort.findById(projectId)
            .orElseThrow(() -> new ProjectDomainException(ProjectErrorCode.PROJECT_NOT_FOUND));
    }

    private ProjectApplication loadApplication(ResourcePermission permission) {
        Long applicationId = permission.getResourceIdAsLong();
        return loadProjectApplicationPort.findById(applicationId)
            .orElseThrow(() -> new ProjectDomainException(ProjectErrorCode.PROJECT_APPLICATION_NOT_FOUND));
    }
}
