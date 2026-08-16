package com.umc.product.authentication.adapter.in.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.Base64;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.web.util.UriComponentsBuilder;

import com.umc.product.authentication.adapter.out.external.OAuthTokenVerificationAdapter;
import com.umc.product.authentication.adapter.out.persistence.MemberOAuthRepository;
import com.umc.product.authentication.application.service.SsoLoginTokenClaims;
import com.umc.product.authentication.domain.MemberOAuth;
import com.umc.product.authentication.domain.OAuthAttributes;
import com.umc.product.common.domain.enums.ClientType;
import com.umc.product.common.domain.enums.OAuthProvider;
import com.umc.product.global.client.ClientContextClaims;
import com.umc.product.global.client.ClientEnvironment;
import com.umc.product.global.client.ClientServiceType;
import com.umc.product.global.security.RefreshTokenClaims;
import com.umc.product.member.adapter.out.persistence.MemberJpaRepository;
import com.umc.product.member.domain.Member;
import com.umc.product.organization.application.port.out.query.LoadGisuPort;
import com.umc.product.organization.domain.Chapter;
import com.umc.product.organization.domain.Gisu;
import com.umc.product.organization.domain.School;
import com.umc.product.support.IntegrationTestSupport;
import com.umc.product.support.fixture.ChapterFixture;
import com.umc.product.support.fixture.GisuFixture;
import com.umc.product.support.fixture.SchoolFixture;

import jakarta.servlet.http.Cookie;

@TestPropertySource(properties = {
    "app.sso.clients.backoffice.environment=DEV",
    "app.sso.clients.backoffice.redirect-uris[0]=https://dev.admin.university.neordinary.com/auth/callback",
    "app.sso.clients.backoffice.allowed-origins[0]=https://dev.admin.university.neordinary.com"
})
@DisplayName("SSO Authorization Code + PKCE 통합 흐름")
class SsoFlowIntegrationTest extends IntegrationTestSupport {

    private static final String EMAIL = "sso-flow@test.com";
    private static final String RAW_PASSWORD = "Password123!";
    private static final String LOGIN_TOKEN = "sso-login-token";
    private static final String ACCESS_TOKEN = "access-token";
    private static final String REFRESH_TOKEN = "refresh-token";
    private static final String CLIENT_ID = "backoffice";
    private static final String REDIRECT_URI = "https://dev.admin.university.neordinary.com/auth/callback";
    private static final URI REDIRECT_URI_EXPECTED = URI.create(REDIRECT_URI);
    private static final ClientContextClaims BACKOFFICE_DEV_CONTEXT = ClientContextClaims.of(
        CLIENT_ID,
        ClientServiceType.UMC_BACKOFFICE,
        ClientEnvironment.DEV
    );

    @Autowired
    private MemberJpaRepository memberJpaRepository;

    @Autowired
    private MemberOAuthRepository memberOAuthRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private GisuFixture gisuFixture;

    @Autowired
    private LoadGisuPort loadGisuPort;

    @Autowired
    private ChapterFixture chapterFixture;

    @Autowired
    private SchoolFixture schoolFixture;

    @MockitoBean
    private OAuthTokenVerificationAdapter oAuthTokenVerificationAdapter;

    @Test
    @DisplayName("browser login 후 authorization code를 발급하고 PKCE verifier로 token을 교환한다")
    void browser_login_authorization_code_token_exchange_성공() throws Exception {
        // given
        Member member = activeMemberWithCredential();
        Instant loginExpiresAt = Instant.now().plusSeconds(3600);
        given(jwtTokenProvider.createSsoLoginToken(eq(member.getId()), eq("email"), any(Instant.class)))
            .willReturn(LOGIN_TOKEN);
        given(jwtTokenProvider.parseSsoLoginToken(LOGIN_TOKEN))
            .willReturn(SsoLoginTokenClaims.of(member.getId(), Instant.now(), loginExpiresAt, "email"));
        given(jwtTokenProvider.createAccessToken(
            eq(member.getId()),
            anyList(),
            eq(ClientType.WEB),
            argThat(BACKOFFICE_DEV_CONTEXT::equals),
            eq(3600L)
        )).willReturn(ACCESS_TOKEN);
        given(jwtTokenProvider.createRefreshToken(eq(member.getId()), argThat(BACKOFFICE_DEV_CONTEXT::equals)))
            .willReturn(REFRESH_TOKEN);
        given(jwtTokenProvider.parseRefreshToken(REFRESH_TOKEN))
            .willReturn(new RefreshTokenClaims(
                member.getId(),
                UUID.randomUUID(),
                Instant.now().plusSeconds(604800),
                BACKOFFICE_DEV_CONTEXT
            ));

        String codeVerifier = "abcdefghijklmnopqrstuvwxyz0123456789ABCDEFG";
        String codeChallenge = s256Challenge(codeVerifier);

        // when: browser login
        mockMvc.perform(post("/api/v1/auth/sso/email")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "email": "sso-flow@test.com",
                      "password": "Password123!"
                    }
                    """))
            .andExpect(status().isOk())
            .andExpect(header().string(HttpHeaders.SET_COOKIE, containsString("UMC_SSO_LOGIN=sso-login-token")))
            .andExpect(header().string(HttpHeaders.SET_COOKIE, containsString("HttpOnly")))
            .andExpect(jsonPath("$.result.memberId").value(member.getId()));

        // when: authorization code 발급
        MvcResult authorizeResult = mockMvc.perform(get("/api/v1/auth/sso/oauth/authorize")
                .param("client_id", CLIENT_ID)
                .param("redirect_uri", REDIRECT_URI)
                .param("response_type", "code")
                .param("state", "state-123")
                .param("code_challenge", codeChallenge)
                .param("code_challenge_method", "S256")
                .cookie(new Cookie("UMC_SSO_LOGIN", LOGIN_TOKEN)))
            .andExpect(status().isFound())
            .andReturn();

        String location = authorizeResult.getResponse().getHeader(HttpHeaders.LOCATION);
        var redirect = UriComponentsBuilder.fromUriString(location).build(true);
        assertThat(redirect.getScheme()).isEqualTo(REDIRECT_URI_EXPECTED.getScheme());
        assertThat(redirect.getHost()).isEqualTo(REDIRECT_URI_EXPECTED.getHost());
        assertThat(redirect.getPort()).isEqualTo(REDIRECT_URI_EXPECTED.getPort());
        assertThat(redirect.getPath()).isEqualTo("/auth/callback");
        assertThat(redirect.getQueryParams().getFirst("state")).isEqualTo("state-123");

        String authorizationCode = redirect.getQueryParams().getFirst("code");
        assertThat(authorizationCode).isNotBlank();

        // then: token exchange
        mockMvc.perform(post("/api/v1/auth/sso/oauth/token")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .param("grant_type", "authorization_code")
                .param("code", authorizationCode)
                .param("client_id", CLIENT_ID)
                .param("redirect_uri", REDIRECT_URI)
                .param("code_verifier", codeVerifier))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.result.accessToken").value(ACCESS_TOKEN))
            .andExpect(jsonPath("$.result.refreshToken").value(REFRESH_TOKEN))
            .andExpect(jsonPath("$.result.expiresIn").value(3600L))
            .andExpect(jsonPath("$.result.tokenType").value("Bearer"))
            .andExpect(jsonPath("$.result.member.id").value(member.getId()))
            .andExpect(jsonPath("$.result.member.name").value("홍길동"))
            .andExpect(jsonPath("$.result.member.nickname").value("길동"))
            .andExpect(jsonPath("$.result.member.email").value(EMAIL));

        // then: authorization code는 1회만 소비할 수 있다
        mockMvc.perform(post("/api/v1/auth/sso/oauth/token")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .param("grant_type", "authorization_code")
                .param("code", authorizationCode)
                .param("client_id", CLIENT_ID)
                .param("redirect_uri", REDIRECT_URI)
                .param("code_verifier", codeVerifier))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.success").value(false))
            .andExpect(jsonPath("$.code").value("AUTHENTICATION-0034"));
    }

    @Test
    @DisplayName("Kakao SSO 로그인 기존 회원은 SSO 쿠키로 authorization code를 발급하고 token을 교환한다")
    void kakao_sso_login_authorization_code_token_exchange_성공() throws Exception {
        // given
        Member member = activeMemberWithOAuth(OAuthProvider.KAKAO, "kakao-provider-id");
        Instant loginExpiresAt = Instant.now().plusSeconds(3600);
        given(oAuthTokenVerificationAdapter.verify(OAuthProvider.KAKAO, "kakao-id-token"))
            .willReturn(new OAuthAttributes(OAuthProvider.KAKAO, "kakao-provider-id", EMAIL));
        given(jwtTokenProvider.createSsoLoginToken(eq(member.getId()), eq("kakao"), any(Instant.class)))
            .willReturn(LOGIN_TOKEN);
        given(jwtTokenProvider.parseSsoLoginToken(LOGIN_TOKEN))
            .willReturn(SsoLoginTokenClaims.of(member.getId(), Instant.now(), loginExpiresAt, "kakao"));
        given(jwtTokenProvider.createAccessToken(
            eq(member.getId()),
            anyList(),
            eq(ClientType.WEB),
            argThat(BACKOFFICE_DEV_CONTEXT::equals),
            eq(3600L)
        )).willReturn(ACCESS_TOKEN);
        given(jwtTokenProvider.createRefreshToken(eq(member.getId()), argThat(BACKOFFICE_DEV_CONTEXT::equals)))
            .willReturn(REFRESH_TOKEN);
        given(jwtTokenProvider.parseRefreshToken(REFRESH_TOKEN))
            .willReturn(new RefreshTokenClaims(
                member.getId(),
                UUID.randomUUID(),
                Instant.now().plusSeconds(604800),
                BACKOFFICE_DEV_CONTEXT
            ));

        String codeVerifier = "abcdefghijklmnopqrstuvwxyz0123456789ABCDEFG";
        String codeChallenge = s256Challenge(codeVerifier);

        // when: social SSO login
        mockMvc.perform(post("/api/v1/auth/sso/kakao")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "idToken": "kakao-id-token"
                    }
                    """))
            .andExpect(status().isOk())
            .andExpect(header().string(HttpHeaders.SET_COOKIE, containsString("UMC_SSO_LOGIN=sso-login-token")))
            .andExpect(content().string(not(containsString("accessToken"))))
            .andExpect(content().string(not(containsString("refreshToken"))))
            .andExpect(jsonPath("$.result.provider").value("KAKAO"))
            .andExpect(jsonPath("$.result.memberId").value(member.getId()));

        // when: authorization code 발급
        MvcResult authorizeResult = mockMvc.perform(get("/api/v1/auth/sso/oauth/authorize")
                .param("client_id", CLIENT_ID)
                .param("redirect_uri", REDIRECT_URI)
                .param("response_type", "code")
                .param("state", "state-123")
                .param("code_challenge", codeChallenge)
                .param("code_challenge_method", "S256")
                .cookie(new Cookie("UMC_SSO_LOGIN", LOGIN_TOKEN)))
            .andExpect(status().isFound())
            .andReturn();

        String location = authorizeResult.getResponse().getHeader(HttpHeaders.LOCATION);
        var redirect = UriComponentsBuilder.fromUriString(location).build(true);
        assertThat(redirect.getQueryParams().getFirst("state")).isEqualTo("state-123");

        String authorizationCode = redirect.getQueryParams().getFirst("code");
        assertThat(authorizationCode).isNotBlank();

        // then: token exchange
        mockMvc.perform(post("/api/v1/auth/sso/oauth/token")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .param("grant_type", "authorization_code")
                .param("code", authorizationCode)
                .param("client_id", CLIENT_ID)
                .param("redirect_uri", REDIRECT_URI)
                .param("code_verifier", codeVerifier))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.result.accessToken").value(ACCESS_TOKEN))
            .andExpect(jsonPath("$.result.refreshToken").value(REFRESH_TOKEN))
            .andExpect(jsonPath("$.result.expiresIn").value(3600L))
            .andExpect(jsonPath("$.result.tokenType").value("Bearer"))
            .andExpect(jsonPath("$.result.member.id").value(member.getId()))
            .andExpect(jsonPath("$.result.member.email").value(EMAIL));
    }

    @Test
    @DisplayName("Google SSO 로그인 신규 회원은 쿠키 없이 가입 필요 응답에서 멈춘다")
    void google_sso_login_신규회원_register_required() throws Exception {
        // given
        given(oAuthTokenVerificationAdapter.verify(OAuthProvider.GOOGLE, "google-id-token"))
            .willReturn(new OAuthAttributes(OAuthProvider.GOOGLE, "google-provider-id", "new-social@test.com"));
        given(jwtTokenProvider.createOAuthVerificationToken(
            "new-social@test.com",
            OAuthProvider.GOOGLE,
            "google-provider-id"
        )).willReturn("oauth-verification-token");

        // when / then
        mockMvc.perform(post("/api/v1/auth/sso/google")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "idToken": "google-id-token"
                    }
                    """))
            .andExpect(status().isOk())
            .andExpect(header().doesNotExist(HttpHeaders.SET_COOKIE))
            .andExpect(jsonPath("$.result.provider").value("GOOGLE"))
            .andExpect(jsonPath("$.result.code").value("REGISTER_REQUIRED"))
            .andExpect(jsonPath("$.result.memberId").doesNotExist())
            .andExpect(jsonPath("$.result.oAuthVerificationToken").value("oauth-verification-token"));
    }

    private Member activeMemberWithCredential() {
        School school = activeSchool();
        Member member = Member.create("홍길동", "길동", EMAIL, school.getId(), null);
        member.registerCredential(passwordEncoder.encode(RAW_PASSWORD));
        return memberJpaRepository.saveAndFlush(member);
    }

    private Member activeMemberWithOAuth(OAuthProvider provider, String providerId) {
        Member member = activeMemberWithCredential();
        memberOAuthRepository.saveAndFlush(MemberOAuth.builder()
            .memberId(member.getId())
            .provider(provider)
            .providerId(providerId)
            .build());
        return member;
    }

    private School activeSchool() {
        Gisu gisu = loadGisuPort.findActiveGisu()
            .orElseGet(() -> gisuFixture.활성_기수(9108L));
        Chapter chapter = chapterFixture.지부(gisu, "SSO통합지부");
        return schoolFixture.지부에_소속된_학교("SSO통합학교", chapter);
    }

    private String s256Challenge(String codeVerifier) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashed = digest.digest(codeVerifier.getBytes(StandardCharsets.US_ASCII));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(hashed);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 알고리즘을 사용할 수 없습니다.", e);
        }
    }
}
