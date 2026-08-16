package com.umc.product.demoday.application.port.in.query;

import java.util.List;

import com.umc.product.demoday.application.port.in.query.dto.DemodayPollInfo;

public interface ListDemodayPollUseCase {

    List<DemodayPollInfo> listPolls();
}
