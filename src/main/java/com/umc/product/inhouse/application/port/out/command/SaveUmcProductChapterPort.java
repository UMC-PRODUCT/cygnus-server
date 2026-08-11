package com.umc.product.inhouse.application.port.out.command;

import com.umc.product.inhouse.domain.UmcProductChapter;

public interface SaveUmcProductChapterPort {

    UmcProductChapter save(UmcProductChapter chapter);

    void delete(UmcProductChapter chapter);
}
