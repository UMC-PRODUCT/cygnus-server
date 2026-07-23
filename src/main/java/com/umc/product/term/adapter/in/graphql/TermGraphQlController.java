package com.umc.product.term.adapter.in.graphql;

import java.util.List;

import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.stereotype.Controller;

import com.umc.product.authorization.adapter.in.aspect.CheckAccess;
import com.umc.product.authorization.domain.PermissionType;
import com.umc.product.authorization.domain.ResourceType;
import com.umc.product.global.security.MemberPrincipal;
import com.umc.product.global.security.annotation.CurrentMember;
import com.umc.product.term.adapter.in.graphql.dto.CreateTermGraphQlRequest;
import com.umc.product.term.adapter.in.graphql.dto.RequiredTermConsentStatusGraphQlResponse;
import com.umc.product.term.adapter.in.graphql.dto.TermGraphQlResponse;
import com.umc.product.term.application.port.in.command.ManageTermAgreementUseCase;
import com.umc.product.term.application.port.in.command.ManageTermUseCase;
import com.umc.product.term.application.port.in.command.dto.CreateTermConsentCommand;
import com.umc.product.term.application.port.in.query.GetRequiredTermConsentStatusUseCase;
import com.umc.product.term.application.port.in.query.GetTermAgreementUseCase;
import com.umc.product.term.application.port.in.query.GetTermUseCase;
import com.umc.product.term.domain.enums.TermType;

import lombok.RequiredArgsConstructor;

@Controller
@RequiredArgsConstructor
public class TermGraphQlController {

    private final GetTermUseCase getTermUseCase;
    private final GetTermAgreementUseCase getTermAgreementUseCase;
    private final GetRequiredTermConsentStatusUseCase getRequiredTermConsentStatusUseCase;
    private final ManageTermAgreementUseCase manageTermAgreementUseCase;
    private final ManageTermUseCase manageTermUseCase;

    @QueryMapping
    public List<TermGraphQlResponse> terms() {
        return getTermUseCase.listActiveTerms().stream().map(TermGraphQlResponse::from).toList();
    }

    @QueryMapping
    public TermGraphQlResponse term(@Argument Long id) {
        return TermGraphQlResponse.from(getTermUseCase.getTermsById(id));
    }

    @QueryMapping
    public TermGraphQlResponse termByType(@Argument TermType type) {
        return TermGraphQlResponse.from(getTermUseCase.getTermsByType(type));
    }

    @QueryMapping
    public List<TermGraphQlResponse> myAgreedTerms(@CurrentMember MemberPrincipal principal) {
        return getTermAgreementUseCase.getAgreedTermsByMemberId(principal.getMemberId()).stream()
            .map(TermGraphQlResponse::from)
            .toList();
    }

    @QueryMapping
    public RequiredTermConsentStatusGraphQlResponse myRequiredTermConsentStatus(
        @CurrentMember MemberPrincipal principal
    ) {
        return requiredConsentStatus(principal.getMemberId());
    }

    @MutationMapping
    public RequiredTermConsentStatusGraphQlResponse setTermConsent(
        @CurrentMember MemberPrincipal principal,
        @Argument Long termId,
        @Argument boolean agreed
    ) {
        manageTermAgreementUseCase.createTermConsent(CreateTermConsentCommand.builder()
            .memberId(principal.getMemberId())
            .termId(termId)
            .isAgreed(agreed)
            .build());
        return requiredConsentStatus(principal.getMemberId());
    }

    @MutationMapping
    @CheckAccess(resourceType = ResourceType.TERM, permission = PermissionType.WRITE)
    public TermGraphQlResponse createTerm(@Argument CreateTermGraphQlRequest input) {
        Long termId = manageTermUseCase.createTerms(input.toCommand());
        return TermGraphQlResponse.from(getTermUseCase.getTermsById(termId));
    }

    private RequiredTermConsentStatusGraphQlResponse requiredConsentStatus(Long memberId) {
        return RequiredTermConsentStatusGraphQlResponse.from(
            getRequiredTermConsentStatusUseCase.getRequiredTermConsentStatus(memberId)
        );
    }
}
