package com.umc.product.demoday.application.service.command;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.umc.product.audit.application.port.in.annotation.Audited;
import com.umc.product.audit.domain.AuditAction;
import com.umc.product.demoday.application.port.in.command.ResetDemodayTestDataUseCase;
import com.umc.product.demoday.application.port.in.command.dto.DemodayTestDataResetInfo;
import com.umc.product.demoday.application.port.out.DeleteDemodayTestDataPort;
import com.umc.product.demoday.application.port.out.dto.DemodayTestDataDeletionCounts;
import com.umc.product.demoday.application.service.DemodayAdminAccessChecker;
import com.umc.product.global.exception.constant.Domain;

import lombok.RequiredArgsConstructor;

@Service
@Profile("!prod")
@ConditionalOnProperty(prefix = "demoday.test-data-reset", name = "enabled", havingValue = "true")
@RequiredArgsConstructor
@Transactional
public class ResetDemodayTestDataCommandService implements ResetDemodayTestDataUseCase {

    private final DemodayAdminAccessChecker adminAccessChecker;
    private final DeleteDemodayTestDataPort deleteDemodayTestDataPort;

    @Override
    @Audited(
        domain = Domain.DEMODAY,
        action = AuditAction.DELETE,
        targetType = "DemodayTestData",
        description = "'데모데이 테스트 데이터를 초기화했습니다.'"
    )
    public DemodayTestDataResetInfo reset(Long requesterMemberId) {
        adminAccessChecker.validateSystemAdminAccess(requesterMemberId);

        DemodayTestDataDeletionCounts counts = deleteDemodayTestDataPort.deleteAll();
        return DemodayTestDataResetInfo.from(counts);
    }
}
