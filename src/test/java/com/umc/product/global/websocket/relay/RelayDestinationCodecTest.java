package com.umc.product.global.websocket.relay;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

@DisplayName("RelayDestinationCodec")
class RelayDestinationCodecTest {

    private final RelayDestinationCodec sut = new RelayDestinationCodec();

    @Test
    @DisplayName("Spring user destination system 경로만 broker key로 가역 변환한다")
    void translateSystemBroadcastDestinations() {
        assertThat(sut.toBroker("/topic/__internal/user-destination"))
            .isEqualTo("/topic/__internal.user-destination");
        assertThat(sut.toBroker("/topic/__internal/user-registry"))
            .isEqualTo("/topic/__internal.user-registry");
        assertThat(sut.toPublic("/topic/__internal.user-destination"))
            .isEqualTo("/topic/__internal/user-destination");
        assertThat(sut.toPublic("/topic/__internal.user-registry"))
            .isEqualTo("/topic/__internal/user-registry");
    }

    @Test
    @DisplayName("Community user destination의 session queue를 slash 없는 broker key로 가역 변환한다")
    void translateResolvedCommunityUserQueue() {
        String publicDestination = "/queue/community/threads/events-userabc-123";

        String brokerDestination = sut.toBroker(publicDestination);

        assertThat(brokerDestination)
            .isEqualTo("/queue/community.threads.events-userabc-123");
        assertThat(brokerDestination.substring("/queue/".length())).doesNotContain("/");
        assertThat(sut.toPublic(brokerDestination)).isEqualTo(publicDestination);
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {
        "",
        "/user/queue/community/threads/events",
        "/queue/community/threads/events-user",
        "/topic/community/threads/12/members/41/events",
        "/topic/community/members/41/events",
        "/topic/community/unowned/path",
        "/topic/__relay.not-base64!"
    })
    @DisplayName("user queue와 legacy community topic을 별도로 변환하지 않는다")
    void leaveApplicationDestinationsUnchanged(String destination) {
        assertThat(sut.toBroker(destination)).isSameAs(destination);
        assertThat(sut.toPublic(destination)).isSameAs(destination);
    }
}
