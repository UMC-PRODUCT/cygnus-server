package com.umc.product.authorization.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.umc.product.authorization.application.port.out.LoadChallengerRolePort;
import com.umc.product.authorization.domain.ChallengerRole;
import com.umc.product.authorization.domain.SubjectAttributes;
import com.umc.product.challenger.application.port.in.query.GetChallengerUseCase;
import com.umc.product.challenger.application.port.in.query.dto.ChallengerInfo;
import com.umc.product.common.domain.enums.ChallengerPart;
import com.umc.product.common.domain.enums.ChallengerRoleType;
import com.umc.product.global.cache.application.port.in.CacheUseCase;
import com.umc.product.global.cache.domain.CacheKey;
import com.umc.product.global.cache.domain.CacheLookup;
import com.umc.product.global.cache.domain.CacheNamespace;
import com.umc.product.global.cache.domain.CacheSpec;
import com.umc.product.global.logging.OperationalMetrics;
import com.umc.product.member.application.port.in.query.GetMemberUseCase;
import com.umc.product.member.application.port.in.query.dto.MemberInfo;
import com.umc.product.organization.application.port.in.query.GetChapterUseCase;
import com.umc.product.organization.application.port.in.query.dto.chapter.ChapterInfo;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

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
    GetChapterUseCase getChapterUseCase;

    @Mock
    GetChallengerUseCase getChallengerUseCase;

    @Mock
    OperationalMetrics operationalMetrics;

    @Test
    @DisplayName("loadSubject는 miss 시 DTO JSON 문자열을 캐시하고 hit 시 외부 UseCase를 다시 호출하지 않는다")
    void load_subject_cache_hit() {
        InMemoryCacheUseCase cacheUseCase = new InMemoryCacheUseCase();
        AuthorizationService sut = new AuthorizationService(
            loadChallengerRolePort,
            List.of(),
            getMemberUseCase,
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

        SubjectAttributes first = sut.loadSubject(MEMBER_ID);
        SubjectAttributes second = sut.loadSubject(MEMBER_ID);

        assertThat(first.roleAttributes()).hasSize(1);
        assertThat(second.roleAttributes()).hasSize(1);
        assertThat(cacheUseCase.latestSpec().namespace()).isEqualTo(CacheNamespace.AUTHORITY_SNAPSHOT);
        assertThat(cacheUseCase.latestSpec().valueType()).isEqualTo(String.class);
        assertThat(cacheUseCase.latestValue()).isInstanceOf(String.class);
        assertThat((String) cacheUseCase.latestValue()).contains("SCHOOL_PRESIDENT");
        verify(getMemberUseCase, times(1)).getById(MEMBER_ID);
        verify(getChallengerUseCase, times(1)).getAllByMemberId(MEMBER_ID);
        verify(getChapterUseCase, times(1)).byGisuAndSchool(GISU_ID, SCHOOL_ID);
        verify(loadChallengerRolePort, times(1)).findByMemberId(MEMBER_ID);
    }

    private static class InMemoryCacheUseCase implements CacheUseCase {

        private Object value;
        private CacheSpec<?> latestSpec;

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
            value = null;
        }

        private Object latestValue() {
            return value;
        }

        private CacheSpec<?> latestSpec() {
            return latestSpec;
        }
    }
}
