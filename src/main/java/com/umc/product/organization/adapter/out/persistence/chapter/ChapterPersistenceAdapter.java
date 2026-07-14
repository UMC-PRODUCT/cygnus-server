package com.umc.product.organization.adapter.out.persistence.chapter;


import java.util.List;
import java.util.Set;

import org.springframework.stereotype.Component;

import com.umc.product.organization.application.port.out.command.SaveChapterPort;
import com.umc.product.organization.application.port.out.query.LoadChapterPort;
import com.umc.product.organization.domain.Chapter;
import com.umc.product.organization.exception.OrganizationDomainException;
import com.umc.product.organization.exception.OrganizationErrorCode;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class ChapterPersistenceAdapter implements LoadChapterPort, SaveChapterPort {

    private final ChapterJpaRepository chapterJpaRepository;

    public void validateExists(Long chapterId) {
        if (!chapterJpaRepository.existsById(chapterId)) {
            throw new OrganizationDomainException(OrganizationErrorCode.CHAPTER_NOT_FOUND);
        }
    }

    public Chapter findById(Long chapterId) {
        return chapterJpaRepository.findById(chapterId).orElseThrow(() ->
            new OrganizationDomainException(OrganizationErrorCode.CHAPTER_NOT_FOUND));
    }

    @Override
    public List<Chapter> findAll() {

        return chapterJpaRepository.findAll();

    }

    @Override
    public List<Chapter> findByGisuId(Long gisuId) {
        return chapterJpaRepository.findByGisuId(gisuId);
    }

    @Override
    public List<Chapter> findByGisuIds(Set<Long> gisuIds) {
        return chapterJpaRepository.findByGisuIdInWithGisu(gisuIds);
    }

    @Override
    public boolean existsByGisuId(Long gisuId) {
        return chapterJpaRepository.existsByGisuId(gisuId);
    }

    @Override
    public boolean existsByIdAndGisuId(Long chapterId, Long gisuId) {
        return chapterJpaRepository.existsByIdAndGisuId(chapterId, gisuId);
    }

    @Override
    public Chapter save(Chapter chapter) {
        return chapterJpaRepository.save(chapter);
    }

    @Override
    public void delete(Chapter chapter) {
        chapterJpaRepository.delete(chapter);
    }
}
