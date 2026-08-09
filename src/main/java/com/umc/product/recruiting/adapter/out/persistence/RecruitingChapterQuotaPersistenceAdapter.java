package com.umc.product.recruiting.adapter.out.persistence;

import java.util.Optional;

import org.springframework.stereotype.Component;

import com.umc.product.recruiting.application.port.out.LoadRecruitingChapterQuotaPort;
import com.umc.product.recruiting.application.port.out.SaveRecruitingChapterQuotaPort;
import com.umc.product.recruiting.domain.RecruitingChapterQuota;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class RecruitingChapterQuotaPersistenceAdapter implements
    LoadRecruitingChapterQuotaPort,
    SaveRecruitingChapterQuotaPort {

    private final RecruitingChapterQuotaJpaRepository repository;

    @Override
    public Optional<RecruitingChapterQuota> findByGisuIdAndChapterId(Long gisuId, Long chapterId) {
        return repository.findByGisuIdAndChapterId(gisuId, chapterId);
    }

    @Override
    public RecruitingChapterQuota save(RecruitingChapterQuota chapterQuota) {
        return repository.save(chapterQuota);
    }
}
