package com.umc.product.demoday.application.port.in.command;

import com.umc.product.demoday.application.port.in.command.dto.CollectDemodayStampCommand;
import com.umc.product.demoday.application.port.in.command.dto.DemodayStampCollectInfo;

public interface CollectDemodayStampUseCase {

    DemodayStampCollectInfo collect(CollectDemodayStampCommand command);
}
