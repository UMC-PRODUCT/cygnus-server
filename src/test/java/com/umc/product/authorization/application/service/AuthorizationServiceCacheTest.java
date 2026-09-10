package com.umc.product.authorization.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.umc.product.authorization.application.port.out.LoadChallengerRolePort;
import com.umc.product.authorization.domain.ChallengerRole;
import com.umc.product.authorization.domain.SubjectAttributes;
import com.umc.product.challenger.application.port.in.query.GetChallengerUseCase;
import com.umc.product.challenger.application.port.in.query.dto.ChallengerInfo;
import com.umc.product.common.domain.enums.ChallengerPart;
import com.umc.product.common.domain.enums.ChallengerRoleType;
import com.umc.product.common.domain.enums.ChallengerTrack;
import com.umc.product.global.cache.application.port.in.CacheUseCase;
import com.umc.product.global.cache.domain.CacheKey;
import com.umc.product.global.cache.domain.CacheLookup;
import com.umc.product.global.cache.domain.CacheNamespace;
import com.umc.product.global.cache.domain.CacheSpec;
import com.umc.product.global.logging.OperationalMetrics;
import com.umc.product.member.application.port.in.query.GetMemberUseCase;
import com.umc.product.member.application.port.in.query.ListMemberSystemRoleUseCase;
import com.umc.product.member.application.port.in.query.dto.MemberInfo;
import com.umc.product.member.application.port.in.query.dto.MemberSystemRoleInfo;
import com.umc.product.member.domain.exception.MemberDomainException;
import com.umc.product.organization.application.port.in.query.GetChapterUseCase;
import com.umc.product.organization.application.port.in.query.dto.chapter.ChapterInfo;
import com.umc.product.organization.exception.OrganizationDomainException;
import com.umc.product.organization.exception.OrganizationErrorCode;

@ExtendWith(MockitoExtension.class)
@DisplayName("AuthorizationService 캐시")
class AuthorizationServiceCacheTest {

    private static final Long MEMBER_ID = 1L;
    private static final Long SCHOOL_ID = 30L;
    private static final Long GISU_ID = 9L;
    private static final Long CHAPTER_ID = 90L;
    private static final Long CHALLENGER_ID = 100L;

    @Mock
    LoadChallengerRolePort loadChallengerRolePort;

    @Mock
    GetMemberUseCase getMemberUseCase;

    @Mock
    ListMemberSystemRoleUseCase listMemberSystemRoleUseCase;

    @Mock
    GetChapterUseCase getChapterUseCase;

    @Mock
    GetChallengerUseCase getChallengerUseCase;

    @Mock
    OperationalMetrics operationalMetrics;

    @Test
    @DisplayName("loadSubject는 hit 시 회원 존재만 확인하고 권한 구성 UseCase를 다시 호출하지 않는다")
    void load_subject_cache_hit() {
        InMemoryCacheUseCase cacheUseCase = new InMemoryCacheUseCase();
        AuthorizationService sut = new AuthorizationService(
            loadChallengerRolePort,
            List.of(),
            getMemberUseCase,
            listMemberSystemRoleUseCase,
            getChapterUseCase,
            getChallengerUseCase,
            operationalMetrics,
            cacheUseCase,
            new AuthoritySnapshotCacheSerializer(new ObjectMapper().findAndRegisterModules())
        );
        given(getMemberUseCase.getById(MEMBER_ID)).willReturn(MemberInfo.builder()
            .id(MEMBER_ID)
            .schoolId(SCHOOL_ID)
            .build());
        given(getMemberUseCase.existsById(MEMBER_ID)).willReturn(true);
        given(getChallengerUseCase.getAllByMemberId(MEMBER_ID)).willReturn(List.of(ChallengerInfo.builder()
            .challengerId(CHALLENGER_ID)
            .memberId(MEMBER_ID)
            .gisuId(GISU_ID)
            .part(ChallengerPart.SPRINGBOOT)
            .build()));
        given(getChapterUseCase.byGisuAndSchool(GISU_ID, SCHOOL_ID)).willReturn(new ChapterInfo(CHAPTER_ID, "9기"));
        given(loadChallengerRolePort.findByMemberId(MEMBER_ID)).willReturn(List.of(ChallengerRole.create(
            CHALLENGER_ID,
            ChallengerRoleType.SCHOOL_PRESIDENT,
            SCHOOL_ID,
            null,
            GISU_ID
        )));
        given(listMemberSystemRoleUseCase.listByMemberId(MEMBER_ID)).willReturn(List.of(
            new MemberSystemRoleInfo(MEMBER_ID, "SUPER_ADMIN")
        ));

        SubjectAttributes first = sut.loadSubject(MEMBER_ID);
        SubjectAttributes second = sut.loadSubject(MEMBER_ID);

        assertThat(first.roleAttributes()).hasSize(1);
        assertThat(second.roleAttributes()).hasSize(1);
        assertThat(first.systemRoles()).hasSize(1);
        assertThat(second.systemRoles()).hasSize(1);
        assertThat(cacheUseCase.latestSpec().namespace()).isEqualTo(CacheNamespace.AUTHORITY_SNAPSHOT);
        assertThat(cacheUseCase.latestSpec().valueType()).isEqualTo(String.class);
        assertThat(cacheUseCase.latestValue()).isInstanceOf(String.class);
        assertThat((String) cacheUseCase.latestValue()).contains("SCHOOL_PRESIDENT");
        assertThat((String) cacheUseCase.latestValue()).contains("SUPER_ADMIN");
        verify(getMemberUseCase, times(1)).getById(MEMBER_ID);
        verify(getMemberUseCase, times(1)).existsById(MEMBER_ID);
        verify(getChallengerUseCase, times(1)).getAllByMemberId(MEMBER_ID);
        verify(getChapterUseCase, times(1)).byGisuAndSchool(GISU_ID, SCHOOL_ID);
        verify(loadChallengerRolePort, times(1)).findByMemberId(MEMBER_ID);
        verify(listMemberSystemRoleUseCase, times(1)).listByMemberId(MEMBER_ID);
    }

    @Test
    @DisplayName("캐시 hit에서도 회원이 삭제되었으면 기존 권한 snapshot을 반환하지 않는다")
    void cache_hit_rejects_deleted_member() {
        InMemoryCacheUseCase cacheUseCase = new InMemoryCacheUseCase();
        AuthorizationService sut = new AuthorizationService(
            loadChallengerRolePort,
            List.of(),
            getMemberUseCase,
            listMemberSystemRoleUseCase,
            getChapterUseCase,
            getChallengerUseCase,
            operationalMetrics,
            cacheUseCase,
            new AuthoritySnapshotCacheSerializer(new ObjectMapper().findAndRegisterModules())
        );
        given(getMemberUseCase.getById(MEMBER_ID))
            .willReturn(MemberInfo.builder().id(MEMBER_ID).schoolId(SCHOOL_ID).build());
        given(getMemberUseCase.existsById(MEMBER_ID)).willReturn(false);
        given(getChallengerUseCase.getAllByMemberId(MEMBER_ID)).willReturn(List.of());
        given(loadChallengerRolePort.findByMemberId(MEMBER_ID)).willReturn(List.of());
        given(listMemberSystemRoleUseCase.listByMemberId(MEMBER_ID)).willReturn(List.of());

        sut.loadSubject(MEMBER_ID);

        assertThatThrownBy(() -> sut.loadSubject(MEMBER_ID))
            .isInstanceOf(MemberDomainException.class);
    }

    @Test
    @DisplayName("지원하지 않는 schema version의 캐시는 제거하고 최신 권한 snapshot을 다시 적재한다")
    void rebuilds_subject_when_cached_schema_version_is_unsupported() {
        InMemoryCacheUseCase cacheUseCase = new InMemoryCacheUseCase();
        cacheUseCase.seed("{\"schemaVersion\":2}");
        AuthorizationService sut = new AuthorizationService(
            loadChallengerRolePort,
            List.of(),
            getMemberUseCase,
            listMemberSystemRoleUseCase,
            getChapterUseCase,
            getChallengerUseCase,
            operationalMetrics,
            cacheUseCase,
            new AuthoritySnapshotCacheSerializer(new ObjectMapper().findAndRegisterModules())
        );
        given(getMemberUseCase.existsById(MEMBER_ID)).willReturn(true);
        given(getMemberUseCase.getById(MEMBER_ID))
            .willReturn(MemberInfo.builder().id(MEMBER_ID).schoolId(SCHOOL_ID).build());
        given(getChallengerUseCase.getAllByMemberId(MEMBER_ID)).willReturn(List.of());
        given(loadChallengerRolePort.findByMemberId(MEMBER_ID)).willReturn(List.of());
        given(listMemberSystemRoleUseCase.listByMemberId(MEMBER_ID)).willReturn(List.of());

        SubjectAttributes result = sut.loadSubject(MEMBER_ID);

        assertThat(result.memberId()).isEqualTo(MEMBER_ID);
        assertThat((String) cacheUseCase.latestValue()).contains("\"schemaVersion\":1");
        assertThat(cacheUseCase.latestEvictedNamespace()).isEqualTo(CacheNamespace.AUTHORITY_SNAPSHOT);
        assertThat(cacheUseCase.latestEvictedKey()).isEqualTo(CacheKey.from("member:" + MEMBER_ID));
        verify(getMemberUseCase).existsById(MEMBER_ID);
        verify(getMemberUseCase).getById(MEMBER_ID);
    }

    @Test
    @DisplayName("캐시 key와 snapshot 회원이 다르면 캐시를 제거하고 요청 회원의 권한을 다시 적재한다")
    void rebuilds_subject_when_cached_member_does_not_match_key() {
        InMemoryCacheUseCase cacheUseCase = new InMemoryCacheUseCase();
        AuthoritySnapshotCacheSerializer serializer =
            new AuthoritySnapshotCacheSerializer(new ObjectMapper().findAndRegisterModules());
        cacheUseCase.seed(serializer.serialize(SubjectAttributes.builder()
            .memberId(999L)
            .build()
            .toAuthoritySnapshot()));
        AuthorizationService sut = new AuthorizationService(
            loadChallengerRolePort,
            List.of(),
            getMemberUseCase,
            listMemberSystemRoleUseCase,
            getChapterUseCase,
            getChallengerUseCase,
            operationalMetrics,
            cacheUseCase,
            serializer
        );
        given(getMemberUseCase.existsById(MEMBER_ID)).willReturn(true);
        given(getMemberUseCase.getById(MEMBER_ID))
            .willReturn(MemberInfo.builder().id(MEMBER_ID).schoolId(SCHOOL_ID).build());
        given(getChallengerUseCase.getAllByMemberId(MEMBER_ID)).willReturn(List.of());
        given(loadChallengerRolePort.findByMemberId(MEMBER_ID)).willReturn(List.of());
        given(listMemberSystemRoleUseCase.listByMemberId(MEMBER_ID)).willReturn(List.of());

        SubjectAttributes result = sut.loadSubject(MEMBER_ID);

        assertThat(result.memberId()).isEqualTo(MEMBER_ID);
        assertThat(cacheUseCase.latestEvictedKey()).isEqualTo(CacheKey.from("member:" + MEMBER_ID));
        verify(getMemberUseCase).getById(MEMBER_ID);
    }

    @Test
    @DisplayName("지부 없는 비수강 중앙 운영진은 권한 로딩과 캐시 복원 후에도 해당 기수 중앙 권한만 가진다")
    void 지부_없는_비수강_중앙_운영진의_권한을_로드하고_캐시한다() {
        // given
        InMemoryCacheUseCase cacheUseCase = new InMemoryCacheUseCase();
        AuthorizationService sut = authorizationService(cacheUseCase);
        givenSubject(null, List.of(), ChallengerRoleType.CENTRAL_OPERATING_TEAM_MEMBER, GISU_ID);
        given(getChapterUseCase.findByGisuAndSchool(GISU_ID, SCHOOL_ID)).willReturn(Optional.empty());
        given(getMemberUseCase.existsById(MEMBER_ID)).willReturn(true);

        // when
        SubjectAttributes first = sut.loadSubject(MEMBER_ID);
        SubjectAttributes cached = sut.loadSubject(MEMBER_ID);

        // then
        assertThat(first.gisuChallengerInfos().getFirst().chapterId()).isNull();
        assertThat(cached).isEqualTo(first);
        assertThat(cached.toAuthoritySnapshot().isCentralMemberInGisu(GISU_ID)).isTrue();
        assertThat(cached.toAuthoritySnapshot().isCentralMemberInGisu(GISU_ID + 1)).isFalse();
        assertThat(cached.toAuthoritySnapshot().isSchoolCoreInGisu(GISU_ID, SCHOOL_ID)).isFalse();
        assertThat(cached.toAuthoritySnapshot().isChapterPresidentInGisu(GISU_ID, CHAPTER_ID)).isFalse();
        verify(getChapterUseCase).findByGisuAndSchool(GISU_ID, SCHOOL_ID);
        verify(getChapterUseCase, never()).byGisuAndSchool(GISU_ID, SCHOOL_ID);
    }

    @Test
    @DisplayName("비수강 중앙 운영진도 해당 기수의 학교 지부 연결이 있으면 권한 정보에 보존한다")
    void 비수강_중앙_운영진의_기존_지부_연결을_보존한다() {
        // given
        AuthorizationService sut = authorizationService(new InMemoryCacheUseCase());
        givenSubject(null, List.of(), ChallengerRoleType.CENTRAL_OPERATING_TEAM_MEMBER, GISU_ID);
        given(getChapterUseCase.findByGisuAndSchool(GISU_ID, SCHOOL_ID))
            .willReturn(Optional.of(new ChapterInfo(CHAPTER_ID, "현재 지부")));

        // when
        SubjectAttributes subject = sut.loadSubject(MEMBER_ID);

        // then
        assertThat(subject.gisuChallengerInfos().getFirst().chapterId()).isEqualTo(CHAPTER_ID);
        assertThat(subject.toAuthoritySnapshot().isCentralMemberInGisu(GISU_ID)).isTrue();
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("지부가_필요한_소속")
    @DisplayName("같은 기수 비수강 중앙 운영진 이외의 소속은 지부 누락을 허용하지 않는다")
    void 지부_누락_허용을_다른_소속으로_확대하지_않는다(
        String description, ChallengerPart part, List<ChallengerTrack> tracks,
        ChallengerRoleType roleType, Long roleGisuId
    ) {
        // given
        InMemoryCacheUseCase cacheUseCase = new InMemoryCacheUseCase();
        AuthorizationService sut = authorizationService(cacheUseCase);
        givenSubject(part, tracks, roleType, roleGisuId);
        given(getChapterUseCase.byGisuAndSchool(GISU_ID, SCHOOL_ID))
            .willThrow(new OrganizationDomainException(OrganizationErrorCode.CHAPTER_NOT_FOUND));

        // when & then
        assertThatThrownBy(() -> sut.loadSubject(MEMBER_ID))
            .isInstanceOfSatisfying(OrganizationDomainException.class,
                exception -> assertThat(exception.getBaseCode()).isEqualTo(OrganizationErrorCode.CHAPTER_NOT_FOUND));
        assertThat(cacheUseCase.latestValue()).isNull();
        verify(getChapterUseCase, never()).findByGisuAndSchool(GISU_ID, SCHOOL_ID);
    }

    private static Stream<Arguments> 지부가_필요한_소속() {
        return Stream.of(
            Arguments.of("운영진 역할 없음", null, List.of(), null, GISU_ID),
            Arguments.of("비수강 학교 회장", null, List.of(), ChallengerRoleType.SCHOOL_PRESIDENT, GISU_ID),
            Arguments.of("다른 기수 중앙 운영진", null, List.of(),
                ChallengerRoleType.CENTRAL_OPERATING_TEAM_MEMBER, GISU_ID + 1),
            Arguments.of("Track 수강 중인 중앙 운영진", null, List.of(ChallengerTrack.WEB_PRODUCT_ENGINEER),
                ChallengerRoleType.CENTRAL_OPERATING_TEAM_MEMBER, GISU_ID),
            Arguments.of("레거시 ADMIN 소속 중앙 운영진", ChallengerPart.ADMIN, List.of(),
                ChallengerRoleType.CENTRAL_OPERATING_TEAM_MEMBER, GISU_ID)
        );
    }

    private void givenSubject(ChallengerPart part, List<ChallengerTrack> tracks,
                              ChallengerRoleType roleType, Long roleGisuId) {
        given(getMemberUseCase.getById(MEMBER_ID))
            .willReturn(MemberInfo.builder().id(MEMBER_ID).schoolId(SCHOOL_ID).build());
        given(getChallengerUseCase.getAllByMemberId(MEMBER_ID)).willReturn(List.of(ChallengerInfo.builder()
            .challengerId(CHALLENGER_ID).memberId(MEMBER_ID).gisuId(GISU_ID).part(part).tracks(tracks).build()));
        given(loadChallengerRolePort.findByMemberId(MEMBER_ID)).willReturn(roleType == null ? List.of() : List.of(
            ChallengerRole.create(CHALLENGER_ID, roleType,
                roleType.isAtLeastCentralMember() ? null : SCHOOL_ID, null, roleGisuId)
        ));
    }

    private AuthorizationService authorizationService(CacheUseCase cacheUseCase) {
        return new AuthorizationService(
            loadChallengerRolePort, List.of(), getMemberUseCase, listMemberSystemRoleUseCase,
            getChapterUseCase, getChallengerUseCase, operationalMetrics, cacheUseCase,
            new AuthoritySnapshotCacheSerializer(new ObjectMapper().findAndRegisterModules())
        );
    }

    private static class InMemoryCacheUseCase implements CacheUseCase {

        private Object value;
        private CacheSpec<?> latestSpec;
        private CacheNamespace latestEvictedNamespace;
        private CacheKey latestEvictedKey;

        @Override
        public <T> CacheLookup<T> get(CacheSpec<T> spec, CacheKey key) {
            latestSpec = spec;
            if (value == null) {
                return new CacheLookup.Miss<>();
            }
            return new CacheLookup.Hit<>(spec.valueType().cast(value));
        }

        @Override
        public <T> void put(CacheSpec<T> spec, CacheKey key, T value) {
            latestSpec = spec;
            this.value = value;
        }

        @Override
        public void evict(CacheNamespace namespace, CacheKey key) {
            latestEvictedNamespace = namespace;
            latestEvictedKey = key;
            value = null;
        }

        private Object latestValue() {
            return value;
        }

        private CacheSpec<?> latestSpec() {
            return latestSpec;
        }

        private void seed(Object value) {
            this.value = value;
        }

        private CacheNamespace latestEvictedNamespace() {
            return latestEvictedNamespace;
        }

        private CacheKey latestEvictedKey() {
            return latestEvictedKey;
        }
    }
}
