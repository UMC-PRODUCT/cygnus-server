package com.umc.product.test.application.port.in.command;

import com.umc.product.test.application.port.in.command.dto.CreateSeedMemberSystemRoleCommand;
import com.umc.product.test.application.port.in.command.dto.CreateSeedMemberSystemRoleResult;

public interface CreateSeedMemberSystemRoleUseCase {

    CreateSeedMemberSystemRoleResult create(CreateSeedMemberSystemRoleCommand command);
}
