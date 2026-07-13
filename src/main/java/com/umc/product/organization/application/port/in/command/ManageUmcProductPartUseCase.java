package com.umc.product.organization.application.port.in.command;

import com.umc.product.organization.application.port.in.command.dto.CreateUmcProductPartCommand;
import com.umc.product.organization.application.port.in.command.dto.UpdateUmcProductPartCommand;

public interface ManageUmcProductPartUseCase {

    Long create(CreateUmcProductPartCommand command);

    void update(UpdateUmcProductPartCommand command);

    void delete(Long partId, Long requesterMemberId);
}
