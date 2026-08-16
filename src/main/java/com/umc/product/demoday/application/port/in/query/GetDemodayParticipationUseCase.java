package com.umc.product.demoday.application.port.in.query;

import com.umc.product.demoday.application.port.in.query.dto.DemodayParticipationInfo;
import com.umc.product.demoday.application.port.in.query.participant.DemodayParticipant;

public interface GetDemodayParticipationUseCase {

    DemodayParticipationInfo getParticipation(Long pollId, DemodayParticipant participant);
}
