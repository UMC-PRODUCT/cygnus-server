package com.umc.product.project.application.service.query;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.umc.product.challenger.application.port.in.query.GetChallengerUseCase;
import com.umc.product.challenger.application.port.in.query.dto.ChallengerInfo;
import com.umc.product.common.domain.enums.ChallengerPart;
import com.umc.product.common.domain.enums.ChallengerStatus;
import com.umc.product.member.application.port.in.query.GetMemberUseCase;
import com.umc.product.project.application.port.in.query.GetProjectStatisticsUseCase;
import com.umc.product.project.application.port.in.query.dto.statistics.ChapterProjectMatchingStatisticsInfo;
import com.umc.product.project.application.port.in.query.dto.statistics.ChapterProjectStatisticsInfo;
import com.umc.product.project.application.port.in.query.dto.statistics.ChapterProjectStatisticsSummaryInfo;
import com.umc.product.project.application.port.in.query.dto.statistics.ProjectMatchingCountInfo;
import com.umc.product.project.application.port.in.query.dto.statistics.ProjectMatchingRoundStatisticsInfo;
import com.umc.product.project.application.port.in.query.dto.statistics.ProjectMemberApplicationStatisticsInfo;
import com.umc.product.project.application.port.in.query.dto.statistics.ProjectMemberStatisticsInfo;
import com.umc.product.project.application.port.in.query.dto.statistics.ProjectRoundMemberCountInfo;
import com.umc.product.project.application.port.in.query.dto.statistics.ProjectRoundMemberStatisticsInfo;
import com.umc.product.project.application.port.in.query.dto.statistics.ProjectStatisticsInfo;
import com.umc.product.project.application.port.in.query.dto.statistics.RoundApplicationStatisticsInfo;
import com.umc.product.project.application.port.in.query.dto.statistics.RoundMatchingStatisticsInfo;
import com.umc.product.project.application.port.in.query.dto.statistics.RoundSchoolApplicationStatisticsInfo;
import com.umc.product.project.application.port.in.query.dto.statistics.SchoolApplicationMatchingStatisticsInfo;
import com.umc.product.project.application.port.in.query.dto.statistics.SchoolApplicationStatisticsInfo;
import com.umc.product.project.application.port.in.query.dto.statistics.SchoolMatchingStatisticsInfo;
import com.umc.product.project.application.port.in.query.dto.statistics.UnclassifiedMatchingStatisticsInfo;
import com.umc.product.project.application.port.out.LoadProjectPort;
import com.umc.product.project.application.port.out.LoadProjectStatisticsPort;
import com.umc.product.project.application.port.out.dto.ProjectStatisticsApplicationRow;
import com.umc.product.project.application.port.out.dto.ProjectStatisticsApprovedApplicationRow;
import com.umc.product.project.application.port.out.dto.ProjectStatisticsMatchingRoundRow;
import com.umc.product.project.application.port.out.dto.ProjectStatisticsMemberRow;
import com.umc.product.project.application.port.out.dto.ProjectStatisticsProjectRow;
import com.umc.product.project.application.service.policy.ProjectStatisticsAccessPolicy;
import com.umc.product.project.domain.Project;
import com.umc.product.project.domain.enums.ProjectApplicationStatus;
import com.umc.product.project.domain.exception.ProjectDomainException;
import com.umc.product.project.domain.exception.ProjectErrorCode;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProjectStatisticsQueryService implements GetProjectStatisticsUseCase {

    private final LoadProjectStatisticsPort loadProjectStatisticsPort;
    private final GetChallengerUseCase getChallengerUseCase;
    private final GetMemberUseCase getMemberUseCase;
    private final LoadProjectPort loadProjectPort;
    private final ProjectStatisticsAccessPolicy projectStatisticsAccessPolicy;

    @Override
    public ProjectStatisticsInfo getByProjectId(Long projectId, Long requesterMemberId) {
        Project target = loadProjectPort.getById(projectId);
        validateProjectAccess(requesterMemberId, target);

        ProjectStatisticsProjectRow project = loadProjectStatisticsPort.getProjectById(projectId);
        List<ProjectStatisticsMatchingRoundRow> rounds =
            loadProjectStatisticsPort.listMatchingRoundsByChapterId(project.chapterId());
        List<ProjectStatisticsMemberRow> members =
            sortMembers(loadProjectStatisticsPort.listActiveMembersByProjectId(projectId));
        List<ProjectStatisticsApplicationRow> applications =
            sortApplications(loadProjectStatisticsPort.listCountedApplicationsByProjectIds(Set.of(projectId)));
        StatisticsPopulation population = resolvePopulation(List.of(project));

        return assembleProject(project, rounds, members, applications, population);
    }

    @Override
    public ChapterProjectStatisticsInfo getByChapterId(Long chapterId, Long requesterMemberId) {
        validateChapterAccess(requesterMemberId, chapterId);

        List<ProjectStatisticsProjectRow> projects = loadProjectStatisticsPort.listProjectsByChapterId(chapterId);
        return assembleChapterStatistics(chapterId, projects, loadProjectStatisticsPort.listActiveMembersByChapterId(chapterId));
    }

    @Override
    public ChapterProjectStatisticsInfo getByProjectIds(Collection<Long> projectIds, Long requesterMemberId) {
        if (projectIds == null) {
            return new ChapterProjectStatisticsInfo(null, List.of(), emptySummary());
        }
        Set<Long> distinctProjectIds = projectIds.stream()
            .filter(Objects::nonNull)
            .collect(Collectors.toCollection(LinkedHashSet::new));
        if (distinctProjectIds.isEmpty()) {
            return new ChapterProjectStatisticsInfo(null, List.of(), emptySummary());
        }

        List<Project> targetProjects = loadProjectPort.listByIds(distinctProjectIds);
        if (targetProjects.size() != distinctProjectIds.size()) {
            throw new ProjectDomainException(ProjectErrorCode.PROJECT_NOT_FOUND);
        }

        targetProjects.forEach(project -> validateProjectAccess(requesterMemberId, project));
        Long chapterId = resolveSingleChapterId(targetProjects);
        List<ProjectStatisticsProjectRow> projects = targetProjects.stream()
            .map(project -> new ProjectStatisticsProjectRow(
                project.getId(),
                project.getGisuId(),
                project.getChapterId()
            ))
            .sorted(Comparator.comparing(ProjectStatisticsProjectRow::projectId))
            .toList();

        return assembleChapterStatistics(
            chapterId,
            projects,
            loadProjectStatisticsPort.listActiveMembersByProjectIds(distinctProjectIds)
        );
    }

    private ChapterProjectStatisticsInfo assembleChapterStatistics(
        Long chapterId,
        List<ProjectStatisticsProjectRow> projects,
        List<ProjectStatisticsMemberRow> rawMembers
    ) {
        if (projects.isEmpty()) {
            return new ChapterProjectStatisticsInfo(chapterId, List.of(), emptySummary());
        }

        Set<Long> projectIds = projects.stream()
            .map(ProjectStatisticsProjectRow::projectId)
            .collect(Collectors.toCollection(LinkedHashSet::new));
        List<ProjectStatisticsMatchingRoundRow> rounds =
            loadProjectStatisticsPort.listMatchingRoundsByChapterId(chapterId);
        List<ProjectStatisticsMemberRow> members =
            sortMembers(rawMembers);
        List<ProjectStatisticsApplicationRow> applications =
            sortApplications(loadProjectStatisticsPort.listCountedApplicationsByProjectIds(projectIds));
        StatisticsPopulation population = resolvePopulation(projects);

        Map<Long, List<ProjectStatisticsMemberRow>> membersByProject = members.stream()
            .collect(Collectors.groupingBy(
                ProjectStatisticsMemberRow::projectId,
                LinkedHashMap::new,
                Collectors.toList()
            ));
        Map<Long, List<ProjectStatisticsApplicationRow>> applicationsByProject = applications.stream()
            .collect(Collectors.groupingBy(
                ProjectStatisticsApplicationRow::projectId,
                LinkedHashMap::new,
                Collectors.toList()
            ));

        List<ProjectStatisticsInfo> projectStatistics = projects.stream()
            .map(project -> assembleProject(
                project,
                rounds,
                membersByProject.getOrDefault(project.projectId(), List.of()),
                applicationsByProject.getOrDefault(project.projectId(), List.of()),
                population
            ))
            .toList();

        StatisticsContext chapterContext = buildContext(rounds, applications, members, population);
        return new ChapterProjectStatisticsInfo(
            chapterId,
            projectStatistics,
            new ChapterProjectStatisticsSummaryInfo(
                buildRoundApplicationStatistics(chapterContext),
                buildRoundSchoolApplicationStatistics(chapterContext),
                buildSchoolMatchingStatistics(chapterContext),
                buildProjectRoundStatistics(projects, chapterContext)
            )
        );
    }

    private Long resolveSingleChapterId(Collection<Project> projects) {
        Set<Long> chapterIds = projects.stream()
            .map(Project::getChapterId)
            .collect(Collectors.toSet());
        if (chapterIds.size() != 1) {
            throw new ProjectDomainException(
                ProjectErrorCode.PROJECT_INVALID_STATE,
                "프로젝트 통계는 같은 지부의 프로젝트끼리만 한 번에 조회할 수 있어요."
            );
        }
        return chapterIds.iterator().next();
    }

    @Override
    public ChapterProjectMatchingStatisticsInfo getPublicMatchingStatisticsByChapterId(Long chapterId) {
        List<ProjectStatisticsProjectRow> projects =
            loadProjectStatisticsPort.listPublicProjectsByChapterId(chapterId);
        if (projects.isEmpty()) {
            return emptyPublicMatchingStatistics(chapterId);
        }

        Set<Long> projectIds = projects.stream()
            .map(ProjectStatisticsProjectRow::projectId)
            .collect(Collectors.toCollection(LinkedHashSet::new));
        List<ProjectStatisticsMatchingRoundRow> rounds =
            loadProjectStatisticsPort.listMatchingRoundsByChapterId(chapterId);
        List<ProjectStatisticsMemberRow> members =
            sortMembers(loadProjectStatisticsPort.listPublicActiveMembersByChapterId(chapterId));
        List<ProjectStatisticsApprovedApplicationRow> approvedApplications =
            sortApprovedApplications(loadProjectStatisticsPort.listApprovedApplicationsByProjectIds(projectIds));
        StatisticsPopulation population = resolvePopulation(projects);

        PublicMatchingContext context = buildPublicMatchingContext(
            rounds,
            members,
            approvedApplications,
            population
        );

        return new ChapterProjectMatchingStatisticsInfo(
            chapterId,
            buildRoundMatchingStatistics(context),
            buildPublicSchoolMatchingStatistics(context),
            buildUnclassifiedMatchingStatistics(context)
        );
    }

    /**
     * 단건 프로젝트 통계 접근 권한 검증. 본인 프로젝트의 PO/Sub-PM 이면 통과, 아니면 지부 운영진 판정으로 위임한다.
     */
    private void validateProjectAccess(Long memberId, Project project) {
        if (!projectStatisticsAccessPolicy.canReadProjectStatistics(memberId, project)) {
            throw new ProjectDomainException(ProjectErrorCode.PROJECT_ACCESS_DENIED);
        }
    }

    /**
     * 지부 단위 통계 접근 권한 검증. SUPER_ADMIN / 총괄단 / 해당 지부장 / 해당 지부 소속 학교 회장·부회장이면 통과,
     * 그 외에는 {@code PROJECT_ACCESS_DENIED}.
     * <p>
     * 요청 chapterId 는 치환하지 않고 통과/거부만 판정한다(총괄단의 타 지부 조회 보장).
     */
    private void validateChapterAccess(Long memberId, Long chapterId) {
        if (!projectStatisticsAccessPolicy.canReadChapterStatistics(memberId, chapterId)) {
            throw new ProjectDomainException(ProjectErrorCode.PROJECT_ACCESS_DENIED);
        }
    }

    private ProjectStatisticsInfo assembleProject(
        ProjectStatisticsProjectRow project,
        List<ProjectStatisticsMatchingRoundRow> rounds,
        List<ProjectStatisticsMemberRow> members,
        List<ProjectStatisticsApplicationRow> applications,
        StatisticsPopulation population
    ) {
        Map<ProjectMemberKey, List<ProjectStatisticsApplicationRow>> applicationsByMember =
            groupApplications(applications);
        StatisticsContext context = buildContext(rounds, applications, members, population);

        return new ProjectStatisticsInfo(
            project.projectId(),
            members.stream()
                .map(member -> toMemberInfo(member, applicationsByMember.getOrDefault(
                    ProjectMemberKey.from(member), List.of())))
                .toList(),
            buildRoundApplicationStatistics(context),
            buildRoundSchoolApplicationStatistics(context)
        );
    }

    private StatisticsPopulation resolvePopulation(List<ProjectStatisticsProjectRow> projects) {
        Set<Long> eligibleMemberIds = projects.stream()
            .map(ProjectStatisticsProjectRow::chapterId)
            .filter(Objects::nonNull)
            .distinct()
            .flatMap(chapterId -> getChallengerUseCase.listByChapterId(chapterId).stream())
            .filter(this::isEligibleChallenger)
            .map(ChallengerInfo::memberId)
            .filter(Objects::nonNull)
            .collect(Collectors.toCollection(LinkedHashSet::new));

        Map<Long, Long> schoolIdByMemberId = eligibleMemberIds.isEmpty()
            ? Map.of()
            : getMemberUseCase.findAllSchoolIdsByIds(eligibleMemberIds);

        return new StatisticsPopulation(eligibleMemberIds, schoolIdByMemberId);
    }

    private boolean isEligibleChallenger(ChallengerInfo challenger) {
        // 지원 가능 모집단(지부 인원)은 활동 중인 챌린저만 집계한다. 수료/제명 인원은 제외.
        return challenger.challengerStatus() == ChallengerStatus.ACTIVE
            && challenger.part() != ChallengerPart.ADMIN
            && challenger.part() != ChallengerPart.PLAN;
    }

    private StatisticsContext buildContext(
        List<ProjectStatisticsMatchingRoundRow> rounds,
        List<ProjectStatisticsApplicationRow> applications,
        List<ProjectStatisticsMemberRow> members,
        StatisticsPopulation population
    ) {
        Set<ProjectMemberKey> activeMemberKeys = members.stream()
            .map(ProjectMemberKey::from)
            .collect(Collectors.toSet());

        List<ProjectStatisticsApplicationRow> eligibleApplications = applications.stream()
            .filter(application -> population.eligibleMemberIds().contains(application.applicantMemberId()))
            .toList();
        List<ProjectStatisticsApplicationRow> matchedApplications = eligibleApplications.stream()
            .filter(application -> application.status() == ProjectApplicationStatus.APPROVED)
            .filter(application -> activeMemberKeys.contains(ProjectMemberKey.from(application)))
            .toList();

        return new StatisticsContext(rounds, eligibleApplications, matchedApplications, members, population);
    }

    private List<RoundApplicationStatisticsInfo> buildRoundApplicationStatistics(StatisticsContext context) {
        Map<Long, Long> applicationCountByRound = countApplicationsByRound(context.applications());
        Map<Long, Set<Long>> matchedByRound = groupMemberIdsByRound(context.matchedApplications());

        List<RoundApplicationStatisticsInfo> statistics = new ArrayList<>();
        Set<Long> cumulativeMatchedMemberIds = new HashSet<>();
        for (ProjectStatisticsMatchingRoundRow round : context.rounds()) {
            long availableMemberCount = Math.max(
                0L,
                context.population().eligibleMemberIds().size() - cumulativeMatchedMemberIds.size()
            );
            statistics.add(new RoundApplicationStatisticsInfo(
                toMatchingRoundInfo(round),
                applicationCountByRound.getOrDefault(round.matchingRoundId(), 0L),
                availableMemberCount
            ));
            cumulativeMatchedMemberIds.addAll(matchedByRound.getOrDefault(round.matchingRoundId(), Set.of()));
        }
        return statistics;
    }

    private List<RoundSchoolApplicationStatisticsInfo> buildRoundSchoolApplicationStatistics(
        StatisticsContext context
    ) {
        Map<Long, Map<Long, Set<Long>>> roundSchoolApplicants = new HashMap<>();
        for (ProjectStatisticsApplicationRow application : context.applications()) {
            if (application.applicantMemberId() == null) {
                continue;
            }
            Long schoolId = context.population().schoolIdByMemberId().get(application.applicantMemberId());
            if (schoolId == null) {
                continue;
            }
            roundSchoolApplicants
                .computeIfAbsent(application.matchingRoundId(), ignored -> new HashMap<>())
                .computeIfAbsent(schoolId, ignored -> new HashSet<>())
                .add(application.applicantMemberId());
        }

        return context.rounds().stream()
            .map(round -> new RoundSchoolApplicationStatisticsInfo(
                toMatchingRoundInfo(round),
                toSchoolApplicationStatistics(
                    roundSchoolApplicants.getOrDefault(round.matchingRoundId(), Map.of()))
            ))
            .toList();
    }

    private List<SchoolApplicationStatisticsInfo> toSchoolApplicationStatistics(
        Map<Long, Set<Long>> memberIdsBySchool
    ) {
        return memberIdsBySchool.entrySet().stream()
            .map(entry -> new SchoolApplicationStatisticsInfo(entry.getKey(), entry.getValue().size()))
            .sorted(Comparator
                .comparingLong(SchoolApplicationStatisticsInfo::applicantCount)
                .reversed()
                .thenComparing(SchoolApplicationStatisticsInfo::schoolId))
            .toList();
    }

    private List<SchoolApplicationMatchingStatisticsInfo> buildSchoolMatchingStatistics(StatisticsContext context) {
        Map<Long, Long> totalMemberCountBySchool = context.population().eligibleMemberIds().stream()
            .map(context.population().schoolIdByMemberId()::get)
            .filter(Objects::nonNull)
            .collect(Collectors.groupingBy(Function.identity(), Collectors.counting()));
        Map<Long, Set<Long>> matchedMemberIdsBySchool = new HashMap<>();
        for (ProjectStatisticsMemberRow member : context.members()) {
            if (member.memberId() == null) {
                continue;
            }
            Long schoolId = context.population().schoolIdByMemberId().get(member.memberId());
            if (schoolId == null) {
                continue;
            }
            matchedMemberIdsBySchool
                .computeIfAbsent(schoolId, ignored -> new HashSet<>())
                .add(member.memberId());
        }

        Map<Long, Set<Long>> appliedMemberIdsBySchool = new HashMap<>();
        for (ProjectStatisticsApplicationRow application : context.applications()) {
            if (application.applicantMemberId() == null) {
                continue;
            }
            Long schoolId = context.population().schoolIdByMemberId().get(application.applicantMemberId());
            if (schoolId == null) {
                continue;
            }
            appliedMemberIdsBySchool
                .computeIfAbsent(schoolId, ignored -> new HashSet<>())
                .add(application.applicantMemberId());
        }

        return totalMemberCountBySchool.entrySet().stream()
            .map(entry -> new SchoolApplicationMatchingStatisticsInfo(
                entry.getKey(),
                matchedMemberIdsBySchool.getOrDefault(entry.getKey(), Set.of()).size(),
                entry.getValue(),
                appliedMemberIdsBySchool.getOrDefault(entry.getKey(), Set.of()).size()
            ))
            .sorted(Comparator.comparing(SchoolApplicationMatchingStatisticsInfo::schoolId))
            .toList();
    }

    private List<ProjectRoundMemberStatisticsInfo> buildProjectRoundStatistics(
        List<ProjectStatisticsProjectRow> projects,
        StatisticsContext context
    ) {
        Map<Long, Map<Long, Set<Long>>> projectRoundApplied = new HashMap<>();
        for (ProjectStatisticsApplicationRow application : context.applications()) {
            projectRoundApplied
                .computeIfAbsent(application.projectId(), ignored -> new HashMap<>())
                .computeIfAbsent(application.matchingRoundId(), ignored -> new HashSet<>())
                .add(application.applicantMemberId());
        }

        Map<Long, Map<Long, Set<Long>>> projectRoundMatched = new HashMap<>();
        for (ProjectStatisticsApplicationRow application : context.matchedApplications()) {
            projectRoundMatched
                .computeIfAbsent(application.projectId(), ignored -> new HashMap<>())
                .computeIfAbsent(application.matchingRoundId(), ignored -> new HashSet<>())
                .add(application.applicantMemberId());
        }

        return projects.stream()
            .sorted(Comparator.comparing(ProjectStatisticsProjectRow::projectId))
            .map(project -> new ProjectRoundMemberStatisticsInfo(
                project.projectId(),
                context.rounds().stream()
                    .map(round -> new ProjectRoundMemberCountInfo(
                        toMatchingRoundInfo(round),
                        projectRoundApplied
                            .getOrDefault(project.projectId(), Map.of())
                            .getOrDefault(round.matchingRoundId(), Set.of())
                            .size(),
                        projectRoundMatched
                            .getOrDefault(project.projectId(), Map.of())
                            .getOrDefault(round.matchingRoundId(), Set.of())
                            .size()
                    ))
                    .toList()
            ))
            .toList();
    }

    private PublicMatchingContext buildPublicMatchingContext(
        List<ProjectStatisticsMatchingRoundRow> rounds,
        List<ProjectStatisticsMemberRow> members,
        List<ProjectStatisticsApprovedApplicationRow> approvedApplications,
        StatisticsPopulation population
    ) {
        Map<ProjectMemberKey, ProjectStatisticsApprovedApplicationRow> earliestApprovedApplicationByMember =
            new HashMap<>();
        for (ProjectStatisticsApprovedApplicationRow application : approvedApplications) {
            earliestApprovedApplicationByMember.putIfAbsent(ProjectMemberKey.from(application), application);
        }

        List<ProjectMemberMatchingAssignment> classifiedAssignments = new ArrayList<>();
        List<ProjectStatisticsMemberRow> unclassifiedMembers = new ArrayList<>();
        for (ProjectStatisticsMemberRow member : members) {
            ProjectStatisticsApprovedApplicationRow application =
                earliestApprovedApplicationByMember.get(ProjectMemberKey.from(member));
            if (application == null) {
                unclassifiedMembers.add(member);
                continue;
            }
            classifiedAssignments.add(new ProjectMemberMatchingAssignment(member, application));
        }

        return new PublicMatchingContext(rounds, classifiedAssignments, unclassifiedMembers, members, population);
    }

    private List<RoundMatchingStatisticsInfo> buildRoundMatchingStatistics(PublicMatchingContext context) {
        Map<Long, List<ProjectMemberMatchingAssignment>> assignmentsByRound = context.classifiedAssignments().stream()
            .collect(Collectors.groupingBy(
                assignment -> assignment.application().matchingRoundId(),
                LinkedHashMap::new,
                Collectors.toList()
            ));

        List<RoundMatchingStatisticsInfo> statistics = new ArrayList<>();
        Set<Long> cumulativeMatchedMemberIds = new HashSet<>();
        for (ProjectStatisticsMatchingRoundRow round : context.rounds()) {
            List<ProjectMemberMatchingAssignment> assignments =
                assignmentsByRound.getOrDefault(round.matchingRoundId(), List.of());
            long availableMemberCount = Math.max(
                0L,
                context.population().eligibleMemberIds().size() - cumulativeMatchedMemberIds.size()
            );

            statistics.add(new RoundMatchingStatisticsInfo(
                toMatchingRoundInfo(round),
                countDistinctAssignmentMembers(assignments),
                availableMemberCount,
                toProjectMatchingCountsFromAssignments(assignments)
            ));

            assignments.stream()
                .map(assignment -> assignment.member().memberId())
                .forEach(cumulativeMatchedMemberIds::add);
        }
        return statistics;
    }

    private List<SchoolMatchingStatisticsInfo> buildPublicSchoolMatchingStatistics(PublicMatchingContext context) {
        Map<Long, Long> totalMemberCountBySchool = context.population().eligibleMemberIds().stream()
            .map(context.population().schoolIdByMemberId()::get)
            .filter(Objects::nonNull)
            .collect(Collectors.groupingBy(Function.identity(), Collectors.counting()));

        Map<Long, Set<Long>> matchedMemberIdsBySchool = new HashMap<>();
        for (ProjectStatisticsMemberRow member : context.allMembers()) {
            if (member.memberId() == null) {
                continue;
            }
            Long schoolId = context.population().schoolIdByMemberId().get(member.memberId());
            if (schoolId == null) {
                continue;
            }
            matchedMemberIdsBySchool
                .computeIfAbsent(schoolId, ignored -> new HashSet<>())
                .add(member.memberId());
        }

        return totalMemberCountBySchool.entrySet().stream()
            .map(entry -> new SchoolMatchingStatisticsInfo(
                entry.getKey(),
                matchedMemberIdsBySchool.getOrDefault(entry.getKey(), Set.of()).size(),
                entry.getValue()
            ))
            .sorted(Comparator.comparing(SchoolMatchingStatisticsInfo::schoolId))
            .toList();
    }

    private UnclassifiedMatchingStatisticsInfo buildUnclassifiedMatchingStatistics(PublicMatchingContext context) {
        return new UnclassifiedMatchingStatisticsInfo(
            countDistinctMembers(context.unclassifiedMembers()),
            toProjectMatchingCountsFromMembers(context.unclassifiedMembers())
        );
    }

    private List<ProjectMatchingCountInfo> toProjectMatchingCountsFromAssignments(
        Collection<ProjectMemberMatchingAssignment> assignments
    ) {
        Map<Long, Set<Long>> memberIdsByProject = new HashMap<>();
        for (ProjectMemberMatchingAssignment assignment : assignments) {
            ProjectStatisticsMemberRow member = assignment.member();
            memberIdsByProject
                .computeIfAbsent(member.projectId(), ignored -> new HashSet<>())
                .add(member.memberId());
        }
        return toProjectMatchingCounts(memberIdsByProject);
    }

    private List<ProjectMatchingCountInfo> toProjectMatchingCountsFromMembers(
        Collection<ProjectStatisticsMemberRow> members
    ) {
        Map<Long, Set<Long>> memberIdsByProject = new HashMap<>();
        for (ProjectStatisticsMemberRow member : members) {
            memberIdsByProject
                .computeIfAbsent(member.projectId(), ignored -> new HashSet<>())
                .add(member.memberId());
        }
        return toProjectMatchingCounts(memberIdsByProject);
    }

    private List<ProjectMatchingCountInfo> toProjectMatchingCounts(Map<Long, Set<Long>> memberIdsByProject) {
        return memberIdsByProject.entrySet().stream()
            .map(entry -> new ProjectMatchingCountInfo(entry.getKey(), entry.getValue().size()))
            .sorted(Comparator.comparing(ProjectMatchingCountInfo::projectId))
            .toList();
    }

    private long countDistinctAssignmentMembers(Collection<ProjectMemberMatchingAssignment> assignments) {
        return assignments.stream()
            .map(assignment -> assignment.member().memberId())
            .filter(Objects::nonNull)
            .distinct()
            .count();
    }

    private long countDistinctMembers(Collection<ProjectStatisticsMemberRow> members) {
        return members.stream()
            .map(ProjectStatisticsMemberRow::memberId)
            .filter(Objects::nonNull)
            .distinct()
            .count();
    }

    private Map<Long, Long> countApplicationsByRound(Collection<ProjectStatisticsApplicationRow> applications) {
        return applications.stream()
            .collect(Collectors.groupingBy(
                ProjectStatisticsApplicationRow::matchingRoundId,
                Collectors.counting()
            ));
    }

    private Map<Long, Set<Long>> groupMemberIdsByRound(Collection<ProjectStatisticsApplicationRow> applications) {
        Map<Long, Set<Long>> memberIdsByRound = new HashMap<>();
        for (ProjectStatisticsApplicationRow application : applications) {
            memberIdsByRound
                .computeIfAbsent(application.matchingRoundId(), ignored -> new HashSet<>())
                .add(application.applicantMemberId());
        }
        return memberIdsByRound;
    }

    private Map<ProjectMemberKey, List<ProjectStatisticsApplicationRow>> groupApplications(
        Collection<ProjectStatisticsApplicationRow> applications
    ) {
        return sortApplications(applications).stream()
            .collect(Collectors.groupingBy(
                ProjectMemberKey::from,
                LinkedHashMap::new,
                Collectors.toList()
            ));
    }

    private List<ProjectStatisticsMemberRow> sortMembers(Collection<ProjectStatisticsMemberRow> members) {
        return members.stream()
            .sorted(Comparator
                .comparing(ProjectStatisticsMemberRow::projectId)
                .thenComparing(ProjectStatisticsMemberRow::projectMemberId))
            .toList();
    }

    private List<ProjectStatisticsApprovedApplicationRow> sortApprovedApplications(
        Collection<ProjectStatisticsApprovedApplicationRow> applications
    ) {
        return applications.stream()
            .sorted(Comparator
                .comparing(ProjectStatisticsApprovedApplicationRow::projectId)
                .thenComparing(ProjectStatisticsApprovedApplicationRow::applicantMemberId)
                .thenComparing(
                    ProjectStatisticsApprovedApplicationRow::matchingRoundStartsAt,
                    Comparator.nullsLast(Comparator.naturalOrder())
                )
                .thenComparing(ProjectStatisticsApprovedApplicationRow::applicationId))
            .toList();
    }

    private List<ProjectStatisticsApplicationRow> sortApplications(
        Collection<ProjectStatisticsApplicationRow> applications
    ) {
        return applications.stream()
            .sorted(Comparator
                .comparing(ProjectStatisticsApplicationRow::projectId)
                .thenComparing(ProjectStatisticsApplicationRow::applicantMemberId)
                .thenComparing(ProjectStatisticsApplicationRow::matchingRoundType)
                .thenComparing(ProjectStatisticsApplicationRow::matchingRoundPhase)
                .thenComparing(ProjectStatisticsApplicationRow::applicationId))
            .toList();
    }

    private ProjectMemberStatisticsInfo toMemberInfo(
        ProjectStatisticsMemberRow member,
        List<ProjectStatisticsApplicationRow> applications
    ) {
        return new ProjectMemberStatisticsInfo(
            member.projectMemberId(),
            member.memberId(),
            member.part(),
            member.status(),
            applications.stream()
                .map(this::toApplicationInfo)
                .toList()
        );
    }

    private ProjectMemberApplicationStatisticsInfo toApplicationInfo(ProjectStatisticsApplicationRow row) {
        return new ProjectMemberApplicationStatisticsInfo(
            row.applicationId(),
            row.status(),
            new ProjectMatchingRoundStatisticsInfo(
                row.matchingRoundId(),
                row.matchingRoundType(),
                row.matchingRoundPhase()
            )
        );
    }

    private ProjectMatchingRoundStatisticsInfo toMatchingRoundInfo(ProjectStatisticsMatchingRoundRow row) {
        return new ProjectMatchingRoundStatisticsInfo(
            row.matchingRoundId(),
            row.matchingRoundType(),
            row.matchingRoundPhase()
        );
    }

    private ChapterProjectStatisticsSummaryInfo emptySummary() {
        return new ChapterProjectStatisticsSummaryInfo(List.of(), List.of(), List.of(), List.of());
    }

    private ChapterProjectMatchingStatisticsInfo emptyPublicMatchingStatistics(Long chapterId) {
        return new ChapterProjectMatchingStatisticsInfo(
            chapterId,
            List.of(),
            List.of(),
            new UnclassifiedMatchingStatisticsInfo(0L, List.of())
        );
    }

    private record StatisticsPopulation(
        Set<Long> eligibleMemberIds,
        Map<Long, Long> schoolIdByMemberId
    ) {
    }

    private record StatisticsContext(
        List<ProjectStatisticsMatchingRoundRow> rounds,
        List<ProjectStatisticsApplicationRow> applications,
        List<ProjectStatisticsApplicationRow> matchedApplications,
        List<ProjectStatisticsMemberRow> members,
        StatisticsPopulation population
    ) {
    }

    private record PublicMatchingContext(
        List<ProjectStatisticsMatchingRoundRow> rounds,
        List<ProjectMemberMatchingAssignment> classifiedAssignments,
        List<ProjectStatisticsMemberRow> unclassifiedMembers,
        List<ProjectStatisticsMemberRow> allMembers,
        StatisticsPopulation population
    ) {
    }

    private record ProjectMemberMatchingAssignment(
        ProjectStatisticsMemberRow member,
        ProjectStatisticsApprovedApplicationRow application
    ) {
    }

    private record ProjectMemberKey(Long projectId, Long memberId) {

        private static ProjectMemberKey from(ProjectStatisticsMemberRow row) {
            return new ProjectMemberKey(row.projectId(), row.memberId());
        }

        private static ProjectMemberKey from(ProjectStatisticsApplicationRow row) {
            return new ProjectMemberKey(row.projectId(), row.applicantMemberId());
        }

        private static ProjectMemberKey from(ProjectStatisticsApprovedApplicationRow row) {
            return new ProjectMemberKey(row.projectId(), row.applicantMemberId());
        }
    }
}
