package com.umc.product.storage.application.port.in.command;

import com.umc.product.storage.application.port.in.command.dto.GeneratedFileInfo;
import com.umc.product.storage.application.port.in.command.dto.StoreGeneratedFileCommand;

public interface StoreGeneratedFileUseCase {

    GeneratedFileInfo store(StoreGeneratedFileCommand command);
}
