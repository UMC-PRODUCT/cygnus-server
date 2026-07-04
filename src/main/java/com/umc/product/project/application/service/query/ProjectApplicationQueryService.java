package com.umc.product.project.application.service.query;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.umc.product.challenger.application.port.in.query.GetChallengerUseCase;
import com.umc.product.challenger.application.port.in.query.dto.ChallengerInfo;
import com.umc.product.common.domain.enums.ChallengerPart;
import com.umc.product.form.application.port.in.query.GetFormResponseUseCase;
import com.umc.product.form.application.port.in.query.GetFormUseCase;
import com.umc.product.form.application.port.in.query.dto.AnswerInfo;
import com.umc.product.form.application.port.in.query.dto.FormResponseWithAnswersInfo;
import com.umc.product.form.application.port.in.query.dto.FormWithStructureInfo;
import com.umc.product.project.application.access.ProjectApplicationAccessScope;
import com.umc.product.project.application.access.ProjectApplicationAccessScopeResolver;
import com.umc.product.project.application.port.in.query.GetMyProjectApplicationsUseCase;
import com.umc.product.project.application.port.in.query.GetProjectApplicationDetailUseCase;
import com.umc.product.project.application.port.in.query.SearchProjectApplicationsUseCase;
import com.umc.product.project.application.port.in.query.dto.GetMyProjectApplicationsQuery;
import com.umc.product.project.application.port.in.query.dto.GetProjectApplicationDetailQuery;
import com.umc.product.project.application.port.in.query.dto.ProjectApplicationDetailInfo;
import com.umc.product.project.application.port.in.query.dto.ProjectApplicationSummaryInfo;
import com.umc.product.project.application.port.in.query.dto.SearchProjectApplicationsBatchQuery;
import com.umc.product.project.application.port.in.query.dto.SearchProjectApplicationsQuery;
import com.umc.product.project.application.port.out.LoadProjectApplicationFormPolicyPort;
import com.umc.product.project.application.port.out.LoadProjectApplicationPort;
import com.umc.product.project.application.port.out.LoadProjectPort;
import com.umc.product.project.domain.Project;
import com.umc.product.project.domain.ProjectApplication;
import com.umc.product.project.domain.ProjectApplicationForm;
import com.umc.product.project.domain.ProjectApplicationFormPolicy;
import com.umc.product.project.domain.enums.MatchingType;
import com.umc.product.project.domain.enums.ProjectApplicationStatus;
import com.umc.product.project.domain.exception.ProjectDomainException;
import com.umc.product.project.domain.exception.ProjectErrorCode;
import com.umc.product.storage.application.port.in.query.GetFileUseCase;
import com.umc.product.storage.application.port.in.query.dto.FileInfo;

import lombok.RequiredArgsConstructor;


@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProjectApplicationQueryService
    implements GetMyProjectApplicationsUseCase,
    SearchProjectApplicationsUseCase,
    GetProjectApplicationDetailUseCase {

    private final LoadProjectApplicationPort loadProjectApplicationPort;
    private final LoadProjectPort loadProjectPort;
    private final LoadProjectApplicationFormPolicyPort loadProjectApplicationFormPolicyPort;
    private final ProjectApplicationAccessScopeResolver accessScopeResolver;

    // Cross-domain
    private final GetChallengerUseCase getChallengerUseCase;
    private final GetFileUseCase getFileUseCase;
    private final GetFormUseCase getFormUseCase;
    private final GetFormResponseUseCase getFormResponseUseCase;

    /**
     * 지원서 단건 상세 조회.
     * <p>
     * 권한 검사(4종 staff + 본인 / DRAFT 본인 한정)는 컨트롤러의 {@code @CheckAccess(PROJECT_APPLICATION, READ)} 가 처리하므로 본 메서드 진입 시점에는
     * L2 권한이 검증된 상태이다.
     * <p>
     * 흐름:
     * <ol>
     *   <li>application fetch join 단건 로드 (form/project/matchingRound 한 번에)</li>
     *   <li>정합성 검증: application 의 form.project.id 가 path 의 projectId 와 일치해야 함.
     *       위반/미존재 모두 PROJECT_APPLICATION_NOT_FOUND 로 통일하여 다른 프로젝트의 지원서 존재 여부가 드러나지 않게 함</li>
     *   <li>지원자 파트 단건 조회 (해당 기수 챌린저 invariant -- 누락 시 not-found 로 통일)</li>
     *   <li>폼 구조/정책/응답 본문/첨부 파일 raw 데이터 cross-domain 조회 (storage 만 IN 쿼리 batch)</li>
     *   <li>{@link ProjectApplicationDetailInfo#of} 가 지원자 파트 기준 폼 제한/메타 분리/answers Map 합성까지 한 번에 처리</li>
     * </ol>
     */
    @Override
    public ProjectApplicationDetailInfo getDetail(GetProjectApplicationDetailQuery query) {
        ProjectApplication application = loadProjectApplicationPort.findByIdWithDetails(query.applicationId())
            .orElseThrow(() -> new ProjectDomainException(ProjectErrorCode.PROJECT_APPLICATION_NOT_FOUND));

        ProjectApplicationForm applicationForm = application.getApplicationForm();
        Project project = applicationForm.getProject();
        if (!Objects.equals(project.getId(), query.projectId())) {
            throw new ProjectDomainException(ProjectErrorCode.PROJECT_APPLICATION_NOT_FOUND);
        }

        // 지원자 본인은 시점 제약 없이 조회 가능하며, PM/운영진 등 타인 조회만 지원 종료(endsAt) 이후로 제한한다.
        boolean isApplicantSelf = Objects.equals(application.getApplicantMemberId(), query.requesterMemberId());
        if (!isApplicantSelf && !canViewOngoingMatchingRoundApplications(query.requesterMemberId(), project)) {
            application.getAppliedMatchingRound().validateIsViewableAt(Instant.now());
        }

        ChallengerPart applicantPart = getChallengerUseCase
            .findByMemberIdAndGisuId(application.getApplicantMemberId(), project.getGisuId())
            .map(ChallengerInfo::part)
            .orElseThrow(() -> new ProjectDomainException(ProjectErrorCode.PROJECT_APPLICATION_NOT_FOUND));

        List<ProjectApplicationFormPolicy> formPolicies =
            loadProjectApplicationFormPolicyPort.listByApplicationFormId(applicationForm.getId());
        // dangling formResponseId 는 invariant 위반이지만, 클라이언트에는 PROJECT_APPLICATION_NOT_FOUND 로 통일한다.
        // 다른 도메인 에러가 외부로 새지 않게 하기 위함이다.
        FormResponseWithAnswersInfo formResponseWithAnswers =
            getFormResponseUseCase.findResponseWithAnswers(application.getFormResponseId())
                .orElseThrow(() -> new ProjectDomainException(ProjectErrorCode.PROJECT_APPLICATION_NOT_FOUND));
        // 답변이 존재하는 questionId 기준으로 폼 구조를 조립한다.
        // fork로 비활성화된 구 버전 질문도 Answer.questionId 역추적으로 포함하기 위함이다.
        Set<Long> answeredQuestionIds = formResponseWithAnswers.answers().stream()
            .map(AnswerInfo::questionId)
            .collect(Collectors.toSet());
        FormWithStructureInfo formStructure = getFormUseCase.getFormWithStructureByQuestionIds(
            applicationForm.getFormId(), answeredQuestionIds);
        Map<String, FileInfo> filesByFileId = resolveFiles(formResponseWithAnswers.answers());

        return ProjectApplicationDetailInfo.of(
            application,
            applicantPart,
            formStructure,
            formPolicies,
            formResponseWithAnswers,
            filesByFileId,
            !isApplicantSelf || isStatusVisibleToApplicantSelf(application)
        );
    }

    @Override
    public Map<Long, ProjectApplicationDetailInfo> batchGetDetails(
        Collection<GetProjectApplicationDetailQuery> queries
    ) {
        if (queries == null || queries.isEmpty()) {
            return Map.of();
        }

        Map<Long, GetProjectApplicationDetailQuery> queriesByApplicationId = queries.stream()
            .collect(Collectors.toMap(
                GetProjectApplicationDetailQuery::applicationId,
                Function.identity(),
                (left, right) -> left,
                LinkedHashMap::new
            ));
        if (queriesByApplicationId.isEmpty()) {
            return Map.of();
        }

        List<ProjectApplication> applications =
            loadProjectApplicationPort.batchGetByIdsWithDetails(queriesByApplicationId.keySet());
        validateProjectConsistency(applications, queriesByApplicationId);
        validateMatchingRoundVisibility(applications, queriesByApplicationId);

        Map<Long, ProjectApplication> applicationsById = applications.stream()
            .collect(Collectors.toMap(
                ProjectApplication::getId,
                Function.identity(),
                (left, right) -> left,
                LinkedHashMap::new
            ));
        Map<GisuMemberKey, ChallengerPart> applicantPartsByKey = resolveApplicantParts(applications);
        Map<Long, List<ProjectApplicationFormPolicy>> policiesByApplicationFormId =
            loadProjectApplicationFormPolicyPort.listByApplicationFormIds(applicationFormIds(applications));
        Map<Long, FormResponseWithAnswersInfo> formResponsesById =
            getFormResponseUseCase.findResponsesWithAnswers(formResponseIds(applications));
        Map<Long, Map<String, FileInfo>> filesByApplicationId =
            resolveFilesByApplicationId(applications, formResponsesById);

        Map<FormStructureKey, FormWithStructureInfo> formStructuresByKey = new LinkedHashMap<>();
        Map<Long, ProjectApplicationDetailInfo> result = new LinkedHashMap<>();
        for (Long applicationId : queriesByApplicationId.keySet()) {
            ProjectApplication application = applicationsById.get(applicationId);
            Project project = application.getApplicationForm().getProject();
            FormResponseWithAnswersInfo formResponseWithAnswers =
                getRequiredFormResponse(formResponsesById, application.getFormResponseId());
            FormStructureKey formStructureKey = formStructureKey(application, formResponseWithAnswers);
            FormWithStructureInfo formStructure = formStructuresByKey.computeIfAbsent(
                formStructureKey,
                key -> getFormUseCase.getFormWithStructureByQuestionIds(key.formId(), key.questionIds())
            );

            boolean isApplicantSelf = Objects.equals(
                application.getApplicantMemberId(),
                queriesByApplicationId.get(applicationId).requesterMemberId()
            );
            result.put(applicationId, ProjectApplicationDetailInfo.of(
                application,
                getRequiredApplicantPart(applicantPartsByKey, project.getGisuId(), application.getApplicantMemberId()),
                formStructure,
                policiesByApplicationFormId.getOrDefault(application.getApplicationForm().getId(), List.of()),
                formResponseWithAnswers,
                filesByApplicationId.getOrDefault(applicationId, Map.of()),
                !isApplicantSelf || isStatusVisibleToApplicantSelf(application)
            ));
        }
        return result;
    }

    /**
     * 본인 지원 내역 목록 조회.
     * <p>
     * 사용자의 챌린저 파트로 {@link MatchingType} 을 자동 결정하여 필터링하고, 매칭 라운드 시작일 ASC -> 지원서 갱신일 DESC 순으로 정렬된 ProjectApplication 목록을
     * 표준 view 로 반환한다.
     * <p>
     * 본 UseCase 는 자기 자원(ProjectApplication) 만 다룬다. 랜덤 매칭/운영진 강제 배정으로 합류한 {@code ProjectMember(application = null)} 는 별도
     * UseCase ({@link com.umc.product.project.application.port.in.query.GetRandomMatchedProjectMemberUseCase}) 로 조회하며,
     * 두 종류의 카드를 한 화면 카드 줄로 합성하는 책임은 Web Assembler 가 진다.
     * <p>
     * 사용자가 해당 기수의 챌린저가 아니거나 매칭 대상 파트가 아닌 경우(PLAN/ADMIN) 빈 리스트를 반환한다.
     */
    @Override
    public List<ProjectApplicationSummaryInfo> listMyApplications(GetMyProjectApplicationsQuery query) {
        Optional<MatchingType> matchingType = resolveMatchingType(query);
        if (matchingType.isEmpty()) {
            return List.of();
        }

        List<ProjectApplication> applications = loadProjectApplicationPort.searchMyApplications(
            query.requesterMemberId(),
            query.gisuId(),
            matchingType.get(),
            query.status()
        );
        return applications.stream()
            .map(application -> ProjectApplicationSummaryInfo.from(
                application,
                isStatusVisibleToApplicantSelf(application)
            ))
            .filter(application -> query.status() == null || application.status() == query.status())
            .toList();
    }

    /**
     * PM/운영진용 단일 프로젝트 지원자 목록 조회.
     * <p>
     * 본 메서드는 자기 자원(ProjectApplication) 만 반환한다. 화면 카드에 들어가는 부가 정보 (지원자의 파트 / 매칭 라운드 / 닉네임 등) 는 Web Assembler 가 다른 도메인에서
     * 합쳐 붙인다. 그래서 파트(part) 필터도 챌린저 도메인이 필요한 정보라 Assembler 단계에서 적용한다.
     * <p>
     * 권한이 없으면 (None scope) 빈 리스트를 반환한다. 정렬: repository 가 phase ASC -> submittedAt ASC 의 DB baseline 만 보장하고,
     * 최종 화면 정렬(phase -> part -> submittedAt)은 part 가 cross-domain 정보라 Assembler 에서 in-memory 로 마무리된다.
     */
    @Override
    public List<ProjectApplicationSummaryInfo> searchByProject(SearchProjectApplicationsQuery query) {
        Project project = loadProjectPort.getById(query.projectId());

        ProjectApplicationAccessScope scope = accessScopeResolver.resolveForProjectApplicantList(
            query.requesterMemberId(), project);
        if (scope instanceof ProjectApplicationAccessScope.None) {
            return List.of();
        }
        boolean includeOngoingMatchingRounds =
            scope instanceof ProjectApplicationAccessScope.ProjectScoped projectScoped
                && projectScoped.includeOngoingMatchingRounds();

        List<ProjectApplication> applications = loadProjectApplicationPort.searchProjectApplications(
            query.projectId(),
            query.matchingRoundId(),
            query.status(),
            Instant.now(),
            includeOngoingMatchingRounds
        );
        return applications.stream()
            .map(ProjectApplicationSummaryInfo::from)
            .toList();
    }

    /**
     * PM/운영진용 복수 프로젝트 지원자 목록 조회.
     * <p>
     * 요청한 projectId 는 결과 Map 의 key 로 보존한다. 존재하지 않거나 권한이 없는 프로젝트는 빈 리스트를 반환하여 단건 조회의 권한 없음 위장 정책과 맞춘다.
     */
    @Override
    public Map<Long, List<ProjectApplicationSummaryInfo>> searchByProjects(
        SearchProjectApplicationsBatchQuery query
    ) {
        // 요청한 projectId key 는 응답에서 그대로 보존한다. 권한 없음/미존재 프로젝트도 빈 리스트로 채운다.
        Map<Long, List<ProjectApplicationSummaryInfo>> result = new LinkedHashMap<>();
        for (Long projectId : query.projectIds()) {
            result.put(projectId, new ArrayList<>());
        }

        List<Project> projects = loadProjectPort.listByIds(query.projectIds());
        if (projects.isEmpty()) {
            return freeze(result);
        }

        Map<Long, ProjectApplicationAccessScope> scopes =
            accessScopeResolver.resolveForProjectApplicantLists(query.requesterMemberId(), projects);

        Set<Long> accessibleProjectIds = scopes.entrySet().stream()
            .filter(entry -> entry.getValue() instanceof ProjectApplicationAccessScope.ProjectScoped)
            .map(Map.Entry::getKey)
            .collect(Collectors.toSet());
        if (accessibleProjectIds.isEmpty()) {
            return freeze(result);
        }

        // 중앙총괄/SUPER_ADMIN scope 프로젝트만 진행 중 차수 지원서를 함께 조회한다.
        Set<Long> includeOngoingProjectIds = scopes.entrySet().stream()
            .filter(entry -> entry.getValue() instanceof ProjectApplicationAccessScope.ProjectScoped projectScoped
                && projectScoped.includeOngoingMatchingRounds())
            .map(Map.Entry::getKey)
            .collect(Collectors.toSet());

        // 권한이 확인된 프로젝트만 DB 조회 대상에 넣어 권한 없는 프로젝트의 존재 여부가 결과로 새지 않게 한다.
        List<ProjectApplication> applications = loadProjectApplicationPort.searchProjectApplicationsByProjectIds(
            accessibleProjectIds,
            includeOngoingProjectIds,
            query.matchingRoundId(),
            query.status(),
            Instant.now()
        );

        for (ProjectApplication application : applications) {
            ProjectApplicationSummaryInfo info = ProjectApplicationSummaryInfo.from(application);
            List<ProjectApplicationSummaryInfo> projectApplications = result.get(info.projectId());
            if (projectApplications != null) {
                projectApplications.add(info);
            }
        }

        return freeze(result);
    }

    // ==============================================================
    //                      Helper Method
    // ==============================================================

    /**
     * 요청자의 챌린저 파트로부터 매칭 종류를 결정한다.
     * <ul>
     *   <li>해당 기수에 챌린저 레코드 없음 -> empty</li>
     *   <li>{@code PLAN} / {@code ADMIN} -> empty (지원 대상 아님)</li>
     *   <li>{@code DESIGN} -> {@code PLAN_DESIGN}</li>
     *   <li>{@code WEB} / {@code ANDROID} / {@code IOS} / {@code NODEJS} / {@code SPRINGBOOT}
     *       -> {@code PLAN_DEVELOPER}</li>
     * </ul>
     */
    private Optional<MatchingType> resolveMatchingType(GetMyProjectApplicationsQuery query) {
        return getChallengerUseCase
            .findByMemberIdAndGisuId(query.requesterMemberId(), query.gisuId())
            .map(ChallengerInfo::part)
            .flatMap(MatchingType::fromPart);
    }

    private boolean canViewOngoingMatchingRoundApplications(Long requesterMemberId, Project project) {
        ProjectApplicationAccessScope scope =
            accessScopeResolver.resolveForProjectApplicantList(requesterMemberId, project);
        return scope instanceof ProjectApplicationAccessScope.ProjectScoped projectScoped
            && projectScoped.includeOngoingMatchingRounds();
    }

    private boolean isStatusVisibleToApplicantSelf(ProjectApplication application) {
        ProjectApplicationStatus status = application.getStatus();
        if (status == ProjectApplicationStatus.DRAFT || status == ProjectApplicationStatus.CANCELLED) {
            return true;
        }
        return application.getAppliedMatchingRound().isDecisionDeadlinePassed(Instant.now());
    }

    private Map<Long, List<ProjectApplicationSummaryInfo>> freeze(
        Map<Long, List<ProjectApplicationSummaryInfo>> source
    ) {
        Map<Long, List<ProjectApplicationSummaryInfo>> frozen = new LinkedHashMap<>();
        source.forEach((projectId, applications) -> frozen.put(projectId, List.copyOf(applications)));
        return frozen;
    }

    /**
     * 단건 상세에 포함될 모든 답변의 fileIds 를 모아 storage 도메인에서 IN 쿼리 1회로 batch 조회한다. 첨부 파일이 하나도 없으면 빈 Map 을 반환한다.
     */
    private Map<String, FileInfo> resolveFiles(List<AnswerInfo> answers) {
        Set<String> fileIds = new HashSet<>();
        for (AnswerInfo answer : answers) {
            if (answer.fileIds() != null) {
                fileIds.addAll(answer.fileIds());
            }
        }
        if (fileIds.isEmpty()) {
            return Map.of();
        }
        return getFileUseCase.findAllByIds(new ArrayList<>(fileIds));
    }

    private void validateProjectConsistency(
        List<ProjectApplication> applications,
        Map<Long, GetProjectApplicationDetailQuery> queriesByApplicationId
    ) {
        for (ProjectApplication application : applications) {
            Long projectId = application.getApplicationForm().getProject().getId();
            GetProjectApplicationDetailQuery query = queriesByApplicationId.get(application.getId());
            if (query == null || !Objects.equals(projectId, query.projectId())) {
                throw new ProjectDomainException(ProjectErrorCode.PROJECT_APPLICATION_NOT_FOUND);
            }
        }
    }

    private void validateMatchingRoundVisibility(
        List<ProjectApplication> applications,
        Map<Long, GetProjectApplicationDetailQuery> queriesByApplicationId
    ) {
        Map<Long, Map<Long, Project>> projectsByRequesterId = new LinkedHashMap<>();
        for (ProjectApplication application : applications) {
            GetProjectApplicationDetailQuery query = queriesByApplicationId.get(application.getId());
            Project project = application.getApplicationForm().getProject();
            if (Objects.equals(application.getApplicantMemberId(), query.requesterMemberId())) {
                continue;
            }
            projectsByRequesterId
                .computeIfAbsent(query.requesterMemberId(), ignored -> new LinkedHashMap<>())
                .putIfAbsent(project.getId(), project);
        }

        Map<RequesterProjectKey, Boolean> ongoingVisibilityByKey = new LinkedHashMap<>();
        projectsByRequesterId.forEach((requesterMemberId, projectsById) -> {
            Map<Long, ProjectApplicationAccessScope> scopes =
                accessScopeResolver.resolveForProjectApplicantLists(requesterMemberId, projectsById.values());
            scopes.forEach((projectId, scope) -> ongoingVisibilityByKey.put(
                new RequesterProjectKey(requesterMemberId, projectId),
                scope instanceof ProjectApplicationAccessScope.ProjectScoped projectScoped
                    && projectScoped.includeOngoingMatchingRounds()
            ));
        });

        Instant now = Instant.now();
        for (ProjectApplication application : applications) {
            GetProjectApplicationDetailQuery query = queriesByApplicationId.get(application.getId());
            Project project = application.getApplicationForm().getProject();
            if (Objects.equals(application.getApplicantMemberId(), query.requesterMemberId())) {
                continue;
            }
            boolean canViewOngoingMatchingRoundApplications = ongoingVisibilityByKey.getOrDefault(
                new RequesterProjectKey(query.requesterMemberId(), project.getId()),
                false
            );
            if (!canViewOngoingMatchingRoundApplications) {
                application.getAppliedMatchingRound().validateIsViewableAt(now);
            }
        }
    }

    private Map<GisuMemberKey, ChallengerPart> resolveApplicantParts(List<ProjectApplication> applications) {
        Map<Long, Set<Long>> memberIdsByGisuId = applications.stream()
            .collect(Collectors.groupingBy(
                application -> application.getApplicationForm().getProject().getGisuId(),
                Collectors.mapping(
                    ProjectApplication::getApplicantMemberId,
                    Collectors.toCollection(LinkedHashSet::new)
                )
            ));

        Map<GisuMemberKey, ChallengerPart> result = new LinkedHashMap<>();
        memberIdsByGisuId.forEach((gisuId, memberIds) ->
            getChallengerUseCase.listByMemberIdsAndGisuId(memberIds, gisuId)
                .forEach((memberId, challenger) ->
                    result.put(new GisuMemberKey(gisuId, memberId), challenger.part()))
        );
        return result;
    }

    private ChallengerPart getRequiredApplicantPart(
        Map<GisuMemberKey, ChallengerPart> applicantPartsByKey,
        Long gisuId,
        Long memberId
    ) {
        ChallengerPart applicantPart = applicantPartsByKey.get(new GisuMemberKey(gisuId, memberId));
        if (applicantPart == null) {
            throw new ProjectDomainException(ProjectErrorCode.PROJECT_APPLICATION_NOT_FOUND);
        }
        return applicantPart;
    }

    private Set<Long> applicationFormIds(List<ProjectApplication> applications) {
        return applications.stream()
            .map(application -> application.getApplicationForm().getId())
            .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    private Set<Long> formResponseIds(List<ProjectApplication> applications) {
        return applications.stream()
            .map(ProjectApplication::getFormResponseId)
            .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    private FormResponseWithAnswersInfo getRequiredFormResponse(
        Map<Long, FormResponseWithAnswersInfo> formResponsesById,
        Long formResponseId
    ) {
        FormResponseWithAnswersInfo formResponse = formResponsesById.get(formResponseId);
        if (formResponse == null) {
            throw new ProjectDomainException(ProjectErrorCode.PROJECT_APPLICATION_NOT_FOUND);
        }
        return formResponse;
    }

    private FormStructureKey formStructureKey(
        ProjectApplication application,
        FormResponseWithAnswersInfo formResponseWithAnswers
    ) {
        Set<Long> answeredQuestionIds = formResponseWithAnswers.answers().stream()
            .map(AnswerInfo::questionId)
            .collect(Collectors.toUnmodifiableSet());
        return new FormStructureKey(application.getApplicationForm().getFormId(), answeredQuestionIds);
    }

    private Map<Long, Map<String, FileInfo>> resolveFilesByApplicationId(
        List<ProjectApplication> applications,
        Map<Long, FormResponseWithAnswersInfo> formResponsesById
    ) {
        List<AnswerInfo> allAnswers = applications.stream()
            .map(ProjectApplication::getFormResponseId)
            .map(formResponsesById::get)
            .filter(Objects::nonNull)
            .flatMap(formResponse -> formResponse.answers().stream())
            .toList();
        Map<String, FileInfo> allFilesByFileId = resolveFiles(allAnswers);
        if (allFilesByFileId.isEmpty()) {
            return Map.of();
        }

        Map<Long, Map<String, FileInfo>> result = new LinkedHashMap<>();
        for (ProjectApplication application : applications) {
            FormResponseWithAnswersInfo formResponse = formResponsesById.get(application.getFormResponseId());
            if (formResponse == null) {
                continue;
            }
            Set<String> applicationFileIds = formResponse.answers().stream()
                .filter(answer -> answer.fileIds() != null)
                .flatMap(answer -> answer.fileIds().stream())
                .collect(Collectors.toSet());
            Map<String, FileInfo> filesByFileId = applicationFileIds.stream()
                .filter(allFilesByFileId::containsKey)
                .collect(Collectors.toMap(
                    Function.identity(),
                    allFilesByFileId::get,
                    (left, right) -> left,
                    LinkedHashMap::new
                ));
            result.put(application.getId(), filesByFileId);
        }
        return result;
    }

    private record RequesterProjectKey(Long requesterMemberId, Long projectId) {
    }

    private record GisuMemberKey(Long gisuId, Long memberId) {
    }

    private record FormStructureKey(Long formId, Set<Long> questionIds) {
    }
}
