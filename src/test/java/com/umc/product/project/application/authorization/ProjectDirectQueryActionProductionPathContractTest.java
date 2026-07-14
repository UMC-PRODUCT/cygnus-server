package com.umc.product.project.application.authorization;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import com.umc.product.authorization.application.port.in.CheckPermissionUseCase;
import com.umc.product.authorization.domain.SubjectAttributes;
import com.umc.product.authorization.domain.SubjectPolicyFacts;
import com.umc.product.challenger.application.port.in.query.GetChallengerUseCase;
import com.umc.product.form.application.port.in.query.GetFormUseCase;
import com.umc.product.project.application.authorization.ProjectDirectActionBindings.Binding;
import com.umc.product.project.application.authorization.ProjectDirectActionBindings.Caller;
import com.umc.product.project.application.port.out.LoadProjectApplicationFormPolicyPort;
import com.umc.product.project.application.port.out.LoadProjectApplicationFormPort;
import com.umc.product.project.application.port.out.LoadProjectMatchingRoundPort;
import com.umc.product.project.application.port.out.LoadProjectMemberPort;
import com.umc.product.project.application.port.out.LoadProjectPartQuotaPort;
import com.umc.product.project.application.port.out.LoadProjectPort;
import com.umc.product.project.application.service.query.ProjectApplicationFormQueryService;
import com.umc.product.project.application.service.query.ProjectMatchingRoundQueryService;
import com.umc.product.project.application.service.query.ProjectPermissionQueryService;
import com.umc.product.project.domain.Project;
import com.umc.product.project.domain.ProjectApplicationForm;
import com.umc.product.project.domain.enums.ProjectStatus;

class ProjectDirectQueryActionProductionPathContractTest
    extends ProjectDirectActionProductionPathContractSupport {

    private final LoadProjectApplicationFormPort loadFormPort = mock(LoadProjectApplicationFormPort.class);
    private final CheckPermissionUseCase checkPermissionUseCase = mock(CheckPermissionUseCase.class);

    private final ProjectApplicationFormQueryService formQueryService = new ProjectApplicationFormQueryService(
        loadFormPort, mock(LoadProjectApplicationFormPolicyPort.class), mock(GetFormUseCase.class), policyService);
    private final ProjectMatchingRoundQueryService matchingQueryService = new ProjectMatchingRoundQueryService(
        mock(LoadProjectMatchingRoundPort.class), policyService);
    private final ProjectPermissionQueryService permissionQueryService = new ProjectPermissionQueryService(
        checkPermissionUseCase,
        mock(LoadProjectPort.class),
        loadFormPort,
        mock(LoadProjectPartQuotaPort.class),
        mock(LoadProjectMemberPort.class),
        mock(LoadProjectMatchingRoundPort.class),
        mock(GetChallengerUseCase.class),
        policyService
    );

    @BeforeEach
    void setUpResources() {
        Project project = mock(Project.class);
        given(project.getId()).willReturn(PROJECT_ID);
        given(project.getGisuId()).willReturn(GISU_ID);
        given(project.getChapterId()).willReturn(CHAPTER_ID);
        given(project.getStatus()).willReturn(ProjectStatus.DRAFT);
        given(project.getProductOwnerMemberId()).willReturn(MEMBER_ID);
        ProjectApplicationForm form = mock(ProjectApplicationForm.class);
        given(form.getProject()).willReturn(project);
        given(loadFormPort.findByProjectId(PROJECT_ID)).willReturn(Optional.of(form));

        SubjectAttributes subject = new SubjectPolicyFacts(EVALUATED_AT, List.of(), List.of(), Map.of())
            .toSubjectAttributes(MEMBER_ID, 1L);
        given(checkPermissionUseCase.loadSubject(MEMBER_ID)).willReturn(subject);
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("queryBindings")
    @DisplayName("query의 direct action은 실제 service에서 exact policy action으로 평가된다")
    void direct_query_action이_production_path에서_exact_action으로_평가된다(Binding binding) {
        assertExactPolicyAction(binding.action(), () -> invoke(binding.action()));
    }

    private void invoke(ProjectPolicyAction action) {
        switch (action) {
            case FORM_READ -> formQueryService.findByProjectId(PROJECT_ID, MEMBER_ID);
            case MATCHING_LIST -> matchingQueryService.list(MEMBER_ID, GISU_ID, CHAPTER_ID, null);
            case CAPABILITY_LIST -> permissionQueryService.listByProjectIds(MEMBER_ID, List.of(PROJECT_ID));
            default -> throw new IllegalArgumentException("query direct action이 아닙니다: " + action);
        }
    }

    private static Stream<Binding> queryBindings() {
        return ProjectDirectActionBindings.valuesFor(
            Caller.FORM_QUERY,
            Caller.MATCHING_QUERY,
            Caller.PERMISSION_QUERY
        ).stream();
    }
}
