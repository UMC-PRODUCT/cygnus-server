package com.umc.product.notification.adapter.out.external.webhook;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.ExpectedCount.once;
import static org.springframework.test.web.client.ExpectedCount.twice;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import com.umc.product.notification.domain.WebhookPlatform;

@DisplayName("TelegramWebhookAdapter")
class TelegramWebhookAdapterTest {

    private static final String REQUEST_URL = "https://api.telegram.org/botbot-token/sendMessage";

    @Test
    @DisplayName("Markdown 예약 문자를 escape해 지정한 채팅방으로 전송한다")
    void markdown_예약_문자를_escape해_전송한다() {
        Fixture fixture = new Fixture();
        fixture.server.expect(once(), requestTo(REQUEST_URL))
            .andExpect(jsonPath("$.chat_id").value("chat-id"))
            .andExpect(jsonPath("$.text").value("*배포\\.완료*\n상태\\_정상"))
            .andExpect(jsonPath("$.parse_mode").value("MarkdownV2"))
            .andRespond(withSuccess());

        fixture.adapter.send("배포.완료", "상태_정상");

        assertThat(fixture.adapter.platform()).isEqualTo(WebhookPlatform.TELEGRAM);
        fixture.server.verify();
    }

    @Test
    @DisplayName("최대 길이를 넘는 본문은 escape 이후 개행을 우선해 분할한다")
    void 긴_본문은_escape_후_개행을_우선해_분할한다() {
        Fixture fixture = new Fixture();
        fixture.server.expect(twice(), requestTo(REQUEST_URL)).andRespond(withSuccess());
        int maxContentLength = 4_096 - "제목".length() - 10;
        String content = "a".repeat(maxContentLength) + "\n나머지";

        fixture.adapter.send("제목", content);

        fixture.server.verify();
    }

    @Test
    @DisplayName("제목 escape 결과가 여유 길이를 소진하면 기본 최대 길이를 사용한다")
    void 매우_긴_제목이면_기본_최대_길이를_사용한다() {
        Fixture fixture = new Fixture();
        fixture.server.expect(once(), requestTo(REQUEST_URL)).andRespond(withSuccess());

        fixture.adapter.send(".".repeat(4_096), "본문");

        fixture.server.verify();
    }

    @Test
    @DisplayName("개행이 없는 긴 본문도 최대 길이 기준으로 분할한다")
    void 개행이_없는_긴_본문도_분할한다() {
        Fixture fixture = new Fixture();
        fixture.server.expect(twice(), requestTo(REQUEST_URL)).andRespond(withSuccess());

        fixture.adapter.send("제목", "a".repeat(4_097));

        fixture.server.verify();
    }

    private static class Fixture {

        private final RestClient.Builder builder = RestClient.builder();
        private final MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        private final TelegramWebhookAdapter adapter = new TelegramWebhookAdapter(
            builder.build(),
            "bot-token",
            "chat-id"
        );
    }
}
