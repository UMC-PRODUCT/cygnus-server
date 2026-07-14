package com.umc.product.project.application.port.in.query;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import com.umc.product.project.application.port.in.query.dto.ProjectMatchingRoundInfo;

public interface GetProjectMatchingRoundUseCase {

    List<ProjectMatchingRoundInfo> list(Long requesterMemberId, Long gisuId, Long chapterId, Instant time);

    /**
     * 여러 ID 의 매칭 차수를 한 번에 Info 로 조회합니다. Assembler 의 N+1 방지용 batch 진입점.
     *
     * @param ids 매칭 차수 ID 집합
     * @return matchingRoundId -> ProjectMatchingRoundInfo 매핑. 누락된 ID 는 결과에서 빠집니다.
     */
    Map<Long, ProjectMatchingRoundInfo> findAllByIds(Collection<Long> ids);
}
