package com.umc.product.integration.term;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.umc.product.member.domain.Member;
import com.umc.product.support.IntegrationTestSupport;
import com.umc.product.support.fixture.MemberFixture;
import com.umc.product.support.fixture.TermFixture;
import com.umc.product.term.application.port.in.command.ManageTermAgreementUseCase;
import com.umc.product.term.application.port.in.command.SubmitRequiredTermReconsentUseCase;
import com.umc.product.term.application.port.in.command.dto.CreateTermConsentCommand;
import com.umc.product.term.application.port.in.command.dto.SubmitRequiredTermReconsentCommand;
import com.umc.product.term.application.port.in.query.GetTermAgreementUseCase;
import com.umc.product.term.application.port.in.query.dto.TermInfo;
import com.umc.product.term.application.port.out.LoadTermConsentPort;
import com.umc.product.term.domain.Term;
import com.umc.product.term.domain.enums.TermType;
import com.umc.product.term.domain.exception.TermDomainException;
import com.umc.product.term.domain.exception.TermErrorCode;

/**
 * {@link IntegrationTestSupport} + Fixture 결합 사용 예시.
 *
 * <p>Member 도메인과 Term 도메인을 가로질러 호출하는 흐름을 검증한다.</p>
 * <ul>
 *   <li>Fixture 로 사전 데이터 적재 → UseCase 호출 → Port 로 영속 결과 검증의 표준 흐름</li>
 *   <li>성공 시나리오 + 도메인 예외 시나리오 양쪽 모두 포함</li>
 * </ul>
 */
class TermAgreementIntegrationTest extends IntegrationTestSupport {

    @Autowired
    private ManageTermAgreementUseCase manageTermAgreementUseCase;

    @Autowired
    private SubmitRequiredTermReconsentUseCase submitRequiredTermReconsentUseCase;

    @Autowired
    private GetTermAgreementUseCase getTermAgreementUseCase;

    @Autowired
    private LoadTermConsentPort loadTermConsentPort;

    @Autowired
    private MemberFixture memberFixture;

    @Autowired
    private TermFixture termFixture;

    @Test
    void saveAndReadTermConsent() {
        // given
        Member member = memberFixture.일반("길동");
        Term term = termFixture.필수_약관(TermType.SERVICE);

        CreateTermConsentCommand command = CreateTermConsentCommand.builder()
            .memberId(member.getId())
            .termId(term.getId())
            .isAgreed(true)
            .build();

        // when
        manageTermAgreementUseCase.createTermConsent(command);

        // then: Port 로 직접 조회해 영속 상태 검증
        assertThat(loadTermConsentPort.existsByMemberIdAndTermId(member.getId(), term.getId()))
            .isTrue();

        // and: Query UseCase 도 동일하게 동의 약관을 반환
        List<TermInfo> agreed = getTermAgreementUseCase.getAgreedTermsByMemberId(member.getId());
        assertThat(agreed)
            .extracting(TermInfo::type)
            .containsExactly(TermType.SERVICE);
    }

    @Test
    void rejectDuplicatedGeneralTermConsent() {
        // given
        Member member = memberFixture.일반("이몽룡");
        Term term = termFixture.필수_약관(TermType.PRIVACY);
        termFixture.약관_동의(member.getId(), term);

        CreateTermConsentCommand command = CreateTermConsentCommand.builder()
            .memberId(member.getId())
            .termId(term.getId())
            .isAgreed(true)
            .build();

        // when & then
        assertThatThrownBy(() -> manageTermAgreementUseCase.createTermConsent(command))
            .isInstanceOf(TermDomainException.class)
            .extracting("baseCode")
            .isEqualTo(TermErrorCode.TERMS_CONSENT_ALREADY_EXISTS);
    }

    @Test
    void allowNewTermOfSameType() {
        // given
        Member member = memberFixture.일반("변사또");
        Term oldTerm = termFixture.필수_약관(TermType.SERVICE);
        termFixture.약관_동의(member.getId(), oldTerm);

        Term newTerm = termFixture.필수_약관(TermType.SERVICE);
        CreateTermConsentCommand command = CreateTermConsentCommand.builder()
            .memberId(member.getId())
            .termId(newTerm.getId())
            .isAgreed(true)
            .build();

        // when
        manageTermAgreementUseCase.createTermConsent(command);

        // then
        List<TermInfo> agreed = getTermAgreementUseCase.getAgreedTermsByMemberId(member.getId());
        assertThat(agreed)
            .extracting(TermInfo::id)
            .containsExactlyInAnyOrder(oldTerm.getId(), newTerm.getId());
    }

    @Test
    void rejectNonexistentTerm() {
        // given
        Member member = memberFixture.일반("성춘향");
        Long nonExistentTermId = 9_999L;

        CreateTermConsentCommand command = CreateTermConsentCommand.builder()
            .memberId(member.getId())
            .termId(nonExistentTermId)
            .isAgreed(true)
            .build();

        // when & then
        assertThatThrownBy(() -> manageTermAgreementUseCase.createTermConsent(command))
            .isInstanceOf(TermDomainException.class)
            .extracting("baseCode")
            .isEqualTo(TermErrorCode.TERMS_NOT_FOUND);
    }

    @Test
    void saveSingleConsentAndLogUnderConcurrentReconsent() throws Exception {
        Member member = memberFixture.일반("홍길동");
        Term term = termFixture.필수_약관(TermType.SERVICE);
        SubmitRequiredTermReconsentCommand command =
            SubmitRequiredTermReconsentCommand.of(member.getId(), term.getId());
        int concurrency = 8;
        CountDownLatch ready = new CountDownLatch(concurrency);
        CountDownLatch start = new CountDownLatch(1);
        ExecutorService executor = Executors.newFixedThreadPool(concurrency);

        try {
            List<Future<Void>> futures = IntStream.range(0, concurrency)
                .mapToObj(ignored -> executor.submit((Callable<Void>) () -> {
                    ready.countDown();
                    if (!start.await(5, TimeUnit.SECONDS)) {
                        throw new IllegalStateException("재동의 동시 실행 시작 신호를 받지 못했습니다.");
                    }
                    submitRequiredTermReconsentUseCase.submitRequiredTermReconsent(command);
                    return null;
                }))
                .toList();
            assertThat(ready.await(5, TimeUnit.SECONDS)).isTrue();
            start.countDown();
            for (Future<Void> future : futures) {
                future.get(10, TimeUnit.SECONDS);
            }
        } finally {
            executor.shutdownNow();
        }

        assertThat(loadTermConsentPort.listByMemberIdAndTermIds(member.getId(), List.of(term.getId())))
            .hasSize(1);
        Long logCount = entityManager.createQuery("""
                SELECT COUNT(log)
                FROM TermConsentLog log
                WHERE log.memberId = :memberId AND log.termId = :termId
                """, Long.class)
            .setParameter("memberId", member.getId())
            .setParameter("termId", term.getId())
            .getSingleResult();
        assertThat(logCount).isEqualTo(1L);
    }
}
