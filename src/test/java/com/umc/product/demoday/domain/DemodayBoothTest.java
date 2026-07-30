package com.umc.product.demoday.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.umc.product.demoday.domain.exception.DemodayDomainException;
import com.umc.product.demoday.domain.exception.DemodayErrorCode;

@DisplayName("DemodayBoothTest")
class DemodayBoothTest {

    private static final Long PROJECT_ID = 1L;

    private static DemodayVoteEvent createVoteEvent() {
        return DemodayVoteEvent.create(
            8L,
            "8기 데모데이",
            Instant.parse("2026-08-01T05:00:00Z"),
            Instant.parse("2026-08-01T08:00:00Z")
        );
    }

    @Test
    @DisplayName("등록된 프로젝트로 만든 부스는 프로젝트만 갖고 표시 이름은 비어 있다.")
    void 프로젝트_부스_생성() throws Exception {

        // given
        DemodayVoteEvent voteEvent = createVoteEvent();
        DemodayBooth demodayBooth = DemodayBooth.forProject(voteEvent, PROJECT_ID);

        // when & then
        assertThat(demodayBooth.getVoteEvent()).isSameAs(voteEvent);
        assertThat(demodayBooth.getProjectId()).isEqualTo(PROJECT_ID);
        assertThat(demodayBooth.getDisplayName()).isNull();
    }

    @Test
    @DisplayName("외부 부스 생성은 프로젝트는 비어 있고 부스 이름은 갖고 있다.")
    void 외부_부스_생성() {
        //given
        DemodayVoteEvent voteEvent = createVoteEvent();
        DemodayBooth demodayBooth = DemodayBooth.forExternal(voteEvent, "external");

        //when & then
        assertThat(demodayBooth.getVoteEvent()).isSameAs(voteEvent);
        assertThat(demodayBooth.getDisplayName()).isEqualTo("external");
        assertThat(demodayBooth.getProjectId()).isNull();
    }

    @Test
    @DisplayName("외부 부스 생성시 부스 이름이 비어 있으면 안된다.")
    void 외부_부스_이름_생성() {
        //when & then
        assertThatThrownBy(() ->DemodayBooth.forExternal(createVoteEvent(), null))
            .isInstanceOf(DemodayDomainException.class)
            .extracting("baseCode")
            .isEqualTo(DemodayErrorCode.DEMODAY_BOOTH_INVALID_NAME);

        assertThatThrownBy(() -> DemodayBooth.forExternal(createVoteEvent(), " "))
            .isInstanceOf(DemodayDomainException.class)
            .extracting("baseCode")
            .isEqualTo(DemodayErrorCode.DEMODAY_BOOTH_INVALID_NAME);
    }

    @Test
    @DisplayName("부스 이른은 255자까지 허용한다.")
    void 부스_이름_길이_경계() {
        // given
        String maxLength = "가".repeat(255);
        String tooLong = "가".repeat(256);

        // when & then
        assertThatCode(() -> DemodayBooth.forExternal(createVoteEvent(), maxLength))
            .doesNotThrowAnyException();

        assertThatThrownBy(() -> DemodayBooth.forExternal(createVoteEvent(), tooLong))
            .isInstanceOf(DemodayDomainException.class)
            .extracting("baseCode")
            .isEqualTo(DemodayErrorCode.DEMODAY_BOOTH_INVALID_NAME);
    }

}
