package com.umc.product.form.adapter.in.graphql;

import java.util.Objects;

import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.stereotype.Controller;

import com.umc.product.form.adapter.in.graphql.dto.FormGraphQlResponse;
import com.umc.product.form.application.port.in.query.GetFormUseCase;
import com.umc.product.form.domain.enums.FormStatus;
import com.umc.product.global.security.MemberPrincipal;
import com.umc.product.global.security.annotation.CurrentMember;

import lombok.RequiredArgsConstructor;

@Controller
@RequiredArgsConstructor
public class FormGraphQlController {

    private final GetFormUseCase getFormUseCase;

    @QueryMapping
    public FormGraphQlResponse form(
        @CurrentMember MemberPrincipal principal,
        @Argument Long id
    ) {
        return getFormUseCase.findById(id)
            .filter(form -> form.status() != FormStatus.DRAFT
                || Objects.equals(form.createdMemberId(), principal.getMemberId()))
            .map(ignored -> FormGraphQlResponse.from(getFormUseCase.getFormWithStructure(id)))
            .orElse(null);
    }
}
