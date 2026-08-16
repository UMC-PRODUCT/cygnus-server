package com.umc.product.challenger.adapter.out.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.umc.product.challenger.domain.Challenger;
import com.umc.product.common.domain.enums.ChallengerPart;
import com.umc.product.common.domain.enums.ChallengerTrack;
import com.umc.product.support.IntegrationTestSupport;

@DisplayName("Challenger tracks JPA 통합 테스트")
class ChallengerTracksPersistenceIntegrationTest extends IntegrationTestSupport {

    @Autowired
    ChallengerJpaRepository repository;

    @Test
    @DisplayName("비어 있지 않은 다중 enum 배열을 저장하고 같은 순서로 조회한다")
    void 비어_있지_않은_다중_enum_배열을_저장하고_같은_순서로_조회한다() {
        Challenger saved = repository.saveAndFlush(Challenger.builder()
            .memberId(88001L)
            .tracks(List.of(
                ChallengerTrack.INFRA_PLUS,
                ChallengerTrack.WEB_PRODUCT_ENGINEER,
                ChallengerTrack.MOBILE_PRODUCT_ENGINEER
            ))
            .gisuId(99001L)
            .build());
        entityManager.clear();

        Challenger found = repository.findById(saved.getId()).orElseThrow();

        assertThat(found.getTracks()).containsExactly(
            ChallengerTrack.INFRA_PLUS,
            ChallengerTrack.WEB_PRODUCT_ENGINEER,
            ChallengerTrack.MOBILE_PRODUCT_ENGINEER
        );
        assertThat(found.getEffectiveTracks()).isEqualTo(found.getTracks());
    }

    @Test
    @DisplayName("기존 part 기반 Challenger는 빈 tracks로 저장되고 fallback을 유지한다")
    void 기존_part_기반_Challenger는_빈_tracks로_저장되고_fallback을_유지한다() {
        Challenger saved = repository.saveAndFlush(Challenger.builder()
            .memberId(88002L)
            .part(ChallengerPart.SPRINGBOOT)
            .gisuId(99001L)
            .build());
        entityManager.clear();

        Challenger found = repository.findById(saved.getId()).orElseThrow();

        assertThat(found.getPart()).isEqualTo(ChallengerPart.SPRINGBOOT);
        assertThat(found.getTracks()).isEmpty();
        assertThat(found.getEffectiveTracks()).containsExactly(ChallengerTrack.WEB_PRODUCT_ENGINEER);
    }
}
