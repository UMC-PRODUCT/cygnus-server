package com.umc.product.demoday.application.port.in.query.participant;

public interface DemodayParticipantResolver<S> {

    DemodayParticipant resolve(S source);
}
