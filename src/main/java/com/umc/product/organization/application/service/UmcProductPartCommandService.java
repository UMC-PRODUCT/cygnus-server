package com.umc.product.organization.application.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.umc.product.audit.application.port.in.annotation.Audited;
import com.umc.product.audit.domain.AuditAction;
import com.umc.product.global.exception.constant.Domain;
import com.umc.product.organization.application.port.in.command.ManageUmcProductPartUseCase;
import com.umc.product.organization.application.port.in.command.dto.CreateUmcProductPartCommand;
import com.umc.product.organization.application.port.in.command.dto.UpdateUmcProductPartCommand;
import com.umc.product.organization.application.port.out.command.SaveUmcProductPartPort;
import com.umc.product.organization.application.port.out.query.LoadUmcProductChapterPort;
import com.umc.product.organization.application.port.out.query.LoadUmcProductPartMembershipPort;
import com.umc.product.organization.application.port.out.query.LoadUmcProductPartPort;
import com.umc.product.organization.domain.UmcProductChapter;
import com.umc.product.organization.domain.UmcProductPart;
import com.umc.product.organization.exception.OrganizationDomainException;
import com.umc.product.organization.exception.OrganizationErrorCode;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class UmcProductPartCommandService implements ManageUmcProductPartUseCase {

    private final LoadUmcProductChapterPort loadUmcProductChapterPort;
    private final LoadUmcProductPartPort loadUmcProductPartPort;
    private final SaveUmcProductPartPort saveUmcProductPartPort;
    private final LoadUmcProductPartMembershipPort loadUmcProductPartMembershipPort;
    private final UmcProductAccessPolicy umcProductAccessPolicy;

    @Audited(
        domain = Domain.ORGANIZATION,
        action = AuditAction.CREATE,
        targetType = "UmcProductPart",
        targetId = "#result",
        description = "'UMC Product 파트를 생성했습니다.'"
    )
    @Override
    public Long create(CreateUmcProductPartCommand command) {
        validateCanManage(command.requesterMemberId());
        UmcProductChapter chapter = loadUmcProductChapterPort.getById(command.chapterId());
        validateCodeNotDuplicated(chapter.getId(), command.code(), null);
        UmcProductPart part = UmcProductPart.create(
            chapter,
            command.code(),
            command.name(),
            command.description(),
            command.sortOrder(),
            command.active()
        );
        return saveUmcProductPartPort.save(part).getId();
    }

    @Audited(
        domain = Domain.ORGANIZATION,
        action = AuditAction.UPDATE,
        targetType = "UmcProductPart",
        targetId = "#command.partId()",
        description = "'UMC Product 파트를 수정했습니다.'"
    )
    @Override
    public void update(UpdateUmcProductPartCommand command) {
        validateCanManage(command.requesterMemberId());
        UmcProductPart part = loadUmcProductPartPort.getByIdWithLock(command.partId());
        if (command.code() != null) {
            validateCodeNotDuplicated(part.getChapter().getId(), command.code(), part.getId());
        }
        part.update(command.code(), command.name(), command.description(), command.sortOrder(), command.active());
        saveUmcProductPartPort.save(part);
    }

    @Audited(
        domain = Domain.ORGANIZATION,
        action = AuditAction.DELETE,
        targetType = "UmcProductPart",
        targetId = "#partId",
        description = "'UMC Product 파트를 삭제했습니다.'"
    )
    @Override
    public void delete(Long partId, Long requesterMemberId) {
        validateCanManage(requesterMemberId);
        UmcProductPart part = loadUmcProductPartPort.getByIdWithLock(partId);
        if (loadUmcProductPartMembershipPort.existsByPartId(partId)) {
            throw new OrganizationDomainException(OrganizationErrorCode.UMC_PRODUCT_PART_HAS_MEMBERSHIPS);
        }
        saveUmcProductPartPort.delete(part);
    }

    private void validateCodeNotDuplicated(Long chapterId, String code, Long excludedPartId) {
        if (code != null
            && loadUmcProductPartPort.existsByChapterIdAndCode(chapterId, code.trim(), excludedPartId)) {
            throw new OrganizationDomainException(OrganizationErrorCode.UMC_PRODUCT_PART_ALREADY_EXISTS);
        }
    }

    private void validateCanManage(Long requesterMemberId) {
        if (!umcProductAccessPolicy.canManageUmcProduct(requesterMemberId)) {
            throw new OrganizationDomainException(OrganizationErrorCode.UMC_PRODUCT_ACCESS_DENIED);
        }
    }
}
