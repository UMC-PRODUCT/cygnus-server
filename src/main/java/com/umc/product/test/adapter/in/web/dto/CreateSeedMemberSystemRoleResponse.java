package com.umc.product.test.adapter.in.web.dto;

import com.umc.product.member.domain.MemberSystemRoleType;
import com.umc.product.test.application.port.in.command.dto.CreateSeedMemberSystemRoleResult;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "테스트 회원 시스템 역할 부여 결과")
public record CreateSeedMemberSystemRoleResponse(
    @Schema(description = "시스템 역할을 부여한 회원 ID", example = "1")
    Long memberId,

    @Schema(description = "부여한 시스템 역할", example = "SUPER_ADMIN")
    MemberSystemRoleType roleType,

    @Schema(description = "이번 요청에서 새 역할 행을 생성했는지 여부", example = "true")
    boolean created
) {

    public static CreateSeedMemberSystemRoleResponse from(CreateSeedMemberSystemRoleResult result) {
        return new CreateSeedMemberSystemRoleResponse(
            result.memberId(),
            result.roleType(),
            result.created()
        );
    }
}
