package com.umc.product.organization.application.port.in.query;

import java.util.List;

import com.umc.product.organization.application.port.in.query.dto.umcproduct.UmcProductPartInfo;

public interface GetUmcProductPartUseCase {

    List<UmcProductPartInfo> list(Long chapterId, Boolean active);
}
