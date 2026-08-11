package com.umc.product.inhouse.application.port.in.query;

import java.util.List;

import com.umc.product.inhouse.application.port.in.query.dto.UmcProductChapterInfo;

public interface GetUmcProductChapterUseCase {

    List<UmcProductChapterInfo> list(Boolean active);
}
