package com.umc.product.inhouse.application.port.out.command;

import com.umc.product.inhouse.domain.UmcProductChapterMembership;

public interface SaveUmcProductChapterMembershipPort {

    UmcProductChapterMembership save(UmcProductChapterMembership chapterMembership);

    void delete(UmcProductChapterMembership chapterMembership);

    void deleteAllByUmcProductMemberId(Long umcProductMemberId);
}
