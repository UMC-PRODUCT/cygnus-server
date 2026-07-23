package com.umc.product.recruiting.application.port.in.query;

import java.util.List;

import com.umc.product.recruiting.application.port.in.query.dto.RecruitingPublicRoundGroupInfo;
import com.umc.product.recruiting.application.port.in.query.dto.RecruitingPublicRoundSearchQuery;

public interface SearchPublicRecruitingRoundUseCase {

    List<RecruitingPublicRoundGroupInfo> searchPublicRounds(RecruitingPublicRoundSearchQuery query);
}
