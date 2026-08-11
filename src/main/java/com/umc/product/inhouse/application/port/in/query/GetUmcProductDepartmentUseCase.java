package com.umc.product.inhouse.application.port.in.query;

import java.time.LocalDate;
import java.util.List;

import com.umc.product.inhouse.application.port.in.query.dto.UmcProductDepartmentInfo;

public interface GetUmcProductDepartmentUseCase {

    List<UmcProductDepartmentInfo> list(Boolean active, LocalDate activeOn);
}
