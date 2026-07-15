package com.umc.product.organization.adapter.out.persistence.umcproduct;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;

import com.umc.product.organization.application.port.out.command.SaveUmcProductChapterMembershipPort;
import com.umc.product.organization.application.port.out.query.LoadUmcProductChapterMembershipPort;
import com.umc.product.organization.domain.UmcProductChapterMembership;
import com.umc.product.organization.exception.OrganizationDomainException;
import com.umc.product.organization.exception.OrganizationErrorCode;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class UmcProductChapterMembershipPersistenceAdapter
    implements LoadUmcProductChapterMembershipPort, SaveUmcProductChapterMembershipPort {

    private static final Map<String, OrganizationErrorCode> CONSTRAINT_ERROR_CODES = Map.of(
        "ex_umc_product_chapter_membership_activity",
        OrganizationErrorCode.UMC_PRODUCT_CHAPTER_MEMBERSHIP_OVERLAPPED
    );

    private final UmcProductChapterMembershipJpaRepository repository;

    @Override
    public UmcProductChapterMembership getById(Long chapterMembershipId) {
        return repository.findById(chapterMembershipId)
            .orElseThrow(() -> new OrganizationDomainException(
                OrganizationErrorCode.UMC_PRODUCT_CHAPTER_MEMBERSHIP_NOT_FOUND
            ));
    }

    @Override
    public List<UmcProductChapterMembership> listByUmcProductMemberId(Long umcProductMemberId) {
        return repository.findAllByUmcProductMemberId(umcProductMemberId);
    }

    @Override
    public List<UmcProductChapterMembership> listByUmcProductMemberIds(Collection<Long> umcProductMemberIds) {
        if (umcProductMemberIds == null || umcProductMemberIds.isEmpty()) {
            return List.of();
        }
        return repository.findAllByUmcProductMemberIds(umcProductMemberIds);
    }

    @Override
    public boolean existsByChapterId(Long chapterId) {
        return repository.existsByChapterId(chapterId);
    }

    @Override
    public boolean existsByMemberActivityPeriodId(Long memberActivityPeriodId) {
        return repository.existsByMemberActivityPeriodId(memberActivityPeriodId);
    }

    @Override
    public boolean existsOverlappingChapterMembership(
        Long umcProductMemberId,
        Long chapterId,
        LocalDate startDate,
        LocalDate endDate,
        Long excludedChapterMembershipId
    ) {
        return repository.existsOverlappingChapterMembership(
            umcProductMemberId,
            chapterId,
            startDate,
            endDate,
            excludedChapterMembershipId
        );
    }

    @Override
    public UmcProductChapterMembership save(UmcProductChapterMembership chapterMembership) {
        try {
            return repository.saveAndFlush(chapterMembership);
        } catch (DataIntegrityViolationException exception) {
            throw UmcProductConstraintViolationTranslator.translate(exception, CONSTRAINT_ERROR_CODES);
        }
    }

    @Override
    public void delete(UmcProductChapterMembership chapterMembership) {
        repository.delete(chapterMembership);
    }

    @Override
    public void deleteAllByUmcProductMemberId(Long umcProductMemberId) {
        repository.deleteAllByUmcProductMemberId(umcProductMemberId);
    }
}
