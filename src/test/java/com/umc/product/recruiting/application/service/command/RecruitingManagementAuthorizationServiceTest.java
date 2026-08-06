package com.umc.product.recruiting.application.service.command;

import static org.mockito.BDDMockito.then;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.umc.product.authorization.application.port.in.CheckPermissionUseCase;
import com.umc.product.authorization.domain.PermissionType;
import com.umc.product.authorization.domain.ResourcePermission;
import com.umc.product.authorization.domain.ResourceType;

@ExtendWith(MockitoExtension.class)
class RecruitingManagementAuthorizationServiceTest {

    @Mock
    CheckPermissionUseCase checkPermissionUseCase;

    @InjectMocks
    RecruitingManagementAuthorizationService sut;

    @Test
    @DisplayName("공통 질문 관리는 대상 시즌의 EDIT 권한을 요구한다")
    void authorizeSeasonManagementWithEditPermission() {
        sut.authorizeSeasonManagement(99L, 11L);

        then(checkPermissionUseCase).should().checkOrThrow(
            99L,
            ResourcePermission.of(ResourceType.RECRUITMENT, 11L, PermissionType.EDIT)
        );
    }
}
