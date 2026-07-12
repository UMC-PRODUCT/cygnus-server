package com.umc.product.recruiting.application.service.command;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.umc.product.authorization.application.port.in.CheckPermissionUseCase;
import com.umc.product.authorization.domain.PermissionType;
import com.umc.product.authorization.domain.ResourcePermission;
import com.umc.product.authorization.domain.ResourceType;
import com.umc.product.recruiting.application.port.in.command.AuthorizeRecruitingManagementUseCase;

import lombok.RequiredArgsConstructor;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class RecruitingManagementAuthorizationService implements AuthorizeRecruitingManagementUseCase {

    private final CheckPermissionUseCase checkPermissionUseCase;

    @Override
    public void authorizeSeasonManagement(Long requesterMemberId, Long seasonId) {
        checkPermissionUseCase.checkOrThrow(
            requesterMemberId,
            ResourcePermission.of(ResourceType.RECRUITMENT, seasonId, PermissionType.EDIT)
        );
    }
}
