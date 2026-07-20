package com.umc.product.authentication.adapter.out.persistence;

import static com.umc.product.authentication.domain.QEmailVerification.emailVerification;
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
import com.umc.product.authentication.domain.EmailVerification;
import com.umc.product.authentication.domain.EmailVerificationPurpose;
import com.umc.product.authentication.domain.MemberOAuth;
import com.umc.product.common.domain.enums.OAuthProvider;

@DisplayName("Authentication persistence 잔여 위임 계약")
class AuthenticationPersistenceResidualTest {

    @Test
    @DisplayName("MemberOAuth adapter의 모든 조회·저장·삭제 연산을 repository에 위임한다")
    void member_oauth_adapter_위임() {
        MemberOAuthRepository repository = mock(MemberOAuthRepository.class);
        MemberOAuthPersistenceAdapter sut = new MemberOAuthPersistenceAdapter(repository);
        MemberOAuth oauth = oauth();
        List<MemberOAuth> oauths = List.of(oauth);
        given(repository.findById(1L)).willReturn(Optional.of(oauth));
        given(repository.findByMemberIdAndProvider(2L, OAuthProvider.GOOGLE)).willReturn(Optional.of(oauth));
        given(repository.findAllByProviderAndProviderIdIn(OAuthProvider.GOOGLE, List.of("provider")))
            .willReturn(oauths);
        given(repository.findAllByMemberIdInAndProvider(List.of(2L), OAuthProvider.GOOGLE)).willReturn(oauths);
        given(repository.save(oauth)).willReturn(oauth);
        given(repository.saveAll(oauths)).willReturn(oauths);

        assertThat(sut.findByMemberOAuthId(1L)).contains(oauth);
        assertThat(sut.findByMemberIdAndProvider(2L, OAuthProvider.GOOGLE)).contains(oauth);
        assertThat(sut.findAllByProviderAndProviderIdIn(OAuthProvider.GOOGLE, List.of("provider")))
            .isSameAs(oauths);
        assertThat(sut.findAllByMemberIdInAndProvider(List.of(2L), OAuthProvider.GOOGLE)).isSameAs(oauths);
        assertThat(sut.save(oauth)).isSameAs(oauth);
        assertThat(sut.saveAll(oauths)).isSameAs(oauths);
        sut.delete(oauth);

        then(repository).should().delete(oauth);
    }

    @Test
    @DisplayName("EmailVerification adapter는 최신 조회·저장·만료 삭제를 각 repository에 위임한다")
    void email_verification_adapter_위임() {
        EmailVerificationJpaRepository jpaRepository = mock(EmailVerificationJpaRepository.class);
        EmailVerificationQueryRepository queryRepository = mock(EmailVerificationQueryRepository.class);
        EmailVerificationPersistenceAdapter sut =
            new EmailVerificationPersistenceAdapter(jpaRepository, queryRepository);
        EmailVerification verification = verification();
        Instant threshold = Instant.parse("2026-01-01T00:00:00Z");
        given(queryRepository.findLatestSentByEmail("member@example.com"))
            .willReturn(Optional.of(verification));
        given(jpaRepository.save(verification)).willReturn(verification);
        given(jpaRepository.deleteByExpiresAtBefore(threshold)).willReturn(3);

        assertThat(sut.findLatestSentByEmail("member@example.com")).contains(verification);
        assertThat(sut.save(verification)).isSameAs(verification);
        assertThat(sut.deleteExpiredBefore(threshold)).isEqualTo(3);
    }

    @Test
    @DisplayName("EmailVerification QueryDSL은 ID 및 이메일 최신 발송 순서 조회를 구성한다")
    @SuppressWarnings("unchecked")
    void email_verification_querydsl_구성() {
        JPAQueryFactory queryFactory = mock(JPAQueryFactory.class);
        JPAQuery<EmailVerification> query = mock(JPAQuery.class, RETURNS_SELF);
        EmailVerification verification = verification();
        given(queryFactory.selectFrom(emailVerification)).willReturn(query);
        given(query.fetchOne()).willReturn(verification);
        EmailVerificationQueryRepository sut = new EmailVerificationQueryRepository(queryFactory);

        assertThat(sut.findById(1L)).contains(verification);
        assertThat(sut.findLatestSentByEmail("member@example.com")).contains(verification);
    }

    private MemberOAuth oauth() {
        return MemberOAuth.builder()
            .memberId(2L)
            .provider(OAuthProvider.GOOGLE)
            .providerId("provider")
            .build();
    }

    private EmailVerification verification() {
        return EmailVerification.builder()
            .email("member@example.com")
            .code("123456")
            .token("verification-token")
            .purpose(EmailVerificationPurpose.REGISTER)
            .build();
    }
}
