package com.umc.product.demoday.adapter.in.web.support;

import org.springframework.stereotype.Component;

import com.umc.product.demoday.application.port.in.query.participant.DemodayParticipant;
import com.umc.product.demoday.application.port.in.query.participant.DemodayParticipantResolver;
import com.umc.product.demoday.application.port.in.query.participant.MemberDemodayParticipant;
import com.umc.product.global.security.MemberPrincipal;

@Component
public class MemberDemodayParticipantResolver implements DemodayParticipantResolver<MemberPrincipal> {

    @Override
    public DemodayParticipant resolve(MemberPrincipal memberPrincipal) {
        return new MemberDemodayParticipant(memberPrincipal.getMemberId());
    }
}
