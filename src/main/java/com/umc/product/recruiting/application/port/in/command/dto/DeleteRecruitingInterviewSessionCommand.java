package com.umc.product.recruiting.application.port.in.command.dto;

public record DeleteRecruitingInterviewSessionCommand(
    Long sessionId,
    Long roundId,
    Long requesterMemberId
) {

    public static DeleteRecruitingInterviewSessionCommand of(
        Long sessionId,
        Long roundId,
        Long requesterMemberId
    ) {
        return new DeleteRecruitingInterviewSessionCommand(sessionId, roundId, requesterMemberId);
    }
}
