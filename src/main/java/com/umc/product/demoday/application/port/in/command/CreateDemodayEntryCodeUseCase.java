package com.umc.product.demoday.application.port.in.command;

import com.umc.product.demoday.application.port.in.command.dto.CreateDemodayEntryCodeCommand;
import com.umc.product.demoday.application.port.in.command.dto.CreateDemodayEntryCodesInfo;

public interface CreateDemodayEntryCodeUseCase {

    CreateDemodayEntryCodesInfo create(Long memberId, CreateDemodayEntryCodeCommand command);

}
