package com.umc.product.recruiting.application.port.out;

import com.umc.product.recruiting.domain.RecruitingRound;

public interface SaveRecruitingRoundPort {

    RecruitingRound save(RecruitingRound round);

    void delete(RecruitingRound round);
}
