package com.umc.product.audit.application.port.in.command;

import com.umc.product.audit.application.port.in.command.dto.RecordAuditLogCommand;

public interface RecordAuditLogUseCase {

    void record(RecordAuditLogCommand command);
}
