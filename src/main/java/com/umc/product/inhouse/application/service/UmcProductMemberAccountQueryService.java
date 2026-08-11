package com.umc.product.inhouse.application.service;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.umc.product.authentication.domain.CredentialPolicy;
import com.umc.product.inhouse.application.port.in.query.GetUmcProductMemberAccountUseCase;
import com.umc.product.inhouse.application.port.in.query.dto.UmcProductAccountCandidateInfo;
import com.umc.product.inhouse.application.port.in.query.dto.UmcProductMemberAccountInfo;
import com.umc.product.inhouse.application.port.out.query.LoadUmcProductMemberAccountPort;
import com.umc.product.inhouse.application.port.out.query.LoadUmcProductMemberPort;
import com.umc.product.inhouse.domain.UmcProductMemberAccount;
import com.umc.product.inhouse.exception.InhouseDomainException;
import com.umc.product.inhouse.exception.InhouseErrorCode;
import com.umc.product.member.application.port.in.query.GetMemberUseCase;
import com.umc.product.member.application.port.in.query.dto.MemberInfo;
import com.umc.product.member.domain.exception.MemberDomainException;
import com.umc.product.member.domain.exception.MemberErrorCode;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UmcProductMemberAccountQueryService implements GetUmcProductMemberAccountUseCase {

    private final LoadUmcProductMemberPort loadUmcProductMemberPort;
    private final LoadUmcProductMemberAccountPort loadUmcProductMemberAccountPort;
    private final GetMemberUseCase getMemberUseCase;
    private final UmcProductAccessPolicy umcProductAccessPolicy;

    @Override
    public List<UmcProductMemberAccountInfo> listAccounts(Long requesterMemberId, Long umcProductMemberId) {
        validateCanManage(requesterMemberId);
        loadUmcProductMemberPort.getById(umcProductMemberId);
        List<UmcProductMemberAccount> accounts = loadUmcProductMemberAccountPort
            .listByUmcProductMemberId(umcProductMemberId);
        Set<Long> memberIds = accounts.stream()
            .map(UmcProductMemberAccount::getMemberId)
            .collect(Collectors.toSet());
        Map<Long, MemberInfo> memberInfos = getMemberUseCase.findAllByIds(memberIds);
        return accounts.stream()
            .map(account -> toInfo(account, requireMemberInfo(memberInfos, account.getMemberId())))
            .toList();
    }

    @Override
    public Optional<UmcProductAccountCandidateInfo> findCandidateByEmail(
        Long requesterMemberId,
        String email
    ) {
        validateCanManage(requesterMemberId);
        String normalizedEmail = normalizeEmail(email);
        CredentialPolicy.validateEmail(normalizedEmail);
        return getMemberUseCase.findByEmail(normalizedEmail)
            .map(member -> new UmcProductAccountCandidateInfo(
                member.id(),
                member.name(),
                member.nickname(),
                member.email(),
                member.profileImageLink(),
                loadUmcProductMemberAccountPort.existsByMemberId(member.id())
            ));
    }

    private UmcProductMemberAccountInfo toInfo(UmcProductMemberAccount account, MemberInfo member) {
        return new UmcProductMemberAccountInfo(
            member.id(),
            member.name(),
            member.nickname(),
            member.email(),
            member.profileImageLink(),
            account.getAccountType()
        );
    }

    private MemberInfo requireMemberInfo(Map<Long, MemberInfo> memberInfos, Long memberId) {
        MemberInfo memberInfo = memberInfos.get(memberId);
        if (memberInfo == null) {
            throw new MemberDomainException(MemberErrorCode.MEMBER_NOT_FOUND);
        }
        return memberInfo;
    }

    private void validateCanManage(Long requesterMemberId) {
        if (!umcProductAccessPolicy.canManageUmcProduct(requesterMemberId)) {
            throw new InhouseDomainException(InhouseErrorCode.UMC_PRODUCT_ACCESS_DENIED);
        }
    }

    private String normalizeEmail(String email) {
        return email == null ? null : email.trim().toLowerCase(Locale.ROOT);
    }
}
