package com.umc.product.demoday.application.service.command;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.umc.product.demoday.application.port.in.query.participant.MemberDemodayParticipant;
import com.umc.product.demoday.application.port.out.GenerateDemodayVoteAuthorizationPort;
import com.umc.product.demoday.application.port.out.LoadDemodayVotePort;
import com.umc.product.demoday.application.port.out.SaveDemodayBoothPort;
import com.umc.product.demoday.application.port.out.SaveDemodayPollPort;
import com.umc.product.demoday.domain.DemodayBooth;
import com.umc.product.demoday.domain.DemodayPoll;
import com.umc.product.demoday.domain.exception.DemodayErrorCode;
import com.umc.product.support.IntegrationTestSupport;

@DisplayName("데모데이 최종 투표 동시 제출")
class DemodayVoteSubmissionConcurrencyIntegrationTest extends IntegrationTestSupport {

    private static final Long MEMBER_ID = 91001L;
    private static final String ACCESS_TOKEN = "demoday-concurrency-access-token";

    @Autowired private SaveDemodayPollPort saveDemodayPollPort;
    @Autowired private SaveDemodayBoothPort saveDemodayBoothPort;
    @Autowired private LoadDemodayVotePort loadDemodayVotePort;
    @Autowired private GenerateDemodayVoteAuthorizationPort generateDemodayVoteAuthorizationPort;
    @Autowired private Clock clock;
    @Autowired private ObjectMapper objectMapper;

    @Test
    @DisplayName("같은 권한의 동시 두 요청은 201 하나와 DEMODAY-0402 하나만 반환한다")
    void acceptOnlyOneConcurrentSubmission() throws Exception {
        // given
        Instant now = clock.instant();
        DemodayPoll poll = saveDemodayPollPort.save(
            DemodayPoll.create(1L, "동시 제출 Poll", now.minusSeconds(60), now.plusSeconds(3600)));
        DemodayBooth booth = saveDemodayBoothPort.save(poll.registerExternalBooth(11, "동시 제출 부스"));
        poll.open();
        saveDemodayPollPort.save(poll);

        MemberDemodayParticipant participant = new MemberDemodayParticipant(MEMBER_ID);
        String token = generateDemodayVoteAuthorizationPort.generate(
            participant, poll.getId(), booth.getId(), now, now.plusSeconds(300));
        String content = objectMapper.writeValueAsString(new VoteBody(token));
        given(jwtTokenProvider.validateAccessToken(ACCESS_TOKEN)).willReturn(true);
        given(jwtTokenProvider.parseAccessToken(ACCESS_TOKEN)).willReturn(MEMBER_ID);
        given(jwtTokenProvider.getRolesFromAccessToken(ACCESS_TOKEN)).willReturn(List.of());

        // when
        List<MvcResult> results = race(() -> performVote(poll.getId(), content));

        // then
        assertThat(results).extracting(result -> result.getResponse().getStatus())
            .containsExactlyInAnyOrder(201, 409);

        MvcResult conflict = results.stream()
            .filter(result -> result.getResponse().getStatus() == 409)
            .findFirst()
            .orElseThrow();
        JsonNode conflictBody = objectMapper.readTree(conflict.getResponse().getContentAsString());
        assertThat(conflictBody.path("code").asText())
            .isEqualTo(DemodayErrorCode.DEMODAY_VOTE_ALREADY_CAST.getCode());
        assertThat(loadDemodayVotePort.listVotes(poll.getId())).hasSize(1);
    }

    private MvcResult performVote(Long pollId, String content) throws Exception {
        return mockMvc.perform(
            post("/api/v1/demoday/polls/{pollId}/votes", pollId)
                .header("Authorization", "Bearer " + ACCESS_TOKEN)
                .contentType(MediaType.APPLICATION_JSON)
                .content(content)
        ).andReturn();
    }

    private List<MvcResult> race(Callable<MvcResult> action) throws Exception {
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);
        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            Future<MvcResult> first = executor.submit(() -> runAfterSignal(action, ready, start));
            Future<MvcResult> second = executor.submit(() -> runAfterSignal(action, ready, start));
            assertThat(ready.await(5, TimeUnit.SECONDS)).isTrue();
            start.countDown();
            return List.of(first.get(10, TimeUnit.SECONDS), second.get(10, TimeUnit.SECONDS));
        } finally {
            start.countDown();
            executor.shutdownNow();
        }
    }

    private MvcResult runAfterSignal(
        Callable<MvcResult> action,
        CountDownLatch ready,
        CountDownLatch start
    ) throws Exception {
        ready.countDown();
        if (!start.await(5, TimeUnit.SECONDS)) {
            throw new IllegalStateException("demoday vote concurrency start timed out");
        }
        return action.call();
    }

    private record VoteBody(String voteAuthorizationToken) {
    }
}
