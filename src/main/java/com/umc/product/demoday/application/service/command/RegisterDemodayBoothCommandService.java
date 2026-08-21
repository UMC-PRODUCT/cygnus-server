package com.umc.product.demoday.application.service.command;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.umc.product.demoday.application.port.in.command.RegisterDemodayBoothUseCase;
import com.umc.product.demoday.application.port.in.command.dto.RegisterDemodayBoothBatchCommand;
import com.umc.product.demoday.application.port.in.command.dto.RegisterDemodayBoothCommand;
import com.umc.product.demoday.application.port.out.LoadDemodayPollPort;
import com.umc.product.demoday.application.port.out.SaveDemodayBoothPort;
import com.umc.product.demoday.application.service.DemodayAdminAccessChecker;
import com.umc.product.demoday.domain.DemodayBooth;
import com.umc.product.demoday.domain.DemodayPoll;
import com.umc.product.demoday.domain.exception.DemodayDomainException;
import com.umc.product.demoday.domain.exception.DemodayErrorCode;

import lombok.RequiredArgsConstructor;

@Service
@Transactional
@RequiredArgsConstructor
public class RegisterDemodayBoothCommandService implements RegisterDemodayBoothUseCase {

    private final DemodayAdminAccessChecker adminAccessChecker;
    private final LoadDemodayPollPort loadDemodayPollPort;
    private final SaveDemodayBoothPort saveDemodayBoothPort;

    @Override
    public Long register(RegisterDemodayBoothCommand command) {
        DemodayPoll poll = loadPoll(command.pollId());

        adminAccessChecker.validateAdminAccess(command.memberId(), poll.getGisuId());

        return saveDemodayBoothPort.save(
            createBooth(poll, command.boothCode(), command.projectId(), command.displayName())).getId();
    }

    /**
     * 일괄 경로는 시스템 관리자만 호출할 수 있다.
     *
     * <p>단건 경로와 달리 기수 총괄단은 통과시키지 않으므로, 투표를 조회하기 전에 권한부터 확인한다.
     * 권한이 없는 요청에 투표의 존재 여부를 알려 줄 이유가 없기 때문이다.
     */
    @Override
    public List<Long> registerAll(RegisterDemodayBoothBatchCommand command) {
        adminAccessChecker.validateSystemAdminAccess(command.memberId());

        DemodayPoll poll = loadPoll(command.pollId());

        List<DemodayBooth> booths = command.registrations().stream()
            .map(registration -> createBooth(
                poll,
                registration.boothCode(),
                registration.projectId(),
                registration.displayName()))
            .toList();

        return saveDemodayBoothPort.saveAll(booths).stream()
            .map(DemodayBooth::getId)
            .toList();
    }

    private DemodayPoll loadPoll(Long pollId) {
        return loadDemodayPollPort.findById(pollId)
            .orElseThrow(() -> new DemodayDomainException(DemodayErrorCode.DEMODAY_POLL_NOT_FOUND));
    }

    /**
     * 두 등록 경로 중 어느 쪽인지 고른다.
     *
     * <p>여기서 판단하는 것은 "요청이 어느 경로를 의도했는가"뿐이다. 부스가 프로젝트와 표시 이름을 동시에
     * 갖지 않는다는 규칙은 투표가 위임하는 두 팩토리가 각각 한쪽만 채우는 것으로 이미 보장되므로, 서비스가
     * 그 규칙을 다시 검사하지는 않는다. 다만 둘 다 담겨 온 요청은 어느 경로인지 정할 수 없어 거부한다.
     */
    private DemodayBooth createBooth(
        DemodayPoll poll,
        Integer boothCode,
        Long projectId,
        String displayName
    ) {
        boolean hasProject = projectId != null;
        boolean hasDisplayName = displayName != null && !displayName.isBlank();

        if (hasProject == hasDisplayName) {
            throw new DemodayDomainException(DemodayErrorCode.DEMODAY_BOOTH_INVALID_IDENTIFIER);
        }

        return hasProject
            ? poll.registerProjectBooth(boothCode, projectId)
            : poll.registerExternalBooth(boothCode, displayName);
    }
}
