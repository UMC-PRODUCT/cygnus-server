package com.umc.product.demoday.application.service.query;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.umc.product.demoday.application.port.in.query.GetDemodayVoteQrUseCase;
import com.umc.product.demoday.application.port.in.query.dto.DemodayVoteQrInfo;
import com.umc.product.demoday.application.port.out.GenerateDemodayVoteQrCredentialPort;
import com.umc.product.demoday.application.port.out.LoadDemodayPollPort;
import com.umc.product.demoday.application.service.DemodayAdminAccessChecker;
import com.umc.product.demoday.config.DemodayQrProperties;
import com.umc.product.demoday.domain.DemodayPoll;
import com.umc.product.demoday.domain.exception.DemodayDomainException;
import com.umc.product.demoday.domain.exception.DemodayErrorCode;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * INFO QR credential은 요청마다 새로 만들지 않는다. 현재 시각이 속한 QR_DURATION_TIME 구간의 시작·끝을 계산해
 * {@link GenerateDemodayVoteQrCredentialPort}에 넘기면, 같은 구간에서는 항상 같은 문자열이 결정적으로
 * 나온다(ADR-023). 그래서 이 클래스는 credential을 "생성"하지 않고 "그 구간의 값을 조회"한다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DemodayVoteQrQueryService implements GetDemodayVoteQrUseCase {

    public static final int QR_DURATION_TIME = 1;
    private static final Duration WINDOW = Duration.ofHours(QR_DURATION_TIME);

    private final LoadDemodayPollPort loadDemodayPollPort;
    private final DemodayAdminAccessChecker adminAccessChecker;
    private final GenerateDemodayVoteQrCredentialPort generateDemodayVoteQrCredentialPort;
    private final DemodayQrProperties demodayQrProperties;
    private final Clock clock;

    @Override
    public DemodayVoteQrInfo get(Long pollId, Long memberId) {
        DemodayPoll poll = loadDemodayPollPort.findById(pollId)
            .orElseThrow(() -> new DemodayDomainException(DemodayErrorCode.DEMODAY_POLL_NOT_FOUND));

        adminAccessChecker.validateAdminAccess(memberId, poll.getGisuId());

        Instant now = clock.instant();
        poll.validVoteQrAvailable(now);

        Instant windowStart = now.truncatedTo(ChronoUnit.HOURS);
        Instant windowEnd = windowStart.plus(WINDOW);

        String token = generateDemodayVoteQrCredentialPort.generate(pollId, windowStart, windowEnd);
        String qrValue = buildQrValue(pollId, token);

        log.info("demoday vote-qr generated pollId={} windowStart={} windowEnd={}", pollId, windowStart, windowEnd);

        return new DemodayVoteQrInfo(pollId, qrValue, windowStart, windowEnd);
    }

    private String buildQrValue(Long pollId, String token) {
        return "%s/demoday/polls/%d/vote-authorization#token=%s"
            .formatted(demodayQrProperties.baseUrl(), pollId, token);
    }
}
