package com.umc.product.authentication.application.service;

import static com.umc.product.support.fixture.AuthenticationFixture.Apple_OAuth_연결_명령;
import static com.umc.product.support.fixture.AuthenticationFixture.OAuth_계정;
import static com.umc.product.support.fixture.AuthenticationFixture.OAuth_속성;
import static com.umc.product.support.fixture.AuthenticationFixture.OAuth_연결_명령;
import static com.umc.product.support.fixture.AuthenticationFixture.OAuth_해제_명령;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.umc.product.authentication.application.port.in.command.dto.AccessTokenLoginCommand;
import com.umc.product.authentication.application.port.in.command.dto.AuthorizationCodeLoginCommand;
import com.umc.product.authentication.application.port.in.command.dto.LinkOAuthCommand;
import com.umc.product.authentication.application.port.in.command.dto.OAuthTokenLoginResult;
import com.umc.product.authentication.application.port.out.LoadMemberOAuthPort;
import com.umc.product.authentication.application.port.out.RevokeOAuthTokenPort;
import com.umc.product.authentication.application.port.out.SaveMemberOAuthPort;
import com.umc.product.authentication.application.port.out.VerifyOAuthTokenPort;
import com.umc.product.authentication.domain.MemberOAuth;
import com.umc.product.authentication.domain.OAuthAttributes;
import com.umc.product.authentication.domain.exception.AuthenticationDomainException;
import com.umc.product.authentication.domain.exception.AuthenticationErrorCode;
import com.umc.product.common.domain.enums.OAuthProvider;
import com.umc.product.global.logging.OperationalMetrics;
import com.umc.product.member.application.port.in.command.LockMemberCredentialUseCase;

@ExtendWith(MockitoExtension.class)
@DisplayName("OAuthAuthenticationService 엣지 케이스")
class OAuthAuthenticationServiceEdgeCaseTest {

    private static final Long MEMBER_ID = 10L;
    private static final Long OAUTH_ID = 100L;

    @Mock
    VerifyOAuthTokenPort verifyOAuthTokenPort;

    @Mock
    LoadMemberOAuthPort loadMemberOAuthPort;

    @Mock
    SaveMemberOAuthPort saveMemberOAuthPort;

    @Mock
    RevokeOAuthTokenPort revokeOAuthTokenPort;

    @Mock
    LockMemberCredentialUseCase lockMemberCredentialUseCase;

    @Mock
    OperationalMetrics operationalMetrics;

    OAuthAuthenticationService sut;

    @BeforeEach
    void setUp() {
        sut = new OAuthAuthenticationService(
            verifyOAuthTokenPort,
            loadMemberOAuthPort,
            saveMemberOAuthPort,
            revokeOAuthTokenPort,
            lockMemberCredentialUseCase,
            operationalMetrics
        );
    }

    @Nested
    @DisplayName("OAuth 로그인")
    class Login {

        @Test
        @DisplayName("연결된 provider 계정이면 기존 회원 결과와 성공 지표를 반환한다")
        void 기존_회원으로_로그인한다() {
            OAuthAttributes attributes = OAuth_속성(OAuthProvider.GOOGLE, "google-user", "user@example.com");
            given(loadMemberOAuthPort.findByProviderAndProviderId(OAuthProvider.GOOGLE, "google-user"))
                .willReturn(Optional.of(OAuth_계정(OAUTH_ID, MEMBER_ID, OAuthProvider.GOOGLE, "google-user")));

            OAuthTokenLoginResult result = sut.loginWithOAuthAttributes(attributes);

            assertThat(result.isExistingMember()).isTrue();
            assertThat(result.memberId()).isEqualTo(MEMBER_ID);
            then(operationalMetrics).should().recordSecurityEvent("AUTHENTICATION", "OAUTH_LOGIN", "success");
        }

        @Test
        @DisplayName("연결 계정이 없고 email도 없으면 신규 회원 결과를 안전하게 반환한다")
        void email_없는_신규_회원_로그인을_처리한다() {
            OAuthAttributes attributes = new OAuthAttributes(OAuthProvider.APPLE, "apple-user", null);
            given(loadMemberOAuthPort.findByProviderAndProviderId(OAuthProvider.APPLE, "apple-user"))
                .willReturn(Optional.empty());

            OAuthTokenLoginResult result = sut.loginWithOAuthAttributes(attributes);

            assertThat(result.isExistingMember()).isFalse();
            assertThat(result.memberId()).isNull();
            assertThat(result.email()).isNull();
            then(operationalMetrics).should()
                .recordSecurityEvent("AUTHENTICATION", "OAUTH_LOGIN", "register_required");
        }

        @Test
        @DisplayName("access token 로그인은 provider 검증 결과를 공통 로그인 흐름에 전달한다")
        void access_token_검증_결과로_로그인한다() {
            AccessTokenLoginCommand command = AccessTokenLoginCommand.of(OAuthProvider.KAKAO, "id-token");
            OAuthAttributes attributes = OAuth_속성(OAuthProvider.KAKAO, "kakao-user", "kakao@example.com");
            given(verifyOAuthTokenPort.verify(OAuthProvider.KAKAO, "id-token")).willReturn(attributes);
            given(loadMemberOAuthPort.findByProviderAndProviderId(OAuthProvider.KAKAO, "kakao-user"))
                .willReturn(Optional.empty());

            OAuthTokenLoginResult result = sut.accessTokenLogin(command);

            assertThat(result.providerId()).isEqualTo("kakao-user");
            assertThat(result.isExistingMember()).isFalse();
        }

        @Test
        @DisplayName("authorization code 로그인은 code와 redirect URI를 그대로 검증 port에 전달한다")
        void authorization_code를_검증해_로그인한다() {
            AuthorizationCodeLoginCommand command = new AuthorizationCodeLoginCommand(
                OAuthProvider.KAKAO,
                "authorization-code",
                "https://app.example/callback"
            );
            OAuthAttributes attributes = OAuth_속성(OAuthProvider.KAKAO, "kakao-user", "kakao@example.com");
            given(verifyOAuthTokenPort.verifyAuthorizationCode(
                OAuthProvider.KAKAO,
                "authorization-code",
                "https://app.example/callback"
            )).willReturn(attributes);
            given(loadMemberOAuthPort.findByProviderAndProviderId(OAuthProvider.KAKAO, "kakao-user"))
                .willReturn(Optional.empty());

            OAuthTokenLoginResult result = sut.authorizationCodeLogin(command);

            assertThat(result.provider()).isEqualTo(OAuthProvider.KAKAO);
            then(verifyOAuthTokenPort).should().verifyAuthorizationCode(
                OAuthProvider.KAKAO,
                "authorization-code",
                "https://app.example/callback"
            );
        }
    }

    @Nested
    @DisplayName("OAuth 연결")
    class Link {

        @Test
        @DisplayName("다른 회원이 이미 연결한 provider 계정은 저장하지 않는다")
        void 이미_연결된_provider_계정을_거부한다() {
            LinkOAuthCommand command = OAuth_연결_명령(MEMBER_ID, OAuthProvider.GOOGLE, "google-user");
            given(loadMemberOAuthPort.findByProviderAndProviderId(OAuthProvider.GOOGLE, "google-user"))
                .willReturn(Optional.of(OAuth_계정(1L, 99L, OAuthProvider.GOOGLE, "google-user")));

            assertError(() -> sut.linkOAuth(command), AuthenticationErrorCode.OAUTH_ALREADY_LINKED);
            then(saveMemberOAuthPort).shouldHaveNoInteractions();
        }

        @Test
        @DisplayName("회원에게 같은 provider가 이미 있으면 다른 provider ID도 중복 연결하지 않는다")
        void 같은_provider_중복_연결을_거부한다() {
            LinkOAuthCommand command = OAuth_연결_명령(MEMBER_ID, OAuthProvider.GOOGLE, "new-google-user");
            given(loadMemberOAuthPort.findByProviderAndProviderId(OAuthProvider.GOOGLE, "new-google-user"))
                .willReturn(Optional.empty());
            given(loadMemberOAuthPort.findByMemberIdAndProvider(MEMBER_ID, OAuthProvider.GOOGLE))
                .willReturn(Optional.of(OAuth_계정(1L, MEMBER_ID, OAuthProvider.GOOGLE, "old-google-user")));

            assertError(() -> sut.linkOAuth(command), AuthenticationErrorCode.OAUTH_PROVIDER_ALREADY_LINKED);
            then(saveMemberOAuthPort).shouldHaveNoInteractions();
        }

        @Test
        @DisplayName("Apple 연결 정보의 refresh token과 client ID를 누락 없이 저장한다")
        void apple_연결_정보를_저장한다() {
            LinkOAuthCommand command =
                Apple_OAuth_연결_명령(MEMBER_ID, "apple-user", "refresh-token", "ios-client");
            given(loadMemberOAuthPort.findByProviderAndProviderId(OAuthProvider.APPLE, "apple-user"))
                .willReturn(Optional.empty());
            given(loadMemberOAuthPort.findByMemberIdAndProvider(MEMBER_ID, OAuthProvider.APPLE))
                .willReturn(Optional.empty());
            given(saveMemberOAuthPort.save(any(MemberOAuth.class)))
                .willReturn(OAuth_계정(
                    OAUTH_ID,
                    MEMBER_ID,
                    OAuthProvider.APPLE,
                    "apple-user",
                    "refresh-token",
                    "ios-client"
                ));

            Long result = sut.linkOAuth(command);

            assertThat(result).isEqualTo(OAUTH_ID);
            ArgumentCaptor<MemberOAuth> captor = ArgumentCaptor.forClass(MemberOAuth.class);
            then(saveMemberOAuthPort).should().save(captor.capture());
            assertThat(captor.getValue().getAppleRefreshToken()).isEqualTo("refresh-token");
            assertThat(captor.getValue().getAppleClientId()).isEqualTo("ios-client");
        }

        @Test
        @DisplayName("bulk 연결에서 하나라도 이미 연결된 provider ID가 있으면 전체 저장을 중단한다")
        void bulk_provider_id_중복을_거부한다() {
            List<LinkOAuthCommand> commands = List.of(
                OAuth_연결_명령(1L, OAuthProvider.GOOGLE, "google-1"),
                OAuth_연결_명령(2L, OAuthProvider.GOOGLE, "google-2")
            );
            given(loadMemberOAuthPort.findAllByProviderAndProviderIdIn(
                OAuthProvider.GOOGLE,
                List.of("google-1", "google-2")
            )).willReturn(List.of(OAuth_계정(5L, 9L, OAuthProvider.GOOGLE, "google-2")));

            assertError(() -> sut.linkOAuthBulk(commands), AuthenticationErrorCode.OAUTH_ALREADY_LINKED);
            then(saveMemberOAuthPort).shouldHaveNoInteractions();
        }

        @Test
        @DisplayName("bulk 연결에서 회원의 같은 provider 연결이 있으면 전체 저장을 중단한다")
        void bulk_회원_provider_중복을_거부한다() {
            List<LinkOAuthCommand> commands = List.of(
                OAuth_연결_명령(1L, OAuthProvider.KAKAO, "kakao-1"),
                OAuth_연결_명령(2L, OAuthProvider.KAKAO, "kakao-2")
            );
            given(loadMemberOAuthPort.findAllByProviderAndProviderIdIn(
                OAuthProvider.KAKAO,
                List.of("kakao-1", "kakao-2")
            )).willReturn(List.of());
            given(loadMemberOAuthPort.findAllByMemberIdInAndProvider(
                List.of(1L, 2L),
                OAuthProvider.KAKAO
            )).willReturn(List.of(OAuth_계정(5L, 2L, OAuthProvider.KAKAO, "old-kakao")));

            assertError(() -> sut.linkOAuthBulk(commands), AuthenticationErrorCode.OAUTH_PROVIDER_ALREADY_LINKED);
            then(saveMemberOAuthPort).shouldHaveNoInteractions();
        }

        @Test
        @DisplayName("bulk 연결은 provider별 검증 후 입력 순서에 대응하는 저장 ID를 반환한다")
        void bulk_연결을_저장한다() {
            List<LinkOAuthCommand> commands = List.of(
                OAuth_연결_명령(1L, OAuthProvider.GOOGLE, "google-1"),
                OAuth_연결_명령(2L, OAuthProvider.KAKAO, "kakao-2")
            );
            given(loadMemberOAuthPort.findAllByProviderAndProviderIdIn(any(), anyList())).willReturn(List.of());
            given(loadMemberOAuthPort.findAllByMemberIdInAndProvider(anyList(), any())).willReturn(List.of());
            given(saveMemberOAuthPort.saveAll(anyList())).willReturn(List.of(
                OAuth_계정(101L, 1L, OAuthProvider.GOOGLE, "google-1"),
                OAuth_계정(102L, 2L, OAuthProvider.KAKAO, "kakao-2")
            ));

            List<Long> result = sut.linkOAuthBulk(commands);

            assertThat(result).containsExactly(101L, 102L);
            then(loadMemberOAuthPort).should()
                .findAllByProviderAndProviderIdIn(OAuthProvider.GOOGLE, List.of("google-1"));
            then(loadMemberOAuthPort).should()
                .findAllByProviderAndProviderIdIn(OAuthProvider.KAKAO, List.of("kakao-2"));
        }

        @Test
        @DisplayName("빈 bulk 요청은 검증 조회 없이 빈 목록을 저장하고 빈 ID 목록을 반환한다")
        void 빈_bulk_요청을_처리한다() {
            given(saveMemberOAuthPort.saveAll(List.of())).willReturn(List.of());

            assertThat(sut.linkOAuthBulk(List.of())).isEmpty();
            then(loadMemberOAuthPort).shouldHaveNoInteractions();
        }
    }

    @Nested
    @DisplayName("OAuth 연결 해제")
    class Unlink {

        @Test
        @DisplayName("존재하지 않는 OAuth ID는 변경 없이 not found로 거부한다")
        void 존재하지_않는_oauth를_거부한다() {
            given(loadMemberOAuthPort.findByMemberOAuthId(OAUTH_ID)).willReturn(Optional.empty());

            assertError(
                () -> sut.unlinkOAuth(OAuth_해제_명령(MEMBER_ID, OAUTH_ID, true, null, null)),
                AuthenticationErrorCode.MEMBER_OAUTH_NOT_FOUND
            );
            then(saveMemberOAuthPort).shouldHaveNoInteractions();
        }

        @Test
        @DisplayName("다른 회원의 OAuth 계정은 token revoke 전에 거부한다")
        void 다른_회원의_oauth를_거부한다() {
            given(loadMemberOAuthPort.findByMemberOAuthId(OAUTH_ID))
                .willReturn(Optional.of(OAuth_계정(OAUTH_ID, 999L, OAuthProvider.GOOGLE, "google-user")));

            assertError(
                () -> sut.unlinkOAuth(OAuth_해제_명령(MEMBER_ID, OAUTH_ID, true, "token", null)),
                AuthenticationErrorCode.NOT_VALID_MEMBER
            );
            then(revokeOAuthTokenPort).shouldHaveNoInteractions();
        }

        @Test
        @DisplayName("Apple refresh token과 client ID가 모두 있으면 provider token을 먼저 폐기한다")
        void apple_token을_폐기한다() {
            MemberOAuth apple = OAuth_계정(
                OAUTH_ID,
                MEMBER_ID,
                OAuthProvider.APPLE,
                "apple-user",
                "refresh-token",
                "ios-client"
            );
            given(loadMemberOAuthPort.findByMemberOAuthId(OAUTH_ID)).willReturn(Optional.of(apple));

            sut.unlinkOAuth(OAuth_해제_명령(MEMBER_ID, OAUTH_ID, true, null, null));

            then(revokeOAuthTokenPort).should().revokeAppleToken("refresh-token", "ios-client");
            then(saveMemberOAuthPort).should().delete(apple);
        }

        @Test
        @DisplayName("Apple refresh token 또는 client ID가 없으면 외부 revoke만 생략하고 연결을 삭제한다")
        void apple_자격_정보가_불완전하면_revoke를_생략한다() {
            MemberOAuth missingRefresh = OAuth_계정(
                OAUTH_ID,
                MEMBER_ID,
                OAuthProvider.APPLE,
                "apple-user",
                null,
                "ios-client"
            );
            given(loadMemberOAuthPort.findByMemberOAuthId(OAUTH_ID)).willReturn(Optional.of(missingRefresh));

            sut.unlinkOAuth(OAuth_해제_명령(MEMBER_ID, OAUTH_ID, true, null, null));

            then(revokeOAuthTokenPort).shouldHaveNoInteractions();
            then(saveMemberOAuthPort).should().delete(missingRefresh);
        }

        @Test
        @DisplayName("Kakao access token 소유자가 일치하면 검증 후 revoke한다")
        void kakao_token_소유자를_검증하고_폐기한다() {
            MemberOAuth kakao = OAuth_계정(OAUTH_ID, MEMBER_ID, OAuthProvider.KAKAO, "kakao-user");
            given(loadMemberOAuthPort.findByMemberOAuthId(OAUTH_ID)).willReturn(Optional.of(kakao));
            given(verifyOAuthTokenPort.verify(OAuthProvider.KAKAO, "kakao-token"))
                .willReturn(OAuth_속성(OAuthProvider.KAKAO, "kakao-user", "kakao@example.com"));

            sut.unlinkOAuth(OAuth_해제_명령(MEMBER_ID, OAUTH_ID, true, null, "kakao-token"));

            then(revokeOAuthTokenPort).should().revokeKakaoToken("kakao-token");
            then(saveMemberOAuthPort).should().delete(kakao);
        }

        @Test
        @DisplayName("Kakao access token이 없으면 외부 revoke를 생략한다")
        void kakao_token이_없으면_revoke를_생략한다() {
            MemberOAuth kakao = OAuth_계정(OAUTH_ID, MEMBER_ID, OAuthProvider.KAKAO, "kakao-user");
            given(loadMemberOAuthPort.findByMemberOAuthId(OAUTH_ID)).willReturn(Optional.of(kakao));

            sut.unlinkOAuth(OAuth_해제_명령(MEMBER_ID, OAUTH_ID, true, null, null));

            then(verifyOAuthTokenPort).shouldHaveNoInteractions();
            then(revokeOAuthTokenPort).shouldHaveNoInteractions();
        }

        @Test
        @DisplayName("Google access token의 provider가 저장 계정과 다르면 revoke와 삭제를 막는다")
        void google_token_provider_불일치를_거부한다() {
            MemberOAuth google = OAuth_계정(OAUTH_ID, MEMBER_ID, OAuthProvider.GOOGLE, "google-user");
            given(loadMemberOAuthPort.findByMemberOAuthId(OAUTH_ID)).willReturn(Optional.of(google));
            given(verifyOAuthTokenPort.verify(OAuthProvider.GOOGLE, "google-token"))
                .willReturn(new OAuthAttributes(OAuthProvider.KAKAO, "google-user", "user@example.com"));

            assertError(
                () -> sut.unlinkOAuth(OAuth_해제_명령(MEMBER_ID, OAUTH_ID, true, "google-token", null)),
                AuthenticationErrorCode.OAUTH_INVALID_ACCESS_TOKEN
            );
            then(revokeOAuthTokenPort).shouldHaveNoInteractions();
            then(saveMemberOAuthPort).shouldHaveNoInteractions();
        }

        @Test
        @DisplayName("Google access token 소유자가 일치하면 검증 후 revoke한다")
        void google_token_소유자를_검증하고_폐기한다() {
            MemberOAuth google = OAuth_계정(OAUTH_ID, MEMBER_ID, OAuthProvider.GOOGLE, "google-user");
            given(loadMemberOAuthPort.findByMemberOAuthId(OAUTH_ID)).willReturn(Optional.of(google));
            given(verifyOAuthTokenPort.verify(OAuthProvider.GOOGLE, "google-token"))
                .willReturn(OAuth_속성(OAuthProvider.GOOGLE, "google-user", "user@example.com"));

            sut.unlinkOAuth(OAuth_해제_명령(MEMBER_ID, OAUTH_ID, true, "google-token", null));

            then(revokeOAuthTokenPort).should().revokeGoogleToken("google-token");
            then(saveMemberOAuthPort).should().delete(google);
        }
    }

    @Nested
    @DisplayName("Apple 자격 정보 갱신")
    class UpdateAppleCredential {

        @Test
        @DisplayName("재로그인한 Apple 계정의 refresh token과 client ID를 함께 갱신한다")
        void apple_자격_정보를_갱신한다() {
            MemberOAuth apple = OAuth_계정(OAUTH_ID, MEMBER_ID, OAuthProvider.APPLE, "apple-user", "old", "old-client");
            given(loadMemberOAuthPort.findByProviderAndProviderId(OAuthProvider.APPLE, "apple-user"))
                .willReturn(Optional.of(apple));

            sut.updateAppleRefreshToken(
                OAuthProvider.APPLE,
                "apple-user",
                "new-refresh-token",
                "new-client"
            );

            assertThat(apple.getAppleRefreshToken()).isEqualTo("new-refresh-token");
            assertThat(apple.getAppleClientId()).isEqualTo("new-client");
            then(saveMemberOAuthPort).should().save(apple);
        }

        @Test
        @DisplayName("연결되지 않은 Apple 계정의 자격 정보는 생성하지 않는다")
        void 없는_apple_계정을_거부한다() {
            given(loadMemberOAuthPort.findByProviderAndProviderId(OAuthProvider.APPLE, "missing"))
                .willReturn(Optional.empty());

            assertError(
                () -> sut.updateAppleRefreshToken(
                    OAuthProvider.APPLE,
                    "missing",
                    "refresh-token",
                    "client"
                ),
                AuthenticationErrorCode.MEMBER_OAUTH_NOT_FOUND
            );
            then(saveMemberOAuthPort).shouldHaveNoInteractions();
        }
    }

    private static void assertError(Runnable action, AuthenticationErrorCode errorCode) {
        assertThatThrownBy(action::run)
            .isInstanceOfSatisfying(AuthenticationDomainException.class, exception ->
                assertThat(exception.getBaseCode()).isEqualTo(errorCode)
            );
    }
}
