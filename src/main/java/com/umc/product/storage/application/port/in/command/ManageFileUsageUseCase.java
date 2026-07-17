package com.umc.product.storage.application.port.in.command;

import com.umc.product.storage.application.port.in.command.dto.BulkRemoveFileUsagesCommand;
import com.umc.product.storage.application.port.in.command.dto.ReplaceFileUsagesCommand;

public interface ManageFileUsageUseCase {

    void replaceUsages(ReplaceFileUsagesCommand command);

    void removeAll(BulkRemoveFileUsagesCommand command);
}
