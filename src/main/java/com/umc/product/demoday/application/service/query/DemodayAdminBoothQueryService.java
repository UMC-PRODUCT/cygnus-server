package com.umc.product.demoday.application.service.query;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.umc.product.demoday.application.port.in.query.ListDemodayAdminBoothUseCase;
import com.umc.product.demoday.application.port.in.query.dto.DemodayAdminBoothListInfo;
import com.umc.product.demoday.application.port.in.query.dto.DemodayBoothInfo;
import com.umc.product.demoday.application.port.out.LoadDemodayBoothPort;
import com.umc.product.demoday.application.port.out.LoadDemodayPollPort;
import com.umc.product.demoday.application.service.DemodayAdminAccessChecker;
import com.umc.product.demoday.domain.DemodayPoll;
import com.umc.product.demoday.domain.exception.DemodayDomainException;
import com.umc.product.demoday.domain.exception.DemodayErrorCode;

import lombok.RequiredArgsConstructor;

/**
 * 운영자용 부스 조회를 참여자용 조회({@link DemodayPollQueryService})와 분리한다.
 *
 * <p>두 조회는 같은 부스를 읽지만 접근 주체가 다르다. 참여자용은 인증 없이 열려 있고 운영자용은 해당 기수
 * 총괄단만 볼 수 있으므로, 한 서비스에 합치면 권한 검사가 있는 메서드와 없는 메서드가 섞인다.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DemodayAdminBoothQueryService implements ListDemodayAdminBoothUseCase {

    private final DemodayAdminAccessChecker adminAccessChecker;
    private final LoadDemodayPollPort loadDemodayPollPort;
    private final LoadDemodayBoothPort loadDemodayBoothPort;

    @Override
    public DemodayAdminBoothListInfo listBooths(Long pollId, Long memberId) {
        DemodayPoll poll = loadDemodayPollPort.findById(pollId)
            .orElseThrow(() -> new DemodayDomainException(DemodayErrorCode.DEMODAY_POLL_NOT_FOUND));

        adminAccessChecker.validateAdminAccess(memberId, poll.getGisuId());

        List<DemodayBoothInfo> booths = loadDemodayBoothPort.listByPollId(pollId)
            .stream()
            .map(booth -> new DemodayBoothInfo(
                booth.getId(),
                booth.getBoothCode(),
                booth.getProjectId(),
                booth.getDisplayName()
            ))
            .toList();

        return new DemodayAdminBoothListInfo(
            poll.getId(),
            poll.getStatus(),
            poll.isBoothRegistrable(),
            booths.size(),
            booths
        );
    }
}
