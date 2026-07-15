package com.umc.product.organization.application.port.in.query;

import java.util.List;

import com.umc.product.organization.application.port.in.query.dto.umcproduct.UmcProductChapterInfo;

public interface GetUmcProductChapterUseCase {

    List<UmcProductChapterInfo> list(Boolean active);
}
