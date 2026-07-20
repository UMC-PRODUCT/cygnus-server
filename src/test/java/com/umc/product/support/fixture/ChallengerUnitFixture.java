package com.umc.product.support.fixture;

import org.springframework.test.util.ReflectionTestUtils;

import com.umc.product.challenger.application.port.out.dto.ChallengerSearchRow;
import com.umc.product.challenger.domain.Challenger;
import com.umc.product.common.domain.enums.ChallengerPart;
import com.umc.product.common.domain.enums.ChallengerStatus;

public final class ChallengerUnitFixture {

    private ChallengerUnitFixture() {
    }

    public static Challenger 챌린저(Long id, Long memberId, Long gisuId) {
        return 챌린저(id, memberId, gisuId, ChallengerPart.SPRINGBOOT, ChallengerStatus.ACTIVE);
    }

    public static Challenger 챌린저(
        Long id,
        Long memberId,
        Long gisuId,
        ChallengerPart part,
        ChallengerStatus status
    ) {
        Challenger challenger = new Challenger(memberId, part, gisuId);
        ReflectionTestUtils.setField(challenger, "id", id);
        ReflectionTestUtils.setField(challenger, "status", status);
        return challenger;
    }

    public static ChallengerSearchRow 검색_행(
        Long challengerId,
        Long memberId,
        Long gisuId,
        String profileImageId
    ) {
        return ChallengerSearchRow.builder()
            .challengerId(challengerId)
            .memberId(memberId)
            .gisuId(gisuId)
            .part(ChallengerPart.SPRINGBOOT)
            .status(ChallengerStatus.ACTIVE)
            .memberName("홍길동")
            .memberNickname("길동")
            .schoolName("테스트대학교")
            .profileImageId(profileImageId)
            .build();
    }
}
