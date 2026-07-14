package com.umc.product.audit.application.service.query;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.umc.product.audit.application.port.in.query.GetAuditLogUseCase;
import com.umc.product.audit.application.port.in.query.dto.AuditLogInfo;
import com.umc.product.audit.application.port.in.query.dto.SearchAuditLogQuery;
import com.umc.product.audit.application.port.out.LoadAuditLogPort;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AuditLogQueryService implements GetAuditLogUseCase {

    private final LoadAuditLogPort loadAuditLogPort;

    @Override
    public Page<AuditLogInfo> search(SearchAuditLogQuery query, Pageable pageable) {
        return loadAuditLogPort.search(
            query.domain(), query.action(), query.actorMemberId(),
            query.from(), query.to(), query.targetType(), query.targetId(),
            query.outcome(), query.source(), query.requestId(), query.traceId(),
            pageable
        ).map(AuditLogInfo::from);
    }
}
