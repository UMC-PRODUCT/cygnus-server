package com.umc.product.demoday.application.port.in.query.dto;

public record ListDemodayAdminVoteQuery(
    Long pollId,
    Long requesterMemberId,
    Long cursor,
    int size,
    Long boothId,
    String participantName
) {

    public ListDemodayAdminVoteQuery {
        participantName = normalizeParticipantName(participantName);
    }

    public boolean hasParticipantNameFilter() {
        return participantName != null;
    }

    private static String normalizeParticipantName(String participantName) {
        if (participantName == null || participantName.isBlank()) {
            return null;
        }
        return participantName.strip();
    }
}
