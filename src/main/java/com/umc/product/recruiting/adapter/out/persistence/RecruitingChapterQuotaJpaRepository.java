package com.umc.product.recruiting.adapter.out.persistence;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.umc.product.recruiting.domain.RecruitingChapterQuota;

public interface RecruitingChapterQuotaJpaRepository extends JpaRepository<RecruitingChapterQuota, Long> {

    Optional<RecruitingChapterQuota> findByGisuIdAndChapterId(Long gisuId, Long chapterId);
}
