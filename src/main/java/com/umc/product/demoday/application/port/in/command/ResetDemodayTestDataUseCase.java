package com.umc.product.demoday.application.port.in.command;

import com.umc.product.demoday.application.port.in.command.dto.DemodayTestDataResetInfo;

public interface ResetDemodayTestDataUseCase {

    DemodayTestDataResetInfo reset(Long requesterMemberId);
}
