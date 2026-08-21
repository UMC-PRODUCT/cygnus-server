package com.umc.product.demoday.adapter.in.web.support;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.umc.product.demoday.adapter.in.web.security.DemodayParticipationPrincipal;
import com.umc.product.demoday.application.port.in.query.participant.DemodayParticipantResolver;
import com.umc.product.global.security.MemberPrincipal;

@Configuration
public class DemodayParticipantResolverConfig {

    @Bean
    public CurrentDemodayParticipantArgumentResolver currentDemodayParticipantArgumentResolver(
        DemodayParticipantResolver<MemberPrincipal> memberDemodayParticipantResolver,
        DemodayParticipantResolver<DemodayParticipationPrincipal> guestDemodayParticipantResolver
    ) {
        return new CurrentDemodayParticipantArgumentResolver(
            memberDemodayParticipantResolver, guestDemodayParticipantResolver);
    }
}
