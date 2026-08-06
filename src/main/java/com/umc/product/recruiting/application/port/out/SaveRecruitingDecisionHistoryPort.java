package com.umc.product.recruiting.application.port.out;

import com.umc.product.recruiting.domain.RecruitingDecisionHistory;

public interface SaveRecruitingDecisionHistoryPort {

    RecruitingDecisionHistory save(RecruitingDecisionHistory decisionHistory);
}
