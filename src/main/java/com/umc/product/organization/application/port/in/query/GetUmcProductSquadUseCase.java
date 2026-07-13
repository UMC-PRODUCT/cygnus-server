package com.umc.product.organization.application.port.in.query;

import java.time.LocalDate;
import java.util.List;

import com.umc.product.organization.application.port.in.query.dto.umcproduct.UmcProductSquadInfo;

public interface GetUmcProductSquadUseCase {

    List<UmcProductSquadInfo> list(Boolean active, LocalDate activeOn);
}
