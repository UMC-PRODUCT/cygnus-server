package com.umc.product.common.domain.enums;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.umc.product.challenger.domain.exception.ChallengerDomainException;
import com.umc.product.common.domain.exception.CommonException;
import com.umc.product.global.exception.constant.CommonErrorCode;

@DisplayName("공통 enum 변환 경계")
class CommonEnumResidualTest {

    @Test
    @DisplayName("챌린저 파트는 정확한 이름만 변환한다")
    void 챌린저_파트를_변환한다() {
        assertThat(ChallengerPart.from("SPRINGBOOT")).isEqualTo(ChallengerPart.SPRINGBOOT);
        assertThatThrownBy(() -> ChallengerPart.from(null)).isInstanceOf(ChallengerDomainException.class);
        assertThatThrownBy(() -> ChallengerPart.from(" ")).isInstanceOf(ChallengerDomainException.class);
        assertThatThrownBy(() -> ChallengerPart.from("springboot"))
            .isInstanceOf(ChallengerDomainException.class);
    }

    @Test
    @DisplayName("OAuth provider는 대소문자를 정규화하고 미지원 값을 거부한다")
    void OAuth_provider를_변환한다() {
        assertThat(OAuthProvider.from("GOOGLE")).isEqualTo(OAuthProvider.GOOGLE);
        assertThat(OAuthProvider.from("apple")).isEqualTo(OAuthProvider.APPLE);
        assertThat(OAuthProvider.from("Kakao")).isEqualTo(OAuthProvider.KAKAO);
        assertThatThrownBy(() -> OAuthProvider.from("github")).isInstanceOf(CommonException.class);
    }

    @Test
    @DisplayName("공통 예외는 호출자가 지정한 안전한 메시지를 보존한다")
    void 공통_예외의_메시지를_보존한다() {
        CommonException exception = new CommonException(CommonErrorCode.BAD_REQUEST, "잘못된 입력");

        assertThat(exception.getMessage()).isEqualTo("잘못된 입력");
        assertThat(exception.getBaseCode()).isEqualTo(CommonErrorCode.BAD_REQUEST);
    }
}
