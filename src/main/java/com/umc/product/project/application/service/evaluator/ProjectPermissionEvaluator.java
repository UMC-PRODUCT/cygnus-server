package com.umc.product.project.application.service.evaluator;

import java.util.Objects;

import org.springframework.stereotype.Component;

import com.umc.product.authorization.application.port.out.ResourcePermissionEvaluator;
import com.umc.product.authorization.domain.ResourcePermission;
import com.umc.product.authorization.domain.ResourceType;
import com.umc.product.authorization.domain.SubjectAttributes;
import com.umc.product.common.domain.enums.ChallengerPart;
import com.umc.product.common.domain.enums.ChallengerRoleType;
import com.umc.product.project.application.port.out.LoadProjectPort;
import com.umc.product.project.domain.Project;
import com.umc.product.project.domain.exception.ProjectDomainException;
import com.umc.product.project.domain.exception.ProjectErrorCode;

import lombok.RequiredArgsConstructor;

/**
 * Project 도메인 단건 액션의 권한 판정 (L2). status × 역할 binary 매트릭스만 다룬다.
 * <p>
 * 가시 범위(scoping)는 {@code ProjectAccessScopeResolver} 가, 응답 마스킹은
 * Service/Assembler 의 분기에서 처리.
 */
@Component
@RequiredArgsConstructor
public class ProjectPermissionEvaluator implements ResourcePermissionEvaluator {

    private final LoadProjectPort loadProjectPort;
    private final SuperAdminProperties superAdminProperties;

    @Override
    public ResourceType supportedResourceType() {
        return ResourceType.PROJECT;
    }

    @Override
    public boolean evaluate(SubjectAttributes subject, ResourcePermission permission) {
        return switch (permission.permission()) {
            case READ -> canRead(subject, permission);
            case WRITE -> canWrite(subject);
            case EDIT -> canEdit(subject, permission);
            case MANAGE -> canManage(subject, permission);
            case DELETE -> canDelete(subject, permission);
            default -> false;
        };
    }

    /**
     * 단건 READ 분기. 비공개 상태(DRAFT/PENDING_REVIEW/ABORTED)는 권한자만 노출.
     * <p>
     * 목록 조회는 {@code resourceId} 가 없어 이 분기를 타지 않고 단순 통과 후 L3-A scope 에서 거른다.
     * PR/ABORTED 는 PO + 해당 기수의 총괄단(SUPER_ADMIN 은 글로벌) ∪ 해당 기수의 지부장(chapter 무관).
     * <p>
     * DRAFT 는 PO 만 노출. 단, {@code app.super-admin.allow-draft-read} 가 켜진 동안은 SUPER_ADMIN 도 DRAFT 단건을
     * 조회할 수 있다 (초기 배포 모니터링용).
     */
    private boolean canRead(SubjectAttributes subject, ResourcePermission permission) {
        if (permission.resourceId() == null) {
            return true;
        }
        Project project = loadProject(permission);
        return switch (project.getStatus()) {
            case IN_PROGRESS, COMPLETED -> true;
            case DRAFT -> isOwner(subject, project)
                || (superAdminProperties.allowDraftRead() && isSuperAdmin(subject));
            case PENDING_REVIEW, ABORTED -> isOwner(subject, project)
                || isCentralCoreInGisu(subject, project.getGisuId())
                || isChapterPresidentInGisu(subject, project.getGisuId());
        };
    }

    /**
     * 신규 프로젝트 작성 진입 권한 체크. PLAN 챌린저(본인 PO 등록) 또는
     * 운영진(학교 회장단/지부장/총괄단 — 본인 scope 안의 PLAN 챌린저 임명) 통과.
     * <p>
     * PO target 이 호출자와 다른 경우의 scope 검증은 Service 레벨에서 수행한다.
     */
    private boolean canWrite(SubjectAttributes subject) {
        boolean isPlanChallenger = subject.gisuChallengerInfos().stream()
            .anyMatch(info -> info.part() == ChallengerPart.PLAN);
        if (isPlanChallenger) {
            return true;
        }
        return subject.roleAttributes().stream()
            .anyMatch(role -> role.roleType().isAtLeastCentralCore()
                || role.roleType() == ChallengerRoleType.CHAPTER_PRESIDENT
                || role.roleType() == ChallengerRoleType.SCHOOL_PRESIDENT
                || role.roleType() == ChallengerRoleType.SCHOOL_VICE_PRESIDENT);
    }

    /**
     * 단건 EDIT 분기. EDIT 을 쓰는 모든 액션(기본정보 수정 · 제출 · 소유권 양도 · 팀원 추가/제거)의
     * "누가" 를 여기서 일원화해 판정한다.
     * <ul>
     *   <li>DRAFT: creator 만 — 임시저장 단계는 작성자의 작업 공간.</li>
     *   <li>PENDING_REVIEW / IN_PROGRESS: PO 또는 운영진(해당 기수 총괄단 / 본인 지부장).</li>
     *   <li>COMPLETED / ABORTED: 절대 차단 (도메인 레벨 가드 {@code Project#validateMutable} 와 정합)</li>
     * </ul>
     */
    private boolean canEdit(SubjectAttributes subject, ResourcePermission permission) {
        Project project = loadProject(permission);
        return switch (project.getStatus()) {
            case DRAFT -> isCreator(subject, project);
            case PENDING_REVIEW, IN_PROGRESS ->
                isOwner(subject, project) || isProjectAdmin(subject, project);
            case COMPLETED, ABORTED -> false;
        };
    }

    private boolean isCreator(SubjectAttributes subject, Project project) {
        return Objects.equals(subject.memberId(), project.getCreatorMemberId());
    }

    /**
     * 단건 DELETE 분기. 프로젝트 hard delete 액션의 권한 판정.
     * <ul>
     *   <li>DRAFT / PENDING_REVIEW: PO 본인 또는 운영진(해당 기수 총괄단 / 본인 지부장)만 통과.</li>
     *   <li>IN_PROGRESS / COMPLETED / ABORTED: 절대 차단 — abort() 로 상태 전이만 허용.</li>
     * </ul>
     */
    private boolean canDelete(SubjectAttributes subject, ResourcePermission permission) {
        Project project = loadProject(permission);
        return switch (project.getStatus()) {
            case DRAFT, PENDING_REVIEW -> isOwner(subject, project) || isProjectAdmin(subject, project);
            case IN_PROGRESS, COMPLETED, ABORTED -> false;
        };
    }

    /**
     * 운영진 전용 액션 (publish / abort / complete / 정원 설정 등). PM 은 차단 — Admin 검토 우회 방지.
     * <p>
     * 해당 기수의 총괄단(SUPER_ADMIN 은 글로벌) 또는 본인 지부장(해당 기수)만 통과.
     * 종료 상태(COMPLETED/ABORTED)는 절대 차단.
     * <p>
     * {@code resourceId} 가 없으면 특정 프로젝트가 아닌 타입 전체에 대한 진입 게이트로 동작한다
     * (배치 complete 등 다건 액션의 Controller 관문). 이때는 리소스를 로드하지 않고 운영진 자격만 본다.
     * 프로젝트별 상태·scope 검증은 각 리소스 id 로 다시 evaluate 하는 Service 레이어가 담당한다.
     */
    private boolean canManage(SubjectAttributes subject, ResourcePermission permission) {
        if (permission.resourceId() == null) {
            return hasAnyProjectAdminRole(subject);
        }
        Project project = loadProject(permission);
        return switch (project.getStatus()) {
            case PENDING_REVIEW, IN_PROGRESS -> isProjectAdmin(subject, project);
            case DRAFT, COMPLETED, ABORTED -> false;
        };
    }

    /**
     * 특정 프로젝트와 무관하게, 어느 기수/지부에서든 프로젝트 운영진(총괄단 또는 지부장) 역할을 가졌는지 여부.
     * 타입 전체 MANAGE 진입 게이트 판정에 사용한다.
     */
    private boolean hasAnyProjectAdminRole(SubjectAttributes subject) {
        return subject.roleAttributes().stream()
            .anyMatch(role -> role.roleType().isAtLeastCentralCore()
                || role.roleType() == ChallengerRoleType.CHAPTER_PRESIDENT);
    }

    /**
     * 해당 프로젝트에 대한 운영진 권한 보유 여부. 해당 기수 총괄단(SUPER_ADMIN 은 글로벌)
     * 또는 해당 프로젝트 지부의 지부장(해당 기수)이면 true.
     */
    private boolean isProjectAdmin(SubjectAttributes subject, Project project) {
        return isCentralCoreInGisu(subject, project.getGisuId())
            || isChapterPresidentOf(subject, project.getChapterId(), project.getGisuId());
    }

    private boolean isChapterPresidentOf(SubjectAttributes subject, Long chapterId, Long gisuId) {
        return subject.roleAttributes().stream()
            .anyMatch(role -> role.roleType() == ChallengerRoleType.CHAPTER_PRESIDENT
                && Objects.equals(role.gisuId(), gisuId)
                && Objects.equals(role.organizationId(), chapterId));
    }

    private boolean isChapterPresidentInGisu(SubjectAttributes subject, Long gisuId) {
        return subject.roleAttributes().stream()
            .anyMatch(role -> role.roleType() == ChallengerRoleType.CHAPTER_PRESIDENT
                && Objects.equals(role.gisuId(), gisuId));
    }

    private boolean isSuperAdmin(SubjectAttributes subject) {
        return subject.roleAttributes().stream()
            .anyMatch(role -> role.roleType().isSuperAdmin());
    }

    private boolean isCentralCoreInGisu(SubjectAttributes subject, Long gisuId) {
        return subject.roleAttributes().stream()
            .anyMatch(role -> role.roleType().isSuperAdmin()
                || (role.roleType().isAtLeastCentralCore() && Objects.equals(role.gisuId(), gisuId)));
    }

    private boolean isOwner(SubjectAttributes subject, Project project) {
        return Objects.equals(subject.memberId(), project.getProductOwnerMemberId());
    }

    private Project loadProject(ResourcePermission permission) {
        Long projectId = permission.getResourceIdAsLong();
        return loadProjectPort.findById(projectId)
            .orElseThrow(() -> new ProjectDomainException(ProjectErrorCode.PROJECT_NOT_FOUND));
    }
}
