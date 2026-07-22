package com.umc.product.notification.adapter.out.external.webhook;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.ExpectedCount.once;
import static org.springframework.test.web.client.ExpectedCount.twice;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import com.umc.product.notification.domain.WebhookPlatform;

@DisplayName("SlackWebhookAdapter")
class SlackWebhookAdapterTest {

    private static final String WEBHOOK_URL = "https://slack.example.com/webhook";

    @Test
    @DisplayName("단문은 제목을 강조한 하나의 메시지로 전송한다")
    void 단문은_하나의_메시지로_전송한다() {
        Fixture fixture = new Fixture();
        fixture.server.expect(once(), requestTo(WEBHOOK_URL))
            .andExpect(content().json("""
                {"text":"*배포 완료*\n정상 배포되었습니다."}
                """))
            .andRespond(withSuccess());

        fixture.adapter.send("배포 완료", "정상 배포되었습니다.");

        assertThat(fixture.adapter.platform()).isEqualTo(WebhookPlatform.SLACK);
        fixture.server.verify();
    }

    @Test
    @DisplayName("제목을 제외한 최대 길이를 넘는 본문은 개행을 우선해 분할한다")
    void 긴_본문은_개행을_우선해_분할한다() {
        Fixture fixture = new Fixture();
        fixture.server.expect(twice(), requestTo(WEBHOOK_URL)).andRespond(withSuccess());
        int maxContentLength = 3_000 - "제목".length() - 10;
        String content = "a".repeat(maxContentLength) + "\n나머지";

        fixture.adapter.send("제목", content);

        fixture.server.verify();
    }

    @Test
    @DisplayName("제목만으로 여유 길이가 소진되면 기본 최대 길이로 본문을 계산한다")
    void 매우_긴_제목이면_기본_최대_길이를_사용한다() {
        Fixture fixture = new Fixture();
        fixture.server.expect(once(), requestTo(WEBHOOK_URL)).andRespond(withSuccess());

        fixture.adapter.send("제".repeat(3_000), "본문");

        fixture.server.verify();
    }

    @Test
    @DisplayName("개행이 없는 긴 본문도 최대 길이 기준으로 분할한다")
    void 개행이_없는_긴_본문도_분할한다() {
        Fixture fixture = new Fixture();
        fixture.server.expect(twice(), requestTo(WEBHOOK_URL)).andRespond(withSuccess());

        fixture.adapter.send("제목", "a".repeat(3_001));

        fixture.server.verify();
    }

    private static class Fixture {

        private final RestClient.Builder builder = RestClient.builder();
        private final MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        private final SlackWebhookAdapter adapter = new SlackWebhookAdapter(builder.build(), WEBHOOK_URL);
    }
}
