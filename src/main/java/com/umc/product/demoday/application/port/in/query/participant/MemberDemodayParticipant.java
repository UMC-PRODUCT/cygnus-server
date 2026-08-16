package com.umc.product.demoday.application.port.in.query.participant;

import java.util.Objects;

public record MemberDemodayParticipant(Long memberId) implements DemodayParticipant {

    public MemberDemodayParticipant {
        Objects.requireNonNull(memberId, "memberId는 null일 수 없습니다.");
    }

    @Override
    public Long participantId() {
        return memberId;
    }

    @Override
    public DemodayParticipantType participantType() {
        return DemodayParticipantType.MEMBER;
    }
}
