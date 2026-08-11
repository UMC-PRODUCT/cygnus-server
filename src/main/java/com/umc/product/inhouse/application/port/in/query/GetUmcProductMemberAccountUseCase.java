package com.umc.product.inhouse.application.port.in.query;

import java.util.List;
import java.util.Optional;

import com.umc.product.inhouse.application.port.in.query.dto.UmcProductAccountCandidateInfo;
import com.umc.product.inhouse.application.port.in.query.dto.UmcProductMemberAccountInfo;

public interface GetUmcProductMemberAccountUseCase {

    List<UmcProductMemberAccountInfo> listAccounts(Long requesterMemberId, Long umcProductMemberId);

    Optional<UmcProductAccountCandidateInfo> findCandidateByEmail(Long requesterMemberId, String email);
}
