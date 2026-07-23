package com.umc.product.authorization.adapter.in.graphql;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.BatchMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.stereotype.Controller;

import com.umc.product.authorization.adapter.in.aspect.CheckAccess;
import com.umc.product.authorization.adapter.in.graphql.dto.ResourcePermissionGraphQlRequest;
import com.umc.product.authorization.adapter.in.graphql.dto.ResourcePermissionGraphQlResponse;
import com.umc.product.authorization.application.port.in.query.ListChallengerRoleUseCase;
import com.umc.product.authorization.application.port.in.query.ResourcePermissionUseCase;
import com.umc.product.authorization.application.port.in.query.dto.ChallengerRoleInfo;
import com.umc.product.authorization.domain.PermissionType;
import com.umc.product.authorization.domain.ResourceType;
import com.umc.product.global.security.MemberPrincipal;
import com.umc.product.global.security.annotation.CurrentMember;
import com.umc.product.organization.adapter.in.graphql.dto.GisuGraphQlResponse;
import com.umc.product.organization.application.port.in.query.GetGisuUseCase;
import com.umc.product.organization.application.port.in.query.dto.gisu.GisuInfo;

import lombok.RequiredArgsConstructor;

@Controller
@RequiredArgsConstructor
public class AuthorizationGraphQlController {

    private final ListChallengerRoleUseCase listChallengerRoleUseCase;
    private final ResourcePermissionUseCase resourcePermissionUseCase;
    private final GetGisuUseCase getGisuUseCase;

    @QueryMapping
    public List<ChallengerRoleInfo> myChallengerRoles(
        @CurrentMember MemberPrincipal principal,
        @Argument Long gisuId
    ) {
        return gisuId == null
            ? listChallengerRoleUseCase.listByMemberId(principal.getMemberId())
            : listChallengerRoleUseCase.listByMemberIdAndGisuId(principal.getMemberId(), gisuId);
    }

    @QueryMapping
    @CheckAccess(
        resourceType = ResourceType.CHALLENGER_ROLE,
        resourceId = "#id",
        permission = PermissionType.READ
    )
    public ChallengerRoleInfo challengerRole(@Argument Long id) {
        return listChallengerRoleUseCase.getById(id);
    }

    @QueryMapping
    public List<ResourcePermissionGraphQlResponse> resourcePermissions(
        @CurrentMember MemberPrincipal principal,
        @Argument List<ResourcePermissionGraphQlRequest> input
    ) {
        return resourcePermissionUseCase.batchHasPermission(
            principal.getMemberId(),
            input.stream().map(ResourcePermissionGraphQlRequest::toQuery).toList()
        ).stream().map(ResourcePermissionGraphQlResponse::from).toList();
    }

    @BatchMapping(typeName = "ChallengerRole", field = "gisu")
    public Map<ChallengerRoleInfo, GisuGraphQlResponse> gisus(List<ChallengerRoleInfo> roles) {
        Set<Long> gisuIds = roles.stream()
            .map(ChallengerRoleInfo::gisuId)
            .collect(Collectors.toSet());
        Map<Long, GisuInfo> gisusById = getGisuUseCase.getByIds(gisuIds).stream()
            .collect(Collectors.toMap(GisuInfo::gisuId, Function.identity()));

        return roles.stream().collect(Collectors.toMap(
            Function.identity(),
            role -> GisuGraphQlResponse.from(gisusById.get(role.gisuId()))
        ));
    }
}
