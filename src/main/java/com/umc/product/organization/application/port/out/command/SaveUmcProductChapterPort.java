package com.umc.product.organization.application.port.out.command;

import com.umc.product.organization.domain.UmcProductChapter;

public interface SaveUmcProductChapterPort {

    UmcProductChapter save(UmcProductChapter chapter);

    void delete(UmcProductChapter chapter);
}
