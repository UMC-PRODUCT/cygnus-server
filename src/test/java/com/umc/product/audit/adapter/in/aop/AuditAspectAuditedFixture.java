package com.umc.product.audit.adapter.in.aop;

import com.umc.product.audit.application.port.in.annotation.Audited;
import com.umc.product.audit.domain.AuditAction;
import com.umc.product.global.exception.constant.Domain;

class AuditAspectAuditedFixture {

    @Audited(
        domain = Domain.CHALLENGER,
        action = AuditAction.CREATE,
        targetType = "AuditFixture",
        targetId = "#targetId",
        description = "#description"
    )
    public void record(String targetId, String description) {
    }

    @Audited(
        domain = Domain.CHALLENGER,
        action = AuditAction.CREATE,
        targetType = "AuditFixture",
        targetId = "@auditRepository.getById(1)"
    )
    public void queryInSpel() {
    }
}
