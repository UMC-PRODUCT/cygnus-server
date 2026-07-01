package com.umc.product.recruiting.application.port.in.command;

import com.umc.product.recruiting.application.port.in.command.dto.DecideRecruitingDocumentCommand;

public interface DecideRecruitingDocumentUseCase {

    void decideDocument(DecideRecruitingDocumentCommand command);
}
