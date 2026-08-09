package com.umc.product.recruiting.application.port.out;

import java.util.Optional;

import com.umc.product.recruiting.domain.RecruitingChapterQuota;

public interface LoadRecruitingChapterQuotaPort {

    Optional<RecruitingChapterQuota> findByGisuIdAndChapterId(Long gisuId, Long chapterId);
}
