package com.umc.product.demoday.adapter.in.web.support;

import org.springframework.stereotype.Component;

import com.umc.product.demoday.adapter.in.web.security.DemodayParticipationPrincipal;
import com.umc.product.demoday.application.port.in.query.participant.DemodayParticipant;
import com.umc.product.demoday.application.port.in.query.participant.DemodayParticipantResolver;
import com.umc.product.demoday.application.port.in.query.participant.GuestDemodayParticipant;

@Component
public class GuestDemodayParticipantResolver implements DemodayParticipantResolver<DemodayParticipationPrincipal> {

    @Override
    public DemodayParticipant resolve(DemodayParticipationPrincipal principal) {
        return new GuestDemodayParticipant(principal.getEntryCodeId());
    }
}
