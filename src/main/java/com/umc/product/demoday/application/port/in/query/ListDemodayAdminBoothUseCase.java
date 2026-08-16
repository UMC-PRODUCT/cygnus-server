package com.umc.product.demoday.application.port.in.query;

import com.umc.product.demoday.application.port.in.query.dto.DemodayAdminBoothListInfo;

public interface ListDemodayAdminBoothUseCase {

    DemodayAdminBoothListInfo listBooths(Long pollId, Long memberId);
}
