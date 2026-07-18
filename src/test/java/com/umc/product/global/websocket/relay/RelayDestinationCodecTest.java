package com.umc.product.global.websocket.relay;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

@DisplayName("RelayDestinationCodec")
class RelayDestinationCodecTest {

    private static final String TOPIC_PREFIX = "/topic/";

    private final RelayDestinationCodec sut = new RelayDestinationCodec();

    @ParameterizedTest
    @ValueSource(strings = {
        "/topic/community/threads/12/members/41/events",
        "/topic/community/members/41/events"
    })
    @DisplayName("소유한 공개 community topic을 slash 없는 broker key로 가역 변환한다")
    void translateOwnedCommunityTopicReversibly(String publicDestination) {
        String brokerDestination = sut.toBroker(publicDestination);

        assertThat(brokerDestination).startsWith(TOPIC_PREFIX);
        assertThat(brokerDestination.substring(TOPIC_PREFIX.length())).doesNotContain("/");
        assertThat(sut.toPublic(brokerDestination)).isEqualTo(publicDestination);
    }

    @Test
    @DisplayName("서로 다른 공개 topic은 충돌하지 않는 broker key를 사용한다")
    void avoidBrokerKeyCollision() {
        String first = sut.toBroker("/topic/community/threads/1/members/23/events");
        String second = sut.toBroker("/topic/community/threads/12/members/3/events");

        assertThat(first).isNotEqualTo(second);
    }

    @Test
    @DisplayName("Spring system broadcast 공개 경로를 고정된 slash 없는 broker key로 변환한다")
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

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {
        "",
        "/queue/community/threads/12/members/41/events",
        "/topic/community/threads/0/members/41/events",
        "/topic/community/threads/12/members/01/events",
        "/topic/community/unowned/path"
    })
    @DisplayName("null, invalid 또는 소유하지 않은 destination은 변환하지 않는다")
    void leaveInvalidOrUnownedDestinationUnchanged(String destination) {
        assertThat(sut.toBroker(destination)).isSameAs(destination);
        assertThat(sut.toPublic(destination)).isSameAs(destination);
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "/topic/__relay.not-base64!",
        "/topic/__relay.L3RvcGljL2NvbW11bml0eS91bm93bmVkL3BhdGg",
        "/topic/__relay."
    })
    @DisplayName("비정상 또는 소유하지 않은 internal key는 공개 경로로 복원하지 않는다")
    void leaveInvalidInternalKeyUnchanged(String destination) {
        assertThat(sut.toPublic(destination)).isSameAs(destination);
    }
}
