package com.umc.product.demoday.application.port.in.command;

import java.util.List;

import com.umc.product.demoday.application.port.in.command.dto.RegisterDemodayBoothBatchCommand;
import com.umc.product.demoday.application.port.in.command.dto.RegisterDemodayBoothCommand;

public interface RegisterDemodayBoothUseCase {

    Long register(RegisterDemodayBoothCommand command);

    /**
     * @return 저장된 부스 ID. 입력 순서와 인덱스가 대응한다.
     */
    List<Long> registerAll(RegisterDemodayBoothBatchCommand command);
}
