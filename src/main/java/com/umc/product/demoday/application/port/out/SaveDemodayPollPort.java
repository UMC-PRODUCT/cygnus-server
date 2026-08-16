package com.umc.product.demoday.application.port.out;

import com.umc.product.demoday.domain.DemodayPoll;

public interface SaveDemodayPollPort {

    DemodayPoll save(DemodayPoll poll);
}
