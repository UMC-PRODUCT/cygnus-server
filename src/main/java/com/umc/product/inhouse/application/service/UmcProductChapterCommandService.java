package com.umc.product.inhouse.application.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.umc.product.audit.application.port.in.annotation.Audited;
import com.umc.product.audit.domain.AuditAction;
import com.umc.product.global.exception.constant.Domain;
import com.umc.product.inhouse.application.port.in.command.ManageUmcProductChapterUseCase;
import com.umc.product.inhouse.application.port.in.command.dto.CreateUmcProductChapterCommand;
import com.umc.product.inhouse.application.port.in.command.dto.UpdateUmcProductChapterCommand;
import com.umc.product.inhouse.application.port.out.command.SaveUmcProductChapterPort;
import com.umc.product.inhouse.application.port.out.query.LoadUmcProductChapterMembershipPort;
import com.umc.product.inhouse.application.port.out.query.LoadUmcProductChapterPort;
import com.umc.product.inhouse.domain.UmcProductChapter;
import com.umc.product.organization.exception.OrganizationDomainException;
import com.umc.product.organization.exception.OrganizationErrorCode;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class UmcProductChapterCommandService implements ManageUmcProductChapterUseCase {

    private final LoadUmcProductChapterPort loadUmcProductChapterPort;
    private final SaveUmcProductChapterPort saveUmcProductChapterPort;
    private final LoadUmcProductChapterMembershipPort loadUmcProductChapterMembershipPort;
    private final UmcProductAccessPolicy umcProductAccessPolicy;

    @Audited(
        domain = Domain.ORGANIZATION,
        action = AuditAction.CREATE,
        targetType = "UmcProductChapter",
        targetId = "#result",
        description = "'UMC PRODUCT 챕터를 생성했습니다.'"
    )
    @Override
    public Long create(CreateUmcProductChapterCommand command) {
        validateCanManage(command.requesterMemberId());
        validateCodeNotDuplicated(command.code(), null);
        UmcProductChapter chapter = UmcProductChapter.create(
            command.code(), command.name(), command.description(), command.sortOrder(), command.active()
        );
        return saveUmcProductChapterPort.save(chapter).getId();
    }

    @Audited(
        domain = Domain.ORGANIZATION,
        action = AuditAction.UPDATE,
        targetType = "UmcProductChapter",
        targetId = "#command.chapterId()",
        description = "'UMC PRODUCT 챕터를 수정했습니다.'"
    )
    @Override
    public void update(UpdateUmcProductChapterCommand command) {
        validateCanManage(command.requesterMemberId());
        UmcProductChapter chapter = loadUmcProductChapterPort.getByIdWithLock(command.chapterId());
        if (command.code() != null) {
            validateCodeNotDuplicated(command.code(), chapter.getId());
        }
        chapter.update(
            command.code(), command.name(), command.description(), command.sortOrder(), command.active()
        );
        saveUmcProductChapterPort.save(chapter);
    }

    @Audited(
        domain = Domain.ORGANIZATION,
        action = AuditAction.DELETE,
        targetType = "UmcProductChapter",
        targetId = "#chapterId",
        description = "'UMC PRODUCT 챕터를 삭제했습니다.'"
    )
    @Override
    public void delete(Long chapterId, Long requesterMemberId) {
        validateCanManage(requesterMemberId);
        UmcProductChapter chapter = loadUmcProductChapterPort.getByIdWithLock(chapterId);
        if (loadUmcProductChapterMembershipPort.existsByChapterId(chapterId)) {
            throw new OrganizationDomainException(OrganizationErrorCode.UMC_PRODUCT_CHAPTER_HAS_MEMBERSHIPS);
        }
        saveUmcProductChapterPort.delete(chapter);
    }

    private void validateCodeNotDuplicated(String code, Long excludedChapterId) {
        if (code != null && loadUmcProductChapterPort.existsByCode(code.trim(), excludedChapterId)) {
            throw new OrganizationDomainException(OrganizationErrorCode.UMC_PRODUCT_CHAPTER_ALREADY_EXISTS);
        }
    }

    private void validateCanManage(Long requesterMemberId) {
        if (!umcProductAccessPolicy.canManageUmcProduct(requesterMemberId)) {
            throw new OrganizationDomainException(OrganizationErrorCode.UMC_PRODUCT_ACCESS_DENIED);
        }
    }
}
