package com.umc.product.term.adapter.out.persistence;

import static com.umc.product.term.domain.QTerm.term;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.RETURNS_SELF;
import static org.mockito.Mockito.mock;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.querydsl.jpa.impl.JPAQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;
import com.umc.product.term.domain.Term;
import com.umc.product.term.domain.TermConsent;
import com.umc.product.term.domain.TermConsentLog;
import com.umc.product.term.domain.enums.TermType;

@DisplayName("약관 persistence 잔여 계약")
class TermPersistenceResidualTest {

    @Test
    @DisplayName("약관 adapter의 모든 조회·존재·저장·삭제 계약을 repository에 위임한다")
    void 약관_adapter를_위임한다() {
        TermRepository repository = mock(TermRepository.class);
        TermQueryRepository queryRepository = mock(TermQueryRepository.class);
        TermPersistenceAdapter sut = new TermPersistenceAdapter(repository, queryRepository);
        Term value = term(TermType.SERVICE);
        List<Term> values = List.of(value);
        given(repository.findById(1L)).willReturn(Optional.of(value));
        given(queryRepository.findActiveByType(TermType.SERVICE)).willReturn(Optional.of(value));
        given(repository.findAllByActiveIsTrueOrderByIdAsc()).willReturn(values);
        given(repository.existsById(1L)).willReturn(true);
        given(repository.findAllByTypeInAndActiveIsTrue(List.of(TermType.SERVICE))).willReturn(values);
        given(repository.findAllById(List.of(1L))).willReturn(values);
        given(queryRepository.findAllActiveRequired()).willReturn(values);
        given(repository.save(value)).willReturn(value);

        assertThat(sut.findById(1L)).contains(value);
        assertThat(sut.findActiveByType(TermType.SERVICE)).contains(value);
        assertThat(sut.listActive()).isSameAs(values);
        assertThat(sut.existsById(1L)).isTrue();
        assertThat(sut.findAllActiveByTypes(List.of(TermType.SERVICE))).isSameAs(values);
        assertThat(sut.listByIds(List.of(1L))).isSameAs(values);
        assertThat(sut.findAllActiveRequired()).isSameAs(values);
        assertThat(sut.save(value)).isSameAs(value);
        sut.delete(value);
        then(repository).should().delete(value);
    }

    @Test
    @DisplayName("약관 동의 adapter는 단건·batch·존재 조회와 저장·삭제를 그대로 위임한다")
    void 약관_동의_adapter를_위임한다() {
        TermConsentRepository repository = mock(TermConsentRepository.class);
        TermConsentPersistenceAdapter sut = new TermConsentPersistenceAdapter(repository);
        TermConsent consent = consent();
        List<TermConsent> values = List.of(consent);
        given(repository.findByMemberId(10L)).willReturn(values);
        given(repository.findByMemberIdAndTermIdIn(10L, List.of(1L))).willReturn(values);
        given(repository.findByMemberIdAndTermType(10L, TermType.SERVICE)).willReturn(Optional.of(consent));
        given(repository.findByMemberIdAndTermId(10L, 1L)).willReturn(Optional.of(consent));
        given(repository.existsByMemberIdAndTermType(10L, TermType.SERVICE)).willReturn(true);
        given(repository.existsByMemberIdAndTermId(10L, 1L)).willReturn(true);
        given(repository.save(consent)).willReturn(consent);

        assertThat(sut.findByMemberId(10L)).isSameAs(values);
        assertThat(sut.listByMemberIdAndTermIds(10L, List.of(1L))).isSameAs(values);
        assertThat(sut.findByMemberIdAndTermType(10L, TermType.SERVICE)).contains(consent);
        assertThat(sut.findByMemberIdAndTermId(10L, 1L)).contains(consent);
        assertThat(sut.existsByMemberIdAndTermType(10L, TermType.SERVICE)).isTrue();
        assertThat(sut.existsByMemberIdAndTermId(10L, 1L)).isTrue();
        assertThat(sut.save(consent)).isSameAs(consent);
        sut.delete(consent);
        then(repository).should().delete(consent);
    }

    @Test
    @DisplayName("동의 이력 adapter는 append할 로그 저장을 위임한다")
    void 동의_이력_저장을_위임한다() {
        TermConsentLogRepository repository = mock(TermConsentLogRepository.class);
        TermConsentLogPersistenceAdapter sut = new TermConsentLogPersistenceAdapter(repository);
        TermConsentLog log = mock(TermConsentLog.class);
        given(repository.save(log)).willReturn(log);

        assertThat(sut.save(log)).isSameAs(log);
    }

    @Test
    @DisplayName("QueryDSL은 활성 타입과 활성 필수 조건을 적용하고 미존재를 Optional.empty로 반환한다")
    @SuppressWarnings("unchecked")
    void 약관_QueryDSL_조건과_미존재를_검증한다() {
        JPAQueryFactory queryFactory = mock(JPAQueryFactory.class);
        JPAQuery<Term> typeQuery = mock(JPAQuery.class, RETURNS_SELF);
        JPAQuery<Term> requiredQuery = mock(JPAQuery.class, RETURNS_SELF);
        Term value = term(TermType.SERVICE);
        given(queryFactory.selectFrom(term)).willReturn(typeQuery);
        given(queryFactory.selectDistinct(term)).willReturn(requiredQuery);
        given(typeQuery.fetchFirst()).willReturn(value).willReturn(null);
        given(requiredQuery.fetch()).willReturn(List.of(value));
        TermQueryRepository sut = new TermQueryRepository(queryFactory);

        assertThat(sut.findActiveByType(TermType.SERVICE)).contains(value);
        assertThat(sut.findActiveByType(TermType.PRIVACY)).isEmpty();
        assertThat(sut.findAllActiveRequired()).containsExactly(value);
    }

    private Term term(TermType type) {
        return Term.builder().type(type).link("https://example.com/terms").required(true).build();
    }

    private TermConsent consent() {
        return TermConsent.builder()
            .memberId(10L)
            .termId(1L)
            .termType(TermType.SERVICE)
            .agreedAt(Instant.parse("2026-07-22T00:00:00Z"))
            .build();
    }
}
