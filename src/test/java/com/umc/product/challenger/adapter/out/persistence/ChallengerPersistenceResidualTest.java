package com.umc.product.challenger.adapter.out.persistence;

import static com.umc.product.challenger.domain.QChallengerPoint.challengerPoint;
import static com.umc.product.support.fixture.ChallengerUnitFixture.챌린저;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.RETURNS_SELF;
import static org.mockito.Mockito.mock;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.querydsl.jpa.impl.JPAQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;
import com.umc.product.challenger.domain.Challenger;
import com.umc.product.challenger.domain.ChallengerPoint;
import com.umc.product.challenger.domain.ChallengerRecord;
import com.umc.product.challenger.domain.enums.PointType;
import com.umc.product.challenger.domain.exception.ChallengerDomainException;
import com.umc.product.common.domain.enums.ChallengerPart;

@DisplayName("Challenger persistence 잔여 위임 계약")
class ChallengerPersistenceResidualTest {

    @Test
    @DisplayName("record adapter의 조회·저장·삭제와 미존재 예외를 검증한다")
    void record_adapter를_위임한다() {
        ChallengerRecordJpaRepository repository = mock(ChallengerRecordJpaRepository.class);
        ChallengerRecordPersistenceAdapter sut = new ChallengerRecordPersistenceAdapter(repository);
        ChallengerRecord record = record();
        List<ChallengerRecord> records = List.of(record);
        given(repository.findById(1L)).willReturn(Optional.of(record));
        given(repository.findById(2L)).willReturn(Optional.empty());
        given(repository.findByCode(record.getCode())).willReturn(Optional.of(record));
        given(repository.existsByCode(record.getCode())).willReturn(true);
        given(repository.findBySchoolId(3L)).willReturn(records);
        given(repository.findByChapterId(2L)).willReturn(records);
        given(repository.save(record)).willReturn(record);
        given(repository.saveAll(records)).willReturn(records);

        assertThat(sut.findById(1L)).contains(record);
        assertThat(sut.getById(1L)).isSameAs(record);
        assertThatThrownBy(() -> sut.getById(2L)).isInstanceOf(ChallengerDomainException.class);
        assertThat(sut.findByCode(record.getCode())).contains(record);
        assertThat(sut.existsByCode(record.getCode())).isTrue();
        assertThat(sut.findBySchoolId(3L)).isSameAs(records);
        assertThat(sut.findByChapterId(2L)).isSameAs(records);
        assertThat(sut.save(record)).isSameAs(record);
        assertThat(sut.saveAll(records)).isSameAs(records);
        sut.delete(record);
        then(repository).should().delete(record);
    }

    @Test
    @DisplayName("point adapter의 조회·저장·삭제와 미존재 예외를 검증한다")
    void point_adapter를_위임한다() {
        ChallengerPointJpaRepository repository = mock(ChallengerPointJpaRepository.class);
        ChallengerPointQueryRepository queryRepository = mock(ChallengerPointQueryRepository.class);
        ChallengerPointPersistenceAdapter sut = new ChallengerPointPersistenceAdapter(repository, queryRepository);
        ChallengerPoint point = point();
        List<ChallengerPoint> points = List.of(point);
        given(queryRepository.findAllByChallengerIdIn(Set.of(1L))).willReturn(points);
        given(repository.findById(100L)).willReturn(Optional.of(point));
        given(repository.findById(200L)).willReturn(Optional.empty());
        given(repository.save(point)).willReturn(point);

        assertThat(sut.findByChallengerIdIn(Set.of(1L))).isSameAs(points);
        assertThat(sut.findById(100L)).contains(point);
        assertThat(sut.getById(100L)).isSameAs(point);
        assertThatThrownBy(() -> sut.getById(200L)).isInstanceOf(ChallengerDomainException.class);
        assertThat(sut.save(point)).isSameAs(point);
        sut.delete(point);
        then(repository).should().delete(point);
    }

    @Test
    @DisplayName("point QueryDSL은 challenger ID 집합 조회 조건을 구성한다")
    @SuppressWarnings("unchecked")
    void point_querydsl을_구성한다() {
        JPAQueryFactory queryFactory = mock(JPAQueryFactory.class);
        JPAQuery<ChallengerPoint> query = mock(JPAQuery.class, RETURNS_SELF);
        List<ChallengerPoint> points = List.of(point());
        given(queryFactory.select(challengerPoint)).willReturn(query);
        given(query.fetch()).willReturn(points);
        ChallengerPointQueryRepository sut = new ChallengerPointQueryRepository(queryFactory);

        assertThat(sut.findAllByChallengerIdIn(Set.of(1L))).isSameAs(points);
    }

    private ChallengerRecord record() {
        return ChallengerRecord.create(99L, 9L, 2L, 3L, ChallengerPart.SPRINGBOOT, "홍길동");
    }

    private ChallengerPoint point() {
        Challenger challenger = 챌린저(1L, 10L, 9L);
        return ChallengerPoint.create(challenger, PointType.WARNING, "warning");
    }
}
