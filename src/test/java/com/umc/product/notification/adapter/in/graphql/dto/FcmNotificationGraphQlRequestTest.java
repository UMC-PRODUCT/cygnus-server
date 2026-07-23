package com.umc.product.notification.adapter.in.graphql.dto;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.umc.product.common.domain.enums.ChallengerPart;

class FcmNotificationGraphQlRequestTest {

    @Test
    @DisplayName("FCM 발송 요청은 대상이 하나 이상이어야 한다")
    void notificationTargetIsRequired() {
        var request = new FcmNotificationGraphQlRequest(
            new FcmNotificationGraphQlRequest.Target(List.of(), null, null, null, Set.of()),
            new FcmNotificationGraphQlRequest.Message("title", "body", List.of(), null, null)
        );

        assertThatThrownBy(() -> request.toCommand(1L))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("대상");
    }

    @Test
    @DisplayName("GraphQL key-value 목록을 FCM data map으로 변환한다")
    void keyValueListIsConvertedToDataMap() {
        var request = new FcmNotificationGraphQlRequest(
            new FcmNotificationGraphQlRequest.Target(
                List.of(2L),
                null,
                null,
                null,
                Set.of(ChallengerPart.SPRINGBOOT)
            ),
            new FcmNotificationGraphQlRequest.Message(
                "title",
                "body",
                List.of(new FcmNotificationGraphQlRequest.KeyValue("screen", "notice")),
                null,
                "umc://notice/1"
            )
        );

        var command = request.toCommand(1L);

        assertThat(command.requesterMemberId()).isEqualTo(1L);
        assertThat(command.data()).containsEntry("screen", "notice");
        assertThat(command.targetParts()).containsExactly(ChallengerPart.SPRINGBOOT);
    }
}
