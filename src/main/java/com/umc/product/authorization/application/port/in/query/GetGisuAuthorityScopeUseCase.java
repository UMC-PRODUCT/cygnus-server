package com.umc.product.authorization.application.port.in.query;

import com.umc.product.authorization.application.port.in.query.dto.GisuAuthorityScopeInfo;

public interface GetGisuAuthorityScopeUseCase {

    GisuAuthorityScopeInfo getByMemberIdAndGisuId(Long memberId, Long gisuId);
}
