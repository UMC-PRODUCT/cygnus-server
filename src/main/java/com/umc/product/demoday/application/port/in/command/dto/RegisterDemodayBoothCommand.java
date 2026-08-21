package com.umc.product.demoday.application.port.in.command.dto;

/**
 * 부스 등록 요청이다. {@code projectId}와 {@code displayName} 중 정확히 하나만 채워야 한다.
 *
 * <p> 둘 중 무엇을 채웠는지를 통해서 등록 경로를 결정한다.
 * <p> {@code projectId}는 UPMS에 등록된 프로젝트에 연결하는 경로, {@code displayName}은 UPMS에 없는 외부 참가팀 경로다.
 */
public record RegisterDemodayBoothCommand(
    Long memberId,
    Long pollId,
    Integer boothCode,
    Long projectId,
    String displayName
) {
}
