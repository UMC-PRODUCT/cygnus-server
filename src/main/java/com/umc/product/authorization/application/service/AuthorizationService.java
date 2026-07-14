package com.umc.product.authorization.application.service;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.umc.product.authorization.application.port.in.CheckPermissionUseCase;
import com.umc.product.authorization.application.port.in.query.GetChallengerRoleUseCase;
import com.umc.product.authorization.application.port.in.query.dto.ChallengerRolePolicyInfo;
import com.umc.product.authorization.application.port.out.LoadChallengerRolePort;
import com.umc.product.authorization.application.port.out.ResourcePermissionEvaluator;
import com.umc.product.authorization.application.port.out.SemanticResourcePermissionEvaluator;
import com.umc.product.authorization.domain.AuthoritySnapshot;
import com.umc.product.authorization.domain.ResourcePermission;
import com.umc.product.authorization.domain.ResourceType;
import com.umc.product.authorization.domain.RoleAttribute;
import com.umc.product.authorization.domain.SubjectAttributes;
import com.umc.product.authorization.domain.SubjectAttributes.GisuChallengerInfo;
import com.umc.product.authorization.domain.SubjectPolicyFacts;
import com.umc.product.authorization.domain.SubjectPolicyFacts.ChallengerPolicyFact;
import com.umc.product.authorization.domain.SubjectPolicyFacts.RolePolicyFact;
import com.umc.product.authorization.domain.SubjectPolicyFacts.SchoolChapterKey;
import com.umc.product.authorization.domain.SystemRoleType;
import com.umc.product.authorization.domain.exception.AuthorizationDomainException;
import com.umc.product.authorization.domain.exception.AuthorizationErrorCode;
import com.umc.product.challenger.application.port.in.query.GetChallengerUseCase;
import com.umc.product.challenger.application.port.in.query.dto.ChallengerInfo;
import com.umc.product.challenger.application.port.in.query.dto.ChallengerPolicyInfo;
import com.umc.product.common.domain.enums.OrganizationType;
import com.umc.product.global.cache.application.port.in.CacheUseCase;
import com.umc.product.global.cache.domain.CacheKey;
import com.umc.product.global.cache.domain.CacheLookup;
import com.umc.product.global.cache.domain.CacheNamespace;
import com.umc.product.global.cache.domain.CacheSpec;
import com.umc.product.global.logging.OperationalMetrics;
import com.umc.product.member.application.port.in.query.GetMemberUseCase;
import com.umc.product.member.application.port.in.query.ListMemberSystemRoleUseCase;
import com.umc.product.member.application.port.in.query.dto.MemberInfo;
import com.umc.product.member.domain.exception.MemberDomainException;
import com.umc.product.member.domain.exception.MemberErrorCode;
import com.umc.product.organization.application.port.in.query.GetChapterUseCase;
import com.umc.product.organization.application.port.in.query.GetGisuUseCase;
import com.umc.product.organization.application.port.in.query.dto.gisu.GisuInfo;

import lombok.extern.slf4j.Slf4j;

@Service
@Transactional(readOnly = true)
@Slf4j
public class AuthorizationService implements CheckPermissionUseCase {

    private static final CacheSpec<String> AUTHORITY_SNAPSHOT_CACHE_SPEC = CacheSpec.of(
        CacheNamespace.AUTHORITY_SNAPSHOT,
        String.class,
        Duration.ofSeconds(30),
        10_000L
    );

    private final LoadChallengerRolePort loadChallengerRolePort;
    private final Map<ResourceType, ResourcePermissionEvaluator> evaluators;

    private final GetMemberUseCase getMemberUseCase;
    private final ListMemberSystemRoleUseCase listMemberSystemRoleUseCase;
    private final GetChapterUseCase getChapterUseCase;
    private final GetChallengerUseCase getChallengerUseCase;
    private final GetGisuUseCase getGisuUseCase;
    private final GetChallengerRoleUseCase getChallengerRoleUseCase;
    private final OperationalMetrics operationalMetrics;
    private final CacheUseCase cacheUseCase;
    private final AuthoritySnapshotCacheSerializer authoritySnapshotCacheSerializer;
    private final Clock clock;

    /**
     * ResourcePermissionEvaluator에 대한 생성자 주입
     * <p>
     * 셍성자에서 List를 Map으로 변환하기 위해서 Lombok을 사용하지 않았음.
     */
    @Autowired
    public AuthorizationService(
        LoadChallengerRolePort loadChallengerRolePort,
        List<ResourcePermissionEvaluator> evaluatorList,
        GetMemberUseCase getMemberUseCase,
        ListMemberSystemRoleUseCase listMemberSystemRoleUseCase,
        GetChapterUseCase getChapterUseCase,
        GetChallengerUseCase getChallengerUseCase,
        GetGisuUseCase getGisuUseCase,
        OperationalMetrics operationalMetrics,
        CacheUseCase cacheUseCase,
        AuthoritySnapshotCacheSerializer authoritySnapshotCacheSerializer,
        Clock clock
    ) {
        this.loadChallengerRolePort = loadChallengerRolePort;
        this.getMemberUseCase = getMemberUseCase;
        this.listMemberSystemRoleUseCase = listMemberSystemRoleUseCase;
        this.getChapterUseCase = getChapterUseCase;
        this.getChallengerUseCase = getChallengerUseCase;
        this.getGisuUseCase = getGisuUseCase;
        this.getChallengerRoleUseCase = null;
        this.operationalMetrics = operationalMetrics;
        this.cacheUseCase = cacheUseCase;
        this.authoritySnapshotCacheSerializer = authoritySnapshotCacheSerializer;
        this.clock = clock;
        this.evaluators = evaluatorList.stream()
            .collect(Collectors.toMap(
                ResourcePermissionEvaluator::supportedResourceType,
                Function.identity()
            ));

        log.info("등록된 ResourcePermissionEvaluator: {}", evaluators.keySet());
    }

    AuthorizationService(
        LoadChallengerRolePort loadChallengerRolePort,
        List<ResourcePermissionEvaluator> evaluatorList,
        GetMemberUseCase getMemberUseCase,
        ListMemberSystemRoleUseCase listMemberSystemRoleUseCase,
        GetChapterUseCase getChapterUseCase,
        GetChallengerUseCase getChallengerUseCase,
        OperationalMetrics operationalMetrics,
        CacheUseCase cacheUseCase,
        AuthoritySnapshotCacheSerializer authoritySnapshotCacheSerializer
    ) {
        this.loadChallengerRolePort = loadChallengerRolePort;
        this.getMemberUseCase = getMemberUseCase;
        this.listMemberSystemRoleUseCase = listMemberSystemRoleUseCase;
        this.getChapterUseCase = getChapterUseCase;
        this.getChallengerUseCase = getChallengerUseCase;
        this.getGisuUseCase = null;
        this.getChallengerRoleUseCase = null;
        this.operationalMetrics = operationalMetrics;
        this.cacheUseCase = cacheUseCase;
        this.authoritySnapshotCacheSerializer = authoritySnapshotCacheSerializer;
        this.clock = Clock.systemUTC();
        this.evaluators = evaluatorList.stream().collect(Collectors.toMap(
            ResourcePermissionEvaluator::supportedResourceType,
            Function.identity()
        ));
    }

    public AuthorizationService(
        GetChallengerRoleUseCase getChallengerRoleUseCase,
        List<ResourcePermissionEvaluator> evaluatorList,
        GetMemberUseCase getMemberUseCase,
        GetChapterUseCase getChapterUseCase,
        GetChallengerUseCase getChallengerUseCase,
        GetGisuUseCase getGisuUseCase,
        OperationalMetrics operationalMetrics,
        Clock clock
    ) {
        this.loadChallengerRolePort = null;
        this.getMemberUseCase = getMemberUseCase;
        this.listMemberSystemRoleUseCase = null;
        this.getChapterUseCase = getChapterUseCase;
        this.getChallengerUseCase = getChallengerUseCase;
        this.getGisuUseCase = getGisuUseCase;
        this.getChallengerRoleUseCase = getChallengerRoleUseCase;
        this.operationalMetrics = operationalMetrics;
        this.cacheUseCase = null;
        this.authoritySnapshotCacheSerializer = null;
        this.clock = clock;
        this.evaluators = evaluatorList.stream().collect(Collectors.toMap(
            ResourcePermissionEvaluator::supportedResourceType,
            Function.identity()
        ));
    }

    @Override
    public boolean check(Long memberId, ResourcePermission permission) {
        SubjectAttributes subjectAttributes = loadSubject(memberId);
        return check(subjectAttributes, permission);
    }

    @Override
    public SubjectAttributes loadSubject(Long memberId) {
        log.debug("권한 평가 시작: memberId={}", memberId);
        if (cacheUseCase == null) {
            return loadFreshSubject(memberId);
        }
        CacheKey cacheKey = authoritySnapshotCacheKey(memberId);

        Optional<SubjectAttributes> cachedSubject = readCachedSubject(cacheKey, memberId);
        if (cachedSubject.isPresent()) {
            SubjectAttributes cached = cachedSubject.get();
            return getGisuUseCase == null ? cached : enrichPolicyFacts(cached);
        }

        SubjectAttributes subjectAttributes = loadFreshSubject(memberId);
        cacheSubject(cacheKey, memberId, subjectAttributes);
        return subjectAttributes;
    }

    private SubjectAttributes loadFreshSubject(Long memberId) {
        if (getGisuUseCase != null) {
            MemberInfo memberInfo = getMemberUseCase.getById(memberId);
            SubjectPolicyFacts facts = loadPolicyFacts(memberId, memberInfo.schoolId());
            return facts.toSubjectAttributes(memberId, memberInfo.schoolId(), listSystemRoles(memberId));
        }

        // 사용자가 활동한 모든 기수를 확인
        // 해당 기수마다 chapterId, challengerRoleId를 가져옴
        MemberInfo memberInfo = getMemberUseCase.getById(memberId);

        // 학교 ID는 회원정보에 저장되어 있음
        Long schoolId = memberInfo.schoolId();

        // memberId로 사용자와 관련된 모든 challenger를 가지고 옴
        // 그 challenger를 기반으로 사용자가 활동했던 모든 기수를 가져옴.
        // 그러면 기수와 학교를 조합해서 챕터들이 나오겠지? 굳 그거 쓰면 될듯
        List<ChallengerInfo> memberChallengerList = getChallengerUseCase.getAllByMemberId(memberId);
        List<GisuChallengerInfo> chapterIds = memberChallengerList.stream().map((challengerInfo) ->
            GisuChallengerInfo.builder()
                .gisuId(challengerInfo.gisuId())
                .chapterId(getChapterUseCase.byGisuAndSchool(challengerInfo.gisuId(), schoolId).id())
                .part(challengerInfo.part())
                .challengerId(challengerInfo.challengerId())
                .build()
        ).toList();
        List<RoleAttribute> roles = loadChallengerRolePort.findByMemberId(memberId).stream()
            .map(RoleAttribute::from)
            .toList();

        SubjectAttributes subjectAttributes = SubjectAttributes.builder()
            .memberId(memberId)
            .schoolId(schoolId)
            .gisuChallengerInfos(chapterIds)
            .roleAttributes(roles)
            .systemRoles(listSystemRoles(memberId))
            .build();

        log.debug("권한 평가 subject를 로드했습니다: memberId={}, roleCount={}, challengerCount={}",
            subjectAttributes.memberId(), subjectAttributes.roleAttributes().size(),
            subjectAttributes.gisuChallengerInfos().size());

        return subjectAttributes;
    }

    private SubjectAttributes enrichPolicyFacts(SubjectAttributes cached) {
        SubjectPolicyFacts facts = loadPolicyFacts(cached.memberId(), cached.schoolId());
        return facts.toSubjectAttributes(cached.memberId(), cached.schoolId(), cached.systemRoles());
    }

    private SubjectPolicyFacts loadPolicyFacts(long memberId, long memberSchoolId) {
        Instant evaluatedAt = clock.instant();
        List<ChallengerRolePolicyInfo> roles = policyRoles(memberId);
        List<ChallengerPolicyInfo> challengers = getChallengerUseCase.listPolicyFactsByMemberId(memberId);

        Set<Long> gisuIds = roles.stream()
            .map(ChallengerRolePolicyInfo::gisuId)
            .collect(Collectors.toCollection(LinkedHashSet::new));
        challengers.stream().map(ChallengerPolicyInfo::gisuId).forEach(gisuIds::add);
        Map<Long, GisuInfo> gisus = gisuIds.isEmpty()
            ? Map.of()
            : getGisuUseCase.getByIds(gisuIds).stream()
                .collect(Collectors.toUnmodifiableMap(GisuInfo::gisuId, Function.identity()));

        Set<Long> schoolIds = roles.stream()
            .filter(role -> role.organizationType() == OrganizationType.SCHOOL)
            .map(ChallengerRolePolicyInfo::organizationId)
            .collect(Collectors.toCollection(LinkedHashSet::new));
        schoolIds.add(memberSchoolId);
        Map<SchoolChapterKey, Long> chapters = new LinkedHashMap<>();
        if (!gisuIds.isEmpty()) {
            getChapterUseCase.getChapterMapByGisuIdsAndSchoolIds(gisuIds, schoolIds)
                .forEach((gisuId, bySchool) -> bySchool.forEach((schoolId, chapter) ->
                    chapters.put(new SchoolChapterKey(gisuId, schoolId), chapter.id())));
        }

        List<RolePolicyFact> roleFacts = roles.stream().map(role -> {
            GisuInfo gisu = requireGisu(gisus, role.gisuId());
            return new RolePolicyFact(
                role.roleType(), role.organizationType(), role.organizationId(), role.responsiblePart(),
                role.gisuId(), gisu.startAt(), gisu.endAt());
        }).toList();
        List<ChallengerPolicyFact> challengerFacts = challengers.stream().map(challenger -> {
            GisuInfo gisu = requireGisu(gisus, challenger.gisuId());
            Long chapterId = chapters.get(new SchoolChapterKey(challenger.gisuId(), memberSchoolId));
            if (chapterId == null) {
                throw policyEvaluationFailure();
            }
            return new ChallengerPolicyFact(
                challenger.challengerId(), challenger.gisuId(), chapterId, challenger.part(),
                gisu.startAt(), gisu.endAt());
        }).toList();
        return new SubjectPolicyFacts(evaluatedAt, roleFacts, challengerFacts, chapters);
    }

    private List<ChallengerRolePolicyInfo> policyRoles(long memberId) {
        if (getChallengerRoleUseCase != null) {
            return getChallengerRoleUseCase.listPolicyFactsByMemberId(memberId);
        }
        return loadChallengerRolePort.findByMemberId(memberId).stream()
            .map(ChallengerRolePolicyInfo::from)
            .toList();
    }

    private GisuInfo requireGisu(Map<Long, GisuInfo> gisus, Long gisuId) {
        GisuInfo gisu = gisus.get(gisuId);
        if (gisu == null) {
            throw policyEvaluationFailure();
        }
        return gisu;
    }

    private AuthorizationDomainException policyEvaluationFailure() {
        return new AuthorizationDomainException(AuthorizationErrorCode.POLICY_EVALUATION_FAILED);
    }

    private Set<SystemRoleType> listSystemRoles(Long memberId) {
        if (listMemberSystemRoleUseCase == null) {
            return Set.of();
        }
        return listMemberSystemRoleUseCase.listByMemberId(memberId).stream()
            .map(role -> SystemRoleType.from(role.roleType()))
            .collect(Collectors.toUnmodifiableSet());
    }

    private Optional<SubjectAttributes> readCachedSubject(CacheKey cacheKey, Long memberId) {
        CacheLookup<String> lookup = cacheUseCase.get(AUTHORITY_SNAPSHOT_CACHE_SPEC, cacheKey);
        if (lookup instanceof CacheLookup.Hit<String> hit) {
            if (!getMemberUseCase.existsById(memberId)) {
                cacheUseCase.evict(CacheNamespace.AUTHORITY_SNAPSHOT, cacheKey);
                throw new MemberDomainException(MemberErrorCode.MEMBER_NOT_FOUND);
            }
            try {
                log.debug("권한 평가 subject 캐시 hit: memberId={}", memberId);
                AuthoritySnapshot snapshot = authoritySnapshotCacheSerializer.deserialize(hit.value());
                if (!Objects.equals(snapshot.memberId(), memberId)) {
                    throw new AuthorizationDomainException(
                        AuthorizationErrorCode.POLICY_EVALUATION_FAILED,
                        "권한 snapshot 캐시의 회원 정보가 일치하지 않습니다."
                    );
                }
                return Optional.of(snapshot.toSubjectAttributes());
            } catch (AuthorizationDomainException e) {
                log.warn("권한 평가 subject 캐시 역직렬화 실패로 캐시를 제거합니다: memberId={}", memberId);
                cacheUseCase.evict(CacheNamespace.AUTHORITY_SNAPSHOT, cacheKey);
            }
        }
        return Optional.empty();
    }

    private void cacheSubject(CacheKey cacheKey, Long memberId, SubjectAttributes subjectAttributes) {
        try {
            AuthoritySnapshot snapshot = subjectAttributes.toAuthoritySnapshot();
            String payload = authoritySnapshotCacheSerializer.serialize(snapshot);
            cacheUseCase.put(AUTHORITY_SNAPSHOT_CACHE_SPEC, cacheKey, payload);
        } catch (AuthorizationDomainException e) {
            log.warn("권한 평가 subject 캐시 저장을 건너뜁니다: memberId={}", memberId);
        }
    }

    private CacheKey authoritySnapshotCacheKey(Long memberId) {
        return AuthoritySnapshotCacheKeys.member(memberId);
    }

    @Override
    public boolean check(SubjectAttributes subjectAttributes, ResourcePermission permission) {
        // 리소스 유형에 맞는 권한 평가기를 선택함. 없다면 에러 발생
        ResourcePermissionEvaluator evaluator = evaluator(permission);

        // 평가기로 평가
        boolean hasPermission = evaluator.evaluate(subjectAttributes, permission);

        log.debug("Permission check - memberId: {}, roles: {}, resource: {}:{}, permission: {}, result: {}",
            subjectAttributes.memberId(), subjectAttributes.roleAttributes(), permission.resourceType(),
            permission.resourceId(),
            permission.permission(), hasPermission);

        return hasPermission;
    }

    @Override
    public boolean check(Long memberId, ResourcePermission permission, String actionId) {
        return check(loadSubject(memberId), permission, actionId);
    }

    @Override
    public boolean check(
        SubjectAttributes subjectAttributes,
        ResourcePermission permission,
        String actionId
    ) {
        ResourcePermissionEvaluator evaluator = evaluator(permission);
        if (!(evaluator instanceof SemanticResourcePermissionEvaluator semanticEvaluator)) {
            throw policyEvaluationFailure();
        }
        return semanticEvaluator.evaluateSemantic(subjectAttributes, permission, actionId);
    }

    private ResourcePermissionEvaluator evaluator(ResourcePermission permission) {
        ResourcePermissionEvaluator evaluator = evaluators.get(permission.resourceType());
        if (evaluator == null) {
            throw new AuthorizationDomainException(AuthorizationErrorCode.NO_EVALUATOR_MATCHING_RESOURCE_TYPE,
                "Evaluator for Resource Type [" + permission.resourceType() + "] not found.");
        }
        return evaluator;
    }

    @Override
    public void checkOrThrow(Long memberId, ResourcePermission permission) {
        if (!check(memberId, permission)) {
            throwDenied(memberId, permission);
        }
    }

    @Override
    public void checkOrThrow(Long memberId, ResourcePermission permission, String actionId) {
        if (!check(memberId, permission, actionId)) {
            throwDenied(memberId, permission);
        }
    }

    @Override
    public void checkOrThrow(
        SubjectAttributes subjectAttributes,
        ResourcePermission permission,
        String actionId
    ) {
        if (!check(subjectAttributes, permission, actionId)) {
            throwDenied(subjectAttributes.memberId(), permission);
        }
    }

    private void throwDenied(Long memberId, ResourcePermission permission) {
        log.warn("Permission denied - memberId: {}, resource: {}:{}, permission: {}",
            memberId, permission.resourceType(), permission.resourceId(), permission.permission());
        operationalMetrics.recordSecurityEvent("AUTHORIZATION", "ACCESS_DENIED", "denied");
        throw new AuthorizationDomainException(AuthorizationErrorCode.RESOURCE_ACCESS_DENIED);
    }
}
