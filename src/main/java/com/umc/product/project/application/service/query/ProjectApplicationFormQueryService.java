package com.umc.product.project.application.service.query;

import static com.umc.product.project.application.authorization.ProjectPolicyDecisionOutcomes.enumValue;

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

import com.umc.product.authorization.domain.SubjectAttributes;
import com.umc.product.authorization.domain.policy.PolicyDecision;
import com.umc.product.authorization.domain.policy.PolicyEffect;
import com.umc.product.common.domain.enums.ChallengerPart;
import com.umc.product.form.application.port.in.query.GetFormUseCase;
import com.umc.product.form.application.port.in.query.dto.FormWithStructureInfo;
import com.umc.product.project.application.authorization.ProjectPolicyAction;
import com.umc.product.project.application.authorization.ProjectPolicyAuthorizationService;
import com.umc.product.project.application.authorization.ProjectPolicyChallengerTuple;
import com.umc.product.project.application.authorization.ProjectPolicyOutcomes;
import com.umc.product.project.application.authorization.ProjectPolicyResourceContext;
import com.umc.product.project.application.authorization.ProjectPolicySubjectSnapshot;
import com.umc.product.project.application.authorization.rollout.ProjectAuthorizationEvaluationPoint;
import com.umc.product.project.application.authorization.rollout.ProjectAuthorizationInternalOrigin;
import com.umc.product.project.application.authorization.rollout.ProjectAuthorizationSurface;
import com.umc.product.project.application.port.in.query.GetProjectApplicationFormUseCase;
import com.umc.product.project.application.port.in.query.dto.ApplicationFormInfo;
import com.umc.product.project.application.port.out.LoadProjectApplicationFormPolicyPort;
import com.umc.product.project.application.port.out.LoadProjectApplicationFormPort;
import com.umc.product.project.domain.Project;
import com.umc.product.project.domain.ProjectApplicationForm;
import com.umc.product.project.domain.ProjectApplicationFormPolicy;
import com.umc.product.project.domain.enums.FormSectionType;
import com.umc.product.project.domain.exception.ProjectDomainException;
import com.umc.product.project.domain.exception.ProjectErrorCode;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProjectApplicationFormQueryService implements GetProjectApplicationFormUseCase {

    private final LoadProjectApplicationFormPort loadApplicationFormPort;
    private final LoadProjectApplicationFormPolicyPort loadPolicyPort;
    private final GetFormUseCase getFormUseCase;
    private final ProjectPolicyAuthorizationService policyAuthorizationService;

    @Override
    public Optional<ApplicationFormInfo> findByProjectId(Long projectId, Long requesterMemberId) {
        Optional<ProjectApplicationForm> form = loadApplicationFormPort.findByProjectId(projectId);
        if (form.isEmpty()) {
            return Optional.empty();
        }
        ProjectPolicySubjectSnapshot snapshot = policyAuthorizationService.snapshot(requesterMemberId);
        return Optional.of(assemble(
            form.orElseThrow(),
            snapshot,
            ProjectAuthorizationEvaluationPoint.surface(ProjectAuthorizationSurface.REST_FORM_READ)
        ));
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
        return assembleBatch(
            uniqueProjectIds,
            formsByProjectId,
            policyAuthorizationService.snapshot(requesterMemberId),
            ProjectAuthorizationEvaluationPoint.internal(ProjectAuthorizationInternalOrigin.FORM_ACCESS_POLICY)
        );
    }

    @Override
    public Map<Long, ApplicationFormInfo> findAllByProjectIds(
        Collection<Long> projectIds,
        SubjectAttributes subject
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
        return assembleBatch(
            uniqueProjectIds,
            formsByProjectId,
            policyAuthorizationService.snapshot(subject),
            ProjectAuthorizationEvaluationPoint.surface(ProjectAuthorizationSurface.GRAPHQL_APPLICATION_FORM)
        );
    }

    private Map<Long, ApplicationFormInfo> assembleBatch(
        List<Long> uniqueProjectIds,
        Map<Long, ProjectApplicationForm> formsByProjectId,
        ProjectPolicySubjectSnapshot snapshot,
        ProjectAuthorizationEvaluationPoint evaluationPoint
    ) {
        List<ProjectApplicationForm> forms = List.copyOf(formsByProjectId.values());
        Map<Long, FormView> views = resolveViews(forms, snapshot, evaluationPoint);
        Map<Long, FormWithStructureInfo> structures =
            getFormUseCase.batchGetFormsWithStructure(formIds(forms));
        Map<Long, List<ProjectApplicationFormPolicy>> policies =
            loadPolicyPort.listByApplicationFormIds(applicationFormIds(forms));

        Map<Long, ApplicationFormInfo> result = new LinkedHashMap<>();
        for (Long projectId : uniqueProjectIds) {
            ProjectApplicationForm form = formsByProjectId.get(projectId);
            if (form == null) {
                continue;
            }
            result.put(projectId, assemble(
                form,
                views.get(projectId),
                snapshot,
                structures.get(form.getFormId()),
                policies.getOrDefault(form.getId(), List.of())
            ));
        }
        return Map.copyOf(result);
    }

    private ApplicationFormInfo assemble(
        ProjectApplicationForm form,
        ProjectPolicySubjectSnapshot snapshot,
        ProjectAuthorizationEvaluationPoint evaluationPoint
    ) {
        FormView view = resolveView(form.getProject(), snapshot, evaluationPoint);
        FormWithStructureInfo structure = getFormUseCase.getFormWithStructure(form.getFormId());
        List<ProjectApplicationFormPolicy> policies = loadPolicyPort.listByApplicationFormId(form.getId());
        return assemble(form, view, snapshot, structure, policies);
    }

    private ApplicationFormInfo assemble(
        ProjectApplicationForm form,
        FormView view,
        ProjectPolicySubjectSnapshot snapshot,
        FormWithStructureInfo structure,
        List<ProjectApplicationFormPolicy> policies
    ) {
        if (view == FormView.FULL) {
            return ApplicationFormInfo.of(form, structure, policies);
        }
        ChallengerPart applicantPart = applicantPart(snapshot, form.getProject().getGisuId());
        requireMatchingPartSection(policies, applicantPart);
        return ApplicationFormInfo.forApplicant(form, structure, policies, applicantPart);
    }

    private Map<Long, FormView> resolveViews(
        List<ProjectApplicationForm> forms,
        ProjectPolicySubjectSnapshot snapshot,
        ProjectAuthorizationEvaluationPoint evaluationPoint
    ) {
        Map<Long, FormView> result = new LinkedHashMap<>();
        for (ProjectApplicationForm form : forms) {
            Project project = form.getProject();
            result.put(project.getId(), resolveView(project, snapshot, evaluationPoint));
        }
        return Map.copyOf(result);
    }

    private FormView resolveView(
        Project project,
        ProjectPolicySubjectSnapshot snapshot,
        ProjectAuthorizationEvaluationPoint evaluationPoint
    ) {
        PolicyDecision decision = policyAuthorizationService.evaluate(
            snapshot,
            ProjectPolicyAction.FORM_READ,
            ProjectPolicyResourceContext.builder()
                .project(project.getId(), project.getGisuId(), project.getChapterId(), project.getStatus())
                .productOwnerMemberId(project.getProductOwnerMemberId())
                .build(),
            evaluationPoint,
            Optional.empty()
        );
        if (decision.effect() != PolicyEffect.ALLOW) {
            throw accessDenied();
        }
        FormView view = FormView.valueOf(enumValue(decision, ProjectPolicyOutcomes.FORM_VIEW, FormView.NONE.name()));
        if (view == FormView.NONE) {
            throw accessDenied();
        }
        return view;
    }

    private ChallengerPart applicantPart(ProjectPolicySubjectSnapshot snapshot, Long gisuId) {
        return snapshot.challengers().stream()
            .filter(challenger -> Objects.equals(challenger.gisuId(), gisuId))
            .map(ProjectPolicyChallengerTuple::part)
            .findFirst()
            .orElseThrow(this::accessDenied);
    }

    private void requireMatchingPartSection(
        List<ProjectApplicationFormPolicy> policies,
        ChallengerPart applicantPart
    ) {
        boolean hasPartSection = policies.stream()
            .anyMatch(policy -> policy.getType() == FormSectionType.PART
                && policy.getAllowedParts().contains(applicantPart));
        if (!hasPartSection) {
            throw accessDenied();
        }
    }

    private Set<Long> formIds(List<ProjectApplicationForm> forms) {
        return forms.stream()
            .map(ProjectApplicationForm::getFormId)
            .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    private Set<Long> applicationFormIds(List<ProjectApplicationForm> forms) {
        return forms.stream()
            .map(ProjectApplicationForm::getId)
            .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    private ProjectDomainException accessDenied() {
        return new ProjectDomainException(ProjectErrorCode.APPLICATION_FORM_ACCESS_NOT_ALLOWED);
    }

    private enum FormView {
        FULL,
        APPLICANT,
        NONE
    }
}
