package com.umc.product.inhouse.application.port.in.query;

import java.time.LocalDate;
import java.util.List;

import com.umc.product.inhouse.application.port.in.query.dto.UmcProductSquadInfo;

public interface GetUmcProductSquadUseCase {

    List<UmcProductSquadInfo> list(Boolean active, LocalDate activeOn);
}
