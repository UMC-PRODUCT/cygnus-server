package com.umc.product.member.adapter.in.web.dto.request;

import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

@Schema(description = "회원가입 요청")
public record OAuthRegisterMemberRequest(

    @Schema(description = "OAuth 인증 토큰", example = "YOUR_JWT_TOKEN")
    @NotBlank(message = "OAuth 인증 토큰은 필수입니다") String oAuthVerificationToken,

    @Schema(description = "이름", example = "홍길동")
    @NotBlank(message = "이름은 필수입니다") @Size(max = 10, message = "이름은 10자 이하여야 합니다") String name,

    @Schema(description = "닉네임 (한글 1~6자)", example = "닉넴")
    @NotBlank(message = "닉네임은 필수입니다") @Size(min = 1, max = 6, message = "닉네임은 한글 1~6자여야 합니다") @Pattern(regexp = "^[가-힣]+$", message = "한글만 입력 가능합니다") String nickname,

    @Schema(description = "이메일 인증 토큰", example = "YOUR_JWT_TOKEN")
    @NotBlank(message = "이메일 인증 토큰은 필수입니다") String emailVerificationToken,

    @Schema(description = "학교 ID", example = "1")
    @NotNull(message = "학교 ID는 필수입니다") Long schoolId,

    @Schema(description = "사용 중단 예정 프로필 이미지 fileId. UI에서 사용하지 않아 이후 제거 예정")
    String profileImageId,

    @Schema(description = "약관 동의 목록")
    @NotNull(message = "약관 동의 목록은 필수입니다") @Size(min = 1, message = "최소 1개 이상의 약관 동의 정보가 필요합니다") @Valid List<TermConsentStatus> termsAgreements,

    @Schema(description = "Apple OAuth refresh token (Apple 로그인 회원가입 시 전달)")
    String appleRefreshToken,

    @Schema(description = "Apple OAuth client_id (Apple 로그인 회원가입 시 전달, Bundle ID 또는 Services ID)")
    String appleClientId
) {
}
