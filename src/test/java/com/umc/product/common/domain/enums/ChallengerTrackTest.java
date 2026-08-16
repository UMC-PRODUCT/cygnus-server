package com.umc.product.common.domain.enums;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.umc.product.challenger.domain.exception.ChallengerDomainException;
import com.umc.product.challenger.domain.exception.ChallengerErrorCode;

@DisplayName("ChallengerTrack")
class ChallengerTrackTest {

    @Test
    @DisplayName("기획과 디자인 파트는 같은 이름의 트랙으로 변환한다")
    void 기획과_디자인_파트는_같은_이름의_트랙으로_변환한다() {
        assertThat(ChallengerTrack.from(ChallengerPart.PLAN)).isEqualTo(ChallengerTrack.PLAN);
        assertThat(ChallengerTrack.from(ChallengerPart.DESIGN)).isEqualTo(ChallengerTrack.DESIGN);
    }

    @Test
    @DisplayName("웹 계열 개발 파트는 웹 프로덕트 엔지니어 트랙으로 변환한다")
    void 웹_계열_개발_파트는_웹_프로덕트_엔지니어_트랙으로_변환한다() {
        assertThat(ChallengerTrack.from(ChallengerPart.WEB)).isEqualTo(ChallengerTrack.WEB_PRODUCT_ENGINEER);
        assertThat(ChallengerTrack.from(ChallengerPart.NODEJS)).isEqualTo(ChallengerTrack.WEB_PRODUCT_ENGINEER);
        assertThat(ChallengerTrack.from(ChallengerPart.SPRINGBOOT)).isEqualTo(ChallengerTrack.WEB_PRODUCT_ENGINEER);
    }

    @Test
    @DisplayName("모바일 개발 파트는 모바일 프로덕트 엔지니어 트랙으로 변환한다")
    void 모바일_개발_파트는_모바일_프로덕트_엔지니어_트랙으로_변환한다() {
        assertThat(ChallengerTrack.from(ChallengerPart.ANDROID)).isEqualTo(ChallengerTrack.MOBILE_PRODUCT_ENGINEER);
        assertThat(ChallengerTrack.from(ChallengerPart.IOS)).isEqualTo(ChallengerTrack.MOBILE_PRODUCT_ENGINEER);
    }

    @Test
    @DisplayName("운영진 파트는 모집 트랙으로 변환할 수 없다")
    void 운영진_파트는_모집_트랙으로_변환할_수_없다() {
        assertThatThrownBy(() -> ChallengerTrack.from(ChallengerPart.ADMIN))
            .isInstanceOf(ChallengerDomainException.class)
            .extracting("baseCode")
            .isEqualTo(ChallengerErrorCode.CHALLENGER_PART_NOT_FOUND);
    }

    @Test
    @DisplayName("모집 트랙은 INFRA_CORE를 지원하지 않는다")
    void 모집_트랙은_INFRA_CORE를_지원하지_않는다() {
        assertThat(ChallengerTrack.values())
            .extracting(Enum::name)
            .doesNotContain("INFRA_CORE");
    }
}
