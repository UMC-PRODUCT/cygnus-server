package com.umc.product.test.application.port.in.command;

import com.umc.product.test.application.port.in.command.dto.SeedBulkDataCommand;
import com.umc.product.test.application.port.in.command.dto.SeedBulkDataResult;

public interface SeedBulkDataUseCase {

    SeedBulkDataResult seed(SeedBulkDataCommand command);
}
