package com.umc.product.demoday.application.port.out;

import com.umc.product.demoday.domain.DemodayVote;

public interface SaveDemodayVotePort {

    DemodayVote save(DemodayVote vote);
}
