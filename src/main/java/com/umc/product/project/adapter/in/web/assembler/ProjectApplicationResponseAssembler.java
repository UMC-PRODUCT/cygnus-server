package com.umc.product.project.adapter.in.web.assembler;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import com.umc.product.challenger.application.port.in.query.GetChallengerUseCase;
import com.umc.product.common.domain.enums.ChallengerPart;
import com.umc.product.member.application.port.in.query.GetMemberUseCase;
import com.umc.product.member.application.port.in.query.dto.MemberInfo;
import com.umc.product.project.adapter.in.web.dto.common.MemberBrief;
import com.umc.product.project.adapter.in.web.dto.response.MyProjectApplicationResponse;
import com.umc.product.project.adapter.in.web.dto.response.ProjectApplicantResponse;
import com.umc.product.project.adapter.in.web.dto.response.ProjectApplicationDetailResponse;
import com.umc.product.project.application.port.in.query.GetMyProjectApplicationsUseCase;
import com.umc.product.project.application.port.in.query.GetProjectApplicationDetailUseCase;
import com.umc.product.project.application.port.in.query.GetProjectMatchingRoundUseCase;
import com.umc.product.project.application.port.in.query.GetProjectUseCase;
import com.umc.product.project.application.port.in.query.GetRandomMatchedProjectMemberUseCase;
import com.umc.product.project.application.port.in.query.SearchProjectApplicationsUseCase;
import com.umc.product.project.application.port.in.query.dto.GetMyProjectApplicationsQuery;
import com.umc.product.project.application.port.in.query.dto.GetProjectApplicationDetailQuery;
import com.umc.product.project.application.port.in.query.dto.ProjectApplicationDetailInfo;
import com.umc.product.project.application.port.in.query.dto.ProjectApplicationSummaryInfo;
import com.umc.product.project.application.port.in.query.dto.ProjectInfo;
import com.umc.product.project.application.port.in.query.dto.ProjectMatchingRoundInfo;
import com.umc.product.project.application.port.in.query.dto.ProjectMemberInfo;
import com.umc.product.project.application.port.in.query.dto.SearchProjectApplicationsBatchQuery;
import com.umc.product.project.application.port.in.query.dto.SearchProjectApplicationsQuery;

import lombok.RequiredArgsConstructor;

/**
 * Project Application 관련 Response 조립기. Controller에서 여러 UseCase 를 조합하는 로직을 캡슐화한다.
 */
@Component
@RequiredArgsConstructor
public class ProjectApplicationResponseAssembler {

    private final GetMyProjectApplicationsUseCase getMyProjectApplicationsUseCase;
    private final GetRandomMatchedProjectMemberUseCase getRandomMatchedProjectMemberUseCase;
    private final SearchProjectApplicationsUseCase searchProjectApplicationsUseCase;
    private final GetProjectApplicationDetailUseCase getProjectApplicationDetailUseCase;
    private final GetProjectUseCase getProjectUseCase;
    private final GetProjectMatchingRoundUseCase getProjectMatchingRoundUseCase;
    private final GetMemberUseCase getMemberUseCase;
    private final GetChallengerUseCase getChallengerUseCase;

    /**
     * 본인 지원 내역 카드 목록 조립 (APPLY-004).
     * <p>
     * 두 종류의 카드를 한 화면에 합성한다:
     * <ul>
     *   <li>지원서 카드 -- 본인이 제출한 {@code ProjectApplication}</li>
     *   <li>랜덤 매칭 카드 -- 본인이 ACTIVE 멤버이면서 {@code application = null} 인 {@code ProjectMember} (한 기수 0 또는 1 건)</li>
     * </ul>
     * <p>
     * 정렬: 지원서 카드는 service 가 반환한 순서(매칭 라운드 시작일 ASC -> 갱신일 DESC) 그대로, 랜덤 매칭 카드는 마지막에 둔다.
     * <p>
     * status 필터가 명시된 호출에서는 랜덤 매칭 카드를 합성하지 않는다 -- 랜덤 매칭 카드는 지원서 상태 필터에 대응하지 않아, 좁은 필터(SUBMITTED/REJECTED/DRAFT)
     * 호출 시 함께 내려가지 않게 한다.
     */
    public List<MyProjectApplicationResponse> myApplicationsFor(GetMyProjectApplicationsQuery query) {
        List<ProjectApplicationSummaryInfo> applications =
            getMyProjectApplicationsUseCase.listMyApplications(query);
        Optional<ProjectMemberInfo> randomMatched = (query.status() == null)
            ? getRandomMatchedProjectMemberUseCase.findRandomMatched(query.requesterMemberId(), query.gisuId())
            : Optional.empty();

        if (applications.isEmpty() && randomMatched.isEmpty()) {
            return List.of();
        }

        Set<Long> projectIds = new HashSet<>();
        Set<Long> roundIds = new HashSet<>();
        for (ProjectApplicationSummaryInfo application : applications) {
            projectIds.add(application.projectId());
            roundIds.add(application.matchingRoundId());
        }
        randomMatched.ifPresent(member -> projectIds.add(member.projectId()));

        Map<Long, ProjectInfo> projects = getProjectUseCase.findAllByIds(projectIds);
        Map<Long, ProjectMatchingRoundInfo> rounds = getProjectMatchingRoundUseCase.findAllByIds(roundIds);

        Set<Long> ownerIds = projects.values().stream()
            .map(ProjectInfo::productOwnerMemberId)
            .filter(Objects::nonNull)
            .collect(Collectors.toSet());
        Map<Long, MemberInfo> memberMap = ownerIds.isEmpty()
            ? Map.of()
            : getMemberUseCase.findAllByIds(ownerIds);

        List<MyProjectApplicationResponse> result = new ArrayList<>(applications.size() + 1);
        for (ProjectApplicationSummaryInfo application : applications) {
            ProjectInfo project = projects.get(application.projectId());
            ProjectMatchingRoundInfo round = rounds.get(application.matchingRoundId());
            MemberBrief owner = ownerBriefOf(project, memberMap);
            result.add(MyProjectApplicationResponse.fromApplication(application, project, round, owner));
        }
        randomMatched.ifPresent(member -> {
            ProjectInfo project = projects.get(member.projectId());
            MemberBrief owner = ownerBriefOf(project, memberMap);
            result.add(MyProjectApplicationResponse.fromRandomMatched(member, project, owner));
        });
        return result;
    }

    /**
     * 단일 프로젝트의 지원자 목록 카드 조립 (APPLY-101).
     * <p>
     * Service 는 ProjectApplication 자원만 돌려준다. 화면 카드에 필요한 나머지 (지원자의 파트 / 매칭 라운드 / 지원자 닉네임/실명/학교) 는 여기서 다른 도메인 UseCase 로
     * batch 조회해 붙인다.
     * <p>
     * 파트(part) 필터도 챌린저 도메인 정보라 in-memory 로 적용한다. 권한 scope 결정은 Service 가 이미 처리했으니 (None 이면 빈 리스트 반환) 여기서는 그 결과를 그대로
     * 신뢰한다.
     * <p>
     * 정렬: 매칭 차수(phase) ASC -> 파트(part) ASC -> 제출 시각(submittedAt) ASC. phase 는 Service/Repository 가 보장하는 DB 정렬과 겹치지만,
     * part 가 cross-domain enrichment 산물이라 파트별 묶음은 여기서 in-memory 로 한 번 더 정렬해야 한다.
     */
    public List<ProjectApplicantResponse> applicantsFor(SearchProjectApplicationsQuery query) {
        List<ProjectApplicationSummaryInfo> applications =
            searchProjectApplicationsUseCase.searchByProject(query);
        if (applications.isEmpty()) {
            return List.of();
        }

        // 챌린저 조회용 기수 ID 가 필요해서 프로젝트를 한 번 가져온다 (단건).
        ProjectInfo project = getProjectUseCase.getById(query.projectId());

        Set<Long> applicantMemberIds = applications.stream()
            .map(ProjectApplicationSummaryInfo::applicantMemberId)
            .collect(Collectors.toSet());
        Set<Long> roundIds = applications.stream()
            .map(ProjectApplicationSummaryInfo::matchingRoundId)
            .collect(Collectors.toSet());

        // 부가 정보 3종을 IN 쿼리 한 번씩으로 가져와 둔다 (N+1 방지).
        Map<Long, ChallengerPart> partsByMember = getChallengerUseCase
            .batchGetByMemberIdsAndGisuId(applicantMemberIds, project.gisuId())
            .entrySet().stream()
            .collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue().part()));
        Map<Long, ProjectMatchingRoundInfo> rounds =
            getProjectMatchingRoundUseCase.findAllByIds(roundIds);
        Map<Long, MemberInfo> memberMap = getMemberUseCase.findAllByIds(applicantMemberIds);

        return applications.stream()
            .filter(application -> {
                ChallengerPart applicantPart = partsByMember.get(application.applicantMemberId());
                // 파트 필터: 다른 파트는 건너뛴다.
                return query.part() == null || query.part() == applicantPart;
            })
            .sorted(applicantSortOrder(rounds, partsByMember))
            .map(application -> {
                ChallengerPart applicantPart = partsByMember.get(application.applicantMemberId());
                ProjectMatchingRoundInfo round = rounds.get(application.matchingRoundId());
                MemberBrief brief = toBrief(memberMap.get(application.applicantMemberId()));
                return ProjectApplicantResponse.from(application, applicantPart, round, brief);
            })
            .toList();
    }

    /**
     * 복수 프로젝트의 지원자 목록 카드 조립 (APPLY-101 batch).
     * <p>
     * Service 는 요청 projectId 별 ProjectApplication 자원 Map 을 돌려준다. 여기서는 프로젝트/차수/회원/챌린저 정보를 batch 로 조회한 뒤 프로젝트별로 기존
     * 지원자 카드 응답을 조립한다.
     */
    public Map<Long, List<ProjectApplicantResponse>> applicantsForBatch(SearchProjectApplicationsBatchQuery query) {
        Map<Long, List<ProjectApplicationSummaryInfo>> applicationsByProject =
            searchProjectApplicationsUseCase.searchByProjects(query);
        if (applicationsByProject.isEmpty()) {
            return Map.of();
        }

        // Service 가 보존한 요청 projectId key 를 Web 응답에서도 그대로 유지한다.
        Map<Long, List<ProjectApplicantResponse>> result = new LinkedHashMap<>();
        applicationsByProject.keySet().forEach(projectId -> result.put(projectId, List.of()));

        List<ProjectApplicationSummaryInfo> applications = applicationsByProject.values().stream()
            .flatMap(List::stream)
            .toList();
        if (applications.isEmpty()) {
            return result;
        }

        Set<Long> projectIds = applications.stream()
            .map(ProjectApplicationSummaryInfo::projectId)
            .collect(Collectors.toSet());
        Set<Long> applicantMemberIds = applications.stream()
            .map(ProjectApplicationSummaryInfo::applicantMemberId)
            .collect(Collectors.toSet());
        Set<Long> roundIds = applications.stream()
            .map(ProjectApplicationSummaryInfo::matchingRoundId)
            .collect(Collectors.toSet());

        Map<Long, ProjectInfo> projects = getProjectUseCase.findAllByIds(projectIds);
        Map<MemberGisuKey, ChallengerPart> partsByMemberAndGisu =
            resolveApplicantParts(applications, projects);
        Map<Long, ProjectMatchingRoundInfo> rounds =
            getProjectMatchingRoundUseCase.findAllByIds(roundIds);
        Map<Long, MemberInfo> memberMap = getMemberUseCase.findAllByIds(applicantMemberIds);

        for (Map.Entry<Long, List<ProjectApplicationSummaryInfo>> entry : applicationsByProject.entrySet()) {
            // 전체 부가 정보는 batch 로 한 번에 가져오고, 마지막 단계에서 프로젝트별 필터/정렬만 적용한다.
            List<ProjectApplicantResponse> responses = entry.getValue().stream()
                .filter(application -> {
                    ChallengerPart applicantPart = applicantPartOf(application, projects, partsByMemberAndGisu);
                    return query.part() == null || query.part() == applicantPart;
                })
                .sorted(applicantSortOrder(rounds, projects, partsByMemberAndGisu))
                .map(application -> {
                    ChallengerPart applicantPart = applicantPartOf(application, projects, partsByMemberAndGisu);
                    ProjectMatchingRoundInfo round = rounds.get(application.matchingRoundId());
                    MemberBrief brief = toBrief(memberMap.get(application.applicantMemberId()));
                    return ProjectApplicantResponse.from(application, applicantPart, round, brief);
                })
                .toList();
            result.put(entry.getKey(), responses);
        }

        return result;
    }

    /**
     * APPLY-101 지원자 카드 정렬자: 매칭 차수(phase) ASC -> 파트(part) ASC -> 제출 시각(submittedAt) ASC.
     */
    private Comparator<ProjectApplicationSummaryInfo> applicantSortOrder(
        Map<Long, ProjectMatchingRoundInfo> rounds,
        Map<Long, ChallengerPart> partsByMember
    ) {
        return Comparator
            .comparingInt((ProjectApplicationSummaryInfo application) ->
                rounds.get(application.matchingRoundId()).phase().ordinal())
            .thenComparingInt(application ->
                partsByMember.get(application.applicantMemberId()).getSortOrder())
            .thenComparing(ProjectApplicationSummaryInfo::submittedAt);
    }

    /**
     * APPLY-101 batch 지원자 카드 정렬자: 매칭 차수(phase) ASC -> 파트(part) ASC -> 제출 시각(submittedAt) ASC.
     */
    private Comparator<ProjectApplicationSummaryInfo> applicantSortOrder(
        Map<Long, ProjectMatchingRoundInfo> rounds,
        Map<Long, ProjectInfo> projects,
        Map<MemberGisuKey, ChallengerPart> partsByMemberAndGisu
    ) {
        return Comparator
            .comparingInt((ProjectApplicationSummaryInfo application) ->
                rounds.get(application.matchingRoundId()).phase().ordinal())
            .thenComparingInt(application ->
                applicantPartOf(application, projects, partsByMemberAndGisu).getSortOrder())
            .thenComparing(ProjectApplicationSummaryInfo::submittedAt);
    }

    private Map<MemberGisuKey, ChallengerPart> resolveApplicantParts(
        List<ProjectApplicationSummaryInfo> applications,
        Map<Long, ProjectInfo> projects
    ) {
        Map<Long, Set<Long>> memberIdsByGisu = new HashMap<>();
        for (ProjectApplicationSummaryInfo application : applications) {
            ProjectInfo project = projects.get(application.projectId());
            if (project == null) {
                continue;
            }
            memberIdsByGisu
                .computeIfAbsent(project.gisuId(), ignored -> new HashSet<>())
                .add(application.applicantMemberId());
        }

        // Challenger part 는 같은 memberId 여도 기수별로 다를 수 있어 gisuId 를 key 에 함께 넣는다.
        Map<MemberGisuKey, ChallengerPart> result = new HashMap<>();
        memberIdsByGisu.forEach((gisuId, memberIds) ->
            getChallengerUseCase.batchGetByMemberIdsAndGisuId(memberIds, gisuId)
                .forEach((memberId, challenger) ->
                    result.put(new MemberGisuKey(memberId, gisuId), challenger.part())));
        return result;
    }

    private ChallengerPart applicantPartOf(
        ProjectApplicationSummaryInfo application,
        Map<Long, ProjectInfo> projects,
        Map<MemberGisuKey, ChallengerPart> partsByMemberAndGisu
    ) {
        ProjectInfo project = projects.get(application.projectId());
        if (project == null) {
            return null;
        }
        return partsByMemberAndGisu.get(new MemberGisuKey(application.applicantMemberId(), project.gisuId()));
    }

    /**
     * 지원서 단건 상세 조회. cross-domain 컨테이너에서 트리(섹션/질문/답변) 합성은 Response DTO 가 처리하고, Assembler 는 지원자(member) 닉네임/실명/학교만 합성한다.
     * <p>
     * 도메인 비즈니스 규칙(DRAFT 는 지원자 본인만 조회 가능)은 Service 단 ({@code getDetail}) 에서 처리되므로 Assembler 는 응답 조립에만 집중한다.
     * <p>
     * TODO: 4종 호출자(@CheckAccess) 자격 검증은 후속 권한 PR 에서 추가된다 -- 본 메서드는 호출 자격이 검증된 호출자에 한해 응답을 조립한다.
     */
    public ProjectApplicationDetailResponse detailFor(GetProjectApplicationDetailQuery query) {
        ProjectApplicationDetailInfo info = getProjectApplicationDetailUseCase.getDetail(query);

        MemberInfo memberInfo = getMemberUseCase.findAllByIds(Set.of(info.applicantMemberId()))
            .get(info.applicantMemberId());
        return ProjectApplicationDetailResponse.from(info, toBrief(memberInfo));
    }

    private MemberBrief ownerBriefOf(ProjectInfo project, Map<Long, MemberInfo> memberMap) {
        if (project == null || project.productOwnerMemberId() == null) {
            return null;
        }
        return toBrief(memberMap.get(project.productOwnerMemberId()));
    }

    private MemberBrief toBrief(MemberInfo info) {
        return info == null ? null : MemberBrief.from(info);
    }

    private record MemberGisuKey(Long memberId, Long gisuId) {
    }
}
