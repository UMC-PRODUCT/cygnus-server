package com.umc.product.challenger.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import com.umc.product.challenger.domain.exception.ChallengerDomainException;
import com.umc.product.challenger.domain.exception.ChallengerErrorCode;
import com.umc.product.common.domain.enums.ChallengerPart;
import com.umc.product.common.domain.enums.ChallengerStatus;
import com.umc.product.common.domain.enums.ChallengerTrack;

@DisplayName("Challenger 도메인")
class ChallengerTest {
    Challenger challenger;

    @BeforeEach
    void setUp() {
        challenger = Challenger.builder()
                .memberId(1L)
                .part(ChallengerPart.SPRINGBOOT)
                .gisuId(9L)
                .build();
    }

    @Test
    void 챌린저_생성_시_기본적으로_활성화_상태이다() {
        assertThat(challenger.getStatus()).isEqualTo(ChallengerStatus.ACTIVE);
    }

    @Test
    @DisplayName("기존 파트만 가진 챌린저는 파트 정책으로 트랙을 해석한다")
    void 기존_파트만_가진_챌린저는_파트_정책으로_트랙을_해석한다() {
        assertThat(challenger.getTrack()).isNull();
        assertThat(challenger.getEffectiveTrack()).isEqualTo(ChallengerTrack.WEB_PRODUCT_ENGINEER);
    }

    @Test
    @DisplayName("트랙 정책 챌린저는 파트 없이 트랙으로 생성된다")
    void 트랙_정책_챌린저는_파트_없이_트랙으로_생성된다() {
        Challenger trackBasedChallenger = Challenger.builder()
            .memberId(1L)
            .track(ChallengerTrack.MOBILE_PRODUCT_ENGINEER)
            .gisuId(9L)
            .build();

        assertThat(trackBasedChallenger.getPart()).isNull();
        assertThat(trackBasedChallenger.getTrack()).isEqualTo(ChallengerTrack.MOBILE_PRODUCT_ENGINEER);
        assertThat(trackBasedChallenger.getEffectiveTrack()).isEqualTo(ChallengerTrack.MOBILE_PRODUCT_ENGINEER);
    }

    @Test
    @DisplayName("파트와 트랙이 모두 없으면 챌린저를 생성할 수 없다")
    void 파트와_트랙이_모두_없으면_챌린저를_생성할_수_없다() {
        assertThatThrownBy(() -> Challenger.builder()
            .memberId(1L)
            .gisuId(9L)
            .build())
            .isInstanceOf(ChallengerDomainException.class)
            .extracting("baseCode")
            .isEqualTo(ChallengerErrorCode.CHALLENGER_PART_NOT_FOUND);
    }

    @Nested
    @DisplayName("changePart")
    class ChangePart {

        @Test
        @DisplayName("ACTIVE 챌린저는 파트를 변경할 수 있다")
        void ACTIVE_챌린저는_파트를_변경할_수_있다() {
            challenger.changePart(ChallengerPart.WEB);

            assertThat(challenger.getPart()).isEqualTo(ChallengerPart.WEB);
        }

        @Test
        @DisplayName("ACTIVE 상태가 아니면 파트를 변경할 수 없다")
        void ACTIVE_상태가_아니면_파트를_변경할_수_없다() {
            challenger.changeStatus(ChallengerStatus.WITHDRAWN, 99L, "탈부");

            assertThatThrownBy(() -> challenger.changePart(ChallengerPart.WEB))
                .isInstanceOf(ChallengerDomainException.class)
                .extracting("baseCode")
                .isEqualTo(ChallengerErrorCode.CHALLENGER_NOT_ACTIVE);
        }
    }

    @Nested
    @DisplayName("changeStatus")
    class ChangeStatus {

        @Test
        @DisplayName("ACTIVE 챌린저는 상태 변경 사유와 수정자를 기록한다")
        void ACTIVE_챌린저는_상태_변경_사유와_수정자를_기록한다() {
            challenger.changeStatus(ChallengerStatus.EXPELLED, 99L, "징계");

            assertThat(challenger.getStatus()).isEqualTo(ChallengerStatus.EXPELLED);
            assertThat(challenger.getModifiedBy()).isEqualTo(99L);
            assertThat(challenger.getModificationReason()).isEqualTo("징계");
        }

        @Test
        @DisplayName("ACTIVE 상태가 아니면 다시 상태를 변경할 수 없다")
        void ACTIVE_상태가_아니면_다시_상태를_변경할_수_없다() {
            challenger.changeStatus(ChallengerStatus.WITHDRAWN, 99L, "탈부");

            assertThatThrownBy(() -> challenger.changeStatus(ChallengerStatus.EXPELLED, 100L, "징계"))
                .isInstanceOf(ChallengerDomainException.class)
                .extracting("baseCode")
                .isEqualTo(ChallengerErrorCode.CHALLENGER_NOT_ACTIVE);
        }
    }

    @Test
    @DisplayName("상벌점 총합을 계산한다")
    void 상벌점_총합을_계산한다() {
        challenger.addPoint(ChallengerPoint.create(
            challenger,
            com.umc.product.challenger.domain.enums.PointType.BEST_WORKBOOK,
            "베스트 워크북"
        ));
        challenger.addPoint(ChallengerPoint.create(
            challenger,
            com.umc.product.challenger.domain.enums.PointType.CUSTOM,
            -3,
            "운영진 조정"
        ));

        assertThat(challenger.getTotalPoints()).isEqualTo(-3.5);
    }
}
