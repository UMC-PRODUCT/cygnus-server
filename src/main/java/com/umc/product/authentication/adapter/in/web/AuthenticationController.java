package com.umc.product.authentication.adapter.in.web;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.umc.product.authentication.adapter.in.web.dto.request.AppleLoginRequest;
import com.umc.product.authentication.adapter.in.web.dto.request.GoogleLoginRequest;
import com.umc.product.authentication.adapter.in.web.dto.request.KakaoCodeLoginRequest;
import com.umc.product.authentication.adapter.in.web.dto.request.KakaoLoginRequest;
import com.umc.product.authentication.adapter.in.web.dto.response.OAuthLoginResponse;
import com.umc.product.authentication.adapter.in.web.swagger.AuthenticationControllerInterface;
import com.umc.product.authentication.application.port.in.command.ManageAuthenticationUseCase;
import com.umc.product.authentication.application.port.in.command.OAuthAuthenticationUseCase;
import com.umc.product.authentication.application.port.in.command.dto.AccessTokenLoginCommand;
import com.umc.product.authentication.application.port.in.command.dto.AuthorizationCodeLoginCommand;
import com.umc.product.authentication.application.port.in.command.dto.IssueAuthenticationTokensCommand;
import com.umc.product.authentication.application.port.in.command.dto.NewTokens;
import com.umc.product.authentication.application.port.in.command.dto.OAuthTokenLoginResult;
import com.umc.product.authentication.application.port.out.AppleAuthorizationCodeResult;
import com.umc.product.authentication.application.port.out.VerifyOAuthTokenPort;
import com.umc.product.common.domain.enums.ClientType;
import com.umc.product.common.domain.enums.OAuthProvider;
import com.umc.product.global.security.JwtTokenProvider;
import com.umc.product.global.security.annotation.Public;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/auth")
public class AuthenticationController implements AuthenticationControllerInterface {

    private final OAuthAuthenticationUseCase oAuthAuthenticationUseCase;
    private final ManageAuthenticationUseCase manageAuthenticationUseCase;
    private final VerifyOAuthTokenPort verifyOAuthTokenPort;
    private final JwtTokenProvider jwtTokenProvider;

    @Override
    @PostMapping("login/google")
    @Public
    public OAuthLoginResponse googleOAuthLogin(
        @Valid @RequestBody GoogleLoginRequest request
    ) {
        return processAccessTokenLogin(OAuthProvider.GOOGLE, request.token(), request.clientType());
    }

    @Override
    @PostMapping("login/kakao")
    @Public
    public OAuthLoginResponse kakaoOAuthLogin(
        @Valid @RequestBody KakaoLoginRequest request
    ) {
        return processAccessTokenLogin(OAuthProvider.KAKAO, request.token(), request.clientType());
    }

    @Override
    @PostMapping("login/kakao/code")
    @Public
    public OAuthLoginResponse kakaoOAuthCodeLogin(
        @Valid @RequestBody KakaoCodeLoginRequest request
    ) {
        OAuthTokenLoginResult result = oAuthAuthenticationUseCase.authorizationCodeLogin(
            new AuthorizationCodeLoginCommand(
                OAuthProvider.KAKAO,
                request.authorizationCode(),
                request.redirectUri()
            )
        );

        return buildLoginResponse(OAuthProvider.KAKAO, result, null, request.clientType());
    }

    @Override
    @Public
    @PostMapping("login/apple")
    public OAuthLoginResponse appleOAuthLogin(
        @Valid @RequestBody AppleLoginRequest request
    ) {
        // Authorization Code 방식. Apple은 플랫폼별로 다른 client_id를 사용하므로 clientType을 함께 전달한다.
        AppleAuthorizationCodeResult codeResult = verifyOAuthTokenPort.verifyAppleAuthorizationCode(
            request.authorizationCode(), request.clientType()
        );
        OAuthTokenLoginResult result = oAuthAuthenticationUseCase.loginWithOAuthAttributes(codeResult.attrs());

        if (result.isExistingMember() && codeResult.refreshToken() != null) {
            // 기존 회원: MemberOAuth에 appleRefreshToken과 appleClientId 갱신
            oAuthAuthenticationUseCase.updateAppleRefreshToken(
                OAuthProvider.APPLE, result.providerId(), codeResult.refreshToken(), codeResult.clientId()
            );
        }

        return buildLoginResponse(OAuthProvider.APPLE, result, codeResult.refreshToken(), request.clientType());
    }

    /**
     * Access Token 기반 OAuth 로그인 처리.
     *
     * @param clientType 클라이언트 플랫폼. 도입 이전 클라이언트 호환을 위해 nullable.
     */
    private OAuthLoginResponse processAccessTokenLogin(OAuthProvider provider, String token, ClientType clientType) {
        OAuthTokenLoginResult result = oAuthAuthenticationUseCase.accessTokenLogin(
            new AccessTokenLoginCommand(provider, token)
        );

        return buildLoginResponse(provider, result, null, clientType);
    }

    /**
     * OAuthTokenLoginResult 를 기반으로 OAuthLoginResponse 를 생성합니다.
     *
     * @param clientType 클라이언트 플랫폼. nullable — null 인 경우 AT claim 에 포함되지 않으며
     *                   다운스트림 통계에서는 "UNKNOWN" 으로 집계된다.
     */
    private OAuthLoginResponse buildLoginResponse(OAuthProvider provider, OAuthTokenLoginResult result,
                                                  String appleRefreshToken, ClientType clientType) {
        if (result.isExistingMember()) {
            // 기존 회원: JWT 발급
            NewTokens newTokens = manageAuthenticationUseCase.issueTokens(
                IssueAuthenticationTokensCommand.of(result.memberId(), clientType)
            );

            return OAuthLoginResponse.ofLoginSuccess(
                provider, newTokens.accessToken(), newTokens.refreshToken()
            );
        } else {
            // 신규 회원: oAuthVerificationToken 발급 (회원가입 시 사용)
            String oAuthVerificationToken = jwtTokenProvider.createOAuthVerificationToken(
                result.email(),
                result.provider(),
                result.providerId()
            );

            return OAuthLoginResponse.ofRegisterRequired(
                provider, oAuthVerificationToken
            );
        }
    }
}
