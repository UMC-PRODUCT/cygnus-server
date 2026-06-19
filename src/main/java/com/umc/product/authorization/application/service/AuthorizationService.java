package com.umc.product.authorization.application.service;

import com.umc.product.authorization.domain.AuthoritySnapshot;
import com.umc.product.authorization.application.port.in.CheckPermissionUseCase;
import com.umc.product.authorization.application.port.out.LoadChallengerRolePort;
import com.umc.product.authorization.application.port.out.ResourcePermissionEvaluator;
import com.umc.product.authorization.domain.ResourcePermission;
import com.umc.product.authorization.domain.ResourceType;
import com.umc.product.authorization.domain.RoleAttribute;
import com.umc.product.authorization.domain.SubjectAttributes;
import com.umc.product.authorization.domain.SubjectAttributes.GisuChallengerInfo;
import com.umc.product.authorization.domain.exception.AuthorizationDomainException;
import com.umc.product.authorization.domain.exception.AuthorizationErrorCode;
import com.umc.product.challenger.application.port.in.query.GetChallengerUseCase;
import com.umc.product.challenger.application.port.in.query.dto.ChallengerInfo;
import com.umc.product.global.cache.application.port.in.CacheUseCase;
import com.umc.product.global.cache.domain.CacheKey;
import com.umc.product.global.cache.domain.CacheLookup;
import com.umc.product.global.cache.domain.CacheNamespace;
import com.umc.product.global.cache.domain.CacheSpec;
import com.umc.product.global.logging.OperationalMetrics;
import com.umc.product.member.application.port.in.query.GetMemberUseCase;
import com.umc.product.member.application.port.in.query.dto.MemberInfo;
import com.umc.product.organization.application.port.in.query.GetChapterUseCase;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
    private final GetChapterUseCase getChapterUseCase;
    private final GetChallengerUseCase getChallengerUseCase;
    private final OperationalMetrics operationalMetrics;
    private final CacheUseCase cacheUseCase;
    private final AuthoritySnapshotCacheSerializer authoritySnapshotCacheSerializer;

    /**
     * ResourcePermissionEvaluator에 대한 생성자 주입
     * <p>
     * 셍성자에서 List를 Map으로 변환하기 위해서 Lombok을 사용하지 않았음.
     */
    public AuthorizationService(LoadChallengerRolePort loadChallengerRolePort,
                                List<ResourcePermissionEvaluator> evaluatorList, GetMemberUseCase getMemberUseCase,
                                GetChapterUseCase getChapterUseCase, GetChallengerUseCase getChallengerUseCase,
                                OperationalMetrics operationalMetrics, CacheUseCase cacheUseCase,
                                AuthoritySnapshotCacheSerializer authoritySnapshotCacheSerializer) {
        this.loadChallengerRolePort = loadChallengerRolePort;
        this.getMemberUseCase = getMemberUseCase;
        this.getChapterUseCase = getChapterUseCase;
        this.getChallengerUseCase = getChallengerUseCase;
        this.operationalMetrics = operationalMetrics;
        this.cacheUseCase = cacheUseCase;
        this.authoritySnapshotCacheSerializer = authoritySnapshotCacheSerializer;
        this.evaluators = evaluatorList.stream()
            .collect(Collectors.toMap(
                ResourcePermissionEvaluator::supportedResourceType,
                Function.identity()
            ));

        log.info("등록된 ResourcePermissionEvaluator: {}", evaluators.keySet());
    }

    @Override
    public boolean check(Long memberId, ResourcePermission permission) {
        SubjectAttributes subjectAttributes = loadSubject(memberId);
        return check(subjectAttributes, permission);
    }

    @Override
    public SubjectAttributes loadSubject(Long memberId) {
        log.debug("권한 평가 시작: memberId={}", memberId);
        CacheKey cacheKey = authoritySnapshotCacheKey(memberId);

        Optional<SubjectAttributes> cachedSubject = readCachedSubject(cacheKey, memberId);
        if (cachedSubject.isPresent()) {
            return cachedSubject.get();
        }

        SubjectAttributes subjectAttributes = loadFreshSubject(memberId);
        cacheSubject(cacheKey, memberId, subjectAttributes);
        return subjectAttributes;
    }

    private SubjectAttributes loadFreshSubject(Long memberId) {
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
        List<RoleAttribute> roles = loadChallengerRolePort.findByMemberId(memberId)
            .stream().map(RoleAttribute::from).toList();

        SubjectAttributes subjectAttributes = SubjectAttributes.builder()
            .memberId(memberId)
            .schoolId(schoolId)
            .gisuChallengerInfos(chapterIds)
            .roleAttributes(roles)
            .build();

        log.debug("권한 평가 subject를 로드했습니다: memberId={}, roleCount={}, challengerCount={}",
            subjectAttributes.memberId(), subjectAttributes.roleAttributes().size(),
            subjectAttributes.gisuChallengerInfos().size());

        return subjectAttributes;
    }

    private Optional<SubjectAttributes> readCachedSubject(CacheKey cacheKey, Long memberId) {
        CacheLookup<String> lookup = cacheUseCase.get(AUTHORITY_SNAPSHOT_CACHE_SPEC, cacheKey);
        if (lookup instanceof CacheLookup.Hit<String> hit) {
            try {
                log.debug("권한 평가 subject 캐시 hit: memberId={}", memberId);
                return Optional.of(authoritySnapshotCacheSerializer.deserialize(hit.value()).toSubjectAttributes());
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
        ResourcePermissionEvaluator evaluator = evaluators.get(permission.resourceType());
        if (evaluator == null) {
            throw new AuthorizationDomainException(AuthorizationErrorCode.NO_EVALUATOR_MATCHING_RESOURCE_TYPE,
                "Evaluator for Resource Type [" + permission.resourceType() + "] not found.");
        }

        // 평가기로 평가
        boolean hasPermission = evaluator.evaluate(subjectAttributes, permission);

        log.debug("Permission check - memberId: {}, roles: {}, resource: {}:{}, permission: {}, result: {}",
            subjectAttributes.memberId(), subjectAttributes.roleAttributes(), permission.resourceType(),
            permission.resourceId(),
            permission.permission(), hasPermission);

        return hasPermission;
    }

    @Override
    public void checkOrThrow(Long memberId, ResourcePermission permission) {
        if (!check(memberId, permission)) {
            log.warn("Permission denied - memberId: {}, resource: {}:{}, permission: {}",
                memberId, permission.resourceType(), permission.resourceId(), permission.permission());
            operationalMetrics.recordSecurityEvent("AUTHORIZATION", "ACCESS_DENIED", "denied");

            throw new AuthorizationDomainException(AuthorizationErrorCode.RESOURCE_ACCESS_DENIED);
        }
    }
}
