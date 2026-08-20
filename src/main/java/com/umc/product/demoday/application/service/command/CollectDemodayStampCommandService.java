package com.umc.product.demoday.application.service.command;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.umc.product.demoday.application.port.in.command.CollectDemodayStampUseCase;
import com.umc.product.demoday.application.port.in.command.dto.CollectDemodayStampCommand;
import com.umc.product.demoday.application.port.in.command.dto.DemodayStampCollectInfo;
import com.umc.product.demoday.application.port.in.query.dto.DemodayStampInfo;
import com.umc.product.demoday.application.port.in.query.participant.DemodayParticipant;
import com.umc.product.demoday.application.port.out.HashDemodayStampCredentialPort;
import com.umc.product.demoday.application.port.out.LoadDemodayBoothPort;
import com.umc.product.demoday.application.port.out.LoadDemodayEntryCodePort;
import com.umc.product.demoday.application.port.out.LoadDemodayPollPort;
import com.umc.product.demoday.application.port.out.LoadDemodayStampPort;
import com.umc.product.demoday.application.port.out.LoadDemodayVotePort;
import com.umc.product.demoday.application.port.out.SaveDemodayStampPort;
import com.umc.product.demoday.domain.DemodayBooth;
import com.umc.product.demoday.domain.DemodayEntryCode;
import com.umc.product.demoday.domain.DemodayPoll;
import com.umc.product.demoday.domain.DemodayStamp;
import com.umc.product.demoday.domain.exception.DemodayDomainException;
import com.umc.product.demoday.domain.exception.DemodayErrorCode;
import com.umc.product.demoday.domain.policy.DemodayParticipationPolicy;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * QR 스캔은 제출이 아니라 카메라를 갖다 대는 동작이라 같은 요청이 정상적으로 반복된다.
 * 그래서 같은 부스 재스캔은 새 스탬프를 만들지 않고 항상 현재 상태를 {@code 201}로 돌려준다(멱등 보장).
 * 재스캔 판정은 저장 전 사전 조회로 하지만, 사전 조회와 저장 사이의 경합까지는 막지 못하므로 DB unique 제약을 최종 방어선으로
 * 함께 쓴다({@link DemodayStampPersistenceAdapter}가 이미 이 위반을 {@code DEMODAY_STAMP_ALREADY_COLLECTED}로 변환해준다).
 * 재스캔 경로는 최대 개수·쿨다운 체크를 타지 않는다.
 *
 * <p>{@code DemodayPoll}의 {@code OPEN} 상태는 행사 전체(부스 스탬프 수집 포함)의 시작을 의미한다.
 * {@code READY} 상태에서는 아직 참여자 활동이 시작되지 않았으므로, 부스가 등록돼 있고 credential이
 * 유효하더라도 poll이 열리기 전에는 스탬프를 적립할 수 없다.
 */
@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class CollectDemodayStampCommandService implements CollectDemodayStampUseCase {

    private static final Duration STAMP_COOLDOWN = Duration.ofMinutes(5);

    private final LoadDemodayBoothPort loadDemodayBoothPort;
    private final LoadDemodayEntryCodePort loadDemodayEntryCodePort;
    private final LoadDemodayPollPort loadDemodayPollPort;
    private final LoadDemodayStampPort loadDemodayStampPort;
    private final LoadDemodayVotePort loadDemodayVotePort;
    private final SaveDemodayStampPort saveDemodayStampPort;
    private final HashDemodayStampCredentialPort hashDemodayStampCredentialPort;
    private final Clock clock;

    @Override
    public DemodayStampCollectInfo collect(CollectDemodayStampCommand command) {
        DemodayParticipant participant = command.participant();
        DemodayBooth booth = loadBoothByCredential(command.qrCredential());
        validateSamePoll(command.pollId(), booth);
        validatePollOpen(command.pollId());

        Optional<DemodayStamp> existingStamp = findExistingStamp(participant, booth.getId());
        if (existingStamp.isPresent()) {
            log.info(
                "demoday stamp rescanned participantType={} participantId={} boothId={}",
                participant.participantType(), participant.participantId(), booth.getId()
            );
            return buildResult(command.pollId(), participant, existingStamp.get());
        }

        DemodayStamp collected = collectNewStamp(command.pollId(), participant, booth);
        return buildResult(command.pollId(), participant, collected);
    }

    private DemodayBooth loadBoothByCredential(String qrCredential) {
        String credentialHash = hashDemodayStampCredentialPort.hash(qrCredential);
        return loadDemodayBoothPort.findByStampCredentialHash(credentialHash)
            .orElseThrow(() -> new DemodayDomainException(DemodayErrorCode.DEMODAY_STAMP_CREDENTIAL_INVALID));
    }

    private void validateSamePoll(Long pollId, DemodayBooth booth) {
        if (!booth.getPollId().equals(pollId)) {
            throw new DemodayDomainException(DemodayErrorCode.DEMODAY_STAMP_POLL_MISMATCH);
        }
    }

    private void validatePollOpen(Long pollId) {
        DemodayPoll poll = loadDemodayPollPort.findById(pollId)
            .orElseThrow(() -> new DemodayDomainException(DemodayErrorCode.DEMODAY_POLL_NOT_FOUND));
        poll.validParticipationAvailable(clock.instant());
    }

    private DemodayStamp collectNewStamp(Long pollId, DemodayParticipant participant, DemodayBooth booth) {
        validateNotMaxed(pollId, participant);
        validateCooldownElapsed(participant);

        DemodayStamp stamp = createStamp(participant, booth);

        try {
            return saveDemodayStampPort.save(stamp);
        } catch (DemodayDomainException exception) {
            if (exception.getBaseCode() != DemodayErrorCode.DEMODAY_STAMP_ALREADY_COLLECTED) {
                throw exception;
            }
            log.info(
                "demoday stamp collect lost the race, returning current state "
                    + "participantType={} participantId={} boothId={}",
                participant.participantType(), participant.participantId(), booth.getId()
            );

            return findExistingStamp(participant, booth.getId()).orElseThrow(() -> exception);
        }
    }

    private void validateNotMaxed(Long pollId, DemodayParticipant participant) {
        if (DemodayParticipationPolicy.hasRequiredStamps(countActiveStamps(pollId, participant))) {
            throw new DemodayDomainException(DemodayErrorCode.DEMODAY_STAMP_MAX_COUNT_REACHED);
        }
    }

    private void validateCooldownElapsed(DemodayParticipant participant) {
        findLatestActiveStamp(participant).ifPresent(latestStamp -> {
            Instant nextAvailableAt = latestStamp.getCreatedAt().plus(STAMP_COOLDOWN);
            if (clock.instant().isBefore(nextAvailableAt)) {
                throw new DemodayDomainException(DemodayErrorCode.DEMODAY_STAMP_COOLDOWN_ACTIVE);
            }
        });
    }

    private DemodayStamp createStamp(DemodayParticipant participant, DemodayBooth booth) {
        return switch (participant.participantType()) {
            case MEMBER -> DemodayStamp.forMember(participant.participantId(), booth);
            case GUEST -> DemodayStamp.forVisitor(loadEntryCode(participant.participantId()), booth);
        };
    }

    private DemodayEntryCode loadEntryCode(Long entryCodeId) {
        return loadDemodayEntryCodePort.findById(entryCodeId)
            .orElseThrow(() -> new DemodayDomainException(DemodayErrorCode.DEMODAY_ENTRY_CODE_NOT_FOUND));
    }

    private Optional<DemodayStamp> findExistingStamp(DemodayParticipant participant, Long boothId) {
        return switch (participant.participantType()) {
            case MEMBER -> loadDemodayStampPort.findMemberStamp(participant.participantId(), boothId);
            case GUEST -> loadDemodayStampPort.findVisitorStamp(participant.participantId(), boothId);
        };
    }

    private int countActiveStamps(Long pollId, DemodayParticipant participant) {
        return switch (participant.participantType()) {
            case MEMBER -> loadDemodayStampPort.countActiveMemberStamps(pollId, participant.participantId());
            case GUEST -> loadDemodayStampPort.countActiveVisitorStamps(pollId, participant.participantId());
        };
    }

    private Optional<DemodayStamp> findLatestActiveStamp(DemodayParticipant participant) {
        return switch (participant.participantType()) {
            case MEMBER -> loadDemodayStampPort.findLatestActiveMemberStamp(participant.participantId());
            case GUEST -> loadDemodayStampPort.findLatestActiveVisitorStamp(participant.participantId());
        };
    }

    private DemodayStampCollectInfo buildResult(
        Long pollId,
        DemodayParticipant participant,
        DemodayStamp stamp
    ) {
        int stampCount = countActiveStamps(pollId, participant);
        boolean maxed = DemodayParticipationPolicy.hasRequiredStamps(stampCount);
        Instant nextStampAvailableAt = maxed
            ? null
            : findLatestActiveStamp(participant).map(
                latest -> latest.getCreatedAt().plus(STAMP_COOLDOWN)).orElse(null);

        return new DemodayStampCollectInfo(
            new DemodayStampInfo(stamp.getBoothId(), stamp.getCreatedAt()),
            stampCount,
            DemodayParticipationPolicy.requiredStampCount(),
            nextStampAvailableAt,
            DemodayParticipationPolicy.canRequestVoteAuthorization(
                stampCount,
                hasUsedVoteSlot(pollId, participant)
            )
        );
    }

    private boolean hasUsedVoteSlot(Long pollId, DemodayParticipant participant) {
        return switch (participant.participantType()) {
            case MEMBER -> loadDemodayVotePort.findMemberVote(pollId, participant.participantId()).isPresent();
            case GUEST -> loadDemodayVotePort.findVisitorVote(participant.participantId()).isPresent();
        };
    }
}
