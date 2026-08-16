package com.umc.product.demoday.application.port.in.query;

import java.util.List;

import com.umc.product.demoday.application.port.in.query.dto.DemodayBoothInfo;

public interface ListDemodayBoothUseCase {

    List<DemodayBoothInfo> listBooths(Long pollId);
}
