package com.umc.product.project.application.authorization;

import java.time.Clock;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import com.umc.product.authorization.application.port.in.query.GetChallengerRoleUseCase;
import com.umc.product.authorization.application.port.in.query.dto.ChallengerRolePolicyInfo;
import com.umc.product.authorization.domain.RoleAttribute;
import com.umc.product.authorization.domain.SubjectAttributes;
import com.umc.product.authorization.domain.SubjectAttributes.GisuChallengerInfo;
import com.umc.product.authorization.domain.SubjectPolicyFacts;
import com.umc.product.authorization.domain.SystemRoleType;
import com.umc.product.authorization.domain.exception.AuthorizationDomainException;
import com.umc.product.authorization.domain.exception.AuthorizationErrorCode;
import com.umc.product.challenger.application.port.in.query.GetChallengerUseCase;
import com.umc.product.challenger.application.port.in.query.dto.ChallengerPolicyInfo;
import com.umc.product.common.domain.enums.OrganizationType;
import com.umc.product.member.application.port.in.query.GetMemberUseCase;
import com.umc.product.organization.application.port.in.query.GetChapterUseCase;
import com.umc.product.organization.application.port.in.query.GetGisuUseCase;
import com.umc.product.organization.application.port.in.query.dto.gisu.GisuInfo;

@Component
public class ProjectPolicySubjectSnapshotLoader {

    private final GetMemberUseCase getMemberUseCase;
    private final GetChallengerRoleUseCase getChallengerRoleUseCase;
    private final GetChallengerUseCase getChallengerUseCase;
    private final GetGisuUseCase getGisuUseCase;
    private final GetChapterUseCase getChapterUseCase;
    private final Clock clock;

    public ProjectPolicySubjectSnapshotLoader(
        GetMemberUseCase getMemberUseCase,
        GetChallengerRoleUseCase getChallengerRoleUseCase,
        GetChallengerUseCase getChallengerUseCase,
        GetGisuUseCase getGisuUseCase,
        GetChapterUseCase getChapterUseCase,
        Clock clock
    ) {
        this.getMemberUseCase = getMemberUseCase;
        this.getChallengerRoleUseCase = getChallengerRoleUseCase;
        this.getChallengerUseCase = getChallengerUseCase;
        this.getGisuUseCase = getGisuUseCase;
        this.getChapterUseCase = getChapterUseCase;
        this.clock = clock;
    }

    public ProjectPolicySubjectSnapshot load(long memberId) {
        Instant evaluatedAt = clock.instant();
        long schoolId = getMemberUseCase.getById(memberId).schoolId();
        List<ChallengerRolePolicyInfo> roles = getChallengerRoleUseCase.listPolicyFactsByMemberId(memberId);
        List<ChallengerPolicyInfo> challengers = getChallengerUseCase.listPolicyFactsByMemberId(memberId);
        Set<Long> gisuIds = gisuIds(roles, challengers);
        Map<Long, GisuInfo> gisus = loadGisus(gisuIds);
        Map<ProjectPolicySchoolChapterKey, Long> chapters = loadSchoolChapters(
            gisuIds, schoolIds(roles, schoolId));
        List<ProjectPolicyRoleTuple> roleTuples = roles.stream()
            .map(role -> roleTuple(role, requireGisu(gisus, role.gisuId())))
            .toList();
        List<ProjectPolicyChallengerTuple> challengerTuples = challengers.stream()
            .map(challenger -> challengerTuple(
                challenger,
                requireGisu(gisus, challenger.gisuId()),
                requireChapter(chapters, challenger.gisuId(), schoolId)))
            .toList();
        return snapshot(
            memberId,
            evaluatedAt,
            getChallengerRoleUseCase.isSuperAdmin(memberId),
            roleTuples,
            challengerTuples,
            chapters
        );
    }

    public ProjectPolicySubjectSnapshot loadAuthenticatedMember(long memberId) {
        return snapshot(memberId, clock.instant(), false, List.of(), List.of(), Map.of());
    }

    public ProjectPolicySubjectSnapshot loadMatchingManager(long memberId) {
        Instant evaluatedAt = clock.instant();
        List<ChallengerRolePolicyInfo> roles = getChallengerRoleUseCase.listPolicyFactsByMemberId(memberId);
        Map<Long, GisuInfo> gisus = loadGisus(roles.stream()
            .map(ChallengerRolePolicyInfo::gisuId)
            .collect(Collectors.toCollection(LinkedHashSet::new)));
        List<ProjectPolicyRoleTuple> roleTuples = roles.stream()
            .map(role -> roleTuple(role, requireGisu(gisus, role.gisuId())))
            .toList();
        return snapshot(
            memberId,
            evaluatedAt,
            getChallengerRoleUseCase.isSuperAdmin(memberId),
            roleTuples,
            List.of(),
            Map.of()
        );
    }

    public ProjectPolicySubjectSnapshot loadSystem(String systemId) {
        return ProjectPolicySubjectSnapshot.system(systemId, clock.instant());
    }

    public ProjectPolicySubjectSnapshot load(SubjectAttributes subject) {
        if (subject.policyFacts() != null) {
            return fromFacts(
                subject.memberId(),
                subject.policyFacts(),
                subject.systemRoles().contains(SystemRoleType.SUPER_ADMIN)
            );
        }
        Instant evaluatedAt = clock.instant();
        Set<Long> gisuIds = new LinkedHashSet<>();
        subject.roleAttributes().forEach(role -> gisuIds.add(role.gisuId()));
        subject.gisuChallengerInfos().forEach(challenger -> gisuIds.add(challenger.gisuId()));
        Map<Long, GisuInfo> gisus = loadGisus(gisuIds);
        Set<Long> schoolIds = subject.roleAttributes().stream()
            .filter(role -> role.organizationType() == OrganizationType.SCHOOL)
            .map(RoleAttribute::organizationId)
            .collect(Collectors.toCollection(LinkedHashSet::new));
        Map<ProjectPolicySchoolChapterKey, Long> chapters = loadSchoolChapters(gisuIds, schoolIds);
        List<ProjectPolicyRoleTuple> roles = subject.roleAttributes().stream()
            .map(role -> roleTuple(role, requireGisu(gisus, role.gisuId())))
            .toList();
        List<ProjectPolicyChallengerTuple> challengers = subject.gisuChallengerInfos().stream()
            .map(challenger -> challengerTuple(challenger, requireGisu(gisus, challenger.gisuId())))
            .toList();
        return snapshot(
            subject.memberId(),
            evaluatedAt,
            subject.systemRoles().contains(SystemRoleType.SUPER_ADMIN),
            roles,
            challengers,
            chapters
        );
    }

    public ProjectPolicySubjectSnapshot fromFacts(long memberId, SubjectPolicyFacts facts) {
        return fromFacts(memberId, facts, false);
    }

    public ProjectPolicySubjectSnapshot fromFacts(
        long memberId,
        SubjectPolicyFacts facts,
        boolean superAdmin
    ) {
        List<ProjectPolicyRoleTuple> roles = facts.roles().stream()
            .map(role -> new ProjectPolicyRoleTuple(
                role.roleType(), role.organizationType(), role.organizationId(), role.responsiblePart(),
                role.gisuId(), role.gisuStartAt(), role.gisuEndAt()))
            .toList();
        List<ProjectPolicyChallengerTuple> challengers = facts.challengers().stream()
            .map(challenger -> new ProjectPolicyChallengerTuple(
                challenger.challengerId(), challenger.gisuId(), challenger.chapterId(), challenger.part(),
                challenger.gisuStartAt(), challenger.gisuEndAt()))
            .toList();
        Map<ProjectPolicySchoolChapterKey, Long> chapters = facts.chapterIdByGisuAndSchool().entrySet().stream()
            .collect(Collectors.toUnmodifiableMap(
                entry -> new ProjectPolicySchoolChapterKey(entry.getKey().gisuId(), entry.getKey().schoolId()),
                Map.Entry::getValue));
        return snapshot(memberId, facts.evaluatedAt(), superAdmin, roles, challengers, chapters);
    }

    private Set<Long> gisuIds(
        List<ChallengerRolePolicyInfo> roles,
        List<ChallengerPolicyInfo> challengers
    ) {
        Set<Long> ids = roles.stream()
            .map(ChallengerRolePolicyInfo::gisuId)
            .collect(Collectors.toCollection(LinkedHashSet::new));
        challengers.stream().map(ChallengerPolicyInfo::gisuId).forEach(ids::add);
        return ids;
    }

    private Set<Long> schoolIds(List<ChallengerRolePolicyInfo> roles, long memberSchoolId) {
        Set<Long> ids = roles.stream()
            .filter(role -> role.organizationType() == OrganizationType.SCHOOL)
            .map(ChallengerRolePolicyInfo::organizationId)
            .collect(Collectors.toCollection(LinkedHashSet::new));
        ids.add(memberSchoolId);
        return ids;
    }

    private Map<Long, GisuInfo> loadGisus(Set<Long> gisuIds) {
        if (gisuIds.isEmpty()) {
            return Map.of();
        }
        return getGisuUseCase.getByIds(gisuIds).stream()
            .collect(Collectors.toUnmodifiableMap(GisuInfo::gisuId, Function.identity()));
    }

    private Map<ProjectPolicySchoolChapterKey, Long> loadSchoolChapters(
        Set<Long> gisuIds,
        Set<Long> schoolIds
    ) {
        if (gisuIds.isEmpty() || schoolIds.isEmpty()) {
            return Map.of();
        }
        Map<ProjectPolicySchoolChapterKey, Long> result = new LinkedHashMap<>();
        getChapterUseCase.getChapterMapByGisuIdsAndSchoolIds(gisuIds, schoolIds)
            .forEach((gisuId, bySchool) -> bySchool.forEach((schoolId, chapter) ->
                result.put(new ProjectPolicySchoolChapterKey(gisuId, schoolId), chapter.id())));
        return Map.copyOf(result);
    }

    private ProjectPolicyRoleTuple roleTuple(ChallengerRolePolicyInfo role, GisuInfo gisu) {
        return new ProjectPolicyRoleTuple(
            role.roleType(), role.organizationType(), role.organizationId(), role.responsiblePart(),
            role.gisuId(), gisu.startAt(), gisu.endAt());
    }

    private ProjectPolicyRoleTuple roleTuple(RoleAttribute role, GisuInfo gisu) {
        return new ProjectPolicyRoleTuple(
            role.roleType(), role.organizationType(), role.organizationId(), role.responsiblePart(),
            role.gisuId(), gisu.startAt(), gisu.endAt());
    }

    private ProjectPolicyChallengerTuple challengerTuple(
        ChallengerPolicyInfo challenger,
        GisuInfo gisu,
        long chapterId
    ) {
        return new ProjectPolicyChallengerTuple(
            challenger.challengerId(), challenger.gisuId(), chapterId, challenger.part(),
            gisu.startAt(), gisu.endAt());
    }

    private ProjectPolicyChallengerTuple challengerTuple(GisuChallengerInfo challenger, GisuInfo gisu) {
        return new ProjectPolicyChallengerTuple(
            challenger.challengerId(), challenger.gisuId(), challenger.chapterId(), challenger.part(),
            gisu.startAt(), gisu.endAt());
    }

    private ProjectPolicySubjectSnapshot snapshot(
        long memberId,
        Instant evaluatedAt,
        boolean superAdmin,
        List<ProjectPolicyRoleTuple> roles,
        List<ProjectPolicyChallengerTuple> challengers,
        Map<ProjectPolicySchoolChapterKey, Long> chapters
    ) {
        return new ProjectPolicySubjectSnapshot(
            new ProjectPolicyPrincipal.Member(memberId), evaluatedAt, superAdmin, roles, challengers, chapters);
    }

    private GisuInfo requireGisu(Map<Long, GisuInfo> gisus, Long gisuId) {
        GisuInfo gisu = gisus.get(gisuId);
        if (gisu == null) {
            throw policyFailure();
        }
        return gisu;
    }

    private long requireChapter(
        Map<ProjectPolicySchoolChapterKey, Long> chapters,
        long gisuId,
        long schoolId
    ) {
        Long chapterId = chapters.get(new ProjectPolicySchoolChapterKey(gisuId, schoolId));
        if (chapterId == null) {
            throw policyFailure();
        }
        return chapterId;
    }

    private AuthorizationDomainException policyFailure() {
        return new AuthorizationDomainException(AuthorizationErrorCode.POLICY_EVALUATION_FAILED);
    }
}
