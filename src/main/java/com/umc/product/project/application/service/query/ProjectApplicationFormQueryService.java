package com.umc.product.project.application.service.query;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.umc.product.authorization.application.port.in.query.GetChallengerRoleUseCase;
import com.umc.product.authorization.application.port.in.query.dto.ChallengerRoleInfo;
import com.umc.product.challenger.application.port.in.query.GetChallengerUseCase;
import com.umc.product.common.domain.enums.ChallengerPart;
import com.umc.product.common.domain.enums.ChallengerRoleType;
import com.umc.product.form.application.port.in.query.GetFormUseCase;
import com.umc.product.form.application.port.in.query.dto.FormWithStructureInfo;
import com.umc.product.project.application.port.in.query.GetProjectApplicationFormUseCase;
import com.umc.product.project.application.port.in.query.dto.ApplicationFormInfo;
import com.umc.product.project.application.port.out.LoadProjectApplicationFormPolicyPort;
import com.umc.product.project.application.port.out.LoadProjectApplicationFormPort;
import com.umc.product.project.domain.Project;
import com.umc.product.project.domain.ProjectApplicationForm;
import com.umc.product.project.domain.ProjectApplicationFormPolicy;
import com.umc.product.project.domain.exception.ProjectDomainException;
import com.umc.product.project.domain.exception.ProjectErrorCode;

import lombok.RequiredArgsConstructor;

/**
 * 지원 폼 조회 서비스 (PROJECT-106-GET).
 * <p>
 * 폼 메타와 섹션→질문→옵션 nested 구조는 Form 도메인에 위임하며, Project 도메인의 정책({@link ProjectApplicationFormPolicy}) 을 합성해 단일 응답을 만든다.
 * 호출자 역할에 따라 전체/마스킹된 섹션을 차등 노출한다.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProjectApplicationFormQueryService implements GetProjectApplicationFormUseCase {

    private final LoadProjectApplicationFormPort loadApplicationFormPort;
    private final LoadProjectApplicationFormPolicyPort loadPolicyPort;

    // Cross-domain
    private final GetFormUseCase getFormUseCase;
    private final GetChallengerUseCase getChallengerUseCase;
    private final GetChallengerRoleUseCase getChallengerRoleUseCase;

    @Override
    public Optional<ApplicationFormInfo> findByProjectId(Long projectId, Long requesterMemberId) {
        return loadApplicationFormPort.findByProjectId(projectId)
            .map(applicationForm -> assemble(applicationForm, requesterMemberId));
    }

    @Override
    public Map<Long, ApplicationFormInfo> findAllByProjectIds(
        Collection<Long> projectIds,
        Long requesterMemberId
    ) {
        List<Long> uniqueProjectIds = projectIds.stream()
            .collect(Collectors.collectingAndThen(
                Collectors.toCollection(LinkedHashSet::new),
                List::copyOf
            ));
        Map<Long, ProjectApplicationForm> formsByProjectId =
            loadApplicationFormPort.findAllByProjectIds(uniqueProjectIds);
        if (formsByProjectId.isEmpty()) {
            return Map.of();
        }

        List<ProjectApplicationForm> applicationForms = List.copyOf(formsByProjectId.values());
        Map<Long, Boolean> fullViewAllowedByProjectId =
            resolveFullViewAllowed(applicationForms, requesterMemberId);
        Map<Long, ChallengerPart> applicantPartsByGisuId =
            resolveApplicantParts(applicationForms, fullViewAllowedByProjectId, requesterMemberId);
        Map<Long, FormWithStructureInfo> formStructuresByFormId =
            getFormUseCase.batchGetFormsWithStructure(formIds(applicationForms));
        Map<Long, List<ProjectApplicationFormPolicy>> policiesByApplicationFormId =
            loadPolicyPort.listByApplicationFormIds(applicationFormIds(applicationForms));

        Map<Long, ApplicationFormInfo> result = new LinkedHashMap<>();
        for (Long projectId : uniqueProjectIds) {
            ProjectApplicationForm applicationForm = formsByProjectId.get(projectId);
            if (applicationForm != null) {
                result.put(projectId, assemble(
                    applicationForm,
                    fullViewAllowedByProjectId.getOrDefault(projectId, false),
                    applicantPartsByGisuId,
                    formStructuresByFormId,
                    policiesByApplicationFormId
                ));
            }
        }
        return result;
    }

    private ApplicationFormInfo assemble(ProjectApplicationForm applicationForm, Long requesterMemberId) {
        Project project = applicationForm.getProject();

        // 권한 체크를 먼저 수행하여 불필요한 데이터 조회를 방지
        boolean isFullViewAllowed = canViewFullForm(project, requesterMemberId);
        ChallengerPart applicantPart = null;

        if (!isFullViewAllowed) {
            applicantPart = getChallengerUseCase
                .findByMemberIdAndGisuId(requesterMemberId, project.getGisuId())
                .orElseThrow(() -> new ProjectDomainException(ProjectErrorCode.APPLICATION_FORM_ACCESS_NOT_ALLOWED))
                .part();
        }

        // 권한 확인이 완료된 경우에만 데이터 조회
        FormWithStructureInfo formStructure = getFormUseCase.getFormWithStructure(applicationForm.getFormId());
        List<ProjectApplicationFormPolicy> policies =
            loadPolicyPort.listByApplicationFormId(applicationForm.getId());

        return isFullViewAllowed
            ? ApplicationFormInfo.of(applicationForm, formStructure, policies)
            : ApplicationFormInfo.forApplicant(applicationForm, formStructure, policies, applicantPart);
    }

    private ApplicationFormInfo assemble(
        ProjectApplicationForm applicationForm,
        boolean isFullViewAllowed,
        Map<Long, ChallengerPart> applicantPartsByGisuId,
        Map<Long, FormWithStructureInfo> formStructuresByFormId,
        Map<Long, List<ProjectApplicationFormPolicy>> policiesByApplicationFormId
    ) {
        Project project = applicationForm.getProject();
        ChallengerPart applicantPart = null;

        if (!isFullViewAllowed) {
            applicantPart = applicantPartsByGisuId.get(project.getGisuId());
            if (applicantPart == null) {
                throw new ProjectDomainException(ProjectErrorCode.APPLICATION_FORM_ACCESS_NOT_ALLOWED);
            }
        }

        FormWithStructureInfo formStructure = formStructuresByFormId.get(applicationForm.getFormId());
        List<ProjectApplicationFormPolicy> policies =
            policiesByApplicationFormId.getOrDefault(applicationForm.getId(), List.of());

        return isFullViewAllowed
            ? ApplicationFormInfo.of(applicationForm, formStructure, policies)
            : ApplicationFormInfo.forApplicant(applicationForm, formStructure, policies, applicantPart);
    }

    /**
     * 정책 우회 가능 여부. PM(owner) / Central Core / 프로젝트 지부의 지부장 만 전체 섹션을 본다. {@code ProjectPermissionEvaluator#canEdit} 의 외부
     * 운영진 정의와 정합을 맞춘다.
     */
    private boolean canViewFullForm(Project project, Long requesterMemberId) {
        if (Objects.equals(requesterMemberId, project.getProductOwnerMemberId())) {
            return true;
        }
        if (getChallengerRoleUseCase.isSuperAdmin(requesterMemberId)) {
            return true;
        }
        if (getChallengerRoleUseCase.isCentralCoreInGisu(requesterMemberId, project.getGisuId())) {
            return true;
        }
        return getChallengerRoleUseCase.isChapterPresidentInGisu(
            requesterMemberId, project.getGisuId(), project.getChapterId());
    }

    private Map<Long, Boolean> resolveFullViewAllowed(
        List<ProjectApplicationForm> applicationForms,
        Long requesterMemberId
    ) {
        boolean needsRoleLookup = applicationForms.stream()
            .map(ProjectApplicationForm::getProject)
            .anyMatch(project -> !Objects.equals(requesterMemberId, project.getProductOwnerMemberId()));
        List<ChallengerRoleInfo> roles = needsRoleLookup
            ? getChallengerRoleUseCase.findAllByMemberId(requesterMemberId)
            : List.of();
        boolean superAdmin = needsRoleLookup && getChallengerRoleUseCase.isSuperAdmin(requesterMemberId);
        Map<Long, List<ChallengerRoleInfo>> rolesByGisuId = roles.stream()
            .filter(role -> role.gisuId() != null)
            .collect(Collectors.groupingBy(ChallengerRoleInfo::gisuId));

        Map<Long, Boolean> result = new LinkedHashMap<>();
        for (ProjectApplicationForm applicationForm : applicationForms) {
            Project project = applicationForm.getProject();
            result.put(project.getId(), canViewFullForm(project, requesterMemberId, superAdmin, rolesByGisuId));
        }
        return result;
    }

    private boolean canViewFullForm(
        Project project,
        Long requesterMemberId,
        boolean superAdmin,
        Map<Long, List<ChallengerRoleInfo>> rolesByGisuId
    ) {
        if (Objects.equals(requesterMemberId, project.getProductOwnerMemberId())) {
            return true;
        }
        if (superAdmin) {
            return true;
        }

        List<ChallengerRoleInfo> rolesInGisu = rolesByGisuId.getOrDefault(project.getGisuId(), List.of());
        if (rolesInGisu.stream().map(ChallengerRoleInfo::roleType).anyMatch(ChallengerRoleType::isAtLeastCentralCore)) {
            return true;
        }
        return rolesInGisu.stream()
            .anyMatch(role -> role.roleType() == ChallengerRoleType.CHAPTER_PRESIDENT
                && Objects.equals(role.organizationId(), project.getChapterId()));
    }

    private Map<Long, ChallengerPart> resolveApplicantParts(
        List<ProjectApplicationForm> applicationForms,
        Map<Long, Boolean> fullViewAllowedByProjectId,
        Long requesterMemberId
    ) {
        Set<Long> restrictedGisuIds = applicationForms.stream()
            .map(ProjectApplicationForm::getProject)
            .filter(project -> !fullViewAllowedByProjectId.getOrDefault(project.getId(), false))
            .map(Project::getGisuId)
            .collect(Collectors.toCollection(LinkedHashSet::new));
        if (restrictedGisuIds.isEmpty()) {
            return Map.of();
        }

        Map<Long, ChallengerPart> result = new LinkedHashMap<>();
        for (Long gisuId : restrictedGisuIds) {
            getChallengerUseCase.listByMemberIdsAndGisuId(Set.of(requesterMemberId), gisuId)
                .values()
                .stream()
                .findFirst()
                .ifPresent(challenger -> result.put(gisuId, challenger.part()));
        }
        return result;
    }

    private Set<Long> formIds(List<ProjectApplicationForm> applicationForms) {
        return applicationForms.stream()
            .map(ProjectApplicationForm::getFormId)
            .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    private Set<Long> applicationFormIds(List<ProjectApplicationForm> applicationForms) {
        return applicationForms.stream()
            .map(ProjectApplicationForm::getId)
            .collect(Collectors.toCollection(LinkedHashSet::new));
    }
}
