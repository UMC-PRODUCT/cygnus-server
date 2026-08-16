package com.umc.product.challenger.application.port.in.command.dto;

/**
 * 챌린저를 영구 삭제합니다. 잘못 생성한 경우에만 사용하며, 제명 및 자진탈부 처리 등은 UPDATE를 사용합니다.
 *
 * @param challengerId 챌린저 ID
 * @param description  삭제 사유 (필수)
 */
public record DeleteChallengerCommand(
    Long challengerId,
    String description
) {
    public static DeleteChallengerCommand of(Long challengerId, String description) {
        return new DeleteChallengerCommand(challengerId, description);
    }
}
