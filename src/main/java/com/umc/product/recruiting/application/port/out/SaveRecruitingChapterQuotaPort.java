package com.umc.product.recruiting.application.port.out;

import com.umc.product.recruiting.domain.RecruitingChapterQuota;

public interface SaveRecruitingChapterQuotaPort {

    RecruitingChapterQuota save(RecruitingChapterQuota chapterQuota);
}
