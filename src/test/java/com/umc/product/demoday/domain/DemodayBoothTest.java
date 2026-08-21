package com.umc.product.demoday.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.umc.product.demoday.domain.exception.DemodayDomainException;
import com.umc.product.demoday.domain.exception.DemodayErrorCode;

@DisplayName("DemodayBoothTest")
class DemodayBoothTest {

    private static final Long POLL_ID = 1L;
    private static final Integer BOOTH_CODE = 11;
    private static final Long PROJECT_ID = 1L;

    @Test
    @DisplayName("등록된 프로젝트로 만든 부스는 프로젝트만 갖고 표시 이름은 비어 있다.")
    void createProjectBooth() {

        // given
        DemodayBooth demodayBooth = DemodayBooth.forProject(POLL_ID, BOOTH_CODE, PROJECT_ID);

        // when & then
        assertThat(demodayBooth.getPollId()).isEqualTo(POLL_ID);
        assertThat(demodayBooth.getBoothCode()).isEqualTo(BOOTH_CODE);
        assertThat(demodayBooth.getProjectId()).isEqualTo(PROJECT_ID);
        assertThat(demodayBooth.getDisplayName()).isNull();
        assertThat(demodayBooth.isProjectBooth()).isTrue();
    }

    @Test
    @DisplayName("외부 부스 생성은 프로젝트는 비어 있고 부스 이름은 갖고 있다.")
    void createExternalBooth() {
        //given
        DemodayBooth demodayBooth = DemodayBooth.forExternal(POLL_ID, BOOTH_CODE, "external");

        //when & then
        assertThat(demodayBooth.getPollId()).isEqualTo(POLL_ID);
        assertThat(demodayBooth.getBoothCode()).isEqualTo(BOOTH_CODE);
        assertThat(demodayBooth.getDisplayName()).isEqualTo("external");
        assertThat(demodayBooth.getProjectId()).isNull();
        assertThat(demodayBooth.isProjectBooth()).isFalse();
    }

    @Test
    @DisplayName("외부 부스는 투표 대상으로 사용할 수 없다.")
    void rejectExternalBoothAsVoteTarget() {
        // given
        DemodayBooth externalBooth = DemodayBooth.forExternal(POLL_ID, BOOTH_CODE, "external");

        // when & then
        assertThatThrownBy(externalBooth::validateVoteTarget)
            .isInstanceOf(DemodayDomainException.class)
            .extracting("baseCode")
            .isEqualTo(DemodayErrorCode.DEMODAY_VOTE_EXTERNAL_BOOTH_NOT_ALLOWED);
    }

    @Test
    @DisplayName("부스는 투표 식별자 없이 만들 수 없다.")
    void rejectBoothWithoutPollId() {
        //when & then
        assertThatThrownBy(() -> DemodayBooth.forProject(null, BOOTH_CODE, PROJECT_ID))
            .isInstanceOf(NullPointerException.class);

        assertThatThrownBy(() -> DemodayBooth.forExternal(null, BOOTH_CODE, "external"))
            .isInstanceOf(NullPointerException.class);
    }

    @Test
    @DisplayName("부스 코드는 양의 정수여야 한다.")
    void rejectNonPositiveBoothCode() {
        // when & then
        assertInvalidBoothCode(null);
        assertInvalidBoothCode(0);
        assertInvalidBoothCode(-1);
    }

    @Test
    @DisplayName("외부 부스 생성시 부스 이름이 비어 있으면 안된다.")
    void rejectBlankExternalBoothName() {
        //when & then
        assertThatThrownBy(() -> DemodayBooth.forExternal(POLL_ID, BOOTH_CODE, null))
            .isInstanceOf(DemodayDomainException.class)
            .extracting("baseCode")
            .isEqualTo(DemodayErrorCode.DEMODAY_BOOTH_INVALID_NAME);

        assertThatThrownBy(() -> DemodayBooth.forExternal(POLL_ID, BOOTH_CODE, " "))
            .isInstanceOf(DemodayDomainException.class)
            .extracting("baseCode")
            .isEqualTo(DemodayErrorCode.DEMODAY_BOOTH_INVALID_NAME);
    }

    @Test
    @DisplayName("부스 이른은 255자까지 허용한다.")
    void validateBoothNameLengthBoundary() {
        // given
        String maxLength = "가".repeat(255);
        String tooLong = "가".repeat(256);

        // when & then
        assertThatCode(() -> DemodayBooth.forExternal(POLL_ID, BOOTH_CODE, maxLength))
            .doesNotThrowAnyException();

        assertThatThrownBy(() -> DemodayBooth.forExternal(POLL_ID, BOOTH_CODE, tooLong))
            .isInstanceOf(DemodayDomainException.class)
            .extracting("baseCode")
            .isEqualTo(DemodayErrorCode.DEMODAY_BOOTH_INVALID_NAME);
    }

    private void assertInvalidBoothCode(Integer boothCode) {
        assertThatThrownBy(() -> DemodayBooth.forProject(POLL_ID, boothCode, PROJECT_ID))
            .isInstanceOf(DemodayDomainException.class)
            .extracting("baseCode")
            .isEqualTo(DemodayErrorCode.DEMODAY_BOOTH_INVALID_CODE);
    }

}
