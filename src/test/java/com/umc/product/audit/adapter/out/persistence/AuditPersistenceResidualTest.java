package com.umc.product.audit.adapter.out.persistence;

import static com.umc.product.audit.domain.QAuditLog.auditLog;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.RETURNS_SELF;
import static org.mockito.Mockito.mock;

import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

import com.querydsl.jpa.impl.JPAQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;
import com.umc.product.audit.domain.AuditAction;
import com.umc.product.audit.domain.AuditLog;
import com.umc.product.audit.domain.AuditLogEvent;
import com.umc.product.global.exception.constant.Domain;

@DisplayName("감사 로그 persistence 잔여 계약")
class AuditPersistenceResidualTest {

    @Test
    @DisplayName("저장과 검색을 각 repository에 위임한다")
    void 저장과_검색을_위임한다() {
        AuditLogJpaRepository repository = mock(AuditLogJpaRepository.class);
        AuditLogQueryRepository queryRepository = mock(AuditLogQueryRepository.class);
        AuditLogPersistenceAdapter sut = new AuditLogPersistenceAdapter(repository, queryRepository);
        AuditLog log = log();
        PageRequest pageable = PageRequest.of(0, 20);
        Page<AuditLog> page = Page.empty(pageable);
        given(repository.save(log)).willReturn(log);
        given(queryRepository.search(Domain.MEMBER, AuditAction.UPDATE, 1L, null, null, pageable))
            .willReturn(page);

        assertThat(sut.save(log)).isSameAs(log);
        assertThat(sut.search(Domain.MEMBER, AuditAction.UPDATE, 1L, null, null, pageable)).isSameAs(page);
    }

    @Test
    @DisplayName("모든 검색 조건과 페이지 경계를 QueryDSL에 반영한다")
    @SuppressWarnings("unchecked")
    void 모든_검색_조건을_반영한다() {
        JPAQueryFactory queryFactory = mock(JPAQueryFactory.class);
        JPAQuery<AuditLog> contentQuery = mock(JPAQuery.class, RETURNS_SELF);
        JPAQuery<Long> countQuery = mock(JPAQuery.class, RETURNS_SELF);
        given(queryFactory.selectFrom(auditLog)).willReturn(contentQuery);
        given(queryFactory.select(auditLog.count())).willReturn(countQuery);
        given(contentQuery.fetch()).willReturn(List.of(log()));
        given(countQuery.fetchOne()).willReturn(11L);
        AuditLogQueryRepository sut = new AuditLogQueryRepository(queryFactory);
        Instant from = Instant.parse("2026-07-01T00:00:00Z");
        Instant to = Instant.parse("2026-07-31T23:59:59Z");

        Page<AuditLog> result = sut.search(
            Domain.MEMBER, AuditAction.UPDATE, 1L, from, to, PageRequest.of(1, 10));

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getTotalElements()).isEqualTo(11L);
        assertThat(result.getNumber()).isOne();
    }

    @Test
    @DisplayName("검색 조건과 count가 없으면 전체 조건과 0건 결과를 반환한다")
    @SuppressWarnings("unchecked")
    void 빈_검색_조건과_null_count를_처리한다() {
        JPAQueryFactory queryFactory = mock(JPAQueryFactory.class);
        JPAQuery<AuditLog> contentQuery = mock(JPAQuery.class, RETURNS_SELF);
        JPAQuery<Long> countQuery = mock(JPAQuery.class, RETURNS_SELF);
        given(queryFactory.selectFrom(auditLog)).willReturn(contentQuery);
        given(queryFactory.select(auditLog.count())).willReturn(countQuery);
        given(contentQuery.fetch()).willReturn(List.of());
        given(countQuery.fetchOne()).willReturn(null);
        AuditLogQueryRepository sut = new AuditLogQueryRepository(queryFactory);

        Page<AuditLog> result = sut.search(null, null, null, null, null, PageRequest.of(0, 20));

        assertThat(result).isEmpty();
        assertThat(result.getTotalElements()).isZero();
    }

    private AuditLog log() {
        AuditLogEvent event = AuditLogEvent.builder()
            .domain(Domain.MEMBER)
            .action(AuditAction.UPDATE)
            .targetType("Member")
            .targetId("10")
            .actorMemberId(1L)
            .build();
        return AuditLog.from(event, null, "127.0.0.1");
    }
}
