package com.umc.product.audit.adapter.in.graphql;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.BatchMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.stereotype.Controller;

import com.umc.product.audit.adapter.in.graphql.dto.AuditLogFilterGraphQlRequest;
import com.umc.product.audit.adapter.in.graphql.dto.AuditLogPageGraphQlResponse;
import com.umc.product.audit.application.port.in.query.GetAuditLogUseCase;
import com.umc.product.audit.application.port.in.query.dto.AuditLogInfo;
import com.umc.product.authorization.adapter.in.aspect.CheckAccess;
import com.umc.product.authorization.domain.PermissionType;
import com.umc.product.authorization.domain.ResourceType;
import com.umc.product.global.graphql.dto.PageGraphQlRequest;
import com.umc.product.member.application.port.in.query.GetMemberUseCase;
import com.umc.product.member.application.port.in.query.dto.MemberInfo;
import com.umc.product.member.application.port.in.query.dto.MemberPublicInfo;

import lombok.RequiredArgsConstructor;

@Controller
@RequiredArgsConstructor
public class AuditGraphQlController {

    private final GetAuditLogUseCase getAuditLogUseCase;
    private final GetMemberUseCase getMemberUseCase;

    @QueryMapping
    @CheckAccess(
        resourceType = ResourceType.AUDIT,
        permission = PermissionType.READ,
        message = "감사 로그는 중앙운영사무국 국원만 조회할 수 있어요. 필요한 권한이 있다면 운영진에게 문의해주세요."
    )
    public AuditLogPageGraphQlResponse auditLogs(
        @Argument AuditLogFilterGraphQlRequest filter,
        @Argument PageGraphQlRequest page
    ) {
        AuditLogFilterGraphQlRequest actualFilter = filter == null
            ? AuditLogFilterGraphQlRequest.empty()
            : filter;
        return AuditLogPageGraphQlResponse.from(
            getAuditLogUseCase.search(
                actualFilter.toQuery(),
                PageGraphQlRequest.defaultIfNull(page).toPageable()
            )
        );
    }

    @BatchMapping(typeName = "AuditLog", field = "actor")
    public Map<AuditLogInfo, MemberPublicInfo> actors(List<AuditLogInfo> logs) {
        Set<Long> actorIds = logs.stream()
            .map(AuditLogInfo::actorMemberId)
            .filter(java.util.Objects::nonNull)
            .collect(Collectors.toCollection(LinkedHashSet::new));
        Map<Long, MemberInfo> membersById = getMemberUseCase.findAllByIds(actorIds);

        Map<AuditLogInfo, MemberPublicInfo> result = new LinkedHashMap<>();
        for (AuditLogInfo log : logs) {
            MemberInfo member = membersById.get(log.actorMemberId());
            result.put(log, member == null ? null : MemberPublicInfo.from(member));
        }
        return result;
    }
}
