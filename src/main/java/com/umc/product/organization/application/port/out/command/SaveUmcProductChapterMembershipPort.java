package com.umc.product.organization.application.port.out.command;

import com.umc.product.organization.domain.UmcProductChapterMembership;

public interface SaveUmcProductChapterMembershipPort {

    UmcProductChapterMembership save(UmcProductChapterMembership chapterMembership);

    void delete(UmcProductChapterMembership chapterMembership);

    void deleteAllByUmcProductMemberId(Long umcProductMemberId);
}
