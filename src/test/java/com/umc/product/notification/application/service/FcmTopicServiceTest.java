package com.umc.product.notification.application.service;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("FcmTopicService deprecated no-op")
class FcmTopicServiceTest {

    @Test
    @DisplayName("모든 legacy topic API는 외부 부작용 없이 호출 가능하다")
    void all_legacy_operations_are_noop() {
        FcmTopicService sut = new FcmTopicService();
        sut.subscribeAllTopicsByMemberId(1L);
        sut.unsubscribeAllTopicsByMemberId(1L);
        sut.unsubscribeTokenFromTopics("token", 1L);
        sut.unsubscribeLegacyTopics(1L);
        sut.subscribeToTopic(List.of("token"), "topic");
        sut.unsubscribeFromTopic(List.of("token"), "topic");
        sut.resubscribeAllLegacyTopics();
    }
}
