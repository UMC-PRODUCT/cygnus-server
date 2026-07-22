package com.umc.product.test.application.port.in.command;

import com.umc.product.test.application.port.in.command.dto.SeedChallengerPointsCommand;
import com.umc.product.test.application.port.in.command.dto.SeedChallengerPointsResult;

public interface SeedChallengerPointsUseCase {

    SeedChallengerPointsResult seed(SeedChallengerPointsCommand command);
}
