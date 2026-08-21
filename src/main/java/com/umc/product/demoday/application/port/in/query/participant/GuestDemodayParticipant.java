package com.umc.product.demoday.application.port.in.query.participant;

import java.util.Objects;

public record GuestDemodayParticipant(Long entryCodeId) implements DemodayParticipant {

    public GuestDemodayParticipant {
        Objects.requireNonNull(entryCodeId, "entryCodeId는 null일 수 없습니다.");
    }

    @Override
    public Long participantId() {
        return entryCodeId;
    }

    @Override
    public DemodayParticipantType participantType() {
        return DemodayParticipantType.GUEST;
    }
}
