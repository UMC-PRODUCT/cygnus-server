package com.umc.product.recruiting.application.port.out;

import com.umc.product.recruiting.domain.RecruitingSeason;

public interface SaveRecruitingSeasonPort {

    RecruitingSeason save(RecruitingSeason season);
}
