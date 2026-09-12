package com.umc.product.member.adapter.in.web.dto.request;

import java.util.List;

import com.umc.product.member.application.port.in.command.dto.EmailRegisterMemberCommand;
import com.umc.product.member.application.port.in.command.dto.TermConsents;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * 이메일 기반 회원가입 요청 DTO. ADR-017 에 따라 loginId 를 받지 않는다.
 * <p>
 * 비밀번호 형식은 CredentialPolicy 에서 사후 검증하므로 본 DTO 에서는 NotBlank 만 강제한다.
 */
public record EmailRegisterMemberRequest(
    @NotBlank(message = "본 회원가입 방법에서는 비밀번호를 필수로 제공하여야 합니다.") String rawPassword,

    @Schema(description = "이름", example = "박세은")
    @NotBlank(message = "이름은 필수입니다") @Size(max = 10, message = "이름은 10자 이하여야 합니다") String name,

    @Schema(description = "닉네임 (한글 1~6자)", example = "하늘")
    @NotBlank(message = "닉네임은 필수입니다") @Size(min = 1, max = 6, message = "닉네임은 한글 1~6자여야 합니다") @Pattern(regexp = "^[가-힣]+$", message = "한글만 입력 가능합니다") String nickname,

    @Schema(description = "이메일 인증 토큰", example = "some_email_token")
    @NotBlank(message = "이메일 인증 토큰은 필수입니다") String emailVerificationToken,

    @Schema(description = "학교 ID", example = "1")
    @NotNull(message = "학교 ID는 필수입니다") Long schoolId,

    @Schema(description = "약관 동의 목록")
    @NotNull(message = "약관 동의 목록은 필수입니다") @Size(min = 1, message = "최소 1개 이상의 약관 동의 정보가 필요합니다") @Valid List<TermConsentStatus> termsAgreements
) {
    public EmailRegisterMemberCommand toCommand(String email) {
        return EmailRegisterMemberCommand.builder()
            .rawPassword(rawPassword)
            .name(name)
            .nickname(nickname)
            .email(email)
            .schoolId(schoolId)
            .termConsents(termsAgreements.stream().map(TermConsents::fromRequest).toList())
            .build();
    }
}
