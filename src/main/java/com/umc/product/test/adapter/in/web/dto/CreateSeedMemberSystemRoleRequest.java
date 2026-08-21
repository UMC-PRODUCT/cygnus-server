package com.umc.product.test.adapter.in.web.dto;

import com.umc.product.member.domain.MemberSystemRoleType;
import com.umc.product.test.application.port.in.command.dto.CreateSeedMemberSystemRoleCommand;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

@Schema(description = "테스트 회원 시스템 역할 부여 요청")
public record CreateSeedMemberSystemRoleRequest(
    @Schema(description = "시스템 역할을 부여할 회원 ID", example = "1")
    @NotNull(message = "회원 ID는 필수입니다") Long memberId,

    @Schema(description = "부여할 시스템 역할", example = "SUPER_ADMIN")
    @NotNull(message = "시스템 역할은 필수입니다") MemberSystemRoleType roleType
) {

    public CreateSeedMemberSystemRoleCommand toCommand() {
        return new CreateSeedMemberSystemRoleCommand(memberId, roleType);
    }
}
