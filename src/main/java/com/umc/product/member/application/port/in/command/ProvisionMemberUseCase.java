package com.umc.product.member.application.port.in.command;

import com.umc.product.member.application.port.in.command.dto.ProvisionMemberCommand;

public interface ProvisionMemberUseCase {

    Long provision(ProvisionMemberCommand command);
}
