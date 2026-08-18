package com.umc.product.demoday.application.port.in.command;

import com.umc.product.demoday.application.port.in.command.dto.CreateStampCredentialCommand;
import com.umc.product.demoday.application.port.in.command.dto.StampCredentialInfo;

public interface CreateDemodayStampUseCase {

    StampCredentialInfo create(Long memberId, CreateStampCredentialCommand command);
}
