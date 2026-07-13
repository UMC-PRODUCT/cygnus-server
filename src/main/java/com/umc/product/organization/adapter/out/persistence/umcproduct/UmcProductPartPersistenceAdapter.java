package com.umc.product.organization.adapter.out.persistence.umcproduct;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;

import com.umc.product.organization.application.port.out.command.SaveUmcProductPartPort;
import com.umc.product.organization.application.port.out.query.LoadUmcProductPartPort;
import com.umc.product.organization.domain.UmcProductPart;
import com.umc.product.organization.exception.OrganizationDomainException;
import com.umc.product.organization.exception.OrganizationErrorCode;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class UmcProductPartPersistenceAdapter implements LoadUmcProductPartPort, SaveUmcProductPartPort {

    private static final Map<String, OrganizationErrorCode> CONSTRAINT_ERROR_CODES = Map.of(
        "uk_umc_product_part_chapter_code",
        OrganizationErrorCode.UMC_PRODUCT_PART_ALREADY_EXISTS
    );

    private final UmcProductPartJpaRepository repository;

    @Override
    public UmcProductPart getById(Long partId) {
        return repository.findById(partId)
            .orElseThrow(() -> new OrganizationDomainException(OrganizationErrorCode.UMC_PRODUCT_PART_NOT_FOUND));
    }

    @Override
    public UmcProductPart getByIdWithLock(Long partId) {
        return repository.findByIdWithLock(partId)
            .orElseThrow(() -> new OrganizationDomainException(OrganizationErrorCode.UMC_PRODUCT_PART_NOT_FOUND));
    }

    @Override
    public List<UmcProductPart> listAll(Long chapterId, Boolean active) {
        return repository.findAll(chapterId, active);
    }

    @Override
    public List<UmcProductPart> listByChapterIds(Collection<Long> chapterIds, Boolean active) {
        if (chapterIds == null || chapterIds.isEmpty()) {
            return List.of();
        }
        return repository.findAllByChapterIds(chapterIds, active);
    }

    @Override
    public List<UmcProductPart> listByIds(Collection<Long> partIds) {
        if (partIds == null || partIds.isEmpty()) {
            return List.of();
        }
        return repository.findByIdIn(partIds);
    }

    @Override
    public boolean existsByChapterId(Long chapterId) {
        return repository.existsByChapterId(chapterId);
    }

    @Override
    public boolean existsByChapterIdAndCode(Long chapterId, String code, Long excludedPartId) {
        return repository.existsByChapterIdAndCode(chapterId, code, excludedPartId);
    }

    @Override
    public UmcProductPart save(UmcProductPart part) {
        try {
            return repository.saveAndFlush(part);
        } catch (DataIntegrityViolationException exception) {
            throw UmcProductConstraintViolationTranslator.translate(exception, CONSTRAINT_ERROR_CODES);
        }
    }

    @Override
    public void delete(UmcProductPart part) {
        repository.delete(part);
    }
}
