package com.umc.product.inhouse.adapter.out.persistence;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;

import com.umc.product.inhouse.application.port.out.command.SaveUmcProductChapterPort;
import com.umc.product.inhouse.application.port.out.query.LoadUmcProductChapterPort;
import com.umc.product.inhouse.domain.UmcProductChapter;
import com.umc.product.inhouse.exception.InhouseDomainException;
import com.umc.product.inhouse.exception.InhouseErrorCode;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class UmcProductChapterPersistenceAdapter
    implements LoadUmcProductChapterPort, SaveUmcProductChapterPort {

    private static final Map<String, InhouseErrorCode> CONSTRAINT_ERROR_CODES = Map.of(
        "uk_umc_product_chapter_code",
        InhouseErrorCode.UMC_PRODUCT_CHAPTER_ALREADY_EXISTS
    );

    private final UmcProductChapterJpaRepository repository;

    @Override
    public UmcProductChapter getById(Long chapterId) {
        return repository.findById(chapterId)
            .orElseThrow(() -> new InhouseDomainException(
                InhouseErrorCode.UMC_PRODUCT_CHAPTER_NOT_FOUND
            ));
    }

    @Override
    public UmcProductChapter getByIdWithLock(Long chapterId) {
        return repository.findByIdWithLock(chapterId)
            .orElseThrow(() -> new InhouseDomainException(
                InhouseErrorCode.UMC_PRODUCT_CHAPTER_NOT_FOUND
            ));
    }

    @Override
    public List<UmcProductChapter> listAll(Boolean active) {
        return repository.findAll(active);
    }

    @Override
    public List<UmcProductChapter> listByIds(Collection<Long> chapterIds) {
        if (chapterIds == null || chapterIds.isEmpty()) {
            return List.of();
        }
        return repository.findByIdIn(chapterIds);
    }

    @Override
    public boolean existsById(Long chapterId) {
        return repository.existsById(chapterId);
    }

    @Override
    public boolean existsByCode(String code, Long excludedChapterId) {
        return repository.existsByCode(code, excludedChapterId);
    }

    @Override
    public UmcProductChapter save(UmcProductChapter chapter) {
        try {
            return repository.saveAndFlush(chapter);
        } catch (DataIntegrityViolationException exception) {
            throw UmcProductConstraintViolationTranslator.translate(exception, CONSTRAINT_ERROR_CODES);
        }
    }

    @Override
    public void delete(UmcProductChapter chapter) {
        repository.delete(chapter);
    }
}
