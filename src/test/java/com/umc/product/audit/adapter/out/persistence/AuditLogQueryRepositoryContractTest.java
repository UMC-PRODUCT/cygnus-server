package com.umc.product.audit.adapter.out.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

import com.umc.product.audit.domain.AuditAction;
import com.umc.product.audit.domain.AuditLog;
import com.umc.product.audit.domain.AuditOutcome;
import com.umc.product.audit.domain.AuditSource;
import com.umc.product.global.exception.constant.Domain;
import com.umc.product.support.PersistenceAdapterTest;

@PersistenceAdapterTest
@Import(AuditLogQueryRepository.class)
@DisplayName("관리자 감사 로그 조회 계약")
class AuditLogQueryRepositoryContractTest extends AuditLogQueryRepositoryContractSupport {

    @Test
    @DisplayName("domain 필터는 해당 도메인 로그만 반환한다")
    void domain_필터는_해당_도메인_로그만_반환한다() {
        // when
        Page<AuditLog> result = sut.search(
            Domain.MEMBER, null, null, null, null, null, null, null, null, null, null, PageRequest.of(0, 20));

        // then
        assertThat(result.getContent()).hasSize(3);
        assertThat(result.getContent()).allMatch(log -> log.getDomain() == Domain.MEMBER);
    }

    @Test
    @DisplayName("action 필터는 해당 액션 로그만 반환한다")
    void action_필터는_해당_액션_로그만_반환한다() {
        // when
        Page<AuditLog> result = sut.search(
            null, AuditAction.UPDATE, null, null, null, null, null, null, null, null, null, PageRequest.of(0, 20));

        // then
        assertThat(result.getContent()).hasSize(7);
        assertThat(result.getContent()).allMatch(log -> log.getAction() == AuditAction.UPDATE);
    }

    @Test
    @DisplayName("actorMemberId 필터는 해당 행위자의 로그만 반환한다")
    void actorMemberId_필터는_해당_행위자의_로그만_반환한다() {
        // when
        Page<AuditLog> result = sut.search(
            null, null, 10L, null, null, null, null, null, null, null, null, PageRequest.of(0, 20));

        // then
        assertThat(result.getContent()).hasSize(3);
        assertThat(result.getContent()).allMatch(log -> log.getActorMemberId().equals(10L));
    }

    @Test
    @DisplayName("from과 to 필터는 양 끝 시각을 포함해 반환한다")
    void from과_to_필터는_양_끝_시각을_포함해_반환한다() {
        // when
        Page<AuditLog> result = sut.search(
            null, null, null, JANUARY_2, JANUARY_3, null, null, null, null, null, null, PageRequest.of(0, 20));

        // then
        assertThat(result.getContent())
            .extracting(AuditLog::getTargetId)
            .containsExactly("schedule-update", "schedule-target-11", "member-update");
        assertThat(result.getContent())
            .extracting(AuditLog::getCreatedAt)
            .containsExactly(JANUARY_3, JANUARY_2.plusSeconds(60), JANUARY_2);
    }

    @Test
    @DisplayName("관리자 기존 필터를 함께 적용하면 모든 조건을 만족하는 로그만 반환한다")
    void 관리자_기존_필터를_함께_적용하면_모든_조건을_만족하는_로그만_반환한다() {
        // when
        Page<AuditLog> result = sut.search(
            Domain.MEMBER,
            AuditAction.UPDATE,
            10L,
            JANUARY_2,
            JANUARY_4,
            null,
            null,
            null,
            null,
            null,
            null,
            PageRequest.of(0, 20)
        );

        // then
        assertThat(result.getContent()).hasSize(1);
        AuditLog log = result.getContent().getFirst();
        assertThat(log.getDomain()).isEqualTo(Domain.MEMBER);
        assertThat(log.getAction()).isEqualTo(AuditAction.UPDATE);
        assertThat(log.getActorMemberId()).isEqualTo(10L);
        assertThat(log.getTargetId()).isEqualTo("member-update-late");
        assertThat(log.getCreatedAt()).isEqualTo(JANUARY_4);
    }

    @Test
    @DisplayName("targetType 필터는 정확히 일치하는 대상 타입 로그만 반환한다")
    void targetType_필터는_정확히_일치하는_대상_타입_로그만_반환한다() {
        // when
        Page<AuditLog> result = sut.search(
            null, null, null, null, null, "Schedule", null, null, null, null, null, PageRequest.of(0, 20));

        // then
        assertThat(result.getContent()).hasSize(4);
        assertThat(result.getContent()).allMatch(log -> log.getTargetType().equals("Schedule"));
    }

    @Test
    @DisplayName("targetId 필터는 정확히 일치하는 대상 ID 로그만 반환한다")
    void targetId_필터는_정확히_일치하는_대상_ID_로그만_반환한다() {
        // when
        Page<AuditLog> result = sut.search(
            null, null, null, null, null, null, "schedule-target-10", null, null, null, null, PageRequest.of(0, 20));

        // then
        assertThat(result.getContent())
            .extracting(AuditLog::getRequestId)
            .containsExactly("req-exact-10");
    }

    @Test
    @DisplayName("outcome 필터는 정확히 일치하는 결과 로그만 반환한다")
    void outcome_필터는_정확히_일치하는_결과_로그만_반환한다() {
        // when
        Page<AuditLog> result = sut.search(
            null, null, null, null, null, null, null, AuditOutcome.FAILURE, null, null, null, PageRequest.of(0, 20));

        // then
        assertThat(result.getContent()).hasSize(3);
        assertThat(result.getContent()).allMatch(log -> log.getOutcome() == AuditOutcome.FAILURE);
    }

    @Test
    @DisplayName("source 필터는 정확히 일치하는 출처 로그만 반환한다")
    void source_필터는_정확히_일치하는_출처_로그만_반환한다() {
        // when
        Page<AuditLog> result = sut.search(
            null, null, null, null, null, null, null, null, AuditSource.EXPLICIT_RECORDER, null, null, PageRequest.of(0, 20));

        // then
        assertThat(result.getContent())
            .extracting(AuditLog::getRequestId)
            .containsExactly("req-exact-11");
    }

    @Test
    @DisplayName("requestId 필터는 LIKE wildcard 없이 정확히 일치하는 요청 ID만 반환한다")
    void requestId_필터는_LIKE_wildcard_없이_정확히_일치하는_요청_ID만_반환한다() {
        // when
        Page<AuditLog> wildcardText = sut.search(
            null, null, null, null, null, null, null, null, null, "req_%_literal", null, PageRequest.of(0, 20));
        Page<AuditLog> wildcardPattern = sut.search(
            null, null, null, null, null, null, null, null, null, "req_%", null, PageRequest.of(0, 20));

        // then
        assertThat(wildcardText.getContent())
            .extracting(AuditLog::getTargetId)
            .containsExactly("schedule-target-12");
        assertThat(wildcardPattern.getContent()).isEmpty();
    }

    @Test
    @DisplayName("traceId 필터는 injection-like 문자열도 데이터로 정확히 일치시킨다")
    void traceId_필터는_injection_like_문자열도_데이터로_정확히_일치시킨다() {
        // when
        Page<AuditLog> exact = sut.search(
            null, null, null, null, null, null, null, null, null, null, "trace_' OR '1'='1", PageRequest.of(0, 20));
        Page<AuditLog> injectionFragment = sut.search(
            null, null, null, null, null, null, null, null, null, null, "' OR '1'='1", PageRequest.of(0, 20));

        // then
        assertThat(exact.getContent())
            .extracting(AuditLog::getTargetId)
            .containsExactly("schedule-target-12");
        assertThat(injectionFragment.getContent()).isEmpty();
    }

    @Test
    @DisplayName("targetType targetId outcome 조합은 모든 조건을 만족하는 로그만 반환한다")
    void targetType_targetId_outcome_조합은_모든_조건을_만족하는_로그만_반환한다() {
        // when
        Page<AuditLog> result = sut.search(
            null,
            null,
            null,
            null,
            null,
            "Schedule",
            "schedule-target-10",
            AuditOutcome.FAILURE,
            null,
            null,
            null,
            PageRequest.of(0, 20)
        );

        // then
        assertThat(result.getContent()).hasSize(1);
        AuditLog log = result.getContent().getFirst();
        assertThat(log.getTargetType()).isEqualTo("Schedule");
        assertThat(log.getTargetId()).isEqualTo("schedule-target-10");
        assertThat(log.getOutcome()).isEqualTo(AuditOutcome.FAILURE);
        assertThat(log.getRequestId()).isEqualTo("req-exact-10");
    }

    @Test
    @DisplayName("신규 필터를 생략하면 legacy null 필드를 가진 로그도 조회된다")
    void 신규_필터를_생략하면_legacy_null_필드를_가진_로그도_조회된다() {
        // when
        Page<AuditLog> result = sut.search(
            Domain.MEMBER, null, null, null, null, null, null, null, null, null, null, PageRequest.of(0, 20));

        // then
        assertThat(result.getContent())
            .extracting(AuditLog::getTargetId)
            .contains("member-create", "member-update", "member-update-late");
    }

}
