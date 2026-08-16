package com.umc.product.authentication.adapter.in.web.swagger;

import org.springframework.web.bind.annotation.RequestBody;

import com.umc.product.authentication.adapter.in.web.dto.request.AppleLoginRequest;
import com.umc.product.authentication.adapter.in.web.dto.request.GoogleLoginRequest;
import com.umc.product.authentication.adapter.in.web.dto.request.KakaoCodeLoginRequest;
import com.umc.product.authentication.adapter.in.web.dto.request.KakaoLoginRequest;
import com.umc.product.authentication.adapter.in.web.dto.response.OAuthLoginResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@Tag(name = "Authentication | 소셜 로그인", description = "소셜 로그인(OAuth)을 처리합니다.")
public interface AuthenticationControllerInterface {
    @Operation(operationId = "LOGIN-001", summary = "Google 로그인",
        description = """
            Web에서 Redirect 방식으로 사용하려면 아래의 Link를 참고해주세요.

            [Local](http://localhost:8080/api/v1/auth/oauth2/authorization/google)
            [Development](https://dev.api.umc.it.kr/api/v1/auth/oauth2/authorization/google)
            [Production](https://api.umc.it.kr/api/v1/auth/oauth2/authorization/google)

            Google 측에서 제공받은 `idToken`을 전달해주세요.
            기존 클라이언트 호환을 위해 `accessToken`도 일시적으로 허용하지만, 신규 클라이언트는 OIDC `idToken`을 사용해야 합니다.

            **응답 설명:**
            - `success=true, code=LOGIN_SUCCESS`: 기존 회원 로그인 성공. accessToken, refreshToken 발급됨.
            - `success=true, code=REGISTER_REQUIRED`: OAuth 인증 성공, 회원가입 필요. oAuthVerificationToken 발급됨.
            """)
    OAuthLoginResponse googleOAuthLogin(
        @Valid @RequestBody GoogleLoginRequest request
    );

    @Operation(operationId = "LOGIN-005", summary = "Kakao 로그인",
        description = """
            Web에서 Redirect 방식으로 사용하려면 아래의 Link를 참고해주세요.

            [Local](http://localhost:8080/api/v1/auth/oauth2/authorization/kakao)
            [Development](https://dev.api.umc.it.kr/api/v1/auth/oauth2/authorization/kakao)
            [Production](https://api.umc.it.kr/api/v1/auth/oauth2/authorization/kakao)

            Kakao 측에서 제공받은 OIDC `idToken`을 전달해주세요.
            기존 클라이언트 호환을 위해 `accessToken`도 일시적으로 허용하지만, 신규 클라이언트는 OIDC `idToken`을 사용해야 합니다.

            **응답 설명:**
            - `success=true, code=LOGIN_SUCCESS`: 기존 회원 로그인 성공. accessToken, refreshToken 발급됨.
            - `success=true, code=REGISTER_REQUIRED`: OAuth 인증 성공, 회원가입 필요. oAuthVerificationToken 발급됨.
            """)
    OAuthLoginResponse kakaoOAuthLogin(
        @Valid @RequestBody KakaoLoginRequest request
    );

    @Operation(operationId = "LOGIN-006", summary = "Kakao 인가 코드 로그인",
        description = """
            표준 OAuth2 authorization code grant 흐름을 사용하는 클라이언트(주로 웹)를 위한 엔드포인트입니다.

            Kakao 로그인 페이지에서 받은 `authorizationCode`와 인가 요청 시 사용한 `redirectUri`를 함께 전달해주세요.
            서버가 Kakao token endpoint를 호출해 토큰으로 교환한 뒤, 응답에 `id_token`이 있으면 OIDC 서명 검증으로 인증을 처리합니다.
            `id_token`이 없는 legacy 응답은 기존 access token userinfo 조회로 fallback합니다.

            `redirectUri`는 서버 화이트리스트에 등록된 값과 일치해야 하며, 일치하지 않으면 `INVALID_OAUTH_REDIRECT_URI` 에러가 발생합니다.
            모바일 네이티브 SDK 사용 시에는 기존 `/login/kakao` 엔드포인트(access token)를 그대로 사용해주세요.

            **응답 설명:**
            - `success=true, code=LOGIN_SUCCESS`: 기존 회원 로그인 성공. accessToken, refreshToken 발급됨.
            - `success=true, code=REGISTER_REQUIRED`: OAuth 인증 성공, 회원가입 필요. oAuthVerificationToken 발급됨.
            """)
    OAuthLoginResponse kakaoOAuthCodeLogin(
        @Valid @RequestBody KakaoCodeLoginRequest request
    );

    @Operation(operationId = "LOGIN-010", summary = "Apple 로그인",
        description = """
            Web에서 Redirect 방식으로 사용하려면 아래의 Link를 참고해주세요.

            [Local](http://localhost:8080/api/v1/auth/oauth2/authorization/apple)
            [Development](https://dev.api.umc.it.kr/api/v1/auth/oauth2/authorization/apple)
            [Production](https://api.umc.it.kr/api/v1/auth/oauth2/authorization/apple)

            Apple 측에서 받은 authorization code와 함께 클라이언트 플랫폼(`clientType`)을 전달해주세요.
            Apple은 플랫폼별로 서로 다른 client_id(iOS Bundle ID vs Web Services ID)를 사용하므로
            `clientType`(ANDROID, IOS, WEB)을 정확히 명시해야 토큰 교환이 가능합니다.
            """)
    OAuthLoginResponse appleOAuthLogin(
        @Valid @RequestBody AppleLoginRequest request
    );
}
