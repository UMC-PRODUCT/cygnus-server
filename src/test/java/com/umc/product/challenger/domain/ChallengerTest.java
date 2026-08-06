package com.umc.product.challenger.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

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
        assertThat(challenger.getTracks()).isEmpty();
        assertThat(challenger.getEffectiveTracks()).containsExactly(ChallengerTrack.WEB_PRODUCT_ENGINEER);
    }

    @Test
    @DisplayName("트랙 정책 챌린저는 파트 없이 여러 트랙으로 생성된다")
    void 트랙_정책_챌린저는_파트_없이_여러_트랙으로_생성된다() {
        Challenger trackBasedChallenger = Challenger.builder()
            .memberId(1L)
            .tracks(List.of(
                ChallengerTrack.WEB_PRODUCT_ENGINEER,
                ChallengerTrack.MOBILE_PRODUCT_ENGINEER
            ))
            .gisuId(9L)
            .build();

        assertThat(trackBasedChallenger.getPart()).isNull();
        assertThat(trackBasedChallenger.getTracks()).containsExactly(
            ChallengerTrack.WEB_PRODUCT_ENGINEER,
            ChallengerTrack.MOBILE_PRODUCT_ENGINEER
        );
        assertThat(trackBasedChallenger.getEffectiveTracks()).containsExactly(
            ChallengerTrack.WEB_PRODUCT_ENGINEER,
            ChallengerTrack.MOBILE_PRODUCT_ENGINEER
        );
    }

    @Test
    @DisplayName("운영진 파트에 트랙이 없으면 유효 트랙은 빈 목록이다")
    void 운영진_파트에_트랙이_없으면_유효_트랙은_빈_목록이다() {
        Challenger admin = Challenger.builder()
            .memberId(1L)
            .part(ChallengerPart.ADMIN)
            .tracks(List.of())
            .gisuId(9L)
            .build();

        assertThat(admin.getEffectiveTracks()).isEmpty();
    }

    @Test
    @DisplayName("파트와 저장 트랙이 모두 없는 기존 데이터의 유효 트랙은 빈 목록이다")
    void 파트와_저장_트랙이_모두_없는_기존_데이터의_유효_트랙은_빈_목록이다() {
        Challenger legacy = Challenger.builder()
            .memberId(1L)
            .tracks(List.of(ChallengerTrack.PLAN))
            .gisuId(9L)
            .build();
        ReflectionTestUtils.setField(legacy, "tracks", new ArrayList<>());

        assertThat(legacy.getEffectiveTracks()).isEmpty();
    }

    @Test
    @DisplayName("생성 시 중복 트랙은 입력 순서를 유지하며 제거한다")
    void 생성_시_중복_트랙은_입력_순서를_유지하며_제거한다() {
        Challenger duplicated = Challenger.builder()
            .memberId(1L)
            .tracks(List.of(
                ChallengerTrack.WEB_PRODUCT_ENGINEER,
                ChallengerTrack.MOBILE_PRODUCT_ENGINEER,
                ChallengerTrack.WEB_PRODUCT_ENGINEER
            ))
            .gisuId(9L)
            .build();

        assertThat(duplicated.getTracks()).containsExactly(
            ChallengerTrack.WEB_PRODUCT_ENGINEER,
            ChallengerTrack.MOBILE_PRODUCT_ENGINEER
        );
    }

    @Test
    @DisplayName("같은 트랙을 반복 추가해도 한 번만 보관한다")
    void 같은_트랙을_반복_추가해도_한_번만_보관한다() {
        challenger.addTrack(ChallengerTrack.WEB_PRODUCT_ENGINEER);
        challenger.addTrack(ChallengerTrack.WEB_PRODUCT_ENGINEER);

        assertThat(challenger.getTracks()).containsExactly(ChallengerTrack.WEB_PRODUCT_ENGINEER);
    }

    @Test
    @DisplayName("파트와 트랙이 모두 없으면 챌린저를 생성할 수 없다")
    void 파트와_트랙이_모두_없으면_챌린저를_생성할_수_없다() {
        assertThatThrownBy(() -> Challenger.builder()
            .memberId(1L)
            .tracks(List.of())
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

}
