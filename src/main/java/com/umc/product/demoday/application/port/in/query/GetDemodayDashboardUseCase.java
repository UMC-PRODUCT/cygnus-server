package com.umc.product.demoday.application.port.in.query;

import com.umc.product.demoday.application.port.in.query.dto.DemodayDashboardInfo;

public interface GetDemodayDashboardUseCase {

    DemodayDashboardInfo getDashboard(Long pollId, Long memberId);
}
