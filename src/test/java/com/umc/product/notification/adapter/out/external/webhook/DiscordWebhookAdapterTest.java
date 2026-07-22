package com.umc.product.notification.adapter.out.external.webhook;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.ExpectedCount.once;
import static org.springframework.test.web.client.ExpectedCount.twice;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import com.umc.product.notification.domain.WebhookPlatform;

@DisplayName("DiscordWebhookAdapter")
class DiscordWebhookAdapterTest {

    private static final String WEBHOOK_URL = "https://discord.example.com/webhook";

    @Test
    @DisplayName("단문은 하나의 embed로 전송한다")
    void 단문은_하나의_embed로_전송한다() {
        Fixture fixture = new Fixture();
        fixture.server.expect(once(), requestTo(WEBHOOK_URL))
            .andExpect(method(HttpMethod.POST))
            .andExpect(content().json("""
                {"embeds":[{"title":"배포 완료","description":"정상 배포되었습니다."}]}
                """))
            .andRespond(withSuccess());

        fixture.adapter.send("배포 완료", "정상 배포되었습니다.");

        assertThat(fixture.adapter.platform()).isEqualTo(WebhookPlatform.DISCORD);
        fixture.server.verify();
    }

    @Test
    @DisplayName("최대 길이를 넘는 본문은 개행을 우선해 여러 embed로 분할한다")
    void 긴_본문은_개행을_우선해_분할한다() {
        Fixture fixture = new Fixture();
        fixture.server.expect(twice(), requestTo(WEBHOOK_URL))
            .andExpect(method(HttpMethod.POST))
            .andRespond(withSuccess());
        String content = "a".repeat(4_096) + "\n나머지";

        fixture.adapter.send("긴 알림", content);

        fixture.server.verify();
    }

    @Test
    @DisplayName("개행이 없는 긴 본문도 최대 길이 기준으로 분할한다")
    void 개행이_없는_긴_본문도_분할한다() {
        Fixture fixture = new Fixture();
        fixture.server.expect(twice(), requestTo(WEBHOOK_URL)).andRespond(withSuccess());

        fixture.adapter.send("긴 알림", "a".repeat(4_097));

        fixture.server.verify();
    }

    private static class Fixture {

        private final RestClient.Builder builder = RestClient.builder();
        private final MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        private final DiscordWebhookAdapter adapter = new DiscordWebhookAdapter(builder.build(), WEBHOOK_URL);
    }
}
