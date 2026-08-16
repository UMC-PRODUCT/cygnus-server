package com.umc.product.authentication.adapter.in.web;

import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.umc.product.authentication.adapter.in.web.dto.request.SsoAppleLoginRequest;
import com.umc.product.authentication.adapter.in.web.dto.request.SsoBrowserLoginByEmailRequest;
import com.umc.product.authentication.adapter.in.web.dto.request.SsoOAuthTokenLoginRequest;
import com.umc.product.authentication.adapter.in.web.dto.response.SsoBrowserLoginResponse;
import com.umc.product.authentication.adapter.in.web.dto.response.SsoSocialLoginResponse;
import com.umc.product.authentication.application.port.in.command.ManageSsoBrowserLoginUseCase;
import com.umc.product.authentication.application.port.in.dto.SsoBrowserLoginInfo;
import com.umc.product.authentication.application.port.in.dto.SsoBrowserOAuthLoginResult;
import com.umc.product.authentication.application.port.in.query.GetSsoBrowserLoginUseCase;
import com.umc.product.authentication.domain.exception.AuthenticationDomainException;
import com.umc.product.authentication.domain.exception.AuthenticationErrorCode;
import com.umc.product.common.domain.enums.OAuthProvider;
import com.umc.product.global.security.annotation.Public;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/auth/sso")
@Tag(name = "Authentication | SSO 브라우저 로그인", description = "Auth App 브라우저 로그인 쿠키를 관리합니다.")
public class SsoBrowserLoginController {

    private final ManageSsoBrowserLoginUseCase manageSsoBrowserLoginUseCase;
    private final GetSsoBrowserLoginUseCase getSsoBrowserLoginUseCase;
    private final SsoCookieWriter ssoCookieWriter;

    @Public
    @PostMapping("/email")
    @Operation(operationId = "SSO-LOGIN-001", summary = "email/password 기반 Auth App 브라우저 로그인")
    public SsoBrowserLoginResponse loginByEmail(
        @Valid @RequestBody SsoBrowserLoginByEmailRequest request,
        HttpServletResponse response
    ) {
        SsoBrowserLoginInfo info = manageSsoBrowserLoginUseCase.loginByEmail(request.toCommand());
        ssoCookieWriter.writeLoginCookie(response, info.loginToken(), info.expiresAt());
        return SsoBrowserLoginResponse.from(info);
    }

    @Public
    @PostMapping("/kakao")
    @Operation(operationId = "SSO-LOGIN-004", summary = "Kakao 기반 SSO 로그인")
    public SsoSocialLoginResponse loginByKakao(
        @Valid @RequestBody SsoOAuthTokenLoginRequest request,
        HttpServletResponse response
    ) {
        SsoBrowserOAuthLoginResult result =
            manageSsoBrowserLoginUseCase.loginByOAuthToken(request.toCommand(OAuthProvider.KAKAO));
        return writeSocialLoginResponse(response, result);
    }

    @Public
    @PostMapping("/google")
    @Operation(operationId = "SSO-LOGIN-005", summary = "Google 기반 SSO 로그인")
    public SsoSocialLoginResponse loginByGoogle(
        @Valid @RequestBody SsoOAuthTokenLoginRequest request,
        HttpServletResponse response
    ) {
        SsoBrowserOAuthLoginResult result =
            manageSsoBrowserLoginUseCase.loginByOAuthToken(request.toCommand(OAuthProvider.GOOGLE));
        return writeSocialLoginResponse(response, result);
    }

    @Public
    @PostMapping("/apple")
    @Operation(operationId = "SSO-LOGIN-006", summary = "Apple 기반 SSO 로그인")
    public SsoSocialLoginResponse loginByApple(
        @Valid @RequestBody SsoAppleLoginRequest request,
        HttpServletResponse response
    ) {
        SsoBrowserOAuthLoginResult result =
            manageSsoBrowserLoginUseCase.loginByAppleAuthorizationCode(request.toCommand());
        return writeSocialLoginResponse(response, result);
    }

    @Public
    @GetMapping("/me")
    @Operation(operationId = "SSO-LOGIN-002", summary = "현재 Auth App 브라우저 로그인 조회")
    public SsoBrowserLoginResponse getLogin(
        @CookieValue(name = "${app.sso.cookie.name}", required = false) String rawLoginToken
    ) {
        if (!StringUtils.hasText(rawLoginToken)) {
            throw new AuthenticationDomainException(AuthenticationErrorCode.SSO_BROWSER_LOGIN_REQUIRED);
        }

        SsoBrowserLoginInfo info = getSsoBrowserLoginUseCase.getLogin(rawLoginToken);
        return SsoBrowserLoginResponse.from(info);
    }

    @Public
    @PostMapping("/logout")
    @Operation(operationId = "SSO-LOGIN-003", summary = "Auth App 브라우저 로그아웃")
    public void logout(HttpServletResponse response) {
        ssoCookieWriter.clearLoginCookie(response);
    }

    private SsoSocialLoginResponse writeSocialLoginResponse(
        HttpServletResponse response,
        SsoBrowserOAuthLoginResult result
    ) {
        if (result.hasLoginCookie()) {
            ssoCookieWriter.writeLoginCookie(response, result.loginToken(), result.expiresAt());
        }
        return SsoSocialLoginResponse.from(result);
    }
}
